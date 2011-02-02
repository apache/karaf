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
package org.apache.karaf.shell.dev;

import java.io.File;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.dev.framework.Equinox;
import org.apache.karaf.shell.dev.framework.Felix;
import org.apache.karaf.shell.dev.framework.Framework;
import org.apache.karaf.util.Properties;

/**
 * Command for enabling/disabling debug logging on the OSGi framework
 */
@Command(scope = "dev", name = "framework", description = "OSGi Framework options.")
public class FrameworkOptions extends OsgiCommandSupport {

    private static final String KARAF_BASE = System.getProperty("karaf.base");

    @Option(name = "-debug", aliases={"--enable-debug"}, description="Enable debug for the OSGi framework", required = false, multiValued = false)
    boolean debug;

    @Option(name = "-nodebug", aliases={"--disable-debug"}, description="Disable debug for the OSGi framework", required = false, multiValued = false)
    boolean nodebug;

    @Argument(name = "framework", required = false, description = "Name of the OSGi framework to use")
    String framework;

    @Override
    protected Object doExecute() throws Exception {

        if (!debug^nodebug && framework == null) {
            System.out.printf("Current OSGi framework is %s%n", getFramework().getName());
            return null;
        }
        if (debug) {
            Framework frwk = getFramework();
            System.out.printf("Enabling debug for OSGi framework (%s)%n", frwk.getName());
            frwk.enableDebug(new File(KARAF_BASE));
        }
        if (nodebug) {
            Framework frwk = getFramework();
            System.out.printf("Disabling debug for OSGi framework (%s)%n", frwk.getName());
            frwk.disableDebug(new File(KARAF_BASE));
        }
        if (framework != null) {
            if (!Felix.NAME.equalsIgnoreCase(framework) && !Equinox.NAME.equalsIgnoreCase(framework)) {
                System.err.printf("Unsupported framework: %s%n", framework);
                return null;
            }
            Properties props = new Properties(new File(System.getProperty("karaf.base"), "etc/config.properties"));
            props.put("karaf.framework", framework.toLowerCase());
            props.save();
        }

        return null;
    }


    public Framework getFramework() {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return new Felix(new File(KARAF_BASE));
        } else {
            return new Equinox(new File(KARAF_BASE));
        }
    }

}
