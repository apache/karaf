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
package org.apache.felix.karaf.shell.config;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "config", name = "proplist", description = "Lists properties from the currently edited configuration.")
public class PropListCommand extends ConfigCommandSupport {

    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        Dictionary props = getEditedProps();
        if (props == null) {
            System.err.println("No configuration is being edited. Run the edit command first");
        } else {
            for (Enumeration e = props.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                System.out.println("   " + key + " = " + props.get(key));
            }
        }
    }

}
