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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.HelpProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class SingleCommandHelpProvider implements HelpProvider {

    private ThreadIO io;
    
    public SingleCommandHelpProvider(ThreadIO io) {
        this.io = io;
    }

    public String getHelp(CommandSession session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("command|")) {
                path = path.substring("command|".length());
            } else {
                return null;
            }
        }
        @SuppressWarnings("unchecked")
        Set<String> names = (Set<String>) session.get(CommandSessionImpl.COMMANDS);
        if (path != null && names.contains(path)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            io.setStreams(new ByteArrayInputStream(new byte[0]), new PrintStream(baos, true), new PrintStream(baos, true));
            try {
                Function function = (Function) session.get(path);
                function = unproxy(function);
                if (function != null && function instanceof CommandProxy) {
                    Hashtable<String, HelpSystem.GogoCommandHelper> helpers = (Hashtable<String, HelpSystem.GogoCommandHelper>) session.get(HelpSystem.GOGO_COMMAND_HELPERS);
                    if (helpers != null) {
                        HelpSystem.GogoCommandHelper gogoCommandHelper = helpers.get(path);
                        if (gogoCommandHelper != null) {
                            gogoCommandHelper.printUsage(session, session.getConsole());
                        }
                    }
                } else {
                    session.execute(path + " --help");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                io.close();
            }
            return baos.toString();
        }
        return null;
    }

    protected Function unproxy(Function function) {
        try {
            if (function.getClass().getName().contains("CommandProxy")) {
                Field contextField = function.getClass().getDeclaredField("context");
                Field referenceField = function.getClass().getDeclaredField("reference");
                contextField.setAccessible(true);
                referenceField.setAccessible(true);
                BundleContext context = (BundleContext) contextField.get(function);
                ServiceReference reference = (ServiceReference) referenceField.get(function);
                Object target = context.getService(reference);
                try {
                    if (target instanceof Function) {
                        function = (Function) target;
                    }
                } finally {
                    context.ungetService(reference);
                }
            }
        } catch (Throwable t) {
            // nothing to do
        }
        return function;
    }

}
