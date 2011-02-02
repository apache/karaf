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
package org.apache.karaf.shell.config;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "config", name = "update", description = "Saves and propagates changes from the configuration being edited.")
public class UpdateCommand extends ConfigCommandSupport {

    @Option(name = "-b", aliases = { "--bypass-storage" }, multiValued = false, required = false, description = "Do not store the configuration in a properties file, but feed it directly to ConfigAdmin")
    private boolean bypassStorage;

    private File storage;

    public File getStorage() {
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        Dictionary props = getEditedProps();
        if (props == null) {
            System.err.println("No configuration is being edited. Run the edit command first");
        } else if (!bypassStorage && storage != null) {
        	String pid = (String) this.session.get(PROPERTY_CONFIG_PID);
        	File storageFile = new File(storage, pid + ".cfg");
            Properties p = new Properties(storageFile);
            for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                Object key = keys.nextElement();
                if (!"service.pid".equals(key) && !"felix.fileinstall.filename".equals(key)) {
                    p.put((String) key, (String) props.get(key));
                }
            }
            storage.mkdirs();
            p.save();
            this.session.put(PROPERTY_CONFIG_PID, null);
            this.session.put(PROPERTY_CONFIG_PROPS, null);
        } else {
            String pid = (String) this.session.get(PROPERTY_CONFIG_PID);
            Configuration cfg = admin.getConfiguration(pid, null);
            cfg.update(props);
            this.session.put(PROPERTY_CONFIG_PID, null);
            this.session.put(PROPERTY_CONFIG_PROPS, null);
        }
    }
}
