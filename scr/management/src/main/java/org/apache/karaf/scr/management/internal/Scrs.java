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
package org.apache.karaf.scr.management.internal;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.scr.management.ScrsMBean;

public class Scrs extends StandardMBean implements ScrsMBean {

    private ScrService scrService;

    /**
     * Creates new Declarative Services mbean.
     * 
     * @throws NotCompliantMBeanException
     */
    public Scrs(ScrService scrService) throws NotCompliantMBeanException {
        super(ScrsMBean.class);
        this.scrService = scrService;
    }

    public String[] listComponents() throws Exception {
        Component[] components = safe(scrService.getComponents());
        String[] componentNames = new String[components.length];
        for (int i = 0; i < componentNames.length; i++) {
            componentNames[i] = components[i].getName();
        }
        return componentNames;
    }

    public boolean isComponentActive(String componentName) throws Exception {
        boolean state = false;
        Component[] components = scrService.getComponents(componentName);
        for (Component component : safe(components)) {
            state = (component.getState() == Component.STATE_ACTIVE)?true:false;
        }
        return state;
    }

    public void activateComponent(String componentName) throws Exception {
        if (scrService.getComponents(componentName) != null) {
            Component[] components = scrService.getComponents(componentName);
            for (Component component : safe(components)) {
                component.enable();
            }
        }
    }

    public void deactiveateComponent(String componentName) throws Exception {
        if (scrService.getComponents(componentName) != null) {
            Component[] components = scrService.getComponents(componentName);
            for (Component component : safe(components)) {
                component.disable();
            }
        }
    }
    
    private Component[] safe( Component[] components ) {
        return components == null ? new Component[0] : components;
    }

}
