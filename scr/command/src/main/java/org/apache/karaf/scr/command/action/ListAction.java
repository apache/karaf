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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.scr.command.ScrUtils;
import org.apache.karaf.scr.command.support.IdComparator;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * List all the components currently installed.
 */
@Command(scope = ScrCommandConstants.SCR_COMMAND, name = ScrCommandConstants.LIST_FUNCTION, description = "Display available components")
@Service
public class ListAction extends ScrActionSupport {

    private final IdComparator idComparator = new IdComparator();

    @Override
    protected Object doScrAction(ServiceComponentRuntime serviceComponentRuntime) throws Exception {
        ShellTable table = new ShellTable();
        table.column("ID");
        table.column("State");
        table.column("Component Name");

        List<ComponentConfigurationDTO> configs = new ArrayList<>();
        for (ComponentDescriptionDTO component : serviceComponentRuntime.getComponentDescriptionDTOs()) {
            configs.addAll(serviceComponentRuntime.getComponentConfigurationDTOs(component));
        }
        Collections.sort(configs, idComparator);
        for (ComponentConfigurationDTO config : configs) {
            // Display only non hidden components, or all if showHidden is true
            if (showHidden || !ScrActionSupport.isHiddenComponent(config)) {
                table.addRow().addContent(config.id, ScrUtils.getState(config.state), config.description.name);
            }
        }
        table.print(System.out);

        return null;
    }

}
