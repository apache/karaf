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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Add Features necessary bundles into system folder Repo
 *
 * @version $Revision$
 * @goal add-features-to-repo
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Add the features to the repository
 */
public class AddFeaturesToRepoMojo extends MojoSupport {

    private final static String KARAF_CORE_STANDARD_FEATURE_URL =
        "mvn:org.apache.karaf.assemblies.features/standard/%s/xml/features";
    private final static String KARAF_CORE_ENTERPRISE_FEATURE_URL =
        "mvn:org.apache.karaf.assemblies.features/enterprise/%s/xml/features";

    /**
     * @parameter
     */
    private List<String> descriptors;

    /**
     * @parameter
     */
    private List<String> features;

    /**
     * @parameter expression="${project.build.directory}/features-repo"
     */
    private File repository;

    /**
     * the target Karaf version used to resolve Karaf core features descriptors
     *
     * @parameter
     */
    private String karafVersion;

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
    private boolean resolveDefinedRepositoriesRecursively = true;

    /**
     * @parameter
     */
    private boolean addTransitiveFeatures = true;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (karafVersion == null) {
            Package p = Package.getPackage("org.apache.karaf.tooling.features");
            karafVersion = p.getImplementationVersion();
        }
        String karafCoreStandardFeatureUrl = String.format(KARAF_CORE_STANDARD_FEATURE_URL, karafVersion);
        Artifact standardFeatureDescriptor = resourceToArtifact(karafCoreStandardFeatureUrl, true);
        if (standardFeatureDescriptor != null) {
            try {
                resolveBundle(standardFeatureDescriptor, remoteRepos);
                descriptors.add(0, karafCoreStandardFeatureUrl);
            } catch (Exception e) {
                getLog().warn("Can't add " + karafCoreStandardFeatureUrl + " in the descriptors set");
                getLog().debug(e);
            }
        }
        String karafCoreEnterpriseFeatureUrl = String.format(KARAF_CORE_ENTERPRISE_FEATURE_URL, karafVersion);
        Artifact enterpriseFeatureDescriptor = resourceToArtifact(karafCoreEnterpriseFeatureUrl, true);
        if (enterpriseFeatureDescriptor != null) {
            try {
                resolveBundle(enterpriseFeatureDescriptor, remoteRepos);
                descriptors.add(0, karafCoreEnterpriseFeatureUrl);
            } catch (Exception e) {
                getLog().warn("Can't add " + karafCoreEnterpriseFeatureUrl + " in the descriptors set");
                getLog().debug(e);
            }
        }
        try {
            Set<String> bundles = new HashSet<String>();
            Map<String, Feature> featuresMap = new HashMap<String, Feature>();
            for (String uri : descriptors) {
                retrieveDescriptorsRecursively(uri, bundles, featuresMap);
            }

            // no features specified, handle all of them
            if (features == null) {
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
                    // transitiveFeatures contains name/version
                    Feature f = featuresMap.get(feature);
                    getLog().info("Adding contents of transitive feature: " + feature);
                    bundles.addAll(f.getBundles());
                    // Treat the config files as bundles, since it is only copying
                    bundles.addAll(f.getConfigFiles());
                }
            }

            // bundles with explicitely specified remote repos. key -> bundle, value -> remote repo
            List<Artifact> explicitRepoBundles = new ArrayList<Artifact>();

