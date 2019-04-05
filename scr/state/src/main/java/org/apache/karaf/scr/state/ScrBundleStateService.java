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

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@Component(
        name = "ServiceComponentRuntimeBundleStateService"
)
public class ScrBundleStateService implements BundleStateService {

    @Reference
    private ServiceComponentRuntime scr;

    @Override
    public String getName() {
        return BundleStateService.NAME_DS;
    }

    @Override
    public String getDiag(Bundle bundle) {
        return allCompConfigs(bundle).map(this::diagComponent).collect(joining());
    }

    @Override
    public BundleState getState(Bundle bundle) {
        boolean waiting = bundle.getState() == Bundle.ACTIVE && allCompConfigs(bundle).anyMatch(this::unsatisfied);
        return waiting ? BundleState.Waiting : BundleState.Unknown;
    }

    private String diagComponent(ComponentConfigurationDTO cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append(cfg.description.name).append(" (").append(cfg.id).append(")\n");
        if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION) != 0) {
            sb.append("  missing configurations: ");
            sb.append(join(", ", cfg.description.configurationPid));
            sb.append("\n");
        }
        if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_REFERENCE) != 0) {
            sb.append("  missing references: ");
            String references = asList(cfg.unsatisfiedReferences).stream().map(ref -> ref.name).collect(joining(", "));
            sb.append(references);
            sb.append("\n");
        }
        return sb.toString();
    }

    private Stream<ComponentConfigurationDTO> allCompConfigs(Bundle bundle) {
        return scr.getComponentDescriptionDTOs(bundle).stream()
            .flatMap(desc -> scr.getComponentConfigurationDTOs(desc).stream());
    }

    private boolean unsatisfied(ComponentConfigurationDTO cfg) {
        return cfg.state != ComponentConfigurationDTO.ACTIVE
                && cfg.state != ComponentConfigurationDTO.SATISFIED;
    }
}
