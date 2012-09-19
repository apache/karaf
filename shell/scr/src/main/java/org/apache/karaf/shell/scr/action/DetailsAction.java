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
package org.apache.karaf.shell.scr.action;

import java.util.Hashtable;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.shell.scr.ScrCommandConstants;
import org.apache.karaf.shell.scr.ScrUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

/**
 * Displays the details associated with a given component by supplying its
 * component name.
 */
@Command(
         scope = ScrCommandConstants.SCR_COMMAND, 
         name = ScrCommandConstants.DETAILS_FUNCTION, 
         description = "Displays a list of available components")
public class DetailsAction extends ScrActionSupport {

    @Argument(index = 0, name = "name", description = "The name of the Component to display the detials of", required = true, multiValued = false)
    String name;

    @SuppressWarnings({"rawtypes"})
    @Override
    protected Object doScrAction(ScrService scrService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing the Details Action");
        }
        System.out.println(getBoldString("Component Details"));
        Component[] components = scrService.getComponents(name);
        for (Component component : ScrUtils.emptyIfNull(Component.class, components)) {
            printDetail("  Name                : ", component.getName());
            printDetail("  State               : ", ScrUtils.getState(component.getState()));

            Hashtable props = (Hashtable)component.getProperties();
            if (!props.isEmpty()) {
                System.out.println(getBoldString("  Properties          : "));
                for (Object key : props.keySet()) {
                    Object value = props.get(key);
                    printDetail("    ", key + "=" + value);
                }
            }
            Reference[] references = component.getReferences();
            System.out.println(getBoldString("References"));

            for (Reference reference : ScrUtils.emptyIfNull(Reference.class, references)) {
                printDetail("  Reference           : ", reference.getName());
                printDetail("    State             : ", (reference.isSatisfied()) ? "satisfied" : "unsatisfied");
                printDetail("    Multiple          : ", (reference.isMultiple() ? "multiple" : "single"));
                printDetail("    Optional          : ", (reference.isOptional() ? "optional" : "mandatory"));
                printDetail("    Policy            : ", (reference.isStatic() ? "static" : "dynamic"));

                // list bound services
                ServiceReference[] boundRefs = reference.getServiceReferences();
                for (ServiceReference serviceReference : ScrUtils.emptyIfNull(ServiceReference.class, boundRefs)) {
                    final StringBuffer b = new StringBuffer();
                    b.append("Bound Service ID ");
                    b.append(serviceReference.getProperty(Constants.SERVICE_ID));

                    String componentName = (String)serviceReference.getProperty(ComponentConstants.COMPONENT_NAME);
                    if (componentName == null) {
                        componentName = (String)serviceReference.getProperty(Constants.SERVICE_PID);
                        if (componentName == null) {
                            componentName = (String)serviceReference.getProperty(Constants.SERVICE_DESCRIPTION);
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
        System.out.println(getBoldString(header) + value);
    }

}
