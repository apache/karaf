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
package org.apache.felix.karaf.gshell.features.management;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.karaf.gshell.features.Feature;
import org.apache.felix.karaf.gshell.features.FeaturesRegistry;
import org.apache.felix.karaf.gshell.features.FeaturesService;
import org.apache.felix.karaf.gshell.features.Repository;

/**
 * The FeaturesServiceRegistry maintains the managed Features and Repositories
 * for JMX management.
 */
public class ManagedFeaturesRegistry implements FeaturesRegistry, ManagedFeaturesRegistryMBean {

    private static final transient Log LOG = LogFactory.getLog(ManagedFeaturesRegistry.class);

    private NamingStrategy namingStrategy;
    private ManagementAgent managementAgent;
    private Map<String, ManagedFeature> availableFeatures;
    private Map<String, ManagedFeature> installedFeatures;
    private Map<String, ManagedRepository> repositories;
    private boolean mbeanServerInitialized;
    private FeaturesService featuresService;
    private MBeanServer mbeanServer;

    public void installFeature(String name) throws Exception {
        featuresService.installFeature(name);
    }

    public void installFeature(String name, String version) throws Exception {
        featuresService.installFeature(name, version);
    }

    public void installRepository(String repositoryUri) throws Exception {
        featuresService.addRepository(new URI(repositoryUri));
    }

    public ManagedFeaturesRegistry() {
        availableFeatures = new ConcurrentHashMap<String, ManagedFeature>();
        installedFeatures = new ConcurrentHashMap<String, ManagedFeature>();
        repositories = new ConcurrentHashMap<String, ManagedRepository>();
    }

    public Map<String, ManagedFeature> getAvailableFeatures() {
        return availableFeatures;
    }

    public Map<String, ManagedFeature> getInstalledFeatures() {
        return installedFeatures;
    }

    public Map<String, ManagedRepository> getRepositories() {
        return repositories;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public void setManagementAgent(ManagementAgent managementAgent) {
        this.managementAgent = managementAgent;
    }

    public void register(Feature feature) {
        try {
            ManagedFeature mf = new ManagedFeature(feature, featuresService);
            availableFeatures.put(feature.getId(), mf);
            if ( mbeanServerInitialized ) {
                managementAgent.register(mf, namingStrategy.getObjectName(mf));
            }
        } catch (Exception e) {
            LOG.warn("Unable to register managed feature: " + e, e);
        }
    }

    public void unregister(Feature feature) {
        try {
            ManagedFeature mf = availableFeatures.remove(feature.getId());
            if ( mbeanServerInitialized ) {
                managementAgent.unregister(namingStrategy.getObjectName(mf));
            }
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed feature: " + e, e);
        }
    }

    public void registerInstalled(Feature feature) {
        try {
            ManagedFeature mf = new ManagedFeature(feature, featuresService);
            installedFeatures.put(feature.getId(), mf);
            if ( mbeanServerInitialized ) {
                managementAgent.register(mf, namingStrategy.getObjectName(mf, true));
            }
        } catch (Exception e) {
            LOG.warn("Unable to register managed feature: " + e, e);
        }
    }

    public void unregisterInstalled(Feature feature) {
        try {
            ManagedFeature mf = installedFeatures.remove(feature.getId());
            if ( mbeanServerInitialized ) {
                managementAgent.unregister(namingStrategy.getObjectName(mf, true));
            }
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed feature: " + e, e);
        }
    }

    public void register(Repository repository) {
        try {
            ManagedRepository mr = new ManagedRepository(repository, featuresService);
            repositories.put(repository.getURI().toString(), mr);

            for (Feature f : repository.getFeatures()) {
                // TODO: Associate the feature with the Repo?
                register(f);
            }

            if ( mbeanServerInitialized ) {
                managementAgent.register(mr, namingStrategy.getObjectName(mr));
            }
        } catch (Exception e) {
            LOG.warn("Unable to register managed repository: " + e, e);
        }
    }

    public void unregister(Repository repository) {
        try {
            ManagedRepository mr = repositories.remove(repository.getURI().toString());

            for (Feature f : repository.getFeatures()) {
                // TODO: Associate the feature with the Repo?
                unregister(f);
            }

            if ( mbeanServerInitialized ) {
                managementAgent.unregister(namingStrategy.getObjectName(mr));
            }
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed repository: " + e, e);
        }
    }

    public void init() throws Exception {
        if (managementAgent == null) {
            throw new IllegalArgumentException("managementAgent must not be null");
        }
        if (namingStrategy == null) {
            throw new IllegalArgumentException("namingStrategy must not be null");
        }
        if (mbeanServer != null) {
            registerMBeanServer(mbeanServer, null);
        }
    }

    public void registerMBeanServer(MBeanServer mbeanServer, Map props ) throws Exception {
        if (mbeanServer != null) {
            this.mbeanServer = mbeanServer;
        }
        if (managementAgent == null) {
            return;
        }
        mbeanServerInitialized = true;

        managementAgent.register(this, namingStrategy.getObjectName(this));

        for (ManagedRepository repository : repositories.values()) {
            managementAgent.register(repository, namingStrategy.getObjectName(repository));
        }

        for (ManagedFeature feature : availableFeatures.values()) {
            managementAgent.register(feature, namingStrategy.getObjectName(feature));
        }

        for (ManagedFeature feature : installedFeatures.values()) {
            installedFeatures.put(feature.getId(), feature);
            managementAgent.register(feature, namingStrategy.getObjectName(feature, true));
        }

    }

    
}
