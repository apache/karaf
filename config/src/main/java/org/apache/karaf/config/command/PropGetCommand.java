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
package org.apache.karaf.config.command;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.command.completers.ConfigurationPropertyCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Command(scope = "config", name = "property-get", description = "Gets the value of a property in the currently edited configuration.")
@Service
public class PropGetCommand extends ConfigPropertyCommandSupport {

    @Option(name = "--raw")
    boolean raw;

    @Argument(index = 0, name = "property", description = "The name of the property to get value for", required = true, multiValued = false)
    @Completion(ConfigurationPropertyCompleter.class)
    String prop;

    @Override
    public void propertyAction(TypedProperties props) {
        if (raw) {
            List<String> strings = props.getRaw(prop);
            for (String s : strings) {
                System.out.println(s);
            }
        } else {
            System.out.println(displayValue(props.get(prop)));
        }
    }


}
