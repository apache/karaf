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

import java.util.Arrays;
import java.util.List;

import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScrActionSupport implements Action {

    @Option(name = ScrActionSupport.SHOW_ALL_OPTION, aliases = {ScrActionSupport.SHOW_ALL_ALIAS}, description = "Show all Components including the System Components (hidden by default)", required = false, multiValued = false)
    boolean showHidden = false;

    public static final String SHOW_ALL_OPTION = "-s";
    public static final String SHOW_ALL_ALIAS = "--show-hidden";
    
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Reference
    private ServiceComponentRuntime serviceComponentRuntime;

    @Reference
    BundleContext bundleContext;

    public ScrActionSupport() {
    }

    @Override
    public Object execute() throws Exception {
        if (serviceComponentRuntime == null) {
            String msg = "ServiceComponentRuntime is unavailable";
            System.out.println(msg);
            logger.warn(msg);
        } else {
            doScrAction(serviceComponentRuntime);
        }
        return null;
    }

    protected abstract Object doScrAction(ServiceComponentRuntime serviceComponentRuntime) throws Exception;

    public static boolean showHiddenComponent(CommandLine commandLine) {
        // first look to see if the show all option is there
        // if it is we set showAllFlag to true so the next section will be skipped
        List<String> arguments = Arrays.asList(commandLine.getArguments());
        return arguments.contains(ScrActionSupport.SHOW_ALL_OPTION) || arguments.contains(ScrActionSupport.SHOW_ALL_ALIAS);
    }

    public static boolean isHiddenComponent(ComponentConfigurationDTO config) {
        boolean answer = false;
        if (config.properties != null) {
            String value = (String) config.properties.get(ScrCommandConstants.HIDDEN_COMPONENT_KEY);
            // if the value is false, show the hidden
            // then someone wants us to display the name of a hidden component
            answer = Boolean.parseBoolean(value);
        }
        return answer;
    }

    /**
     * Get the bundleContext Object associated with this instance of
     * ScrActionSupport.
     * 
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Get the ServiceComponentRuntime Object associated with this instance of
     * ScrActionSupport.
     * 
     * @return the ServiceComponentRuntime
     */
    public ServiceComponentRuntime getServiceComponentRuntime() {
        return serviceComponentRuntime;
    }

    /**
     * Sets the ServiceComponentRuntime Object for this ScrActionSupport instance.
     * 
     * @param serviceComponentRuntime the ServiceComponentRuntime to set
     */
    public void setServiceComponentRuntime(ServiceComponentRuntime serviceComponentRuntime) {
        this.serviceComponentRuntime = serviceComponentRuntime;
    }

}
