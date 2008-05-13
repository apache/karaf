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
package org.apache.felix.ipojo.test.scenarios.bad;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManagerFactory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class BadFactories extends OSGiTestCase {
    
    private Element getElementFactoryWithNoClassName() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("name", "noclassname"));
        return elem;        
    }
    
    private Element getElementHandlerFactoryWithNoClassName() {
        Element elem = new Element("handler", "");
        elem.addAttribute(new Attribute("name", "noclassname"));
        return elem;        
    }
    
    private Element getElementHandlerFactoryWithNoName() {
        Element elem = new Element("handler", "");
        elem.addAttribute(new Attribute("className", "noclassname"));
        return elem;        
    }
    
    public void testBadFactory() {
        try {
            new ComponentFactory(context, getElementFactoryWithNoClassName());
            fail("A factory with no class name must be rejected");
        } catch (ConfigurationException e) {
          // OK.   
        }
    }
    
    public void testBadHandlerFactory1() {
        try {
            new HandlerManagerFactory(context, getElementHandlerFactoryWithNoClassName());
            fail("An handler factory with no class name must be rejected");
        } catch (ConfigurationException e) {
          // OK.   
        }
    }
    
    public void testBadHandlerFactory2() {
        try {
            new HandlerManagerFactory(context, getElementHandlerFactoryWithNoName());
            fail("An handler factory with no name must be rejected");
        } catch (ConfigurationException e) {
          // OK.   
        }
    }
    
    

}
