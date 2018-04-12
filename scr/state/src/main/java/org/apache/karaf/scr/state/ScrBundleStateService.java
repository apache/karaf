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
package org.apache.karaf.scr.state;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

public class ScrBundleStateService implements BundleStateService {

    ServiceComponentRuntime scr;

    public ScrBundleStateService(ServiceComponentRuntime scr) {
        this.scr = scr;
    }

    @Override
    public String getName() {
        return BundleStateService.NAME_DS;
    }

    @Override
    public String getDiag(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs(bundle)) {
            for (ComponentConfigurationDTO cfg : scr.getComponentConfigurationDTOs(desc)) {
                if (cfg.state != ComponentConfigurationDTO.ACTIVE
                        && cfg.state != ComponentConfigurationDTO.SATISFIED) {
                    sb.append(cfg.description.name).append(" (").append(cfg.id).append(")\n");
                    if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION) != 0) {
                        sb.append("  missing configurations: ");
                        boolean first = true;
                        for (String s : cfg.description.configurationPid) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(s);
                            first = false;
                        }
                        sb.append("\n");
                    }
                    if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_REFERENCE) != 0) {
                        sb.append("  missing references: ");
                        boolean first = true;
                        for (UnsatisfiedReferenceDTO ur : cfg.unsatisfiedReferences) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(ur.name);
                            first = false;
                        }
                        sb.append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public BundleState getState(Bundle bundle) {
        if (bundle.getState() == Bundle.ACTIVE) {
            for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs(bundle)) {
                for (ComponentConfigurationDTO cfg : scr.getComponentConfigurationDTOs(desc)) {
                    if (cfg.state != ComponentConfigurationDTO.ACTIVE
                            && cfg.state != ComponentConfigurationDTO.SATISFIED) {
                        return BundleState.Waiting;
                    }
                }
            }
        }
        return BundleState.Unknown;
    }
}
