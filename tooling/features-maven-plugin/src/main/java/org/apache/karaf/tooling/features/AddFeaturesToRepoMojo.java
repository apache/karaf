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
package org.apache.karaf.tooling.features;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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

    /**
     * @parameter
     */
    private boolean includeMvnBasedDescriptors = false;

    /**
     * @parameter
     */
    private List<CopyFileBasedDescriptor> copyFileBasedDescriptors;

    /**
     * @parameter
     */
    private boolean skipNonMavenProtocols = true;

    /**
     * @parameter
     */
    private boolean failOnArtifactResolutionError = true;

    /**
     * @parameter
     */
    private boolean addTransitiveFeatures = true;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Set<String> bundles = new HashSet<String>();
            Map<String, Feature> featuresMap = new HashMap<String, Feature>();
            for (String uri : descriptors) {
                if (includeMvnBasedDescriptors) {
                    bundles.add(uri);
                }
                Repository repo = new Repository(URI.create(translateFromMaven(uri)));
                for (Feature f : repo.getFeatures()) {
                    featuresMap.put(f.getName(), f);
                }
            }
            
            // no features specified, handle all of them
            if(features == null) {
                features = new ArrayList<String>(featuresMap.keySet());
            }
            
            Set<String> featuresBundles = new HashSet<String>();
            Set<String> transitiveFeatures = new HashSet<String>();
            addFeatures(features, featuresBundles, transitiveFeatures, featuresMap);

            // add the bundles of the configured features to the bundles list
            bundles.addAll(featuresBundles);

            // if transitive features are enabled we add the contents of those
            // features to the bundles list
            if (addTransitiveFeatures) {
                for (String feature : transitiveFeatures) {
                    getLog().info("Adding contents of transitive feature: " + feature);
                    bundles.addAll(featuresMap.get(feature).getBundles());
                    //Treat the config files as bundles, since it is only copying
                    bundles.addAll(featuresMap.get(feature).getConfigFiles());
                }
            }
            
            // bundles with explicitely specified remote repos. key -> bundle, value -> remote repo
            List<Artifact> explicitRepoBundles = new ArrayList<Artifact>();

            getLog().info("Base repo: " + localRepo.getUrl());
            for (String bundle : bundles) {
                Artifact artifact = bundleToArtifact(bundle, skipNonMavenProtocols);
                if (artifact == null) {
                    continue;
                }
                if (artifact.getRepository() != null) {
                    explicitRepoBundles.add(artifact);
                } else {
                    //bundle URL without repository information are resolved now
                    resolveBundle(artifact, remoteRepos);
                }
            }
            // resolving all bundles with explicitly specified remote repository
            for(Artifact explicitBundle : explicitRepoBundles) {
                resolveBundle(explicitBundle, Collections.singletonList(explicitBundle.getRepository()));
            }
            if (copyFileBasedDescriptors != null) {
                for (CopyFileBasedDescriptor fileBasedDescritpor : copyFileBasedDescriptors) {
                    copy(new FileInputStream(fileBasedDescritpor.getSourceFile()),
                        repository,
                        fileBasedDescritpor.getTargetFileName(),
                        fileBasedDescritpor.getTargetDirectory(),
                        new byte[8192]);
                }
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
    }
    
    // resolves the bundle in question
    //TODO neither remoteRepos nor bundle's Repository are used, only the local repo?????
    private void resolveBundle(Artifact bundle, List<ArtifactRepository> remoteRepos) throws IOException, MojoFailureException {
        //TODO consider DefaultRepositoryLayout
    	String dir = bundle.getGroupId().replace('.', '/') + "/" + bundle.getArtifactId() + "/" + bundle.getBaseVersion() + "/";
    	String name = bundle.getArtifactId() + "-" + bundle.getBaseVersion() + (bundle.getClassifier() != null ? "-" + bundle.getClassifier() : "") + "." + bundle.getType();

    	try {
    		getLog().info("Copying bundle: " + bundle);
    		resolver.resolve(bundle, remoteRepos, localRepo);
    		copy(new FileInputStream(bundle.getFile()),
    				repository,
    				name,
    				dir,
    				new byte[8192]);
    	} catch (ArtifactResolutionException e) {
    		if (failOnArtifactResolutionError) {
    			throw new MojoFailureException("Can't resolve bundle " + bundle, e);
    		}
    		getLog().error("Can't resolve bundle " + bundle, e);
    	} catch (ArtifactNotFoundException e) {
    		if (failOnArtifactResolutionError) {
    			throw new MojoFailureException("Can't resolve bundle " + bundle, e);
    		}
    		getLog().error("Can't resolve bundle " + bundle, e);
    	}
    }

    private void addFeatures(List<String> features, Set<String> featuresBundles, Set<String> transitiveFeatures, Map<String, Feature> featuresMap) {
        for (String feature : features) {
            Feature f = featuresMap.get(feature);
            if (f == null) {
                throw new IllegalArgumentException("Unable to find the feature '" + feature + "'");
            }
            // only add the feature to transitives if it is not
            // listed in the features list defined by the config
            if (!this.features.contains(feature)) {
                transitiveFeatures.add(feature);
            } else {
                // add the bundles of the feature to the bundle set
                getLog().info("Adding contents for feature: " + feature);
                featuresBundles.addAll(featuresMap.get(feature).getBundles());
                //Treat the config files as bundles, since it is only copying
                featuresBundles.addAll(featuresMap.get(feature).getConfigFiles());
            }
            addFeatures(f.getDependencies(), featuresBundles, transitiveFeatures, featuresMap);
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
        private List<String> configFiles = new ArrayList<String>();

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

        public List<String> getConfigFiles() {
        	return configFiles;
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

        public void addConfigFile(String configFile) {
        	configFiles.add(configFile);
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
                    NodeList configFileNodes = e.getElementsByTagName("configfile");
                    for (int j = 0; j < configFileNodes.getLength(); j++) {
                    	Element c = (Element) configFileNodes.item(j);
                    	f.addConfigFile(c.getTextContent());
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
