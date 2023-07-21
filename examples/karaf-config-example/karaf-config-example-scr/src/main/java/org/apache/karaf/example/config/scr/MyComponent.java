/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.example.config.scr;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

@Component(name = "my-component", immediate = true, configurationPid = "org.apache.karaf.example.config")
public class MyComponent {

    @Activate
    public void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (properties.get(key) instanceof int[]) {
                System.out.println(key + " = " + Arrays.toString((int[])properties.get(key)));
            } else {
                System.out.println(key + " = " + properties.get(key));
            }
        }
    }

}
