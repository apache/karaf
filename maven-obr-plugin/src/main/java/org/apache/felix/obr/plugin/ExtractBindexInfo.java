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
package org.apache.felix.obr.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.CapabilityImpl;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.RequirementImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.VersionRange;

/**
 * this class is used to configure bindex and get information built by bindex about targeted bundle.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ExtractBindexInfo {

    /**
     * attribute get from bindex which describe targeted resource.
     */
    private ResourceImpl m_resource;

    /**
     * configure bindex and build information.
     * @param repoFilename URI on OBR descriptor file
     * @param outFile path on targeted jar-file
     * @throws MojoExecutionException occurs if bindex configuration failed
     */
    public ExtractBindexInfo(URI repoFilename, String outFile) throws MojoExecutionException {

        this.m_resource = null;
        RepositoryImpl repository = null;
        try {
            repository = new RepositoryImpl(new File(repoFilename).getAbsoluteFile().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new MojoExecutionException("MalformedURLException");
        }
        BundleInfo info = null;
        try {
            info = new BundleInfo(repository, new File(outFile));
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Exception");
        }

        try {
            m_resource = info.build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Exception");
        }
    }

    /**
     * transform logical operator in xml syntax.
     * @param filter string which contains logical operator
     * @return string in correct xml syntax
     */
    private String parseFilter(String filter) {
        filter.replaceAll("&", "&amp");
        filter.replaceAll(">=", "&gt");

        return filter;
    }

    /**
     * extract capabilities from bindex information.
     * @return bundle capabilities List
     */
    public List getCapabilities() {
        List list = new ArrayList();
        Collection res = m_resource.getCapabilityList();
        Iterator it = res.iterator();
        while (it.hasNext()) {
            Capability capability = new Capability();
            CapabilityImpl ci = (CapabilityImpl) it.next();
            capability.setName(ci.getName());
            // System.out.println(ci.getName()) ;
            if (!(ci.getName().compareTo("bundle") == 0)) {
                Map properties = ci.getProperties();
                for (Iterator k = properties.keySet().iterator(); k.hasNext();) {
                    PElement p = new PElement();
                    String key = (String) k.next();
                    List values = (List) properties.get(key);
                    for (Iterator v = values.iterator(); v.hasNext();) {
                        Object value = v.next();
                        p.setN(key);
                        if (value != null) {
                            p.setV(value.toString());
                        } else {
                            System.out.println("Missing value " + key);
                        }
                        String type = null;
                        if (value instanceof Number) {
                            type = "number";
                        } else { 
                            if (value.getClass() == VersionRange.class) { type = "version"; }
                        }
                        if (type != null) {
                            p.setT(type);
                        }
                    }
                    capability.addP(p);
                }

                list.add(capability);
            }
        }
        return list;
    }

    /**
     * extract requirement from bindex information.
     * @return bundle requirement List
     */
    public List getRequirement() {
        List list = new ArrayList();
        Collection res = m_resource.getRequirementList();
        Iterator it = res.iterator();
        while (it.hasNext()) {
            RequirementImpl ci = (RequirementImpl) it.next();
            Require require = new Require();

            require.setExtend(String.valueOf(ci.isExtend()));
            require.setMultiple(String.valueOf(ci.isMultiple()));
            require.setOptional(String.valueOf(ci.isOptional()));
            require.setName(ci.getName());
            require.setFilter(this.parseFilter(ci.getFilter()));
            require.setValue(ci.getComment());
            list.add(require);
        }
        return list;
    }

    /**
     * extract symbolic name from bindex information.
     * @return bundle symbolic name
     */
    public String getSymbolicName() {
        return m_resource.getSymbolicName();
    }

    /**
     * extract version from bindex information.
     * @return bundle version
     */
    public String getVersion() {
        if (m_resource.getVersion() != null) {
            return m_resource.getVersion().toString();
        } else {
            return null;
        }
    }

    /**
     * extract presentation name from bindex information.
     * @return bundle presentation name
     */
    public String getPresentationName() {
        return m_resource.getPresentationName();
    }

    /**
     * extract copyright from bindex information.
     * @return bundle copyright
     */
    public String getCopyright() {
        return m_resource.getCopyright();
    }

    /**
     * extract description from bindex information.
     * @return bundle description
     */
    public String getDescription() {
        return m_resource.getDescription();
    }

    /**
     * extract documentation from bindex information.
     * @return bundle documentation
     */
    public String getDocumentation() {
        if (m_resource.getDocumentation() != null) {
            return m_resource.getDocumentation().toString();
        } else {
            return null;
        }
    }

    /**
     * extract license from bindex information.
     * @return bundle license
     */
    public String getLicense() {
        if (m_resource.getLicense() != null) {
            return m_resource.getLicense().toString();
        } else {
            return null;
        }
    }

    /**
     * extract source from bindex information.
     * @return bundle source
     */
    public String getSource() {
        if (m_resource.getSource() != null) {
            return m_resource.getSource().toString();
        } else {
            return null;
        }
    }
    
    /**
     * extract source from bindex information.
     * @return bundle source
     */
    public String getId() {
        if (m_resource.getId() != null) {
            return m_resource.getId();
        } else {
            return null;
        }
    }

}
