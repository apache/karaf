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
package org.apache.karaf.profile.assembly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.model.Config;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.Library;
import org.apache.karaf.features.internal.service.Blacklist;
import org.apache.karaf.features.internal.service.Deployer;
import org.apache.karaf.features.internal.service.State;
import org.apache.karaf.features.internal.service.StaticInstallSupport;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.util.maven.Parser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssemblyDeployCallback extends StaticInstallSupport implements Deployer.DeployCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final DownloadManager manager;
    private final Builder builder;
    private Blacklist featureBlacklist;
    private Blacklist bundleBlacklist;
    private final Path homeDirectory;
    private final int defaultStartLevel;
    private final Path etcDirectory;
    private final Path systemDirectory;
    private final Deployer.DeploymentState dstate;
    private final AtomicLong nextBundleId = new AtomicLong(0);

    private final Map<String, Bundle> bundles = new HashMap<>();

    public AssemblyDeployCallback(DownloadManager manager, Builder builder, BundleRevision systemBundle, Collection<Features> repositories) {
        this.manager = manager;
        this.builder = builder;
//        this.featureBlacklist = new Blacklist(builder.getBlacklistedFeatures());
//        this.bundleBlacklist = new Blacklist(builder.getBlacklistedBundles());
        this.homeDirectory = builder.homeDirectory;
        this.etcDirectory = homeDirectory.resolve("etc");
        this.systemDirectory = homeDirectory.resolve("system");
        this.defaultStartLevel = builder.defaultStartLevel;
        dstate = new Deployer.DeploymentState();
        dstate.bundles = new HashMap<>();
        dstate.features = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.filtersPerRegion = new HashMap<>();
        dstate.state = new State();

        MapUtils.addToMapSet(dstate.bundlesPerRegion, FeaturesService.ROOT_REGION, 0l);
        dstate.bundles.put(0l, systemBundle.getBundle());
        for (Features repo : repositories) {
            for (Feature f : repo.getFeature()) {
                dstate.features.put(f.getId(), f);
            }
        }
    }

    public Map<String, Integer> getStartupBundles() {
        Map<String, Integer> startup = new HashMap<>();
        for (Map.Entry<String, Bundle> bundle : bundles.entrySet()) {
            int level = bundle.getValue().adapt(BundleStartLevel.class).getStartLevel();
            if (level <= 0) {
                level = defaultStartLevel;
            }
            startup.put(bundle.getKey(), level);
        }
        return startup;
    }

    public Deployer.DeploymentState getDeploymentState() {
        return dstate;
    }

    @Override
    public void saveState(State state) {
        dstate.state.replace(state);
    }

    @Override
    public void persistResolveRequest(Deployer.DeploymentRequest request) {
    }

    @Override
    public void installConfigs(org.apache.karaf.features.Feature feature) throws IOException {
        assertNotBlacklisted(feature);
        // Install
        Downloader downloader = manager.createDownloader();
        for (Config config : ((Feature) feature).getConfig()) {
            if (config.isExternal()) {
                downloader.download(config.getValue().trim(), provider -> {
                    Path input = provider.getFile().toPath();
                    byte[] data = Files.readAllBytes(input);
                    Path configFile = etcDirectory.resolve(config.getName() + ".cfg");
                    LOGGER.info("      adding config file: {}", homeDirectory.relativize(configFile));
                    if (!Files.exists(configFile)) {
                        Files.write(configFile, data);
                    } else if (config.isAppend()) {
                        Files.write(configFile, data, StandardOpenOption.APPEND);
                    }
                });
            } else {
                byte[] data = config.getValue().getBytes();
                Path configFile = etcDirectory.resolve(config.getName() + ".cfg");
                LOGGER.info("      adding config file: {}", homeDirectory.relativize(configFile));
                if (!Files.exists(configFile)) {
                    Files.write(configFile, data);
                } else if (config.isAppend()) {
                    Files.write(configFile, data, StandardOpenOption.APPEND);
                }
            }
        }
        for (final ConfigFile configFile : ((Feature) feature).getConfigfile()) {
            downloader.download(configFile.getLocation(), provider -> {
                Path input = provider.getFile().toPath();
                String path = configFile.getFinalname();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                path = substFinalName(path);
                Path output = homeDirectory.resolve(path);
                LOGGER.info("      adding config file: {}", path);
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
            });
        }
    }

    
    @Override
    public void installLibraries(org.apache.karaf.features.Feature feature) throws IOException {
        assertNotBlacklisted(feature);
        Downloader downloader = manager.createDownloader();
        List<String> libraries = new ArrayList<>();
        for (Library library : ((Feature) feature).getLibraries()) {
            String lib = library.getLocation() +
                    ";type:=" + library.getType() +
                    ";export:=" + library.isExport() +
                    ";delegate:=" + library.isDelegate();
            libraries.add(lib);
        }
        if (!libraries.isEmpty()) {
            Path configPropertiesPath = etcDirectory.resolve("config.properties");
            Properties configProperties = new Properties(configPropertiesPath.toFile());
            builder.downloadLibraries(downloader, configProperties, libraries, "   ");
        }
        try {
            downloader.await();
        } catch (Exception e) {
            throw new IOException("Error downloading configuration files", e);
        }
    }
    
    private void assertNotBlacklisted(org.apache.karaf.features.Feature feature) {
//        if (featureBlacklist.isFeatureBlacklisted(feature.getName(), feature.getVersion())) {
//            if (builder.getBlacklistPolicy() == Builder.BlacklistPolicy.Fail) {
//                throw new RuntimeException("Feature " + feature.getId() + " is blacklisted");
//            }
//        }
    }

    @Override
    public void callListeners(DeploymentEvent deployEvent) {
    }

    @Override
    public void callListeners(FeatureEvent featureEvent) {
    }

    @Override
    public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
        // Check blacklist
