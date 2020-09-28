/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.staticcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

public class StaticConfigAdminImpl implements ConfigurationAdmin {
    private final BundleContext context;
    private final List<Configuration> configurations;
    
    public StaticConfigAdminImpl(BundleContext context, List<Configuration> configs) throws IOException {
        Objects.requireNonNull(configs, "configs");
        this.context = context;
        this.configurations = configs;
        ServiceTracker<ManagedService, ManagedService> serviceTracker = new ServiceTracker<ManagedService, ManagedService>(context, ManagedService.class, null) {
            @Override
            public ManagedService addingService(ServiceReference<ManagedService> reference) {
                ManagedService service = context.getService(reference);
                Object pidObj = reference.getProperty(Constants.SERVICE_PID);
                
                boolean found = false;
                
                if (pidObj instanceof String) {
                    String pid = (String) pidObj;
                    
                    for (Configuration config : configurations) {
                        if (config.getPid().equals(pid) && config.getFactoryPid() == null) {
                        	found = true;
                        	invokeUpdate(service, config);
                        }
                    }
                    
                }
                
                if (!found) {
                	invokeUpdate(service, null);
                }
                
                return service;
            }

            @Override
            public void removedService(ServiceReference<ManagedService> reference, ManagedService service) {
                context.ungetService(reference);
            }
        };
        serviceTracker.open();

        ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> factoryTracker
                = new ServiceTracker<ManagedServiceFactory, ManagedServiceFactory>(context, ManagedServiceFactory.class, null) {
            @Override
            public ManagedServiceFactory addingService(ServiceReference<ManagedServiceFactory> reference) {
                ManagedServiceFactory factory = context.getService(reference);
                Object pidObj = reference.getProperty(Constants.SERVICE_PID);
                if (pidObj instanceof String) {
                    String pid = (String) pidObj;
                    for (Configuration config : configurations) {
                        if (config.getPid().equals(pid) && config.getFactoryPid() != null) {
                            try {
                                factory.updated(config.getFactoryPid(), config.getProcessedProperties(null));
                            } catch (ConfigurationException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    System.err.println("Unsupported pid: " + pidObj);
                }
                return factory;
            }

            @Override
            public void removedService(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
                super.removedService(reference, service);
            }
        };
        factoryTracker.open();
    }
    
    private void invokeUpdate(ManagedService service, Configuration config) {
		try {
			service.updated(config == null ? null : config.getProcessedProperties(null));
		} catch (final Exception e) {
			e.printStackTrace();
		}
    }

    @Override
    public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration getConfiguration(String pid, String location) throws IOException {
        return getConfiguration(pid);
    }

    @Override
    public Configuration getConfiguration(String pid) throws IOException {
        for (Configuration config : configurations) {
            if (config.getPid().equals(pid) && config.getFactoryPid() == null) {
                return config;
            }
        }
        Hashtable<String, Object> cfg = new Hashtable<>();
        cfg.put(Constants.SERVICE_PID, pid);
        return new StaticConfigurationImpl(pid, null, cfg);
    }

    @Override
    public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
        List<Configuration> configs;
        if (filter == null) {
            configs = configurations;
        } else {
            configs = new ArrayList<>();
            Filter flt = context.createFilter(filter);
            for (Configuration config : configurations) {
                if (flt.match(config.getProcessedProperties(null))) {
                    configs.add(config);
                }
            }
        }
        return configs.isEmpty() ? null : configs.toArray(new Configuration[configs.size()]);
    }

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {
		throw new UnsupportedOperationException("getFactoryConfiguration");
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {
		throw new UnsupportedOperationException("getFactoryConfiguration(pid,name)");
	}

}
