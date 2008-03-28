/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.handler.properties.example;

import java.text.DateFormat;
import java.util.Date;

public class PropertiesTester {
    
    // These two fields will be injected. 
    private String property1;
    private String property2;
    
    /**
     * Starting method.
     * This method will be called when the instance starts.
     */
    public void start() {
        System.out.println("PropertiesTester is starting ...");
        // Read the injected properties.
        System.out.println("Property 1 : " + property1);
        System.out.println("Property 2 : " + property2);
        
        // Update the properties.
        updateProperties();
    }
    
    /**
     * Stopping method.
     * This method will be called when the instance stops.
     */
    public void stop() {
        System.out.println("PropertiesTester is stopping ...");
        System.out.println("Property 1 : " + property1);
        System.out.println("Property 2 : " + property2);
    }

    /**
     * This method just updates managed properties.
     * It appends the current date to the actual property value.
     */
    private void updateProperties() {
        System.out.println("Update properties");
       Date date = new Date();
       DateFormat df = DateFormat.getDateTimeInstance();
       // The properties will be updated in the property file
       property1 = property1 + " - " + df.format(date);
       property2 = property2 + " - " + df.format(date);
        
    }

}
