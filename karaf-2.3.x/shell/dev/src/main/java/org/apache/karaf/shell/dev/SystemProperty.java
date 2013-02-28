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
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Command that allow access to system properties easily.
 */
@Command(scope = "dev", name = "system-property", description = "Get or set a system property.")
public class SystemProperty extends OsgiCommandSupport {

    @Option(name = "-p", aliases = { "--persistent" }, description = "Persist the new value to the etc/system.properties file")
    boolean persistent;

    @Argument(index = 0, name = "key", description = "The system property name")
    String key;

    @Argument(index = 1, name = "value", required = false, description = "New value for the system property")
    String value;

    @Override
    protected Object doExecute() throws Exception {
        if (value != null) {
            if (persistent) {
                String base = System.getProperty("karaf.base");
                Properties props = new Properties(new File(base, "etc/system.properties"));
                props.put(key, value);
                props.save();
            }
            return System.setProperty(key, value);
        } else {
            return System.getProperty(key);
        }
    }
}
