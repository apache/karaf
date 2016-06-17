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
package org.apache.karaf.scr.command.completer;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.scr.command.action.ScrActionSupport;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScrCompleterSupport implements Completer {

    protected final transient Logger logger = LoggerFactory.getLogger(ScrCompleterSupport.class);

    @Reference
    private ServiceComponentRuntime serviceComponentRuntime;

    /**
     * Overrides the super method noted below. See super documentation for
     * details.
     *
     * @see org.apache.karaf.shell.api.console.Completer#complete(org.apache.karaf.shell.api.console.Session, org.apache.karaf.shell.api.console.CommandLine, java.util.List)
     */
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            List<ComponentConfigurationDTO> configs = new ArrayList<>();
            for (ComponentDescriptionDTO component : serviceComponentRuntime.getComponentDescriptionDTOs()) {
                configs.addAll(serviceComponentRuntime.getComponentConfigurationDTOs(component));
            }
            for (ComponentConfigurationDTO component : configs) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Component Name to work on: " + component.description.name);
                }
                if (ScrActionSupport.showHiddenComponent(commandLine)) {
                    // we display all because we are overridden
                    if (availableComponent(component)) {
                        delegate.getStrings().add(component.description.name);
                    }
                } else {
                    if (ScrActionSupport.isHiddenComponent(component)) {
                    // do nothing
                    } else {
                        // we aren't hidden so print it
                        if (availableComponent(component)) {
                            delegate.getStrings().add(component.description.name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception completing the command request: " + e.getLocalizedMessage());
        }
        return delegate.complete(session, commandLine, candidates);
    }

    public abstract boolean availableComponent(ComponentConfigurationDTO component) throws Exception;

    /**
     * Get the scrService Object associated with this instance of
     * ScrCompleterSupport.
     *
     * @return the scrService
     */
    public ServiceComponentRuntime getServiceComponentRuntime() {
        return serviceComponentRuntime;
    }

    /**
     * Sets the scrService Object for this ScrCompleterSupport instance.
     *
     * @param serviceComponentRuntime the ServiceComponentRuntime to set
     */
    public void setSServiceComponentRuntime(ServiceComponentRuntime serviceComponentRuntime) {
        this.serviceComponentRuntime = serviceComponentRuntime;
    }

}
