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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "config", name = "property-list", description = "Lists properties from the currently edited configuration.")
@Service
public class PropListCommand extends ConfigPropertyCommandSupport {

    @Option(name = "--raw")
    boolean raw;

    @Override
    public void propertyAction(TypedProperties props) {
        if (raw) {
            try {
                StringWriter sw = new StringWriter();
                props.save(sw);
                System.out.print(sw.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            try {
                StringWriter sw = new StringWriter();
                props.save(sw);
                TypedProperties p = new TypedProperties();
                p.load(new StringReader(sw.toString()));
                props = p;
            } catch (IOException e) {
                // Ignore
            }
            Map<String, Object> sortedProps = new TreeMap<>(props);
            for (Map.Entry<String, Object> entry : sortedProps.entrySet()) {
                System.out.println("   " + entry.getKey() + " = " + displayValue(entry.getValue()));
            }
        }
    }

    /**
     * Check if a configuration (identified by PID) requires an update or not.
     *
     * @param pid the configuration PID.
     * @return true if the configuration requires an update, false else (always returns false).
     */
    @Override
    protected boolean requiresUpdate(String pid) {
        return false;
    }

}
