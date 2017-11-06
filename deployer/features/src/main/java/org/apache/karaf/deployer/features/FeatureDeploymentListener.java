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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A deployment listener able to hot deploy a feature descriptor
 */
public class FeatureDeploymentListener implements ArtifactUrlTransformer, BundleListener {

    public static final String FEATURE_PATH = "org.apache.karaf.shell.features";

    private final Logger logger = LoggerFactory.getLogger(FeatureDeploymentListener.class);

    private DocumentBuilderFactory dbf;
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
                InputStream input = new FileInputStream(file);
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            }
        }
    }

    private void saveProperties() throws IOException {
        File file = getPropertiesFile();
        if (file != null) {
            OutputStream output = new FileOutputStream(file);
            try {
                properties.store(output, null);
            } finally {
                output.close();
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

    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("features".equals(name) ) {
                	if(isKnownFeaturesURI(uri)){
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

    public void bundleChanged(BundleEvent bundleEvent) {
            Bundle bundle = bundleEvent.getBundle();
            if (bundleEvent.getType() == BundleEvent.RESOLVED) {
                try {
                    List<URI> toAdd = new ArrayList<>();
                    Enumeration<URL> featuresUrlEnumeration = bundle.findEntries("/META-INF/" + FEATURE_PATH + "/", "*.xml", false);
                    while (featuresUrlEnumeration != null && featuresUrlEnumeration.hasMoreElements()) {
                        URI uri = featuresUrlEnumeration.nextElement().toURI();
                        toAdd.add(uri);
                    }
                    Set<Repository> toRemove = new HashSet<>();
                    synchronized (this) {
                        String prefix = bundle.getSymbolicName() + "-" + bundle.getVersion();
                        String countStr = (String) properties.remove(prefix + ".count");
                        if (countStr != null) {
                            int count = Integer.parseInt(countStr);
                            for (int i = 0; i < count; i++) {
                                URI uri = new URI((String) properties.remove(prefix + ".url." + i));
                                if (!toAdd.contains(uri)) {
                                    toRemove.add(featuresService.getRepository(uri));
                                }
                            }
                        }
                    }
                    if (toRemove.isEmpty() && toAdd.isEmpty()) {
                        return;
                    }
                    // Now add and remove repositories
                    try {
                        for (Repository repo : toRemove) {
                            featuresService.removeRepository(repo.getURI(), true);
                        }
                        Set<String> featuresToInstall = new HashSet<>();
                        for (URI uri : toAdd) {
                            featuresService.addRepository(uri, false);
                            Repository repo = featuresService.getRepository(uri);
                            for (Feature f : repo.getFeatures()) {
                                if (Feature.DEFAULT_INSTALL_MODE.equals(f.getInstall())) {
                                    featuresToInstall.add(f.getId());
                                }
                            }
                        }
                        if (!featuresToInstall.isEmpty()) {
                            featuresService.installFeatures(featuresToInstall, EnumSet.noneOf(FeaturesService.Option.class));
                        }
                    } catch (Exception e) {
                        logger.error("Unable to install features", e);
                    }
                    synchronized (this) {
                        String prefix = bundle.getSymbolicName() + "-" + bundle.getVersion();
                        String old = (String) properties.get(prefix + ".count");
                        if (old != null && toAdd.isEmpty()) {
                            properties.remove(prefix + ".count");
                            saveProperties();
                        } else if (!toAdd.isEmpty()) {
                            properties.put(prefix + ".count", Integer.toString(toAdd.size()));
                            for (int i = 0; i < toAdd.size(); i++) {
                                properties.put(prefix + ".url." + i, toAdd.get(i).toString());
                            }
                            saveProperties();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to install deployed features for bundle: " + bundle.getSymbolicName() + " - " + bundle.getVersion(), e);
                }
            } else if (bundleEvent.getType() == BundleEvent.UNINSTALLED) {
                try {
                    synchronized (this) {
                        String prefix = bundle.getSymbolicName() + "-" + bundle.getVersion();
                        String countStr = (String) properties.remove(prefix + ".count");
                        if (countStr != null) {
                            int count = Integer.parseInt(countStr);
                            for (int i = 0; i < count; i++) {
                                URL url = new URL((String) properties.remove(prefix + ".url." + i));
                                for (Repository repo : featuresService.listRepositories()) {
                                    try {
                                        if (repo.getURI().equals(url.toURI())) {
                                            for (Feature f : repo.getFeatures()) {
                                                try {
                                                    featuresService.uninstallFeature(f.getName(), f.getVersion());
                                                } catch (Exception e) {
                                                    logger.error("Unable to uninstall feature: " + f.getName(), e);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Unable to uninstall features: " + url, e);
                                    }
                                }
                                try {
                                    featuresService.removeRepository(url.toURI());
                                } catch (URISyntaxException e) {
                                    logger.error("Unable to remove repository: " + url, e);
                                }
                            }
                            saveProperties();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to uninstall deployed features for bundle: " + bundle.getSymbolicName() + " - " + bundle.getVersion(), e);
                }
            }
    }

    protected Document parse(File artifact) throws Exception {
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
            }
            public void error(SAXParseException exception) throws SAXException {
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return db.parse(artifact);
    }

}
