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
package org.apache.karaf.bundle.core.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundlesMBean;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundlesMBean implementation.
 */
public class BundlesMBeanImpl extends StandardMBean implements BundlesMBean {

    private Logger LOG = LoggerFactory.getLogger(BundlesMBeanImpl.class);

    private BundleContext bundleContext;
    private final BundleService bundleService;

    public BundlesMBeanImpl(BundleContext bundleContext, BundleService bundleService) throws NotCompliantMBeanException {
        super(BundlesMBean.class);
        this.bundleContext = bundleContext;
        this.bundleService = bundleService;
    }

    private List<Bundle> selectBundles(String id) throws Exception {
        List<String> ids = Collections.singletonList(id);
        return this.bundleService.selectBundles(ids, false);
    }

    public TabularData getBundles() throws MBeanException {
        try {
            CompositeType bundleType = new CompositeType("Bundle", "OSGi Bundle",
                    new String[]{"ID", "Name", "Symbolic Name", "Version", "Start Level", "State", "Update Location"},
                    new String[]{"ID of the Bundle", "Name of the Bundle", "Symbolic Name of the Bundle", "Version of the Bundle", "Start Level of the Bundle", "Current State of the Bundle", "Update location of the Bundle"},
                    new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("Bundles", "Tables of all bundles", bundleType, new String[]{"ID"});
            TabularData table = new TabularDataSupport(tableType);

            Bundle[] bundles = bundleContext.getBundles();

            for (Bundle bundle : bundles) {
                try {
                    BundleInfo info = bundleService.getInfo(bundle);
                    String bundleStateString = info.getState().toString();
                    CompositeData data = new CompositeDataSupport(bundleType,
                            new String[]{"ID", "Name", "Symbolic Name", "Version", "Start Level", "State", "Update Location"},
                            new Object[]{info.getBundleId(), info.getName(), info.getSymbolicName(), info.getVersion(), info.getStartLevel(), bundleStateString, info.getUpdateLocation()});
                    table.put(data);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public int getStartLevel(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            if (bundles.size() != 1) {
                throw new IllegalArgumentException("Provided bundle Id doesn't return any bundle or more than one bundle selected");
            }

            return getBundleStartLevel(bundles.get(0)).getStartLevel();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void setStartLevel(String bundleId, int bundleStartLevel) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            for (Bundle bundle : bundles) {
                getBundleStartLevel(bundle).setStartLevel(bundleStartLevel);
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    private BundleStartLevel getBundleStartLevel(Bundle bundle) {
        return bundle.adapt(BundleStartLevel.class);
    }

    public void refresh() throws MBeanException {
        getFrameworkWiring().refreshBundles(null);
    }

    public void refresh(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);
            if (bundles.isEmpty()) {
                getFrameworkWiring().refreshBundles(null);
            } else {
                getFrameworkWiring().refreshBundles(bundles);
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void update(String bundleId) throws MBeanException {
        update(bundleId, null, false);
    }

    public void update(String bundleId, boolean refresh) throws MBeanException {
        update(bundleId, null, refresh);
    }

    public void update(String bundleId, String location) throws MBeanException {
        update(bundleId, location, false);
    }

    public void update(String bundleId, String location, boolean refresh) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            if (location == null) {
                for (Bundle bundle : bundles) {
                    bundle.update();
                }
                return;
            }

            if (bundles.size() != 1) {
                throw new IllegalArgumentException("Provided bundle Id doesn't return any bundle or more than one bundle selected");
            }

            InputStream is = new URL(location).openStream();
            bundles.get(0).update(is);

            if (refresh) {
                FrameworkWiring wiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
                wiring.refreshBundles(null);
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void resolve() throws MBeanException {
        getFrameworkWiring().resolveBundles(null);
    }

    public void resolve(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);
            getFrameworkWiring().resolveBundles(bundles);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    private FrameworkWiring getFrameworkWiring() {
        return getBundleContext().getBundle(0).adapt(FrameworkWiring.class);
    }

    public void restart(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            for (Bundle bundle : bundles) {
                bundle.stop();
                bundle.start();
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public long install(String url) throws MBeanException {
        return install(url, false);
    }

    public long install(String url, boolean start) throws MBeanException {
        try {
            Bundle bundle = bundleContext.installBundle(url, null);
            if (start) {
                bundle.start();
            }
            return bundle.getBundleId();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void start(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            for (Bundle bundle : bundles) {
                bundle.start();
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void stop(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            for (Bundle bundle : bundles) {
                bundle.stop();
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void uninstall(String bundleId) throws MBeanException {
        try {
            List<Bundle> bundles = selectBundles(bundleId);

            for (Bundle bundle : bundles) {
                bundle.uninstall();
            }
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public TabularData getDiag() throws MBeanException {
        try {
            CompositeType diagType = new CompositeType("Diag", "OSGi Bundle Diag",
                    new String[]{"Name", "Status", "Diag"},
                    new String[]{"Bundle Name", "Current Status", "Diagnostic"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("Diagnostics", "Tables of all bundles diagnostic", diagType, new String[]{"Name"});
            TabularData table = new TabularDataSupport(tableType);

            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                BundleInfo bundleInfo = bundleService.getInfo(bundle);
                String name = ShellUtil.getBundleName(bundle);
                String status = bundleInfo.getState().toString();
                String diag = bundleService.getDiag(bundle);
                CompositeData data = new CompositeDataSupport(diagType,
                        new String[]{"Name", "Status", "Diag"},
                        new Object[]{name, status, diag});
                table.put(data);
            }

            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public String getDiag(long bundleId) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new RuntimeException("Bundle with id " + bundleId + " not found");
        }
        return bundleService.getDiag(bundle);
    }

    @Override
    public String getStatus(String bundleId) throws MBeanException {
        try {
            return bundleService.getStatus(bundleId);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

}
