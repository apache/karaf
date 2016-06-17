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

import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.scr.command.completer.ActivateCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * Activates the given component by supplying its component name.
 */
@Command(scope = ScrCommandConstants.SCR_COMMAND, name = ScrCommandConstants.ACTIVATE_FUNCTION, description = "Activates a Component for the given name")
@Service
public class ActivateAction extends ScrActionSupport {

    @Argument(index = 0, name = "name", description = "The name of the Component to activate ", required = true, multiValued = false)
    @Completion(ActivateCompleter.class)
    String name;

    @Override
    protected Object doScrAction(ServiceComponentRuntime serviceComponentRuntime) throws Exception {
        logger.debug("Activate Action");
        logger.debug("  Activating the Component: " + name);
        for (ComponentDescriptionDTO component : serviceComponentRuntime.getComponentDescriptionDTOs()) {
            if (name.equals(component.name)) {
                serviceComponentRuntime.enableComponent(component);
            }
        }
        return null;
    }

}
