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

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.service.Blacklist;
import org.apache.karaf.util.maven.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.download.impl.DownloadManagerHelper.stripUrl;

/**
 * Downloads a maven artifact and installs it into the given system directory.
 * The layout follows the conventions of a maven local repository.
 */
public class ArtifactInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactInstaller.class);

    private Path systemDirectory;
    private Downloader downloader;
    private Blacklist blacklist;

    public ArtifactInstaller(Path systemDirectory, Downloader downloader, Blacklist blacklist) {
        this.systemDirectory = systemDirectory;
        this.downloader = downloader;
        this.blacklist = blacklist;
    }
    
    public void installArtifact(BundleInfo bundle) throws Exception {
        if (bundle.isBlacklisted()) {
            LOGGER.info("      skipping blacklisted maven artifact: " + bundle.getLocation());
            return;
        }
        if (bundle.isOverriden()) {
            LOGGER.info("      adding overriden maven artifact: " + bundle.getLocation() + " (original location: " + bundle.getOriginalLocation() + ")");
        } else {
            LOGGER.info("      adding maven artifact: " + bundle.getLocation());
        }
        String location = bundle.getLocation().trim();
        location = removeTrailingSlash(stripUrl(location));
        if (!location.startsWith("mvn:")) {
            LOGGER.warn("Ignoring non maven artifact " + location);
            return;
        }
        final String finalLocation = location;
        downloader.download(location, provider -> {
            String uri = provider.getUrl();
            Path path = pathFromProviderUrl(systemDirectory, finalLocation);
            synchronized (provider) {
                Files.createDirectories(path.getParent());
                Files.copy(provider.getFile().toPath(), path, StandardCopyOption.REPLACE_EXISTING);
            }
        });
    }

    public void installArtifact(String location) throws Exception {
        LOGGER.info("      adding maven artifact: " + location);
        location = removeTrailingSlash(stripUrl(location));
        if (!location.startsWith("mvn:")) {
            LOGGER.warn("Ignoring non maven artifact " + location);
            return;
        }
        final String finalLocation = location;
        downloader.download(location, provider -> {
            String uri = provider.getUrl();
            if (blacklist.isBundleBlacklisted(finalLocation)) {
                throw new RuntimeException("Bundle " + finalLocation + " is blacklisted");
            }
            Path path = pathFromProviderUrl(systemDirectory, finalLocation);
            synchronized (provider) {
                Files.createDirectories(path.getParent());
                Files.copy(provider.getFile().toPath(), path, StandardCopyOption.REPLACE_EXISTING);
            }
        });
    }

    /**
     * for bad formed URL (like in Camel for mustache-compiler), we remove the trailing /
     */
    private String removeTrailingSlash(String location) {
        return location.endsWith("/") ? location.substring(0, location.length() - 1) : location;
    }
    
    public static Path pathFromProviderUrl(Path systemDirectory, String url) throws MalformedURLException {
        String pathString;
        if (url.startsWith("file:")) {
            return Paths.get(URI.create(url));
        }
        else if (url.startsWith("mvn:")) {
            pathString = Parser.pathFromMaven(url);
        }
        else {
            pathString = url;
        }
        return systemDirectory.resolve(pathString);
    }
}
