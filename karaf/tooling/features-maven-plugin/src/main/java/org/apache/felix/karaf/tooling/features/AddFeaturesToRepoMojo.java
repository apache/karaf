/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.tooling.features;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.xml.sax.SAXException;

/**
 * Generates the features XML file
 *
 * @version $Revision: 1.1 $
 * @goal add-features-to-repo
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Add the features to the repository
 */
public class AddFeaturesToRepoMojo extends MojoSupport {

    /**
     * @parameter
     */
    private List<String> descriptors;

    /**
     * @parameter
     */
    private List<String> features;

    /**
     * @parameter
     */
    private File repository;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Map<String, Feature> featuresMap = new HashMap<String, Feature>();
            for (String uri : descriptors) {
                Repository repo = new Repository(URI.create(translateFromMaven(uri)));
                for (Feature f : repo.getFeatures()) {
                    featuresMap.put(f.getName(), f);
                }
            }
            Set<String> transitiveFeatures = new HashSet<String>();
            addFeatures(features, transitiveFeatures, featuresMap);
            Set<String> bundles = new HashSet<String>();
            for (String feature : transitiveFeatures) {
                bundles.addAll(featuresMap.get(feature).getBundles());
            }
            System.out.println("Base repo: " + localRepo.getUrl());
            for (String bundle : bundles) {
                if (bundle.startsWith("wrap:")) {
                    bundle = bundle.substring(5);
                }
                if (!bundle.startsWith("mvn:")) {
                    throw new MojoExecutionException("Bundle url is not a maven url: " + bundle);
                }
                String[] parts = bundle.substring("mvn:".length()).split("/");
                String groupId = parts[0];
                String artifactId = parts[1];
                String version = null;
                String classifier = null;
                String type = "jar";
                if (parts.length > 2) {
                    version = parts[2];
                    if (parts.length > 3) {
                        type = parts[3];
                        if (parts.length > 4) {
                            classifier = parts[4];
                        }
                    }
                }
                String dir = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
                String name = artifactId + "-" + version + (classifier != null ? "-" + classifier : "") + "." + type;
                System.out.println("Copy:      " + dir + name);
                copy(new URL(getLocalRepoUrl() + "/" + dir + name).openStream(),
                     repository,
                     name,
                     dir,
                     new byte[8192]);

            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
    }

    private String translateFromMaven(String uri) {
        if (uri.startsWith("mvn:")) {
            String[] parts = uri.substring("mvn:".length()).split("/");
            String groupId = parts[0];
            String artifactId = parts[1];
            String version = null;
            String classifier = null;
            String type = "jar";
            if (parts.length > 2) {
                version = parts[2];
                if (parts.length > 3) {
                    type = parts[3];
                    if (parts.length > 4) {
                        classifier = parts[4];
                    }
                }
            }
            String dir = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
            String name = artifactId + "-" + version + (classifier != null ? "-" + classifier : "") + "." + type;

            return getLocalRepoUrl() + "/" + dir + name;
        }
        if (System.getProperty("os.name").startsWith("Windows") && uri.startsWith("file:")) {
        	String baseDir = uri.substring(5).replace('\\', '/').replaceAll(" ", "%20");
        	String result = baseDir;
        	if (baseDir.indexOf(":") > 0) {
        		result = "file:///" + baseDir;
        	}
        	return result;
        }
        return uri;
    }

    private String getLocalRepoUrl() {
    	 if (System.getProperty("os.name").startsWith("Windows")) {
             String baseDir = localRepo.getBasedir().replace('\\', '/').replaceAll(" ", "%20");
             return localRepo.getProtocol() + ":///" + baseDir;
    	 } else {
    		 return localRepo.getUrl();
    	 }
    }

    private void addFeatures(List<String> features, Set<String> transitiveFeatures, Map<String, Feature> featuresMap) {
        for (String feature : features) {
            Feature f = featuresMap.get(feature);
            if (f == null) {
                throw new IllegalArgumentException("Unable to find the feature '" + feature + "'");
            }
            transitiveFeatures.add(feature);
            addFeatures(f.getDependencies(), transitiveFeatures, featuresMap);
        }
    }

    public static void copy(
        InputStream is, File dir, String destName, String destDir, byte[] buffer)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(dir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(buffer)) > 0)
        {
            bos.write(buffer, 0, count);
        }
        bos.close();
    }

    public static class Feature {

        private String name;
        private List<String> dependencies = new ArrayList<String>();
        private List<String> bundles = new ArrayList<String>();
        private Map<String, Map<String,String>> configs = new HashMap<String, Map<String,String>>();

        public Feature(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public List<String> getBundles() {
            return bundles;
        }

        public Map<String, Map<String, String>> getConfigurations() {
            return configs;
        }

        public void addDependency(String dependency) {
            dependencies.add(dependency);
        }

        public void addBundle(String bundle) {
            bundles.add(bundle);
        }

        public void addConfig(String name, Map<String,String> properties) {
            configs.put(name, properties);
        }
    }

    public static class Repository {

        private URI uri;
        private List<Feature> features;

        public Repository(URI uri) {
            this.uri = uri;
        }

        public URI getURI() {
            return uri;
        }

        public Feature[] getFeatures() throws Exception {
            if (features == null) {
                load();
            }
            return features.toArray(new Feature[features.size()]);
        }

        public void load() throws IOException {
            try {
                features = new ArrayList<Feature>();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(uri.toURL().openStream());
                NodeList nodes = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Node node = nodes.item(i);
                    if (!(node instanceof Element) || !"feature".equals(node.getNodeName())) {
                        continue;
                    }
                    Element e = (Element) nodes.item(i);
                    String name = e.getAttribute("name");
                    Feature f = new Feature(name);
                    NodeList featureNodes = e.getElementsByTagName("feature");
                    for (int j = 0; j < featureNodes.getLength(); j++) {
                        Element b = (Element) featureNodes.item(j);
                        f.addDependency(b.getTextContent());
                    }
                    NodeList configNodes = e.getElementsByTagName("config");
                    for (int j = 0; j < configNodes.getLength(); j++) {
                        Element c = (Element) configNodes.item(j);
                        String cfgName = c.getAttribute("name");
                        String data = c.getTextContent();
                        Properties properties = new Properties();
                        properties.load(new ByteArrayInputStream(data.getBytes()));
                        Map<String, String> hashtable = new Hashtable<String, String>();
                        for (Object key : properties.keySet()) {
                            String n = key.toString();
                            hashtable.put(n, properties.getProperty(n));
                        }
                        f.addConfig(cfgName, hashtable);
                    }
                    NodeList bundleNodes = e.getElementsByTagName("bundle");
                    for (int j = 0; j < bundleNodes.getLength(); j++) {
                        Element b = (Element) bundleNodes.item(j);
                        f.addBundle(b.getTextContent());
                    }
                    features.add(f);
                }
            } catch (SAXException e) {
                throw (IOException) new IOException().initCause(e);
            } catch (ParserConfigurationException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }

    }
}
