/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.kar.internal;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.maven.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the KAR service.
 */
public class KarServiceImpl implements KarService {
    private static final String FEATURE_CONFIG_FILE = "features.cfg";
    private static final Logger LOGGER = LoggerFactory.getLogger(KarServiceImpl.class);

    private File storage;
    private File base;
    private FeaturesService featuresService;
    
    private boolean noAutoRefreshBundles;
    private List<Kar> unsatisfiedKars;
    private AtomicBoolean busy;
    private DelayedDeployerThread delayedDeployerThread;

    public KarServiceImpl(String karafBase, String karStorage, FeaturesService featuresService) {
        this.base = new File(karafBase);
        this.storage = new File(karStorage);
        this.featuresService = featuresService;
        this.storage.mkdirs();
        if (!storage.isDirectory()) {
            throw new IllegalStateException("KAR storage " + storage + " is not a directory");
        }
        unsatisfiedKars = Collections.synchronizedList(new ArrayList<Kar>());
        busy = new AtomicBoolean();
    }
    
    @Override
    public void install(URI karUri) throws Exception {
        String karName = new Kar(karUri).getKarName();
        LOGGER.debug("Installing KAR {} from {}", karName, karUri);
        File karDir = new File(storage, karName);
        install(karUri, karDir, base);
    }
    
    @Override
    public void install(URI karUri, File repoDir, File resourceDir) throws Exception {
        busy.set(true);
        try {
            Kar kar = new Kar(karUri);
            kar.extract(repoDir, resourceDir);
            writeToFile(kar.getFeatureRepos(), new File(repoDir, FEATURE_CONFIG_FILE));
            for (URI uri : kar.getFeatureRepos()) {
                addToFeaturesRepositories(uri);
            }
            
            if (kar.isShouldInstallFeatures()) {
                List<URI> featureRepos = kar.getFeatureRepos();
                Dependency missingDependency = findMissingDependency(featureRepos);
                if(missingDependency==null) {
                    installFeatures(featureRepos);              
                }
                else {
                    LOGGER.warn("Feature dependency {} is not available. Kar deployment postponed to see if it is about to be deployed",missingDependency);
                    unsatisfiedKars.add(kar);
                    if(delayedDeployerThread==null) {
                        delayedDeployerThread = new DelayedDeployerThread();                        
                        delayedDeployerThread.start();
                    }
                }
            }
            if(!unsatisfiedKars.isEmpty()) {
                for (Iterator<Kar> iterator = unsatisfiedKars.iterator(); iterator.hasNext();) {
                    Kar delayedKar = iterator.next();
                    if(findMissingDependency(delayedKar.getFeatureRepos())==null) {
                        LOGGER.info("Dependencies of kar {} are now satisfied. Installing",delayedKar.getKarName());
                        iterator.remove();
                        installFeatures(delayedKar.getFeatureRepos());
                    }
                }
            }
            if(unsatisfiedKars.isEmpty()) {
                if(delayedDeployerThread!=null) {
                    delayedDeployerThread.cancel();                 
                }
                delayedDeployerThread = null;
            }
        } finally {
            busy.set(false);
        }

    }

    /**
     * checks if all required features are available
     * @param featureRepos the repositories within the kar
     * @return <code>null</code> if the contained features have no unresolvable dependencies. Otherwise the first missing dependency
     * @throws Exception
     */
    private Dependency findMissingDependency(List<URI> featureRepos)
            throws Exception {
        for (URI uri : featureRepos) {
            Feature[] includedFeatures = featuresService.getRepository(uri).getFeatures();
            for (Feature includedFeature : includedFeatures) {
                List<Dependency> dependencies = includedFeature.getDependencies();
                for (Dependency dependency : dependencies) {
                    Feature feature = featuresService.getFeature(dependency.getName(), dependency.getVersion());
                    if(feature==null)
                    {
                        return dependency;
                    }
                }
            }
        }
        return null;
    }


