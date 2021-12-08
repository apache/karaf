/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.config.command.completers;

import static org.apache.karaf.config.core.impl.MetaServiceCaller.doWithMetaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.karaf.config.command.ConfigCommandSupport;
import org.apache.karaf.config.core.impl.MetaServiceCaller.MetaInfo;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * {@link Completer} for Configuration Admin properties.
 *
 * Displays a list of existing properties based on the current configuration being edited.
 *
 */
@Service
public class ConfigurationPropertyCompleter implements Completer {

    private static final String OPTION = "-p";
    private static final String ALIAS = "--pid";

    @Reference
    private ConfigurationAdmin configAdmin;
    
    @Reference
    private BundleContext context;

    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        StringsCompleter strings = new StringsCompleter();
        if (session != null) {
            String pid = getPid(session, commandLine);
            Set<String> propertyNames = getPropertyNames(pid);
            if (propertyNames != null && !propertyNames.isEmpty()) {
                strings.getStrings().addAll(propertyNames);
            }
        }
        return strings.complete(session, commandLine, candidates);
    }

    /**
     * Retrieves the pid stored in the {@link Session} or passed as an argument.
     * Argument takes precedence from pid stored in the {@link Session}.
     */
    private String getPid(Session session, CommandLine commandLine) {
        String pid = (String) session.get(ConfigCommandSupport.PROPERTY_CONFIG_PID);
        if (commandLine.getArguments().length > 0) {
            List<String> arguments = Arrays.asList(commandLine.getArguments());
            if (arguments.contains(OPTION)) {
                int index = arguments.indexOf(OPTION);
                if (arguments.size() > index) {
                    return arguments.get(index + 1);
                }
            }

            if (arguments.contains(ALIAS)) {
                int index = arguments.indexOf(ALIAS);
                if (arguments.size() > index) {
                    return arguments.get(index + 1);
                }
            }
        }
        return pid;
    }

    /**
     * Returns the property names for the given pid.
     * @param pid
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Set<String> getPropertyNames(String pid) {
        Set<String> propertyNames = new HashSet<>();
        if (pid != null) {     
            try {
                String metaPid = pid;
                Configuration[] configs = configAdmin.listConfigurations("(service.pid="+pid+")");
                if (configs != null && configs.length > 0) {
                    Configuration configuration = configs[0];
                    if (configuration != null) {
                        Dictionary properties = configuration.getProcessedProperties(null);
                        if (properties != null) {
                            Enumeration keys = properties.keys();
                            while (keys.hasMoreElements()) {
                                propertyNames.add(String.valueOf(keys.nextElement()));
                            }
                        } 
                        //If we have a factory config that is the PID to use
                        if(configuration.getFactoryPid()!=null) {
                            metaPid = configuration.getFactoryPid();
                        }                                              
                    }
                }   
                //Try to fetch additional properties from the Metatype service
                List<String> metaTypeProperties = getMetaTypeProperties(metaPid);
                propertyNames.addAll(metaTypeProperties);
            } catch (IOException | InvalidSyntaxException e) {
                //Ignore
            }            
        }
        return propertyNames;
    }
    
    private List<String> getMetaTypeProperties(String pid){
        try {
            return doWithMetaType(context, pid, this::collectMetaConfigProperties);
        } catch (Throwable e) {
            Throwable ncdfe = e;
            while (ncdfe != null && !(ncdfe instanceof NoClassDefFoundError)) {
                ncdfe = ncdfe.getCause();
            }
            if (ncdfe != null && ncdfe.getMessage().equals("org/osgi/service/metatype/MetaTypeService")) {
                //It seems we don't have MetaTypeService 
            } else {
                throw e;
            }
        }
        return Collections.emptyList();
            
    }
    
    private List<String> collectMetaConfigProperties(Optional<MetaInfo> info){
        Optional<AttributeDefinition[]> attrs = info.map(e -> e.getDefinition().getAttributeDefinitions(ObjectClassDefinition.ALL));            
        if(attrs.isPresent()) {
            List<String> properties = new ArrayList<>(attrs.get().length);            
            for (AttributeDefinition attr : attrs.get()) {                
                properties.add(attr.getID());
            }           
            return properties;    
        }
        return Collections.emptyList();
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
       
}
