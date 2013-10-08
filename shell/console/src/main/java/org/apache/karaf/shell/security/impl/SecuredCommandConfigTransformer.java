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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecuredCommandConfigTransformer implements ConfigurationListener {
    static final String PROXY_COMMAND_ACL_PID_PREFIX = "org.apache.karaf.command.acl.";
    static final String PROXY_SERVICE_ACL_PID_PREFIX = "org.apache.karaf.service.acl.command.";

    private static final Logger LOG = LoggerFactory.getLogger(SecuredCommandConfigTransformer.class);
    private static final String CONFIGURATION_FILTER =
            "(" + Constants.SERVICE_PID  + "=" + PROXY_COMMAND_ACL_PID_PREFIX + "*)";

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
            // Not a command scope configuration file
            return;
        }

        String scopeName = config.getPid().substring(PROXY_COMMAND_ACL_PID_PREFIX.length());
        if (scopeName.indexOf('.') >= 0) {
            // Scopes don't contains dots, not a command scope
            return;
        }
        scopeName = scopeName.trim();

        Map<String, Dictionary<String, Object>> configMaps = new HashMap<String, Dictionary<String,Object>>();
        for (Enumeration<String> e = config.getProperties().keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String bareCommand = key;
            String arguments = "";
            int idx = bareCommand.indexOf('[');
            if (idx >= 0) {
                arguments = convertArgs(bareCommand.substring(idx));
                bareCommand = bareCommand.substring(0, idx);
            }
            if (bareCommand.indexOf('.') >= 0) {
                // Not a command
                continue;
            }
            bareCommand = bareCommand.trim();

            String pid = PROXY_SERVICE_ACL_PID_PREFIX + scopeName + "." + bareCommand;
            Dictionary<String, Object> map;
            if (!configMaps.containsKey(pid)) {
                map = new Hashtable<String, Object>();
                map.put("service.guard", "(&(" +
                        CommandProcessor.COMMAND_SCOPE + "=" + scopeName + ")(" +
                        CommandProcessor.COMMAND_FUNCTION + "=" + bareCommand + "))");
                configMaps.put(pid, map);
            } else {
                map = configMaps.get(pid);
            }

            // Put rules on the map twice, once for commands that 'execute' (implement Function) and
            // once for commands that are invoked directly.
            Object roleString = config.getProperties().get(key);
            map.put("execute" + arguments, roleString);
            map.put(key, roleString);
            map.put("*", "*"); // Any other method may be invoked by anyone
        }

        LOG.info("Generating command ACL config {} into service ACL configs {}",
                config.getPid(), configMaps.keySet());

        // Update config admin with the generated configuration
        for (Map.Entry<String, Dictionary<String, Object>> entry : configMaps.entrySet()) {
            Configuration genConfig = configAdmin.getConfiguration(entry.getKey());
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
        StringBuilder sb = new StringBuilder();
        sb.append("[/.*/,"); // Add a wildcard argument since the Function execute method has the arguments as second arg
        sb.append(commandACLArgs.substring(1));
        return sb.toString();
    }

    void deleteServiceGuardConfig(String originatingPid, String scope) throws IOException, InvalidSyntaxException {
        if (scope.contains("."))
            // This is not a command scope as that should be a single word without any further dots
            return;

        // Delete all the generated configurations for this scope
        Configuration[] configs = configAdmin.listConfigurations("(service.pid=" + PROXY_SERVICE_ACL_PID_PREFIX + scope + ".*)");
        if (configs == null)
            return;

        LOG.info("Config ACL deleted: {}. Deleting generated service ACL configs {}", originatingPid, configs);
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
                generateServiceGuardConfig(configAdmin.getConfiguration(event.getPid()));
                break;
            }
        } catch (Exception e) {
            LOG.error("Problem processing Configuration Event {}", event, e);
        }
    }
}
