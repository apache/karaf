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
package org.apache.karaf.deployer.features;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import static org.apache.karaf.features.FeaturesService.ROOT_REGION;

/**
 * A deployment listener able to hot deploy a feature descriptor
 */
public class FeatureDeploymentListener implements ArtifactUrlTransformer, BundleListener {

    public static final String FEATURE_PATH = "org.apache.karaf.shell.features";

    private final Logger logger = LoggerFactory.getLogger(FeatureDeploymentListener.class);

    private XMLInputFactory xif;
    private FeaturesService featuresService;
    private BundleContext bundleContext;
    private Properties properties = new Properties();

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws Exception {
        bundleContext.addBundleListener(this);
        loadProperties();
        // Scan bundles
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.STARTING
                    || bundle.getState() == Bundle.ACTIVE)
            bundleChanged(new BundleEvent(BundleEvent.RESOLVED, bundle));
        }
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
    }

    private boolean isKnownFeaturesURI(String uri){
    	if(uri == null){
    		return false;
    	}
    	for (String ns : FeaturesNamespaces.SUPPORTED_URIS) {
            if (ns.equalsIgnoreCase(uri)){
                return true;
            }
        }
        return false;
    }

    private void loadProperties() throws IOException {
        // Load properties
        File file = getPropertiesFile();
        if (file != null) {
            if (file.exists()) {
                try (InputStream input = new FileInputStream(file)) {
                    properties.load(input);
                }
            }
        }
    }

    private void saveProperties() throws IOException {
        File file = getPropertiesFile();
        if (file != null) {
            try (OutputStream output = new FileOutputStream(file)) {
                properties.store(output, null);
            }
        }
    }

    private File getPropertiesFile() {
        try {
            return bundleContext.getDataFile("FeatureDeploymentListener.cfg");
        } catch (Exception e){
            logger.debug("Unable to get FeatureDeploymentListener.cfg", e);
            return null;
        }
    }

    @Override
    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".json")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(artifact))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("\"feature\"")) {
                            return true;
                        }
                    }
                }
                return false;
            }
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                QName qname = getRootElementName(artifact);
                String name = qname.getLocalPart();
                String uri  = qname.getNamespaceURI();
                if ("features".equals(name) ) {
                	if (isKnownFeaturesURI(uri)){
                        return true;
                	} else {
                		logger.error("unknown features uri", new Exception("" + uri));
                	}
                }
            }
        } catch (Exception e) {
            logger.error("Unable to parse deployed file " + artifact.getAbsolutePath(), e);
        }
        return false;
    }

    public URL transform(URL artifact) {
        // We can't really install the feature right now and just return nothing.
        // We would not be aware of the fact that the bundle has been uninstalled
        // and therefore require the feature to be uninstalled.
        // So instead, create a fake bundle with the file inside, which will be listened by
        // this deployer: installation / uninstallation of the feature will be done
        // while the bundle is installed / uninstalled.
        try {
            return new URL("feature", null, artifact.toString());
        } catch (Exception e) {
            logger.error("Unable to build feature bundle", e);
            return null;
        }
    }

    public synchronized void bundleChanged(BundleEvent bundleEvent) {
        // Only handle resolved and uninstalled events
        if (bundleEvent.getType() != BundleEvent.RESOLVED
                && bundleEvent.getType() != BundleEvent.UNINSTALLED) {
            return;
        }
        Bundle bundle = bundleEvent.getBundle();
        try {
            // Remove previous informations
            List<URI> repsToRemove = new ArrayList<>();
            List<String> reqsToRemove = new ArrayList<>();
            // Remove old properties
            String prefix = "bundle." + bundle.getBundleId();
            String countStr = (String) properties.remove(prefix + ".reps.count");
            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                for (int i = 0; i < count; i++) {
                    String rep = (String) properties.remove(prefix + ".reps.item" + i);
                    repsToRemove.add(URI.create(rep));
                }
            }
            countStr = (String) properties.remove(prefix + ".reqs.count");
            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                for (int i = 0; i < count; i++) {
                    String req = (String) properties.remove(prefix + ".reqs.item" + i);
                    reqsToRemove.add(req);
                }
            }
            saveProperties();

            // Compute new information
            List<URI> repsToAdd = new ArrayList<>();
            List<String> reqsToAdd = new ArrayList<>();
            if (bundleEvent.getType() == BundleEvent.RESOLVED) {
                Enumeration featuresUrlEnumeration = bundle.findEntries("/META-INF/" + FEATURE_PATH + "/", "*.xml", false);
                while (featuresUrlEnumeration != null && featuresUrlEnumeration.hasMoreElements()) {
                    URL url = (URL) featuresUrlEnumeration.nextElement();
                    URI uri = url.toURI();
                    repsToAdd.add(uri);
                    Repository rep = featuresService.createRepository(uri);
                    Stream.of(rep.getFeatures())
                            .filter(f -> f.getInstall() == null || Feature.DEFAULT_INSTALL_MODE.equals(f.getInstall()))
                            .map(f -> "feature:" + f.getName() + "/" + new VersionRange(f.getVersion(), true))
                            .forEach(reqsToAdd::add);
                }
                if (!repsToAdd.isEmpty()) {
                    properties.put(prefix + ".reps.count", Integer.toString(repsToAdd.size()));
                    for (int i = 0; i < repsToAdd.size(); i++) {
                        properties.put(prefix + ".reps.item" + i, repsToAdd.get(i).toASCIIString());
                    }
                    properties.put(prefix + ".reqs.count", Integer.toString(reqsToAdd.size()));
                    for (int i = 0; i < reqsToAdd.size(); i++) {
                        properties.put(prefix + ".reqs.item" + i, reqsToAdd.get(i));
                    }
                }
            }
            saveProperties();

            // Call features service
            List<Repository> requiredRepos = Arrays.asList(featuresService.listRequiredRepositories());
            Set<URI> requiredReposUris = requiredRepos.stream()
                    .map(Repository::getURI).collect(Collectors.toSet());
            requiredReposUris.removeAll(repsToRemove);
            requiredReposUris.addAll(repsToAdd);

            Map<String, Set<String>> requirements = featuresService.listRequirements();
            requirements.get(ROOT_REGION).removeAll(reqsToRemove);
            requirements.get(ROOT_REGION).addAll(reqsToAdd);

            if (!reqsToRemove.isEmpty() || !reqsToAdd.isEmpty()) {
                featuresService.updateReposAndRequirements(requiredReposUris, requirements, EnumSet.noneOf(FeaturesService.Option.class));
            }
        } catch (Exception e) {
            logger.error("Unable to update deployed features for bundle: " + bundle.getSymbolicName() + " - " + bundle.getVersion(), e);
        }
    }

    private QName getRootElementName(File artifact) throws Exception {
        if (xif == null) {
            xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        }
        try (InputStream is = new FileInputStream(artifact)) {
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            sr.nextTag();
            return sr.getName();
        }
    }

}
