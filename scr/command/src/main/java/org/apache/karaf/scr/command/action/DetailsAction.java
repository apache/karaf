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

import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.scr.command.ScrUtils;
import org.apache.karaf.scr.command.completer.DetailsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

/**
 * Display the details associated with a given component by supplying its component name.
 */
@Command(scope = ScrCommandConstants.SCR_COMMAND, name = ScrCommandConstants.DETAILS_FUNCTION, description = "Display available components")
@Service
public class DetailsAction extends ScrActionSupport {

    @Argument(index = 0, name = "name", description = "The component name", required = true, multiValued = false)
    @Completion(DetailsCompleter.class)
    String name;

	@Override
    protected Object doScrAction(ServiceComponentRuntime serviceComponentRuntime) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing the Details Action");
        }
        System.out.println(SimpleAnsi.INTENSITY_BOLD + "Component Details" + SimpleAnsi.INTENSITY_NORMAL);
        for (ComponentDescriptionDTO component : serviceComponentRuntime.getComponentDescriptionDTOs()) {
            for (ComponentConfigurationDTO config : serviceComponentRuntime.getComponentConfigurationDTOs(component)) {
                if (name.equals(component.name)) {
                    printDetail("  Name                : ", component.name);
                    printDetail("  State               : ", ScrUtils.getState(config.state));

                    Map<String, Object> map = new TreeMap<>(component.properties);
                    if (!map.isEmpty()) {
                        System.out.println(SimpleAnsi.INTENSITY_BOLD + "  Properties          : " + SimpleAnsi.INTENSITY_NORMAL);
                        for (Object key : map.keySet()) {
                            Object value = map.get(key);
                            printDetail("    ", key + "=" + value);
                        }
                    }
                    ReferenceDTO[] references = component.references;
                    System.out.println(SimpleAnsi.INTENSITY_BOLD + "References" + SimpleAnsi.INTENSITY_NORMAL);

                    for (ReferenceDTO reference : ScrUtils.emptyIfNull(ReferenceDTO.class, references)) {
                        ServiceReferenceDTO[] boundServices = null;
                        boolean satisfied = true;
                        for (SatisfiedReferenceDTO satRef : config.satisfiedReferences) {
                            if (satRef.name.equals(reference.name)) {
                                boundServices = satRef.boundServices;
                                satisfied = true;
                            }
                        }
                        for (UnsatisfiedReferenceDTO satRef : config.unsatisfiedReferences) {
                            if (satRef.name.equals(reference.name)) {
                                boundServices = satRef.targetServices;
                                satisfied = false;
                            }
                        }
                        printDetail("  Reference           : ", reference.name);
                        printDetail("    State             : ", satisfied ? "satisfied" : "unsatisfied");
                        printDetail("    Cardinality       : ", reference.cardinality);
                        printDetail("    Policy            : ", reference.policy);
                        printDetail("    PolicyOption      : ", reference.policyOption);

                        // list bound services
                        for (ServiceReferenceDTO serviceReference : ScrUtils.emptyIfNull(ServiceReferenceDTO.class, boundServices)) {
                            final StringBuffer b = new StringBuffer();
                            b.append("Bound Service ID ");
                            b.append(serviceReference.properties.get(Constants.SERVICE_ID));

                            String componentName = (String) serviceReference.properties.get(ComponentConstants.COMPONENT_NAME);
                            if (componentName == null) {
                                componentName = (String) serviceReference.properties.get(Constants.SERVICE_PID);
                                if (componentName == null) {
                                    componentName = (String) serviceReference.properties.get(Constants.SERVICE_DESCRIPTION);
                                }
                            }
                            if (componentName != null) {
                                b.append(" (");
                                b.append(componentName);
                                b.append(")");
                            }
                            printDetail("    Service Reference : ", b.toString());
                        }

                        if (ScrUtils.emptyIfNull(ServiceReferenceDTO.class, boundServices).length == 0) {
                            printDetail("    Service Reference : ", "No Services bound");
                        }
                    }
                }
            }
        }
    
        return null;
    }

    private void printDetail(String header, String value) {
        System.out.println(SimpleAnsi.INTENSITY_BOLD + header + SimpleAnsi.INTENSITY_NORMAL + value);
    }

}
