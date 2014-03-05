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
package org.apache.karaf.scr.command.action;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.scr.command.ScrUtils;
import org.apache.karaf.scr.command.completer.DetailsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

import java.util.Hashtable;

/**
 * Display the details associated with a given component by supplying its component name.
 */
@Command(scope = ScrCommandConstants.SCR_COMMAND, name = ScrCommandConstants.DETAILS_FUNCTION, description = "Display available components")
@Service
public class DetailsAction extends ScrActionSupport {

    @Argument(index = 0, name = "name", description = "The component name", required = true, multiValued = false)
    @Completion(DetailsCompleter.class)
    String name;

    @SuppressWarnings("rawtypes")
	@Override
    protected Object doScrAction(ScrService scrService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing the Details Action");
        }
        System.out.println(SimpleAnsi.INTENSITY_BOLD + "Component Details" + SimpleAnsi.INTENSITY_NORMAL);
        Component[] components = scrService.getComponents(name);
        for (Component component : ScrUtils.emptyIfNull(Component.class, components)) {
            printDetail("  Name                : ", component.getName());
            printDetail("  State               : ", ScrUtils.getState(component.getState()));

            Hashtable props = (Hashtable)component.getProperties();
            if (!props.isEmpty()) {
                System.out.println(SimpleAnsi.INTENSITY_BOLD + "  Properties          : " + SimpleAnsi.INTENSITY_NORMAL);
                for (Object key : props.keySet()) {
                    Object value = props.get(key);
                    printDetail("    ", key + "=" + value);
                }
            }
            Reference[] references = component.getReferences();
            System.out.println(SimpleAnsi.INTENSITY_BOLD + "References" + SimpleAnsi.INTENSITY_NORMAL);

            for (Reference reference : ScrUtils.emptyIfNull(Reference.class, references)) {
                printDetail("  Reference           : ", reference.getName());
                printDetail("    State             : ", (reference.isSatisfied()) ? "satisfied" : "unsatisfied");
                printDetail("    Multiple          : ", (reference.isMultiple() ? "multiple" : "single" ));
                printDetail("    Optional          : ", (reference.isOptional() ? "optional" : "mandatory" ));
                printDetail("    Policy            : ", (reference.isStatic() ?  "static" : "dynamic" ));

                // list bound services
				ServiceReference[] boundRefs = reference.getServiceReferences();
                for (ServiceReference serviceReference : ScrUtils.emptyIfNull(ServiceReference.class, boundRefs)) {
                    final StringBuffer b = new StringBuffer();
                    b.append("Bound Service ID ");
                    b.append(serviceReference.getProperty(Constants.SERVICE_ID));

                    String componentName = (String) serviceReference.getProperty(ComponentConstants.COMPONENT_NAME);
                    if (componentName == null) {
                        componentName = (String) serviceReference.getProperty(Constants.SERVICE_PID);
                        if (componentName == null) {
                            componentName = (String) serviceReference.getProperty(Constants.SERVICE_DESCRIPTION);
                        }
                    }
                    if (componentName != null) {
                        b.append(" (");
                        b.append(componentName);
                        b.append(")");
                    }
                    printDetail("    Service Reference : ", b.toString());
                }
                
                if (ScrUtils.emptyIfNull(ServiceReference.class, boundRefs).length == 0) {
                    printDetail("    Service Reference : ", "No Services bound");
                }
            }

        }
    
        return null;
    }

    private void printDetail(String header, String value) {
        System.out.println(SimpleAnsi.INTENSITY_BOLD + header + SimpleAnsi.INTENSITY_NORMAL + value);
    }

}
