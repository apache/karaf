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
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;

public class BadLFCController extends OSGiTestCase {
    
    private String clazz = "org.apache.felix.ipojo.test.scenarios.component.LifecycleControllerTest";
    
    private Element getNoFieldController() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element controller = new Element("controller", "");
        elem.addElement(controller);
        return elem;
    }
    
    private Element getBadFieldController() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));
        
        Element controller = new Element("controller", "");
        controller.addAttribute(new Attribute("field", "controller")); // Missing field
        elem.addElement(controller);
        elem.addElement(getManipulationForComponent("lcTest"));
        return elem;
    }
    
    private Element getManipulationForComponent(String comp_name) {
        String header = (String) context.getBundle().getHeaders().get("iPOJO-Components");
        Element elem = null;
        try {
            elem = ManifestMetadataParser.parse(header);
        } catch (ParseException e) {
            fail("Parse Exception when parsing iPOJO-Component");
        }
        
        assertNotNull("Check elem not null", elem);
        
        Element manip = getManipulationForComponent(elem, comp_name);
        assertNotNull("Check manipulation metadata not null for " + comp_name, manip);
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
    
    public void testNoField() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getNoFieldController());
            cf.start();
            cf.stop();
            fail("A lifecycle controller with a missing field must be rejected " + cf);
        } catch (Exception e) {
            // OK
        }
    }
    
    public void testBadField() {
        try {
            ComponentFactory cf = new ComponentFactory(context, getBadFieldController());
            cf.start();
            cf.stop();
            fail("A lifecycle controller with a bad field must be rejected " + cf);
        } catch (Exception e) {
            // OK
        }
    }

}
