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
package org.apache.felix.karaf.deployer.features;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.FeaturesService;
import org.apache.felix.karaf.features.Repository;
import org.apache.felix.fileinstall.ArtifactTransformer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

/**
 * A deployment listener able to hot deploy a feature descriptor
 */
public class FeatureDeploymentListener implements ArtifactTransformer, SynchronousBundleListener {

    public static final String FEATURE_PATH = "org.apache.felix.karaf.shell.features";

    private static final Log LOGGER = LogFactory.getLog(FeatureDeploymentListener.class);

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
            bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bundle));
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
                if ("features".equals(name) && (uri == null || "".equals(uri))) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to parse deployed file " + artifact.getAbsolutePath(), e);
        }
        return false;
    }

    public File transform(File artifact, File tmpDir) {
        // We can't really install the feature right now and just return nothing.
        // We would not be aware of the fact that the bundle has been uninstalled
        // and therefore require the feature to be uninstalled.
        // So instead, create a fake bundle with the file inside, which will be listened by
        // this deployer: installation / uninstallation of the feature will be done
        // while the bundle is installed / uninstalled.
        try {
            File destFile = new File(tmpDir, artifact.getName() + ".jar");
            OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile));

            String name = artifact.getCanonicalPath();
            int idx = name.lastIndexOf('/');
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
            String[] str = extractNameVersionType(name);
            // Create manifest
            Manifest m = new Manifest();
            m.getMainAttributes().putValue("Manifest-Version", "2");
            m.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
            m.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, str[0]);
            m.getMainAttributes().putValue(Constants.BUNDLE_VERSION, str[1]);
            // Put content
            JarOutputStream out = new JarOutputStream(os);
            ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
            out.putNextEntry(e);
            m.write(out);
            out.closeEntry();
            e = new ZipEntry("META-INF/");
            out.putNextEntry(e);
            e = new ZipEntry("META-INF/" + FEATURE_PATH + "/");
            out.putNextEntry(e);
            out.closeEntry();
            e = new ZipEntry("META-INF/" + FEATURE_PATH + "/" + name);
            out.putNextEntry(e);
            InputStream fis = new BufferedInputStream(new FileInputStream(artifact));
            copyInputStream(fis, out);
            fis.close();
            out.closeEntry();
            out.close();
            os.close();
            return destFile;
        } catch (Exception e) {
            LOGGER.error("Unable to build spring application bundle", e);
            return null;
        }
    }

    public void bundleChanged(BundleEvent bundleEvent) {
        try {
            Bundle bundle = bundleEvent.getBundle();
            if (bundleEvent.getType() == BundleEvent.INSTALLED) {
                Enumeration featuresUrlEnumeration = bundle.findEntries("/META-INF/" + FEATURE_PATH + "/", "*.xml", false);
                while (featuresUrlEnumeration != null && featuresUrlEnumeration.hasMoreElements()) {
                    URL url = (URL) featuresUrlEnumeration.nextElement();
                    featuresService.addRepository(url.toURI());
                    for (Repository repo : featuresService.listRepositories()) {
                        if (repo.getURI().equals(url.toURI())) {
                            for (Feature f : repo.getFeatures()) {
                                try {
                                    featuresService.installFeature(f.getName(), f.getVersion());
                                } catch (Exception e) {
                                    LOGGER.error("Unable to install feature: " + f.getName(), e);
                                }
                            }
                        }
                    }
                }
            } else if (bundleEvent.getType() == BundleEvent.UNINSTALLED) {
                Enumeration featuresUrlEnumeration = bundle.findEntries("/META-INF/" + FEATURE_PATH + "/", "*.xml", false);
                while (featuresUrlEnumeration != null && featuresUrlEnumeration.hasMoreElements()) {
                    URL url = (URL) featuresUrlEnumeration.nextElement();
                    for (Repository repo : featuresService.listRepositories()) {
                        if (repo.getURI().equals(url.toURI())) {
                            for (Feature f : repo.getFeatures()) {
                                try {
                                    featuresService.uninstallFeature(f.getName(), f.getVersion());
                                } catch (Exception e) {
                                    LOGGER.error("Unable to uninstall feature: " + f.getName(), e);
                                }
                            }
                        }
                    }
                    featuresService.removeRepository(url.toURI());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to install / uninstall feature", e);
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

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
    }
    private static final String DEFAULT_VERSION = "0.0.0";

    private static final Pattern ARTIFACT_MATCHER = Pattern.compile("(.+)(?:-(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:[^a-zA-Z0-9](.*))?)(?:\\.([^\\.]+))", Pattern.DOTALL);
    private static final Pattern FUZZY_MODIFIDER = Pattern.compile("(?:\\d+[.-])*(.*)", Pattern.DOTALL);

    public static String[] extractNameVersionType(String url) {
        Matcher m = ARTIFACT_MATCHER.matcher(url);
        if (!m.matches()) {
            return new String[] { url, DEFAULT_VERSION };
        }
        else {
            //System.err.println(m.groupCount());
            //for (int i = 1; i <= m.groupCount(); i++) {
            //    System.err.println("Group " + i + ": " + m.group(i));
            //}

            StringBuffer v = new StringBuffer();
            String d1 = m.group(1);
            String d2 = m.group(2);
            String d3 = m.group(3);
            String d4 = m.group(4);
            String d5 = m.group(5);
            String d6 = m.group(6);
            if (d2 != null) {
                v.append(d2);
                if (d3 != null) {
                    v.append('.');
                    v.append(d3);
                    if (d4 != null) {
                        v.append('.');
                        v.append(d4);
                        if (d5 != null) {
                            v.append(".");
                            cleanupModifier(v, d5);
                        }
                    } else if (d5 != null) {
                        v.append(".0.");
                        cleanupModifier(v, d5);
                    }
                } else if (d5 != null) {
                    v.append(".0.0.");
                    cleanupModifier(v, d5);
                }
            }
            return new String[] { d1, v.toString(), d6 };
        }
    }

    private static void cleanupModifier(StringBuffer result, String modifier) {
        Matcher m = FUZZY_MODIFIDER.matcher(modifier);
        if (m.matches()) {
            modifier = m.group(1);
        }
        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-') {
                result.append(c);
            }
        }
    }

}
