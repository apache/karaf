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
package org.apache.felix.karaf.shell.dev;

import java.io.File;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.karaf.shell.dev.framework.Equinox;
import org.apache.felix.karaf.shell.dev.framework.Felix;
import org.apache.felix.karaf.shell.dev.framework.Framework;

/**
 * Command for enabling/disabling debug logging on the OSGi framework
 */
@Command(scope = "dev", name = "framework",
         description = "Enable/disable debugging for the OSGi Framework")
public class FrameworkDebug extends OsgiCommandSupport {

    private static final String KARAF_BASE = System.getProperty("karaf.base");

    @Option(name = "-debug", aliases={"--enable-debug"}, description="Enable debug for the OSGi framework", required = false, multiValued = false)
    boolean debug;

    @Option(name = "-nodebug", aliases={"--disable-debug"}, description="Disable debug for the OSGi framework", required = false, multiValued = false)
    boolean nodebug;

    @Override
    protected Object doExecute() throws Exception {
        Framework framework = getFramework();

        if (!debug^nodebug) {
            System.err.printf("Required option missing: use -debug or -nodebug%n");
            return null;
        }
        if (debug) {
            System.out.printf("Enabling debug for OSGi framework (%s)%n", framework.getName());
            framework.enableDebug(new File(KARAF_BASE));
        }
        if (nodebug) {
            System.out.printf("Disabling debug for OSGi framework (%s)%n", framework.getName());
            framework.disableDebug(new File(KARAF_BASE));
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public Framework getFramework() {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return new Felix(new File(KARAF_BASE));
        } else {
            return new Equinox(new File(KARAF_BASE));
        }
    }
}