//        if (bundleBlacklist.isBundleBlacklisted(uri)) {
//            if (builder.getBlacklistPolicy() == Builder.BlacklistPolicy.Fail) {
//                throw new RuntimeException("Bundle " + uri + " is blacklisted");
//            }
//        }
        // Install
        LOGGER.info("      adding maven artifact: " + uri);
        try {
            String regUri;
            String path;
            if (uri.startsWith("mvn:")) {
                regUri = uri;
                path = Parser.pathFromMaven(uri);
            } else {
                uri = uri.replaceAll("[^0-9a-zA-Z.\\-_]+", "_");
		        if (uri.length() > 256) {
                    //to avoid the File name too long exception
                    uri = uri.substring(0, 255);
                }
                path = "generated/" + uri;
                regUri = "file:" + path;
            }
            final Path bundleSystemFile = systemDirectory.resolve(path);
            Files.createDirectories(bundleSystemFile.getParent());
            Files.copy(is, bundleSystemFile, StandardCopyOption.REPLACE_EXISTING);

            Hashtable<String, String> headers = new Hashtable<>();
            try (JarFile jar = new JarFile(bundleSystemFile.toFile())) {
                Attributes attributes = jar.getManifest().getMainAttributes();
                for (Map.Entry<Object, Object> attr : attributes.entrySet()) {
                    headers.put(attr.getKey().toString(), attr.getValue().toString());
                }
            }
            BundleRevision revision = new FakeBundleRevision(headers, uri, nextBundleId.incrementAndGet());
            Bundle bundle = revision.getBundle();
            MapUtils.addToMapSet(dstate.bundlesPerRegion, region, bundle.getBundleId());
            dstate.bundles.put(bundle.getBundleId(), bundle);

            bundles.put(regUri, bundle);
            return bundle;
        } catch (IOException e) {
            throw new BundleException("Unable to install bundle", e);
        }
    }

    @Override
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
    }

    private String substFinalName(String finalname) {
        final String markerVarBeg = "${";
        final String markerVarEnd = "}";

        boolean startsWithVariable = finalname.startsWith(markerVarBeg) && finalname.contains(markerVarEnd);
        if (startsWithVariable) {
            String marker = finalname.substring(markerVarBeg.length(), finalname.indexOf(markerVarEnd));
            switch (marker) {
            case "karaf.base":
                return this.homeDirectory + finalname.substring(finalname.indexOf(markerVarEnd)+markerVarEnd.length());
            case "karaf.etc":
                return this.etcDirectory + finalname.substring(finalname.indexOf(markerVarEnd)+markerVarEnd.length());
            default:
                break;
            }
        }
        return finalname;
    }

}
