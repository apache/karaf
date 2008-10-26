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
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;

public class Test extends TestCase {

    public void test() throws Exception {

        System.setProperty("startLocalConsole", "true");

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "META-INF/spring/gshell.xml",
                               "META-INF/spring/gshell-vfs.xml",
                               "META-INF/spring/gshell-commands.xml",
                               "org/apache/servicemix/kernel/gshell/core/gshell-test.xml" });
        ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
        assertNotNull(appMgr);
        Shell shell = appMgr.create();
        assertNotNull(shell);
        shell.execute("help");
    }
}
