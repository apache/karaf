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
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.scr.command.ScrCommandConstants;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScrActionSupport implements Action {

    @Option(name = ScrActionSupport.SHOW_ALL_OPTION, aliases = {ScrActionSupport.SHOW_ALL_ALIAS}, description = "Show all Components including the System Components (hidden by default)", required = false, multiValued = false)
    boolean showHidden = false;

    public static final String SHOW_ALL_OPTION = "-s";
    public static final String SHOW_ALL_ALIAS = "--show-hidden";
    
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Reference
    private ScrService scrService;

    @Reference
    BundleContext bundleContext;

    public ScrActionSupport() {
    }

    @Override
    public Object execute() throws Exception {
        if (scrService == null) {
            String msg = "ScrService is unavailable";
            System.out.println(msg);
            logger.warn(msg);
        } else {
            doScrAction(scrService);
        }
        return null;
    }

    protected abstract Object doScrAction(ScrService scrService) throws Exception;

    protected boolean isActionable(Component component) {
        boolean answer = true;
        return answer;
    }

    public static boolean showHiddenComponent(CommandLine commandLine, Component component) {
        // first look to see if the show all option is there
        // if it is we set showAllFlag to true so the next section will be skipped
        List<String> arguments = Arrays.asList(commandLine.getArguments());
        return arguments.contains(ScrActionSupport.SHOW_ALL_OPTION) || arguments.contains(ScrActionSupport.SHOW_ALL_ALIAS);
    }

    @SuppressWarnings("rawtypes")
    public static boolean isHiddenComponent(Component component) {
        boolean answer = false;
        Dictionary properties = component.getProperties();
        if (properties != null) {
            String value = (String) properties.get(ScrCommandConstants.HIDDEN_COMPONENT_KEY);
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
     * Get the scrService Object associated with this instance of
     * ScrActionSupport.
     * 
     * @return the scrService
     */
    public ScrService getScrService() {
        return scrService;
    }

    /**
     * Sets the scrService Object for this ScrActionSupport instance.
     * 
     * @param scrService the scrService to set
     */
    public void setScrService(ScrService scrService) {
        this.scrService = scrService;
    }

}
