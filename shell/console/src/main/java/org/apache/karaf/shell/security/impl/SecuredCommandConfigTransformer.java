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
package org.apache.karaf.shell.security.impl;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

@Deprecated
public class SecuredCommandConfigTransformer implements ConfigurationListener {

    static final String PROXY_COMMAND_ACL_PID_PREFIX = "org.apache.karaf.command.acl.";
    static final String PROXY_SERVICE_ACL_PID_PREFIX = "org.apache.karaf.service.acl.command.";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredCommandConfigTransformer.class);
    private static final String CONFIGURATION_FILTER =
            "(" + Constants.SERVICE_PID + "=" + PROXY_COMMAND_ACL_PID_PREFIX + "*)";
    private static final String ACL_SCOPE_BUNDLE_MAP = "org.apache.karaf.command.acl.scope_bundle";

    private ConfigurationAdmin configAdmin;
    

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void init() throws Exception {
        Configuration[] configs = configAdmin.listConfigurations(CONFIGURATION_FILTER);
        if (configs == null)
            return;

        for (Configuration config : configs) {
            generateServiceGuardConfig(config);
        }
    }

    void generateServiceGuardConfig(Configuration config) throws IOException {
        if (!config.getPid().startsWith(PROXY_COMMAND_ACL_PID_PREFIX)) {
            // not a command scope configuration file
            return;
        }

        String scopeName = config.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length());
        if (scopeName.indexOf('.') >= 0) {
            // scopes don't contains dots, not a command scope
            return;
        }
        scopeName = scopeName.trim();

        Dictionary<String, Object> configProps = config.getProcessedProperties(null);

        Map<String, Dictionary<String, Object>> configMaps = new HashMap<>();
        for (Enumeration<String> e = configProps.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String bareCommand = key;
            String arguments = "";
            int idx = bareCommand.indexOf('[');
            if (idx >= 0) {
                arguments = convertArgs(bareCommand.substring(idx));
                bareCommand = bareCommand.substring(0, idx);
            }
            if (bareCommand.indexOf('.') >= 0) {
                // not a command
                continue;
            }
            bareCommand = bareCommand.trim();

            String pid = PROXY_SERVICE_ACL_PID_PREFIX + scopeName + "." + bareCommand;
            Dictionary<String, Object> map;
            if (!configMaps.containsKey(pid)) {
                map = new Hashtable<>();
                map.put("service.guard", "(&(" +
                        CommandProcessor.COMMAND_SCOPE + "=" + scopeName + ")(" +
                        CommandProcessor.COMMAND_FUNCTION + "=" + bareCommand + "))");
                configMaps.put(pid, map);
            } else {
                map = configMaps.get(pid);
            }

            // put rules on the map twice, once for commands that 'execute' (implement Function) and
            // once for commands that are invoked directly
            Object roleString = configProps.get(key);
            map.put("execute" + arguments, roleString);
            map.put(key, roleString);
            map.put("*", "*"); // any other method may be invoked by anyone
        }

        LOGGER.info("Generating command ACL config {} into service ACL configs {}",
                config.getPid(), configMaps.keySet());

        // update config admin with the generated configuration
        for (Map.Entry<String, Dictionary<String, Object>> entry : configMaps.entrySet()) {
            Configuration genConfig = configAdmin.getConfiguration(entry.getKey(), null);
            genConfig.update(entry.getValue());
        }
    }

    private String convertArgs(String commandACLArgs) {
        if (!commandACLArgs.startsWith("[/")) {
            throw new IllegalStateException("Badly formatted argument match: " + commandACLArgs + " Should start with '[/'");
        }
        if (!commandACLArgs.endsWith("/]")) {
            throw new IllegalStateException("Badly formatted argument match: " + commandACLArgs + " Should end with '/]'");
        }
        return "[/.*/," // add a wildcard argument since the Function execute method has the arguments as second arg
            + commandACLArgs.substring(1);
    }

    void deleteServiceGuardConfig(String originatingPid, String scope) throws IOException, InvalidSyntaxException {
        if (scope.contains("."))
            // This is not a command scope as that should be a single word without any further dots
            return;

        // Delete all the generated configurations for this scope
        Configuration[] configs = configAdmin.listConfigurations("(service.pid=" + PROXY_SERVICE_ACL_PID_PREFIX + scope + ".*)");
        if (configs == null)
            return;

        LOGGER.info("Config ACL deleted: {}. Deleting generated service ACL configs {}", originatingPid, configs);
        for (Configuration config : configs) {
            config.delete();
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (!event.getPid().startsWith(PROXY_COMMAND_ACL_PID_PREFIX))
            return;

        try {
            switch (event.getType()) {
                case ConfigurationEvent.CM_DELETED:
                    deleteServiceGuardConfig(event.getPid(), event.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length()));
                    break;
                case ConfigurationEvent.CM_UPDATED:
                    Configuration config = configAdmin.getConfiguration(event.getPid(), null);
                    generateServiceGuardConfig(config);
                    refreshTheAffectedShellCommandBundle(event, config);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Problem processing Configuration Event {}", event, e);
        }
    }

    private void refreshTheAffectedShellCommandBundle(ConfigurationEvent event, Configuration config) {
        if (!config.getPid().startsWith(PROXY_COMMAND_ACL_PID_PREFIX)) {
            // not a command scope configuration file
            return;
        }
        String filter = "";
        String scopeName = config.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length());
        if (scopeName.indexOf('.') >= 0) {
            // scopes don't contains dots, not a command scope
            return;
        }
        scopeName = scopeName.trim();
        for (Entry<String, String> entry : loadScopeBundleMaps().entrySet()) {
            if (entry.getKey().equals(scopeName)) {
                filter = "(" +
                    "osgi.blueprint.container.symbolicname" + "=" + entry.getValue() + ")";
                break;
            }
        }
        
        if (filter.length() == 0) {
            return;
        }

        
        BundleContext bundleContext = event.getReference().getBundle().getBundleContext();
        
        try {
            ServiceReference<?>[] sr = bundleContext.getServiceReferences("org.osgi.service.blueprint.container.BlueprintContainer", filter);
            if (sr == null) {
                LOGGER.error("can't find the command bundle for scope " + scopeName);
                return;
            }
            LOGGER.debug("the refreshed bundle is " + sr[0].getBundle().getSymbolicName());
            
            ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
            if (ref == null) {
                LOGGER.error("PackageAdmin service is unavailable.");
                return;
            }
            try {
                PackageAdmin pa = (PackageAdmin) bundleContext.getService(ref);
                if (pa == null) {
                    LOGGER.error("PackageAdmin service is unavailable.");
                    return;
                }
                pa.refreshPackages(new Bundle[]{sr[0].getBundle()});
            }
            finally {
                bundleContext.ungetService(ref);
            }
        } catch (InvalidSyntaxException ex) {
            LOGGER.error("Problem refresh the affected shell command bundle", ex);
        }
        
        
    }
    
    private Map<String, String> loadScopeBundleMaps() {
        Map<String, String> scopeBundleMaps = new HashMap<>();
        try {
            for (Configuration config : configAdmin.listConfigurations("(service.pid=" + ACL_SCOPE_BUNDLE_MAP + ")")) {
                Enumeration<String> keys = config.getProcessedProperties(null).keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    scopeBundleMaps.put(key, (String)config.getProcessedProperties(null).get(key));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Problem load the scope bundle map", ex);
        } 
        return scopeBundleMaps;
    }
    
}

