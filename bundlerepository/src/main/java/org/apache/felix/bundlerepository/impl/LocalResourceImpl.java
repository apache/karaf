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
package org.apache.felix.bundlerepository.impl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class LocalResourceImpl extends ResourceImpl
{
    private Bundle m_bundle = null;

    LocalResourceImpl(Bundle bundle) throws InvalidSyntaxException
    {
        m_bundle = bundle;
        initialize();
    }

    public boolean isLocal()
    {
        return true;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    private void initialize() throws InvalidSyntaxException
    {
        final Dictionary dict = m_bundle.getHeaders();

        DataModelHelperImpl.populate(new DataModelHelperImpl.Headers()
        {
            public String getHeader(String name)
            {
                return (String) dict.get(name);
            }
            public void close() { }
        }, this);

        // Convert export service declarations and services into capabilities.
        convertExportServiceToCapability(dict, m_bundle);

        // For the system bundle, add a special platform capability.
        if (m_bundle.getBundleId() == 0)
        {
            // set the execution environment(s) as Capability ee of the
            // system bundle to resolve bundles with specific requirements
            String ee = m_bundle.getBundleContext().getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
            if (ee != null)
            {
                StringTokenizer tokens = new StringTokenizer(ee, ",");
                while (tokens.hasMoreTokens()) {
                    CapabilityImpl cap = new CapabilityImpl(Capability.EXECUTIONENVIRONMENT);
                    cap.addProperty(Capability.EXECUTIONENVIRONMENT, tokens.nextToken().trim());
                    addCapability(cap);
                }
            }

/* TODO: OBR - Fix system capabilities.
            // Create a case-insensitive map.
            Map map = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
            map.put(
                Constants.FRAMEWORK_VERSION,
                m_context.getProperty(Constants.FRAMEWORK_VERSION));
            map.put(
                Constants.FRAMEWORK_VENDOR,
                m_context.getProperty(Constants.FRAMEWORK_VENDOR));
            map.put(
                Constants.FRAMEWORK_LANGUAGE,
                m_context.getProperty(Constants.FRAMEWORK_LANGUAGE));
            map.put(
                Constants.FRAMEWORK_OS_NAME,
                m_context.getProperty(Constants.FRAMEWORK_OS_NAME));
            map.put(
                Constants.FRAMEWORK_OS_VERSION,
                m_context.getProperty(Constants.FRAMEWORK_OS_VERSION));
            map.put(
                Constants.FRAMEWORK_PROCESSOR,
                m_context.getProperty(Constants.FRAMEWORK_PROCESSOR));
//                map.put(
//                    FelixConstants.FELIX_VERSION_PROPERTY,
//                    m_context.getProperty(FelixConstants.FELIX_VERSION_PROPERTY));
            Map[] capMaps = (Map[]) bundleMap.get("capability");
            if (capMaps == null)
            {
                capMaps = new Map[] { map };
            }
            else
            {
                Map[] newCaps = new Map[capMaps.length + 1];
                newCaps[0] = map;
                System.arraycopy(capMaps, 0, newCaps, 1, capMaps.length);
                capMaps = newCaps;
            }
            bundleMap.put("capability", capMaps);
*/
        }
    }

    private void convertExportServiceToCapability(Dictionary dict, Bundle bundle)
    {
        Set services = new HashSet();

        // add actual registered services
        ServiceReference[] refs = bundle.getRegisteredServices();
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            String[] cls = (String[]) refs[i].getProperty(Constants.OBJECTCLASS);
            for (int j = 0; cls != null && j < cls.length; j++)
            {
                CapabilityImpl cap = new CapabilityImpl();
                cap.setName(Capability.SERVICE);
                cap.addProperty(new PropertyImpl(Capability.SERVICE, null, cls[j]));
                // TODO: add service properties
                addCapability(cap);
            }
        }
        // TODO: check duplicates with service-export properties
    }

    public String toString()
    {
        return m_bundle.toString();
    }
}
