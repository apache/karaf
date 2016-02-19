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
package org.apache.karaf.management.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * <p>Class to optimize ConfigAdmin access with the lifecycle of single
 * {@link org.apache.karaf.management.JMXSecurityMBean#canInvoke(Map) bulk query invocation}. This prevents countless
 * {@link org.osgi.service.cm.ConfigurationAdmin#listConfigurations(String) listings of ConfigAdmin configurations}
 * for each checked MBean/method.</p>
 * <p>Access to this object doesn't have to be synchronized, as it is passed down the <code>canInvoke</code> chain.</p>
 */
public class BulkRequestContext {

    private List<String> allPids = new ArrayList<String>();
    private List<Dictionary<String, Object>> whiteListProperties = new ArrayList<Dictionary<String, Object>>();

    private ConfigurationAdmin configAdmin;

    // cache with lifecycle bound to BulkRequestContext instance
    private Map<String, Dictionary<String, Object>> cachedConfigurations = new HashMap<String, Dictionary<String, Object>>();

    private BulkRequestContext() {}

    public static BulkRequestContext newContext(ConfigurationAdmin configAdmin) throws IOException {
        BulkRequestContext context = new BulkRequestContext();
        context.configAdmin = configAdmin;
        try {
            // list available ACL configs - valid for this instance only
            for (Configuration config : configAdmin.listConfigurations("(service.pid=jmx.acl*)")) {
                context.allPids.add(config.getPid());
            }
            // list available ACT whitelist configs
            Configuration[] configs = configAdmin.listConfigurations("(service.pid=jmx.acl.whitelist)");
            if (configs != null) {
                for (Configuration config : configs) {
                    context.whiteListProperties.add(config.getProperties());
                }
            }
        } catch (InvalidSyntaxException ise) {
            throw new RuntimeException(ise);
        }

        return context;
    }

    /**
     * Returns list of PIDs related to RBAC/ACL
     * @return
     */
    public List<String> getAllPids() {
        return allPids;
    }

    /**
     * Returns list of configurations from
     * @return
     */
    public List<Dictionary<String,Object>> getWhitelistProperties() {
        return whiteListProperties;
    }

    /**
     * Returns {@link Configuration ConfigAdmin configuration} - may be cached in this instance of
     * {@link BulkRequestContext context}
     * @param generalPid
     * @return
     */
    public Dictionary<String, Object> getConfiguration(String generalPid) throws IOException {
        if (!cachedConfigurations.containsKey(generalPid)) {
            cachedConfigurations.put(generalPid, configAdmin.getConfiguration(generalPid, null).getProperties());
        }
        return cachedConfigurations.get(generalPid);
    }

}
