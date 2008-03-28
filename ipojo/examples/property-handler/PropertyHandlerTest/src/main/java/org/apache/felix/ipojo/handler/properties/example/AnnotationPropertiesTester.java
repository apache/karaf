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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;

import example.handler.properties.Properties;

/**
 * A simple component implementation using the property handler.
 *@author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(name = "annotationTester")
@Properties(file = "props\\properties.txt")
public class AnnotationPropertiesTester {
 
    /**
     * Property 1 : injected. 
     */
    private String m_property1;

    /**
     * Property 1 : injected. 
     */
    private String m_property2;

    /**
     * Start method : 
     * displays loaded & injected properties before modifying them.
     */
    @Validate
    public void start() {
        System.out.println("AnnotationPropertiesTester is starting ...");
        System.out.println("Property 1 : " + m_property1);
        System.out.println("Property 2 : " + m_property2);

        updateProperties();
    }

    /**
     * Stop method :
     * displays properties values.
     */
    @Invalidate
    public void stop() {
        System.out.println("AnnotationPropertiesTester is stopping ...");
        System.out.println("Property 1 : " + m_property1);
        System.out.println("Property 2 : " + m_property2);
    }

    /**
     * Update property value.
     */
    private void updateProperties() {
        System.out.println("Update properties");
        Date date = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        m_property1 = m_property1 + " - " + df.format(date);
        m_property2 = m_property2 + " - " + df.format(date);

    }
}
