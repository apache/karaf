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
package org.apache.karaf.management.mbeans.obr.internal;

import org.apache.felix.bundlerepository.*;
import org.apache.karaf.management.mbeans.obr.ObrMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the OBR MBean.
 */
public class ObrMBeanImpl extends StandardMBean implements ObrMBean {

    private static final char VERSION_DELIM = ',';

    private BundleContext bundleContext;
    private RepositoryAdmin repositoryAdmin;

    public ObrMBeanImpl() throws NotCompliantMBeanException {
        super(ObrMBean.class);
    }

    public List<String> getUrls() throws Exception {
        Repository[] repositories = repositoryAdmin.listRepositories();
        List<String> urls = new ArrayList<String>();
        for (int i = 0; i < repositories.length; i++) {
            urls.add(repositories[i].getURI());
        }
        return urls;
    }

    /**
     * @deprecated use getUrls() instead.
     */
    public List<String> listUrls() throws Exception {
        return getUrls();
    }

    public void addUrl(String url) throws Exception {
        repositoryAdmin.addRepository(url);
    }

    public void removeUrl(String url) throws Exception {
        repositoryAdmin.removeRepository(url);
    }

    public void refreshUrl(String url) throws Exception {
        repositoryAdmin.addRepository(url);
    }

    public TabularData getBundles() throws Exception {
        CompositeType bundleType = new CompositeType("OBR Resource", "Bundle available in the OBR",
                new String[]{"presentationname", "symbolicname", "version"},
                new String[]{"Presentation Name", "Symbolic Name", "Version"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("OBR Resources", "Table of all resources/bundles available in the OBR",
                bundleType, new String[]{"presentationname"});
        TabularData table = new TabularDataSupport(tableType);

        Resource[] resources = repositoryAdmin.discoverResources("(|(presentationname=*)(symbolicname=*))");
        for (int i = 0; i < resources.length; i++) {
            try {
                CompositeData data = new CompositeDataSupport(bundleType,
                        new String[]{"presentationname", "symbolicname", "version"},
                        new Object[]{resources[i].getPresentationName(), resources[i].getSymbolicName(), resources[i].getVersion()});
                table.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return table;
    }

    /**
     * @deprecated use getBundles() instead.
     */
    public TabularData list() throws Exception {
        return getBundles();
    }

    public void deploy(String bundle) throws Exception {
        deploy(bundle, false, false);
    }

    public void deploy(String bundle, boolean start, boolean deployOptional) throws Exception {
        Resolver resolver = repositoryAdmin.resolver();
        String[] target = getTarget(bundle);
        Resource resource = selectNewestVersion(searchRepository(repositoryAdmin, target[0], target[1]));
        if (resource == null) {
            throw new IllegalArgumentException("Unknown bundle " + target[0]);
        }
        resolver.add(resource);
        if ((resolver.getAddedResources() != null) &&
                (resolver.getAddedResources().length > 0)) {
            if (resolver.resolve(deployOptional ? 0 : Resolver.NO_OPTIONAL_RESOURCES)) {
                try {
                    resolver.deploy(start ? Resolver.START : 0);
                } catch (IllegalStateException ex) {
                    throw new IllegalStateException("Can't deploy using OBR", ex);
                }
            }
        }
    }

    private Resource[] searchRepository(RepositoryAdmin admin, String targetId, String targetVersion) throws InvalidSyntaxException {
        // Try to see if the targetId is a bundle ID.
        try {
            Bundle bundle = getBundleContext().getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        } catch (NumberFormatException ex) {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null) {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return admin.discoverResources(sb.toString());
    }

    private Resource selectNewestVersion(Resource[] resources) {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++) {
            if (i == 0) {
                idx = 0;
                v = resources[i].getVersion();
            } else {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0) {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    private String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[]{bundle.substring(0, idx), bundle.substring(idx + 1)};
        } else {
            target = new String[]{bundle, null};
        }
        return target;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public RepositoryAdmin getRepositoryAdmin() {
        return this.repositoryAdmin;
    }

    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        this.repositoryAdmin = repositoryAdmin;
    }

}
