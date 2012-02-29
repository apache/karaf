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
package org.apache.karaf.scr.examples.component;

import java.util.Map;

import org.apache.karaf.scr.examples.service.ExampleService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

@Component(name = MetaTypeManagedComponent.COMPONENT_NAME, designateFactory = MetaTypeManagedComponentConfig.class)
public class MetaTypeManagedComponent {

    public static final String COMPONENT_NAME = "MetaTypeManagedComponent";

    public static final String COMPONENT_LABEL = "Example Managed Component with MetaType";

    private LogService logService;

    private ExampleService exampleService;

    /**
     * Called when all of the SCR Components required dependencies have been
     * satisfied.
     */
    @Activate
    public void activate(final Map<String, ?> properties, ComponentContext componentContext) {
        logService.log(LogService.LOG_INFO, "Activating the " + COMPONENT_LABEL);
        
        MetaTypeManagedComponentConfig config = Configurable.createConfigurable(MetaTypeManagedComponentConfig.class, properties);
        
        exampleService.setName(config.name());
        exampleService.setSalutation(config.salutation());
        for (int i = 0; i < config.numberOfGreetings(); i++) {
            exampleService.printGreetings();
		}
    }

    /**
     * Called when any of the SCR Components required dependencies become
     * unsatisfied.
     */
    @Deactivate
    public void deactivate() {
        logService.log(LogService.LOG_INFO, "Deactivating the " + COMPONENT_LABEL);
    }

    @Reference
    public void setExampleService(final ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    public void unsetExampleService(final ExampleService exampleService) {
        this.exampleService = null;
    }

    @Reference
    protected void setLogService(LogService logService) {
        this.logService = logService;
    }

    protected void unsetLogService(LogService logService) {
        this.logService = null;
    }

}
