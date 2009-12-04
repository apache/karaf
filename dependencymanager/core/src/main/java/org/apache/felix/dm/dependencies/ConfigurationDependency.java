/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.management.ServiceComponentDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Configuration dependency that can track the availability of a (valid) configuration. To use
 * it, specify a PID for the configuration. The dependency is always required, because if it is
 * not, it does not make sense to use the dependency manager. In that scenario, simply register
 * your service as a <code>ManagedService(Factory)</code> and handle everything yourself. Also,
 * only managed services are supported, not factories. There are a couple of things you need to
 * be aware of when implementing the <code>updated(Dictionary)</code> method:
 * <ul>
 * <li>Make sure it throws a <code>ConfigurationException</code> when you get a configuration
 * that is invalid. In this case, the dependency will not change: if it was not available, it
 * will still not be. If it was available, it will remain available and implicitly assume you
 * keep working with your old configuration.</li>
 * <li>This method will be called before all required dependencies are available. Make sure you
 * do not depend on these to parse your settings.</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ConfigurationDependency extends Dependency, ServiceComponentDependency
{
  ConfigurationDependency setCallback(String callback);

  /**
   * Sets the <code>service.pid</code> of the configuration you are depending on.
   */
  ConfigurationDependency setPid(String pid);

  /**
   * Sets propagation of the configuration properties to the service properties. Any additional
   * service properties specified directly are merged with these.
   */
  ConfigurationDependency setPropagate(boolean propagate);
}
