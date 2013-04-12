/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.help.impl;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.karaf.shell.console.HelpProvider;
import org.apache.karaf.shell.console.NameScoping;
import org.apache.karaf.shell.util.IndentFormatter;
import org.apache.karaf.util.InterpolationHelper;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class HelpSystem implements HelpProvider {

    private BundleContext context;
    Hashtable<String, GogoCommandHelper> helpers = new Hashtable<String, GogoCommandHelper>();

    public HelpSystem(BundleContext context) {
        this.context = context;
        try {
            ServiceTracker commandTracker = trackOSGiCommands(context);
            commandTracker.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized List<HelpProvider> getProviders() {
        ServiceReference<HelpProvider> [] refs = null;
        try {
            refs = context.getServiceReferences(HelpProvider.class, null).toArray(new ServiceReference[]{});
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        Arrays.sort(refs);
        List<HelpProvider> providers = new ArrayList<HelpProvider>();
        for (int i = refs.length - 1; i >= 0; i--) {
            providers.add(context.getService(refs[i]));
        }
        return providers;
    }
    
    public String getHelp(final CommandSession session, String path) {
        session.put(GOGO_COMMAND_HELPERS, helpers);

        if (path == null) {
            path = "%root%";
        }
        Map<String,String> props = new HashMap<String,String>();
        props.put("data", "${" + path + "}");
        final List<HelpProvider> providers = getProviders();
        InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
            public String getValue(final String key) {
                for (HelpProvider hp : providers) {
                    String result = hp.getHelp(session, key);
                    if (result != null) {
                        return removeNewLine(result);
                    }
                }
                return null;
            }
        });
        return props.get("data");
    }
    
    private String removeNewLine(String help) {
        if (help != null && help.endsWith("\n")) {
            help = help.substring(0, help.length()  -1);
        }
        return help;
    }

    public static String GOGO_COMMAND_HELPERS = "gogocommand_helpers";

    private ServiceTracker trackOSGiCommands(final BundleContext context) throws InvalidSyntaxException {
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))", CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));

        return new ServiceTracker(context, filter, null) {

            @Override
            public Object addingService(ServiceReference reference) {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
                List<Object> commands = new ArrayList<Object>();

                Object commandObject = context.getService(reference);

                if (scope != null && function != null) {
                    if (function.getClass().isArray()) {
                        for (Object f : ((Object[]) function)) {
                            GogoCommandHelper gogoCommandHelper = new GogoCommandHelper(commandObject, (String) scope, f.toString());
                            helpers.put(scope + ":" + f.toString(), gogoCommandHelper);
                        }
                    } else {
                        GogoCommandHelper gogoCommandHelper = new GogoCommandHelper(commandObject, (String) scope, function.toString());
                        helpers.put(scope + ":" + function.toString(), gogoCommandHelper);
                    }
                    return commands;
                }
                return null;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                super.removedService(reference, service);
            }
        };
    }

    class GogoCommandHelper {

        private Object commandObject;
        private String scope;
        private String function;
        private String description = "";

        public GogoCommandHelper(Object commandObject, String scope, String function) {
            this.commandObject = commandObject;
            this.scope = scope;
            this.function = function;

            for (Method m : commandObject.getClass().getMethods()) {
                if (m.getName().equals(function)) {
                    Descriptor descriptor = m.getAnnotation(Descriptor.class);
                    if (descriptor != null) {
                        description = descriptor.value();
                    }
                }
            }
        }

        public String getDescription() {
            return description;
        }

        public void printUsage(CommandSession session, PrintStream out) {
            Terminal term = session != null ? (Terminal) session.get(".jline.terminal") : null;
            int termWidth = term != null ? term.getWidth() : 80;
            boolean globalScope = NameScoping.isGlobalScope(session, scope);

            Hashtable<String, String> arguments = new Hashtable<String, String>();
            for (Method m : commandObject.getClass().getMethods()) {
                if (m.getName().equals(function)) {
                    Annotation[][] annotations = m.getParameterAnnotations();
                    int i = 0;
                    for (Class<?> paramClass : m.getParameterTypes()) {
                        String argumentDescription = "";
                        for (Annotation annotation : annotations[i++]) {
                            if (annotation.annotationType().equals(Descriptor.class)) {
                                argumentDescription = ((Descriptor) annotation).value();
                                break;
                            }
                        }
                        arguments.put(paramClass.getSimpleName(), argumentDescription);
                    }
                }
            }
            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("DESCRIPTION").a(Ansi.Attribute.RESET));
            out.print("        ");
            if (globalScope) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(function).a(Ansi.Attribute.RESET));
            } else {
                out.println(Ansi.ansi().a(scope).a(":").a(Ansi.Attribute.INTENSITY_BOLD).a(function).a(Ansi.Attribute.RESET));
            }
            out.println();
            out.print("\t");
            out.println(getDescription());
            out.println();

            if (arguments.size() > 0) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("ARGUMENTS").a(Ansi.Attribute.RESET));
                for (String argumentName : arguments.keySet()) {
                    out.print("        ");
                    out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(argumentName).a(Ansi.Attribute.RESET));
                    IndentFormatter.printFormatted("                ", arguments.get(argumentName), termWidth, out);
                }
                out.println();
            }
        }

    }

}
