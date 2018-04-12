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
package org.apache.karaf.system.commands;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Command that allow access to system properties easily.
 */
@Command(scope = "system", name = "property", description = "Get or set a system property.")
@Service
public class SystemProperty implements Action {

    @Option(name = "-p", aliases = {"--persistent"}, description = "Persist the new value to the etc/system.properties file")
    boolean persistent;

    @Option(name = "-f", aliases = {"--file-dump"}, description = "Dump all system properties in a file (in data folder)")
    boolean dumpToFile;

    @Option(name = "-u", aliases = {"--unset"}, description = "Show unset know properties with value unset")
    boolean unset;

    @Argument(index = 0, name = "key", required = false, description = "The system property name")
    String key;

    @Argument(index = 1, name = "value", required = false, description = "New value for the system property")
    String value;

    @Reference
    BundleContext bundleContext;

    @Reference
    SystemService systemService;

    @Override
    public Object execute() throws Exception {
        if (key == null && value == null) {
            Properties props = (Properties) System.getProperties().clone();

            String def = null;
            if (unset) {
                def = "unset";
            }

            setProperty(props, Constants.FRAMEWORK_BEGINNING_STARTLEVEL, def);
            setProperty(props, Constants.FRAMEWORK_BOOTDELEGATION, def);
            setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT, def);
            setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_APP, def);
            setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT, def);
            setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_EXT, def);
            setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK, def);
            setProperty(props, Constants.FRAMEWORK_EXECPERMISSION, def);
            setProperty(props, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, def);
            setProperty(props, Constants.FRAMEWORK_LANGUAGE, def);
            setProperty(props, Constants.FRAMEWORK_LIBRARY_EXTENSIONS, def);
            setProperty(props, Constants.FRAMEWORK_OS_NAME, def);
            setProperty(props, Constants.FRAMEWORK_OS_VERSION, def);
            setProperty(props, Constants.FRAMEWORK_PROCESSOR, def);
            setProperty(props, Constants.FRAMEWORK_SECURITY, def);
            setProperty(props, Constants.FRAMEWORK_SECURITY_OSGI, def);
            setProperty(props, Constants.FRAMEWORK_STORAGE, def);
            setProperty(props, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT, def);
            setProperty(props, Constants.FRAMEWORK_SYSTEMPACKAGES, def);
            setProperty(props, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, def);
            setProperty(props, Constants.FRAMEWORK_VENDOR, def);
            setProperty(props, Constants.FRAMEWORK_VERSION, def);
            setProperty(props, Constants.FRAMEWORK_WINDOWSYSTEM, def);

            setProperty(props, Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, def);
            setProperty(props, Constants.SUPPORTS_FRAMEWORK_EXTENSION, def);
            setProperty(props, Constants.SUPPORTS_FRAMEWORK_FRAGMENT, def);
            setProperty(props, Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, def);

            if (dumpToFile) {
                PrintStream ps = new PrintStream(
                        new File(
                                bundleContext.getProperty("karaf.data"),
                                "dump-properties-" + System.currentTimeMillis() + ".properties"
                        )
                );
                ps.println("#Dump of the System and OSGi properties with the command system:property");
                ps.println("#Dump executed at " + new SimpleDateFormat().format(new Date()));
                printOrderedProperties(props, ps);
                ps.flush();
                ps.close();
            } else {
                printOrderedProperties(props, System.out);
            }

            return null;
        }

        if (value != null) {
            systemService.setSystemProperty(key, value, persistent);
        } else {
            System.out.println(System.getProperty(key));
        }

        return null;
    }

    private void printOrderedProperties(Properties props, PrintStream out) {
        Set<Object> keys = props.keySet();
        List<String> order = new ArrayList<>(keys.size());
        for (Object str : keys) {
            order.add((String) str);
        }
        Collections.sort(order);
        for (String key : order) {
            out.println(key + "=" + props.getProperty(key));
        }
    }

    private void setProperty(Properties props, String key, String def) {
        String val = bundleContext.getProperty(key);
        if (val == null && def != null) {
            props.setProperty(key, def);
        } else if (val != null) {
            props.setProperty(key, val);
        }
    }

}
