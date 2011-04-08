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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.features.Feature;
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
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.STARTING
                    || bundle.getState() == Bundle.ACTIVE)
            bundleChanged(new BundleEvent(BundleEvent.RESOLVED, bundle));
        }
    }

    public void destroy() throws Exception {
        bundleContext.removeBundleListener(this);
    }

    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("features".equals(name) && (uri == null || "".equals(uri)  || "http://karaf.apache.org/xmlns/features/v1.0.0".equalsIgnoreCase(uri))) {
                    return true;
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
                    List<URL> urls = new ArrayList<URL>();
                    Enumeration featuresUrlEnumeration = bundle.findEntries("/META-INF/" + FEATURE_PATH + "/", "*.xml", false);
                    while (featuresUrlEnumeration != null && featuresUrlEnumeration.hasMoreElements()) {
                        URL url = (URL) featuresUrlEnumeration.nextElement();
                        try {
                            featuresService.addRepository(url.toURI());
                            for (Repository repo : featuresService.listRepositories()) {
                                if (repo.getURI().equals(url.toURI())) {
                                    Set<Feature> features = new HashSet<Feature>(Arrays.asList(repo.getFeatures()));
                                    featuresService.installFeatures(features, EnumSet.noneOf(FeaturesService.Option.class));
                                }
                            }
                            urls.add(url);
                        } catch (Exception e) {
                            logger.error("Unable to install features", e);
                        }
                    }
                    synchronized (this) {
                        File file = bundleContext.getDataFile("FeatureDeploymentListener.cfg");
                        if (file != null) {
                            Properties props = new Properties();
                            if (file.exists()) {
                                InputStream input = new FileInputStream(file);
                                try {
                                    props.load(input);
                                } finally {
                                    input.close();
                                }
                            }
                            String prefix = bundle.getSymbolicName() + "-" + bundle.getVersion();
                            props.put(prefix + ".count", Integer.toString(urls.size()));
                            for (int i = 0; i < urls.size(); i++) {
                                props.put(prefix + ".url." + i, urls.get(i).toExternalForm());
                            }
                            OutputStream output = new FileOutputStream(file);
                            try {
                                props.store(output, null);
                            } finally {
                                output.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to install deployed features for bundle: " + bundle.getSymbolicName() + " - " + bundle.getVersion(), e);
                }
            } else if (bundleEvent.getType() == BundleEvent.UNINSTALLED) {
                try {
                    synchronized (this) {
                        File file = bundleContext.getDataFile("FeatureDeploymentListener.cfg");
                        if (file != null) {
                            Properties props = new Properties();
                            if (file.exists()) {
                                InputStream input = new FileInputStream(file);
                                try {
                                    props.load(input);
                                } finally {
                                    input.close();
                                }
                            }
                            String prefix = bundle.getSymbolicName() + "-" + bundle.getVersion();
                            String countStr = (String) props.get(prefix + ".count");
                            if (countStr != null) {
                                int count = Integer.parseInt(countStr);
                                for (int i = 0; i < count; i++) {
                                    URL url = new URL((String) props.get(prefix + ".url." + i));
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
                            }
                            for (Iterator<Object> it = props.keySet().iterator(); it.hasNext();) {
                                if (it.next().toString().startsWith(prefix + ".")) {
                                    it.remove();
                                }
                            }
                            OutputStream output = new FileOutputStream(file);
                            try {
                                props.store(output, null);
                            } finally {
                                output.close();
                            }
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
