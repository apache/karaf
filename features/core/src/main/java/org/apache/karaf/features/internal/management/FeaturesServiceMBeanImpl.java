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
package org.apache.karaf.features.internal.management;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;
import javax.management.openmbean.TabularData;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.management.FeaturesServiceMBean;
import org.apache.karaf.features.management.codec.JmxFeature;
import org.apache.karaf.features.management.codec.JmxFeatureEvent;
import org.apache.karaf.features.management.codec.JmxRepository;
import org.apache.karaf.features.management.codec.JmxRepositoryEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Implementation of {@link FeaturesServiceMBean}.
 */
public class FeaturesServiceMBeanImpl extends StandardEmitterMBean implements
        MBeanRegistration, FeaturesServiceMBean {

    private ServiceRegistration<FeaturesListener> registration;

    private BundleContext bundleContext;

    private ObjectName objectName;

    private volatile long sequenceNumber;

    private FeaturesService featuresService;

    public FeaturesServiceMBeanImpl() throws NotCompliantMBeanException {
        super(FeaturesServiceMBean.class,
              new NotificationBroadcasterSupport(getBroadcastInfo()));
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        objectName = name;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        registration = bundleContext.registerService(FeaturesListener.class,
                getFeaturesListener(), new Hashtable<String, String>());
    }

    public void preDeregister() throws Exception {
        registration.unregister();
    }

    public void postDeregister() {
    }

    @Override
    public TabularData getFeatures() throws Exception {
        try {
            List<Feature> allFeatures = Arrays.asList(featuresService.listFeatures());
            List<Feature> insFeatures = Arrays.asList(featuresService.listInstalledFeatures());
            ArrayList<JmxFeature> features = new ArrayList<>();
            for (Feature feature : allFeatures) {
                try {
                    features.add(new JmxFeature(feature, insFeatures.contains(feature), featuresService.isRequired(feature)));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return JmxFeature.tableFrom(features);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public TabularData getRepositories() throws Exception {
        try {
            List<Repository> allRepositories = Arrays.asList(featuresService.listRepositories());
            ArrayList<JmxRepository> repositories = new ArrayList<>();
            for (Repository repository : allRepositories) {
                try {
                    repositories.add(new JmxRepository(repository));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return JmxRepository.tableFrom(repositories);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public TabularData repositoryProvidedFeatures(String uri) throws Exception {
        return infoFeature(featuresService.repositoryProvidedFeatures(new URI(uri)));
    }

    @Override
    public void addRepository(String uri) throws Exception {
        URI repoUri = new URI(uri);
        if (featuresService.isRepositoryUriBlacklisted(repoUri)) {
            throw new RuntimeException("Feature URL " + uri + " is blacklisted");
        }
        featuresService.addRepository(repoUri);
    }

    @Override
    public void addRepository(String uri, boolean install) throws Exception {
        URI repoUri = new URI(uri);
        if (featuresService.isRepositoryUriBlacklisted(repoUri)) {
            throw new RuntimeException("Feature URL " + uri + " is blacklisted");
        }
        featuresService.addRepository(repoUri, install);
    }

    @Override
    public void removeRepository(String uri) throws Exception {
        removeRepository(uri, false);
    }

    @Override
    public void removeRepository(String uri, boolean uninstall) throws Exception {
        List<URI> uris = new ArrayList<>();
        Pattern pattern = Pattern.compile(uri);
        for (Repository repository : featuresService.listRepositories()) {
            if (repository.getName() != null && !repository.getName().isEmpty()) {
                // first regex on the repository name
                Matcher nameMatcher = pattern.matcher(repository.getName());
                if (nameMatcher.matches()) {
                    uris.add(repository.getURI());
                } else {
                    // fallback to repository URI regex
                    Matcher uriMatcher = pattern.matcher(repository.getURI().toString());
                    if (uriMatcher.matches()) {
                        uris.add(repository.getURI());
                    }
                }
            } else {
                // repository name is not defined, fallback to repository URI regex
                Matcher uriMatcher = pattern.matcher(repository.getURI().toString());
                if (uriMatcher.matches()) {
                    uris.add(repository.getURI());
                }
            }
        }
        for (URI u : uris) {
            featuresService.removeRepository(u, uninstall);
        }
    }

    @Override
    public void refreshRepository(String uri) throws Exception {
        if (uri == null || uri.isEmpty()) {
            for (Repository repository : featuresService.listRepositories()) {
                featuresService.refreshRepository(repository.getURI());
            }
        } else {
            // regex support
            Pattern pattern = Pattern.compile(uri);
            List<URI> uris = new ArrayList<>();
            for (Repository repository : featuresService.listRepositories()) {
                URI u = repository.getURI();
                Matcher matcher = pattern.matcher(u.toString());
                if (matcher.matches()) {
                    uris.add(u);
                }
            }
            for (URI u :uris) {
                featuresService.refreshRepository(u);
            }
        }
    }

    @Override
    public void installFeature(String name) throws Exception {
        featuresService.installFeature(name);
    }

    @Override
    public void installFeature(String name, boolean noRefresh) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles);
        }
        featuresService.installFeature(name, options);
    }

    @Override
    public void installFeature(String name, boolean noRefresh, boolean noStart) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (noStart) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoStartBundles);
        }
        featuresService.installFeature(name, options);
    }

    @Override
    public void installFeature(String name, String version) throws Exception {
        featuresService.installFeature(name, version);
    }

    @Override
    public void installFeature(String name, String version, boolean noRefresh) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles);
        }
        featuresService.installFeature(name, version, options);
    }

    @Override
    public void installFeature(String name, String version, boolean noRefresh, boolean noStart) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (noStart) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoStartBundles);
        }
        featuresService.installFeature(name, version, options);
    }

    @Override
    public TabularData infoFeature(String name) throws Exception {
        try {
            Feature[] features = featuresService.getFeatures(name);
            return infoFeature(features);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public TabularData infoFeature(String name, String version) throws Exception {
        try {
            Feature[] features = featuresService.getFeatures(name, version);
            return infoFeature(features);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private TabularData infoFeature(Feature[] f) throws Exception {
        ArrayList<JmxFeature> features = new ArrayList<>();
        for (Feature feature:f) {
            boolean installed = featuresService.isInstalled(feature);
            boolean required = featuresService.isRequired(feature);
            JmxFeature jmxFeature = new JmxFeature(feature, installed, required);
            features.add(jmxFeature);
        }
        return JmxFeature.tableFrom(features);
    }

    @Override
    public void uninstallFeature(String name) throws Exception {
        featuresService.uninstallFeature(name);
    }

    @Override
    public void uninstallFeature(String name, boolean noRefresh) throws Exception {
        uninstallFeature(name, noRefresh, false);
    }

    @Override
    public void uninstallFeature(String name, boolean noRefresh, boolean deleteConfigurations) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (deleteConfigurations) {
            options.add(org.apache.karaf.features.FeaturesService.Option.DeleteConfigurations);
        }
        featuresService.uninstallFeature(name, options);
    }

    @Override
    public void uninstallFeature(String name, String version) throws Exception {
        featuresService.uninstallFeature(name, version);
    }

    @Override
    public void uninstallFeature(String name, String version, boolean noRefresh) throws Exception {
        uninstallFeature(name, version, noRefresh, false);
    }

    @Override
    public void uninstallFeature(String name, String version, boolean noRefresh, boolean deleteConfigurations) throws Exception {
        EnumSet<org.apache.karaf.features.FeaturesService.Option> options = EnumSet.noneOf(org.apache.karaf.features.FeaturesService.Option.class);
        if (noRefresh) {
            options.add(FeaturesService.Option.NoAutoRefreshBundles);
        }
        if (deleteConfigurations) {
            options.add(FeaturesService.Option.DeleteConfigurations);
        }
        featuresService.uninstallFeature(name, version, options);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public FeaturesListener getFeaturesListener() {
        return new FeaturesListener() {
            public void featureEvent(FeatureEvent event) {
                if (!event.isReplay()) {
                    Notification notification = new Notification(FEATURE_EVENT_TYPE, objectName, sequenceNumber++);
                    notification.setUserData(new JmxFeatureEvent(event).asCompositeData());
                    sendNotification(notification);
                }
            }

            public void repositoryEvent(RepositoryEvent event) {
                if (!event.isReplay()) {
                    Notification notification = new Notification(REPOSITORY_EVENT_TYPE, objectName, sequenceNumber++);
                    notification.setUserData(new JmxRepositoryEvent(event).asCompositeData());
                    sendNotification(notification);
                }
            }

        };
    }

    private static MBeanNotificationInfo[] getBroadcastInfo() {
        String type = Notification.class.getCanonicalName();
        MBeanNotificationInfo info1 = new MBeanNotificationInfo(new String[]{FEATURE_EVENT_EVENT_TYPE},
                type, "Some features notification");
        MBeanNotificationInfo info2 = new MBeanNotificationInfo(new String[]{REPOSITORY_EVENT_EVENT_TYPE},
                type, "Some repository notification");
        return new MBeanNotificationInfo[]{info1, info2};
    }

}