            getLog().info("Base repo: " + localRepo.getUrl());
            int currentBundle = 0;
            for (String bundle : bundles) {
                Artifact artifact = resourceToArtifact(bundle, skipNonMavenProtocols);

                // Maven ArtifactResolver leaves file handles around so need to clean up
                // or we will run out of file descriptors
                if (currentBundle++ % 100 == 0) {
                    System.gc();
                    System.runFinalization();
                }

                if (artifact == null) {
                    continue;
                }
                if (artifact.getRepository() != null) {
                    explicitRepoBundles.add(artifact);
                } else {
                    // bundle URL without repository information are resolved now
                    resolveBundle(artifact, remoteRepos);
                }
            }
            // resolving all bundles with explicitly specified remote repository
            for (Artifact explicitBundle : explicitRepoBundles) {
                resolveBundle(explicitBundle, Collections.singletonList(explicitBundle.getRepository()));
            }
            if (copyFileBasedDescriptors != null) {
                for (CopyFileBasedDescriptor fileBasedDescriptor : copyFileBasedDescriptors) {
                    copy(new FileInputStream(fileBasedDescriptor.getSourceFile()),
                        repository,
                        fileBasedDescriptor.getTargetFileName(),
                        fileBasedDescriptor.getTargetDirectory(),
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

    private void retrieveDescriptorsRecursively(String uri, Set<String> bundles, Map<String, Feature> featuresMap)
        throws Exception {
        // let's ensure a mvn: based url is sitting in the local repo before we try reading it
        Artifact descriptor = resourceToArtifact(uri, true);
        if (descriptor != null) {
            resolveBundle(descriptor, remoteRepos);
        }
        if (includeMvnBasedDescriptors) {
            bundles.add(uri);
        }
        Repository repo = new Repository(URI.create(translateFromMaven(uri.replaceAll(" ", "%20"))));
        for (Feature f : repo.getFeatures()) {
            featuresMap.put(f.getName() + "/" + f.getVersion(), f);
        }
        if (resolveDefinedRepositoriesRecursively) {
            for (String r : repo.getDefinedRepositories()) {
                retrieveDescriptorsRecursively(r, bundles, featuresMap);
            }
        }
    }

    // resolves the bundle in question
    // TODO neither remoteRepos nor bundle's Repository are used, only the local repo?????
    private void resolveBundle(Artifact bundle, List<ArtifactRepository> remoteRepos) throws IOException,
        MojoFailureException {
        // TODO consider DefaultRepositoryLayout
        String dir =
            bundle.getGroupId().replace('.', '/') + "/" + bundle.getArtifactId() + "/" + bundle.getBaseVersion() + "/";
        String name =
            bundle.getArtifactId() + "-" + bundle.getBaseVersion()
                    + (bundle.getClassifier() != null ? "-" + bundle.getClassifier() : "") + "." + bundle.getType();

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

    private void addFeatures(List<String> features, Set<String> featuresBundles, Set<String> transitiveFeatures,
            Map<String, Feature> featuresMap) {
        for (String feature : features) {
            
            // feature could be only the name or name/version
            int delimIndex = feature.indexOf('/');
            String version = null;
            if (delimIndex > 0) {
                version = feature.substring(delimIndex + 1);
                feature = feature.substring(0, delimIndex);
            }
 
            Feature f = null;
            if (version != null) {
                // looking for a specific feature with name and version
                f = featuresMap.get(feature + "/" + version);
            } else {
                // looking for the feature name (with the greatest version)
                for (String key : featuresMap.keySet()) {
                    String[] nameVersion = key.split("/");
                    if (feature.equals(nameVersion[0])) {
                        if (f == null || f.getVersion().compareTo(featuresMap.get(key).getVersion()) < 0) {
                            f = featuresMap.get(key);
                        }
                    }
                }
            }
            if (f == null) {
                throw new IllegalArgumentException("Unable to find the feature '" + feature + "'");
            }
            // only add the feature to transitives if it is not
            // listed in the features list defined by the config
            if (!this.features.contains(f.getName() + "/" + f.getVersion())) {
                transitiveFeatures.add(f.getName() + "/" + f.getVersion());
            } else {
                // add the bundles of the feature to the bundle set
                getLog().info("Adding contents for feature: " + f.getName() + "/" + f.getVersion());
                featuresBundles.addAll(f.getBundles());
                // Treat the config files as bundles, since it is only copying
                featuresBundles.addAll(f.getConfigFiles());
            }
            addFeatures(f.getDependencies(), featuresBundles, transitiveFeatures, featuresMap);
        }
    }

    public static void copy(
            InputStream is, File dir, String destName, String destDir, byte[] buffer)
        throws IOException {
        if (destDir == null) {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(dir, destDir);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("Unable to create target directory: "
                        + targetDir);
            }
        } else if (!targetDir.isDirectory()) {
            throw new IOException("Target is not a directory: "
                    + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(buffer)) > 0) {
            bos.write(buffer, 0, count);
        }
        bos.close();
    }

    public static class Feature {

        private String name;
        private String version;
        private List<String> dependencies = new ArrayList<String>();
        private List<String> bundles = new ArrayList<String>();
        private Map<String, Map<String, String>> configs = new HashMap<String, Map<String, String>>();
        private List<String> configFiles = new ArrayList<String>();

        public Feature(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
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

        public void addConfig(String name, Map<String, String> properties) {
            configs.put(name, properties);
        }

        public void addConfigFile(String configFile) {
            configFiles.add(configFile);
        }
    }

    public static class Repository {

        private URI uri;
        private List<Feature> features;
        private List<String> repositories;

        public Repository(URI uri) {
            this.uri = uri;
        }

        public URI getURI() {
            return uri;
        }

        public Feature[] getFeatures() throws Exception {
            if (features == null) {
                loadFeatures();
            }
            return features.toArray(new Feature[features.size()]);
        }

        public String[] getDefinedRepositories() throws Exception {
            if (repositories == null) {
                loadRepositories();
            }
            return repositories.toArray(new String[repositories.size()]);
        }

        private void loadRepositories() throws IOException {
            try {
                repositories = new ArrayList<String>();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(uri.toURL().openStream());
                NodeList nodes = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Node node = nodes.item(i);
                    if (!(node instanceof Element) || !"repository".equals(node.getNodeName())) {
                        continue;
                    }
                    Element e = (Element) nodes.item(i);
                    repositories.add(e.getTextContent());
                }
            } catch (SAXException e) {
                throw (IOException) new IOException().initCause(e);
            } catch (ParserConfigurationException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }

        private void loadFeatures() throws IOException {
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
                    String version = e.getAttribute("version");
                    Feature f = new Feature(name);
                    f.setVersion(version);
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
