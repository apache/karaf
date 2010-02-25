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
package org.apache.felix.bundlerepository;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.obr.Resource;

public class LocalResourceImpl extends ResourceImpl
{
    private Bundle m_bundle = null;

    LocalResourceImpl(Bundle bundle) throws InvalidSyntaxException
    {
        this(null, bundle);
    }

    LocalResourceImpl(ResourceImpl resource, Bundle bundle)
        throws InvalidSyntaxException
    {
        super(resource);
        m_bundle = bundle;
        initialize();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    private void initialize() throws InvalidSyntaxException
    {
        Dictionary dict = m_bundle.getHeaders();

        // Convert bundle manifest header attributes to resource properties.
        convertAttributesToProperties(dict);

        // Convert properties to bundle capability
        convertAttributesToBundleCapability();

        // Convert import package declarations into requirements.
        convertImportPackageToRequirement(dict);

        // Convert import service declarations into requirements.
        convertImportServiceToRequirement(dict);

        // Convert export package declarations into capabilities.
        convertExportPackageToCapability(dict);

        // Convert export service declarations and services into capabilities.
        convertExportServiceToCapability(dict, m_bundle);

        // For the system bundle, add a special platform capability.
        if (m_bundle.getBundleId() == 0)
        {
            // set the execution environment(s) as Capability ee of the
            // system bundle to resolve bundles with specifc requirements
            String ee = m_bundle.getBundleContext().getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
            if (ee != null)
            {
                StringTokenizer tokener = new StringTokenizer(ee, ",");
                List eeList = new ArrayList();
                while (tokener.hasMoreTokens())
                {
                    String eeName = tokener.nextToken().trim();
                    if (eeName.length() > 0)
                    {
                        eeList.add(eeName);
                    }
                }
                CapabilityImpl cap = new CapabilityImpl();
                cap.setName("ee");
                cap.addP("ee", eeList);
                addCapability(cap);
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

    private void convertAttributesToProperties(Dictionary dict)
    {
        for (Enumeration keys = dict.keys(); keys.hasMoreElements(); )
        {
            String key = (String) keys.nextElement();
            if (key.equalsIgnoreCase(Constants.BUNDLE_SYMBOLICNAME))
            {
                String sn = (String) dict.get(key);
                sn = sn.trim();
                int index = sn.indexOf(";singleton:=true");
                if (index != -1) {
                    sn = sn.substring(0, index);
                }
                put(Resource.SYMBOLIC_NAME, sn);
            }
            else if (key.equalsIgnoreCase(Constants.BUNDLE_NAME))
            {
                put(Resource.PRESENTATION_NAME, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase(Constants.BUNDLE_VERSION))
            {
                put(Resource.VERSION, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase("Bundle-Source"))
            {
                put(Resource.SOURCE_URL, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase(Constants.BUNDLE_DESCRIPTION))
            {
                put(Resource.DESCRIPTION, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase(Constants.BUNDLE_DOCURL))
            {
                put(Resource.DOCUMENTATION_URL, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase(Constants.BUNDLE_COPYRIGHT))
            {
                put(Resource.COPYRIGHT, (String) dict.get(key));
            }
            else if (key.equalsIgnoreCase("Bundle-License"))
            {
                put(Resource.LICENSE_URL, (String) dict.get(key));
            }
        }
    }

    private void convertAttributesToBundleCapability()
    {
        CapabilityImpl cap = new CapabilityImpl();
        cap.setName("bundle");
        if (getPresentationName() != null) {
            cap.addP(Resource.PRESENTATION_NAME, getPresentationName());
        }
        cap.addP(Resource.SYMBOLIC_NAME, getSymbolicName());
        cap.addP(Resource.VERSION, getVersion());
        addCapability(cap);
    }

    private void convertImportPackageToRequirement(Dictionary dict)
        throws InvalidSyntaxException
    {
        String target = (String) dict.get(Constants.IMPORT_PACKAGE);
        if (target != null)
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
            R4Import[] imports = new R4Import[pkgs.length];
            for (int i = 0; i < pkgs.length; i++)
            {
                imports[i] = new R4Import(pkgs[i]);
            }

            for (int impIdx = 0; impIdx < imports.length; impIdx++)
            {
                RequirementImpl req = new RequirementImpl();
                req.setMultiple("false");
                req.setOptional(Boolean.toString(imports[impIdx].isOptional()));
                req.setName("package");
                req.addText("Import package " + imports[impIdx].toString());

                String low = imports[impIdx].isLowInclusive()
                            ? "(version>=" + imports[impIdx].getVersion() + ")"
                            : "(!(version<=" + imports[impIdx].getVersion() + "))";

                if (imports[impIdx].getVersionHigh() != null)
                {
                    String high = imports[impIdx].isHighInclusive()
                        ? "(version<=" + imports[impIdx].getVersionHigh() + ")"
                        : "(!(version>=" + imports[impIdx].getVersionHigh() + "))";
                    req.setFilter("(&(package="
                        + imports[impIdx].getName() + ")"
                        + low + high + ")");
                }
                else
                {
                    req.setFilter(
                        "(&(package="
                        + imports[impIdx].getName() + ")"
                        + low + ")");
                }

                addRequire(req);
            }
        }
    }

    private void convertImportServiceToRequirement(Dictionary dict)
        throws InvalidSyntaxException
    {
        String target = (String) dict.get(Constants.IMPORT_SERVICE);
        if (target != null)
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
            for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
            {
                RequirementImpl req = new RequirementImpl();
                req.setMultiple("false");
                req.setName("service");
                req.addText("Import service " + pkgs[pkgIdx].toString());
                req.setFilter("(service="
                    + pkgs[pkgIdx].getName() + ")");
                addRequire(req);
            }
        }
    }

    private void convertExportPackageToCapability(Dictionary dict)
    {
        String target = (String) dict.get(Constants.EXPORT_PACKAGE);
        if (target != null)
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
            for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
            {
                CapabilityImpl cap = new CapabilityImpl();
                cap.setName("package");
                cap.addP(new PropertyImpl("package", null, pkgs[pkgIdx].getName()));
                cap.addP(new PropertyImpl("version", "version", pkgs[pkgIdx].getVersion().toString()));
                for (int i = 0; i < pkgs[pkgIdx].getAttributes().length; i++)
                {
                    R4Attribute attribute = pkgs[pkgIdx].getAttributes()[i];
                    String key = attribute.getName();
                    if (!key.equalsIgnoreCase("specification-version")
                        && !key.equalsIgnoreCase("version"))
                    {
                        Object value = attribute.getValue();
                        cap.addP(key, value);
                    }
                }
                for (int i = 0; i < pkgs[pkgIdx].getDirectives().length; i++)
                {
                    R4Directive directive = pkgs[pkgIdx].getDirectives()[i];
                    String key = directive.getName() + ":";
                    Object value = directive.getValue();
                    cap.addP(key, value);
                }
                addCapability(cap);
            }
        }
    }

    private void convertExportServiceToCapability(Dictionary dict, Bundle bundle)
    {
        Set services = new HashSet();

        // collect Export-Service
        String target = (String) dict.get(Constants.EXPORT_SERVICE);
        if (target != null)
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
            for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
            {
                services.add(pkgs[pkgIdx].getName());
            }
        }

        // add actual registered services
        ServiceReference[] refs = bundle.getRegisteredServices();
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            String[] cls = (String[]) refs[i].getProperty(Constants.OBJECTCLASS);
            for (int j = 0; cls != null && j < cls.length; j++)
            {
                services.add(cls[j]);
            }
        }

        // register capabilities for combined set
        for (Iterator si = services.iterator(); si.hasNext();)
        {
            CapabilityImpl cap = new CapabilityImpl();
            cap.setName("service");
            cap.addP(new PropertyImpl("service", null, (String) si.next()));
            addCapability(cap);
        }
    }

    public String toString()
    {
        return m_bundle.toString();
    }
}
