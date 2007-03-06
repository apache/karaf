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
package org.apache.felix.ipojo.handlers.architecture;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Achtiecture Handler : do reflection on your component.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ArchitectureHandler extends Handler implements Architecture {

    /**
     * Instance Manager.
     */
    private InstanceManager m_manager;

    /**
     * Service Registration of the Architecture service provided by this handler.
     */
    private ServiceRegistration m_sr;

    /**
     * Name of the component.
     */
    private String m_name;

    /**
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element)
     */
    public void configure(InstanceManager im, Element metadata, Dictionary configuration) {
        if (metadata.containsAttribute("architecture")) {
            String isArchitectureEnabled = (metadata.getAttribute("architecture")).toLowerCase();
            if (isArchitectureEnabled.equalsIgnoreCase("true")) { im.register(this); }
        }

        m_name = (String) configuration.get("name");

        m_manager = im;
    }

    /**
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        try {
            if (m_sr != null) { m_sr.unregister(); m_sr = null; }
        } catch (Exception e) { return; }
    }

    /**
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Unregister the service if already registred
        if (m_sr != null) { m_sr.unregister(); }

        // Register the ManagedService
        BundleContext bc = m_manager.getContext();
        Dictionary properties = new Properties();
        properties.put("Component.Implementation.Class", m_manager.getClassName());
        properties.put(Constants.SERVICE_PID, m_name);

        m_sr = bc.registerService(Architecture.class.getName(), this, properties);

    }

    /**
     * @see org.apache.felix.ipojo.architecture.Architecture#getComponentDescription()
     */
    public InstanceDescription getInstanceDescription() {
        return m_manager.getInstanceDescription();
    }

}
