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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.shell.scr.ScrCommandConstants;
import org.apache.karaf.shell.scr.ScrUtils;
import org.apache.karaf.shell.scr.support.IdComparator;

import java.util.Arrays;

/**
 * Lists all the components currently installed.
 */
@Command(scope = ScrCommandConstants.SCR_COMMAND, 
         name = ScrCommandConstants.LIST_FUNCTION, 
         description = "Displays a list of available components")
public class ListAction extends ScrActionSupport {

    private final IdComparator idComparator = new IdComparator();

    @Override
    protected Object doScrAction(ScrService scrService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing the List Action");
        }
        System.out.println(getBoldString("   ID   State             Component Name"));
        Component[] components = scrService.getComponents();
        Arrays.sort(components, idComparator);
        for (Component component : ScrUtils.emptyIfNull(Component.class, components)) {
            if (showHidden) {
                // We display all because we are overridden
                printComponent(component);
            } else {
                if (ScrActionSupport.isHiddenComponent(component)) {
                    // do nothing
                } else {
                    // We aren't hidden so print it
                    printComponent(component);
                }
            }
        }
        return null;
    }

    private void printComponent(Component component) {
        String name = component.getName();
        String id = buildLeftPadBracketDisplay(component.getId() + "", 4);
        String state = buildRightPadBracketDisplay(ScrUtils.getState(component.getState()), 16);
        System.out.println("[" + id + "] [" + state + "] " + name);
    }

}
