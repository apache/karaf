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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Appends a value to the current property value.
 */
@Command(scope = "config", name = "propappend", description = "Appends the given value to an existing property or creates the property with the specified name and value.")
public class PropAppendCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the property", required = true, multiValued = false)
    String prop;

    @Argument(index = 1, name = "value", description = "The value to append to the property", required = true, multiValued = false)
    String value;
    
	@Override
	protected void doExecute(ConfigurationAdmin admin) throws Exception {
        Dictionary props = getEditedProps();
        if (props == null) {
            System.err.println("No configuration is being edited. Run the edit command first");
        } else {
        	final Object currentValue = props.get(prop);
        	if (currentValue == null) {
        		props.put(prop, value);
        	} else if (currentValue instanceof String) {
        		props.put(prop, currentValue + value);
        	} else {
        		System.err.println("Append Failed: current value is not a String.");
        	}
        }
	}

}
