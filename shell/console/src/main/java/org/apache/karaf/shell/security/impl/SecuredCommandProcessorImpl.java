/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.security.impl;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.gogo.runtime.activator.Activator;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import javax.security.auth.Subject;

import java.io.InputStream;
import java.io.PrintStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

/**
 * Note: this CommandProcessor can only be used to create a single session, as closing the
 * session will also close the command processor.
 */
public class SecuredCommandProcessorImpl extends CommandProcessorImpl {

    private final BundleContext bundleContext;
    private final ServiceTracker<Object, Map<String, CommandProxy>> commandTracker;
    private final ServiceTracker<Converter, Converter> converterTracker;
    private final ServiceTracker<CommandSessionListener, CommandSessionListener> listenerTracker;

    public SecuredCommandProcessorImpl(BundleContext bc, ThreadIO io) {
        super(io);
        bundleContext = bc;

        String roleClause = "";

        AccessControlContext acc = AccessController.getContext();
        Subject sub = Subject.getSubject(acc);
        if (sub != null) {
            Set<RolePrincipal> rolePrincipals = sub.getPrincipals(RolePrincipal.class);
            if (rolePrincipals.size() == 0)
                throw new SecurityException("Current user has no associated roles.");

            // TODO cater for custom roles
            StringBuilder sb = new StringBuilder();
            sb.append("(|");
            for (RolePrincipal rp : rolePrincipals) {
                sb.append('(');
                sb.append("org.apache.karaf.service.guard.roles");
                sb.append('=');
                sb.append(escapeforFilterString(rp.getName()));
                sb.append(')');
            }
            sb.append("(!(org.apache.karaf.service.guard.roles=*))"); // Or no roles specified at all
            sb.append(')');
            roleClause = sb.toString();
        }

        addConstant(Activator.CONTEXT, bc);
        addCommand("osgi", this, "addCommand");
        addCommand("osgi", this, "removeCommand");
        addCommand("osgi", this, "eval");

        try {
            // The role clause is used to only display commands that the current user can invoke.
            commandTracker = trackCommands(bc, roleClause);
            commandTracker.open();

            converterTracker = trackConverters(bc);
            converterTracker.open();
            listenerTracker = trackListeners(bc);
            listenerTracker.open();
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        commandTracker.close();
        converterTracker.close();
        listenerTracker.close();
    }

    public CommandSession createSession(InputStream in, PrintStream out, PrintStream err) {
        synchronized (sessions) {
            if (stopped) {
                throw new IllegalStateException("CommandProcessor has been stopped");
            }
            CommandSessionImpl session = new CommandSessionImpl(this, in, out, err) {
                @Override
                public void close() {
                    super.close();
                    SecuredCommandProcessorImpl.this.close();
                }
            };
            sessions.put(session, null);
            return session;
        }
    }

    private ServiceTracker<Object, Map<String, CommandProxy>> trackCommands(final BundleContext context, String roleClause) throws InvalidSyntaxException {
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*)%s)",
                CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION, roleClause));

        return new ServiceTracker<Object, Map<String, CommandProxy>>(context, filter, null) {

            @Override
            public Map<String, CommandProxy> addingService(ServiceReference<Object> reference)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
                Object ranking = reference.getProperty(Constants.SERVICE_RANKING);

                int rank = 0;
                if (ranking != null)
                {
                    try
                    {
                        rank = Integer.parseInt(ranking.toString());
                    }
                    catch (NumberFormatException e)
                    {
                        // Ignore
                    }
                }
                if (scope != null && function != null)
                {
                    Map<String, CommandProxy> proxyMap = new HashMap<String, CommandProxy>();
                    if (function.getClass().isArray())
                    {
                        for (Object f : ((Object[]) function))
                        {
                            String func = f.toString();
                            CommandProxy target = new CommandProxy(context, reference, func);
                            if (!proxyMap.containsKey(func)) {
                                proxyMap.put(func, target);
                                addCommand(scope.toString(), target, func, rank);
                            }
                        }
                    }
                    else
                    {
                        String func = function.toString();
                        CommandProxy target = new CommandProxy(context, reference, func);
                        proxyMap.put(func, target);
                        addCommand(scope.toString(), target, func, rank);
                    }
                    return proxyMap;
                }
                return null;
            }

            @Override
            public void removedService(ServiceReference<Object> reference, Map<String, CommandProxy> proxyMap)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);

                for (Map.Entry<String, CommandProxy> entry : proxyMap.entrySet())
                {
                    removeCommand(scope.toString(), entry.getKey(), entry.getValue());
                }

                context.ungetService(reference);
            }
        };
    }

    private ServiceTracker<Converter, Converter> trackConverters(BundleContext context) {
        return new ServiceTracker<Converter, Converter>(context, Converter.class.getName(), null) {
            @Override
            public Converter addingService(ServiceReference<Converter> reference) {
                Converter converter = super.addingService(reference);
                addConverter(converter);
                return converter;
            }

            @Override
            public void removedService(ServiceReference<Converter> reference, Converter service) {
                removeConverter(service);
                super.removedService(reference, service);
            }
        };
    }

    private ServiceTracker<CommandSessionListener, CommandSessionListener> trackListeners(BundleContext context) {
        return new ServiceTracker<CommandSessionListener, CommandSessionListener>(context, CommandSessionListener.class.getName(), null) {
            @Override
            public CommandSessionListener addingService(ServiceReference<CommandSessionListener> reference) {
                CommandSessionListener listener = super.addingService(reference);
                addListener(listener);
                return listener;
            }

            @Override
            public void removedService(ServiceReference<CommandSessionListener> reference, CommandSessionListener service) {
                removeListener(service);
                super.removedService(reference, service);
            }
        };
    }

    private String escapeforFilterString(String original) {
        //the filter string follow the LDAP rule
        //where we need escape the special char
        String ret = original;
        ret = ret.replace("\\", "\\\\");
        ret = ret.replace("*", "\\*");
        ret = ret.replace("(", "\\(");
        ret = ret.replace(")", "\\)");
        return ret;
    }
}
