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
package org.apache.servicemix.kernel.gshell.core;

import junit.framework.TestCase;
import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.shell.Shell;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test extends TestCase {

    public void test() throws Exception {
        System.setProperty("startLocalConsole", "true");
        System.setProperty("servicemix.name", "root");

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(
                    new String[] { "META-INF/spring/gshell.xml",
                                   "META-INF/spring/gshell-vfs.xml",
                                   "META-INF/spring/gshell-commands.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test.xml" });
            ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
            assertNotNull(appMgr);
            Shell shell = appMgr.create();
            assertNotNull(shell);
            shell.execute("help");
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    public void testBanner() throws Exception {
        System.setProperty("startLocalConsole", "true");
        System.setProperty("servicemix.name", "root");

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(
                    new String[] { "META-INF/spring/gshell.xml",
                                   "META-INF/spring/gshell-vfs.xml",
                                   "META-INF/spring/gshell-commands.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test.xml"});
            ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
            assertNotNull(appMgr);
            Shell shell = appMgr.create();
            ServiceMixBranding smxBrandng = (ServiceMixBranding)appMgr.getApplication().getModel().getBranding();
            assertNotNull(smxBrandng.getWelcomeMessage());
            System.out.println(smxBrandng.getWelcomeMessage());
            assertNotNull(shell);
            shell.execute("about");
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    public void testLs() throws Exception {
        System.setProperty("startLocalConsole", "true");
        System.setProperty("servicemix.name", "root");

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(
                    new String[] { "META-INF/spring/gshell.xml",
                                   "META-INF/spring/gshell-vfs.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test-commands.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test.xml"});
            ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
            assertNotNull(appMgr);
            Shell shell = appMgr.create();
            ServiceMixBranding smxBrandng = (ServiceMixBranding)appMgr.getApplication().getModel().getBranding();
            assertNotNull(smxBrandng.getWelcomeMessage());
            System.out.println(smxBrandng.getWelcomeMessage());
            assertNotNull(shell);
            shell.execute("vfs/ls meta:/commands/");
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    public void testCommandGroups() throws Exception {
        System.setProperty("startLocalConsole", "true");
        System.setProperty("servicemix.name", "root");

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(
                    new String[] { "META-INF/spring/gshell.xml",
                                   "META-INF/spring/gshell-vfs.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test-commands.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test.xml"});
            ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
            assertNotNull(appMgr);
            Shell shell = appMgr.create();
            ServiceMixBranding smxBrandng = (ServiceMixBranding)appMgr.getApplication().getModel().getBranding();
            assertNotNull(smxBrandng.getWelcomeMessage());
            System.out.println(smxBrandng.getWelcomeMessage());
            assertNotNull(shell);

            shell.execute("vfs");
            shell.execute("help");
            shell.execute("..");
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    public void testFileAccessCommands() throws Exception {
        System.setProperty("startLocalConsole", "true");
        System.setProperty("servicemix.name", "root");

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(
                    new String[] { "META-INF/spring/gshell.xml",
                                   "META-INF/spring/gshell-vfs.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test-commands.xml",
                                   "org/apache/servicemix/kernel/gshell/core/gshell-test.xml"});
            ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
            assertNotNull(appMgr);
            Shell shell = appMgr.create();            
            assertNotNull(shell);
            shell.execute("optional/cat src/test/resources/org/apache/servicemix/kernel/gshell/core/gshell-test.xml");
            shell.execute("optional/find src/test/resources/org/apache/servicemix/kernel/gshell/core/gshell-test.xml");
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

}