    private List<URI> readFromFile(File repoListFile) {
        ArrayList<URI> uriList = new ArrayList<>();
        FileReader fr = null;
        try {
            fr = new FileReader(repoListFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                uriList.add(new URI(line));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading repo list from file " + repoListFile.getAbsolutePath(), e);
        } finally {
            try {
                fr.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing reader for file " + repoListFile, e);
            }
        }
        return uriList;
    }
    
    private void writeToFile(List<URI> featuresRepositoriesInKar, File repoListFile) {
        FileOutputStream fos = null;
        PrintStream ps = null;
        try {
            fos = new FileOutputStream(repoListFile);
            ps = new PrintStream(fos);
            for (URI uri : featuresRepositoriesInKar) {
                ps.println(uri);
            }
            ps.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Error writing feature repo list to file " + repoListFile.getAbsolutePath(), e);
        } finally {
            closeStream(ps);
            closeStream(fos);
        }
    }

    private void deleteRecursively(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        dir.delete();
    }

    @Override
    public void uninstall(String karName) throws Exception {
        File karDir = new File(storage, karName);

        if (!karDir.exists()) {
            throw new IllegalArgumentException("The KAR " + karName + " is not installed");
        }

        List<URI> featuresRepositories = readFromFile(new File(karDir, FEATURE_CONFIG_FILE));
        uninstallFeatures(featuresRepositories);
        for (URI featuresRepository : featuresRepositories) {
            featuresService.removeRepository(featuresRepository);
        }
        
        deleteRecursively(karDir);
    }
    
    @Override
    public List<String> list() throws Exception {
        List<String> kars = new ArrayList<>();
        for (File kar : storage.listFiles()) {
            if (kar.isDirectory()) {
                kars.add(kar.getName());
            }
        }
        return kars;
    }



    /**
     * Add an URI to the list of features repositories.
     *
     * @param uri the URI to add.
     * @throws Exception in case of add failure.
     */
    private void addToFeaturesRepositories(URI uri) throws Exception {
        try {
            featuresService.removeRepository(uri);
            featuresService.addRepository(uri);
            LOGGER.info("Added feature repository '{}'", uri);
        } catch (Exception e) {
            LOGGER.warn("Unable to add repository '{}'", uri, e);
        }
    }

    /**
     * Install all features contained in the list of features XML.
     *
     * @param featuresRepositories the list of features XML.
     */
    private void installFeatures(List<URI> featuresRepositories) throws Exception {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            if (feature.getInstall() == null || Feature.DEFAULT_INSTALL_MODE.equals(feature.getInstall())) {
                                try {
                                    LOGGER.debug("noAutoRefreshBundles is " + isNoAutoRefreshBundles());
                                    if (isNoAutoRefreshBundles()) {
                                        featuresService.installFeature(feature, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
                                    } else {
                                        featuresService.installFeature(feature, EnumSet.noneOf(FeaturesService.Option.class));
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("Unable to install Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                                }
                            } else {
                                LOGGER.warn("Feature " + feature.getName() + "/" + feature.getVersion() + " has install flag set to \"manual\", so it's not automatically installed");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }
    
    @Override
    public void create(String repoName, List<String> features, PrintStream console) {
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            Repository repo = featuresService.getRepository(repoName);
            if (repo == null) {
                throw new RuntimeException("Could not find a repository with name " + repoName);
            }
            String karPath = storage + File.separator + repoName + ".kar";
            File karFile = new File(karPath);
            karFile.getParentFile().mkdirs();
            fos = new FileOutputStream(karFile);
            Manifest manifest = createNonAutoStartManifest(repo.getURI());
            jos = new JarOutputStream(new BufferedOutputStream(fos, 100000), manifest);
            
            Map<URI, Integer> locationMap = new HashMap<>();
            copyResourceToJar(jos, repo.getURI(), locationMap);
        
            Map<String, Feature> featureMap = new HashMap<>();
            for (Feature feature : repo.getFeatures()) {
                featureMap.put(feature.getName(), feature);
            }
            
            Set<Feature> featuresToCopy = getFeatures(featureMap, features, 1);
            
            for (Feature feature : featuresToCopy) {
                if (console != null)
                    console.println("Adding feature " + feature.getName());
                copyFeatureToJar(jos, feature, locationMap);
            }

            if (console != null)
                console.println("Kar file created : " + karPath);
        } catch (Exception e) {
            throw new RuntimeException("Error creating kar: " + e.getMessage(), e);
        } finally {
            closeStream(jos);
            closeStream(fos);
        }
        
    }

    private Set<Feature> getFeatures(Map<String, Feature> featureMap, List<String> features, int depth) {
        Set<Feature> featureSet = new HashSet<>();
        if (depth > 5) {
            // Break after some recursions to avoid endless loops 
            return featureSet;
        }
        if (features == null) {
            featureSet.addAll(featureMap.values());
            return featureSet;
        }
        for (String featureName : features) {
            Feature feature = featureMap.get(featureName);
            if (feature == null) {
                System.out.println("Feature " + featureName + " not found in repository.");
                //throw new RuntimeException();
            } else {
                featureSet.add(feature);
                List<Dependency> deps = feature.getDependencies();
                List<String> depNames = new ArrayList<>();
                for (Dependency dependency : deps) {
                    depNames.add(dependency.getName());
                }
                featureSet.addAll(getFeatures(featureMap, depNames, depth ++));
            }
        }
        return featureSet;
    }

    private Manifest createNonAutoStartManifest(URI repoUri) throws IOException {
        String manifestSt = "Manifest-Version: 1.0\n" +
            Kar.MANIFEST_ATTR_KARAF_FEATURE_START +": false\n" +
            Kar.MANIFEST_ATTR_KARAF_FEATURE_REPOS + ": " + repoUri.toString() + "\n";
        InputStream manifestIs = new ByteArrayInputStream(manifestSt.getBytes("UTF-8"));
        Manifest manifest = new Manifest(manifestIs);
        return manifest;
    }

    private void closeStream(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing stream", e);
            }
        }
    }

    private void copyFeatureToJar(JarOutputStream jos, Feature feature, Map<URI, Integer> locationMap)
        throws URISyntaxException {
        for (BundleInfo bundleInfo : feature.getBundles()) {
            URI location = new URI(bundleInfo.getLocation());
            copyResourceToJar(jos, location, locationMap);
        }
        for (ConfigFileInfo configFileInfo : feature.getConfigurationFiles()) {
            URI location = new URI(configFileInfo.getLocation());
            copyResourceToJar(jos, location, locationMap);
        }
    }

    private void copyResourceToJar(JarOutputStream jos, URI location, Map<URI, Integer> locationMap) {
        if (locationMap.containsKey(location)) {
            return;
        }
        try {
            String noPrefixLocation = location.toString().substring(location.toString().lastIndexOf(":") + 1);
            Parser parser = new Parser(noPrefixLocation);
            String path = "repository/" + parser.getArtifactPath();
            jos.putNextEntry(new JarEntry(path));
            try (
                InputStream is = location.toURL().openStream()
            ) {
                StreamUtils.copy(is, jos);
            }
            locationMap.put(location, 1);
        } catch (Exception e) {
            LOGGER.error("Error adding " + location, e);
        }
    }

    /**
     * Uninstall all features contained in the list of features XML.
     *
     * @param featuresRepositories the list of features XML.
     */
    private void uninstallFeatures(List<URI> featuresRepositories) throws Exception {
        for (Repository repository : featuresService.listRepositories()) {
            for (URI karFeatureRepoUri : featuresRepositories) {
                if (repository.getURI().equals(karFeatureRepoUri)) {
                    try {
                        for (Feature feature : repository.getFeatures()) {
                            if (feature.getInstall() == null || Feature.DEFAULT_INSTALL_MODE.equals(feature.getInstall())) {
                                try {
                                    featuresService.uninstallFeature(feature.getName(), feature.getVersion());
                                } catch (Exception e) {
                                    LOGGER.warn("Unable to uninstall Kar feature {}", feature.getName() + "/" + feature.getVersion(), e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Can't get features for KAR {}", karFeatureRepoUri, e);
                    }
                }
            }
        }
    }

    public boolean isNoAutoRefreshBundles() {
        return noAutoRefreshBundles;
    }

    public void setNoAutoRefreshBundles(boolean noAutoRefreshBundles) {
        this.noAutoRefreshBundles = noAutoRefreshBundles;
    }

    private class DelayedDeployerThread extends Thread
    {
        private AtomicBoolean cancel;
        
        public DelayedDeployerThread() {
            super("Delayed kar deployment");
            cancel = new AtomicBoolean();
        }
        
        public void cancel() {
            cancel.set(true);
        }
        
        @Override
        public void run() {
            
            try {
                while(busy.get() && !cancel.get()) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                }
            } catch (InterruptedException e) {
                // nothing to do
            }
            if (!cancel.get()) {
                installDelayedKars();
            }
        }

        private void installDelayedKars() {
            for (Iterator<Kar> iterator = unsatisfiedKars.iterator(); iterator.hasNext();) {
                Kar kar = iterator.next();
                iterator.remove();
                try {   
                    installFeatures(kar.getFeatureRepos());
                } catch (Exception e) {
                    LOGGER.error("Delayed deployment of kar "+kar.getKarName()+" failed",e);
                }
            }
        }
    }

}
