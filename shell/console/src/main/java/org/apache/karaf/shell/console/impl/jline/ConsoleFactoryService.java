/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console.impl.jline;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.factory.ConsoleFactory;
import org.apache.karaf.shell.util.ShellUtil;
import org.osgi.framework.BundleContext;

public class ConsoleFactoryService implements ConsoleFactory {

    private static final Class<?>[] SECURITY_BUGFIX = {
            JaasHelper.class,
            JaasHelper.OsgiSubjectDomainCombiner.class,
            JaasHelper.DelegatingProtectionDomain.class,
    };

    private final BundleContext bundleContext;

    private CommandProcessor processor;

    private ThreadIO threadIO;

    public ConsoleFactoryService(BundleContext bc, CommandProcessor processor, ThreadIO threadIO) {
        bundleContext = bc;
        this.processor = processor;
        this.threadIO = threadIO;
    }
    
    @Override
    public Console create(InputStream in, PrintStream out, PrintStream err, final Terminal terminal,
            String encoding, Runnable closeCallback) {
        ConsoleImpl console = new ConsoleImpl(processor, threadIO, in, out, err, terminal, encoding, closeCallback, bundleContext, true);
        CommandSession session = console.getSession();
        
        session.put("USER", ShellUtil.getCurrentUserName());
        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
        session.put("#LINES", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getHeight());
            }
        });
        session.put("#COLUMNS", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getWidth());
            }
        });
        session.put(".jline.terminal", terminal);
        addSystemProperties(session);
        session.put("pid", getPid());
        return console;
    }

    private String getPid() {
    	String name = ManagementFactory.getRuntimeMXBean().getName();
    	String[] parts = name.split("@");
		return parts[0];
	}

	private void addSystemProperties(CommandSession session) {
        Properties sysProps = System.getProperties();
        Iterator<Object> it = sysProps.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            session.put(key, System.getProperty(key));
        }
    }

}
