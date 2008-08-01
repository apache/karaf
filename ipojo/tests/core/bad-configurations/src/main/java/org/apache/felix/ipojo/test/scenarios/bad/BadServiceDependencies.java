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

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.test.scenarios.bad.service.BarService;

public class BadServiceDependencies extends OSGiTestCase {
    
    private String clazz = "org.apache.felix.ipojo.test.scenarios.component.CheckServiceProvider";
    private String type = "BAD-BothCheckServiceProvider";
    private Element manipulation;
    private Properties props;
    
    public void setUp() {
        manipulation = getManipulationForComponent();
        props = new Properties();
        props.put("name", "BAD");
    }
    
    
    private Element getNothing() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getNoField() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("filter", "(foo=bar)"));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getBadField() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("field", "BAD_FIELD")); // missing field.
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getBadFilter() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("field", "fs"));
        callback.addAttribute(new Attribute("filter", "(foo=bar)&(bar=foo)")); // Incorrect filter
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getBadFrom() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("field", "fs"));
        callback.addAttribute(new Attribute("from", "ba(d&_")); // Incorrect from
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getBadType() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("field", "fs"));
        callback.addAttribute(new Attribute("interface", BarService.class.getName()));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    private Element getBadAggregate() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element callback = new Element("requires", "");
        callback.addAttribute(new Attribute("field", "fs"));
        callback.addAttribute(new Attribute("aggregate", "true"));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }
    
    
    private Element getManipulationForComponent() {
        String header = (String) context.getBundle().getHeaders().get("iPOJO-Components");
        Element elem = null;
        try {
            elem = ManifestMetadataParser.parse(header);
        } catch (ParseException e) {
            fail("Parse Exception when parsing iPOJO-Component");
        }
        
        assertNotNull("Check elem not null", elem);
        
        Element manip = getManipulationForComponent(elem, type);
        assertNotNull("Check manipulation metadata not null for " + type, manip);
        return manip;
    }
    
    private Element getManipulationForComponent(Element metadata, String comp_name) {
        Element[] comps = metadata.getElements("component");
        for(int i = 0; i < comps.length; i++) {
            if(comps[i].containsAttribute("factory") && comps[i].getAttribute("factory").equals(comp_name)) {
                return comps[i].getElements("manipulation")[0];
            }
            if(comps[i].containsAttribute("name") && comps[i].getAttribute("name").equals(comp_name)) {
                return comps[i].getElements("manipulation")[0];
            }
        }
        return null;
    }
    
    public void testNothing() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getNothing());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with neither field and method must be rejected " + cf);
        } catch (ConfigurationException e) {
           // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
    
    public void testNoField() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getNoField());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with neither field and method must be rejected " + cf);
        } catch (ConfigurationException e) {
           // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
    
    public void testBadField() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getBadField());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with a bad field must be rejected " + cf);
        }catch (ConfigurationException e) {
           // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
    
    public void testBadFilter() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getBadFilter());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with a bad filter must be rejected " + cf);
        }catch (ConfigurationException e) {
            //OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
    
    public void testBadFrom() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getBadFrom());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with a bad from must be rejected " + cf);
        }catch (ConfigurationException e) {
            //OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
    
    public void testBadType() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getBadType());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A service requirement with a bad type must be rejected " + cf);
        }catch (ConfigurationException e) {
           // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }
  }
