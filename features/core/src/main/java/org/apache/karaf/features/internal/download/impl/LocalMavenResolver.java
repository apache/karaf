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
package org.apache.karaf.features.internal.download.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.util.maven.Parser;

/**
 * Resolves Maven coordinates from the Karaf distribution's local system repository only.
 */
public class LocalMavenResolver implements MavenResolver {

    private final Path systemRepository;

    public LocalMavenResolver(Path systemRepository) {
        this.systemRepository = systemRepository.toAbsolutePath().normalize();
    }

    /**
     * Create a resolver for the repository configured by the running Karaf distribution.
     *
     * @return a resolver rooted at {@code karaf.home/karaf.default.repository}.
     */
    public static LocalMavenResolver forKarafSystem() {
        Path home = Paths.get(System.getProperty("karaf.home", "karaf"));
        Path repository = Paths.get(System.getProperty("karaf.default.repository", "system"));
        if (!repository.isAbsolute()) {
            repository = home.resolve(repository);
        }
        return new LocalMavenResolver(repository);
    }

    @Override
    public File resolve(String url) throws IOException {
        if (url == null || !url.startsWith("mvn:")) {
            throw new MalformedURLException("Expected a mvn: URI: " + url);
        }
        String artifactPath = Parser.pathFromMaven(url);
        Path artifact = systemRepository.resolve(artifactPath).normalize();
        if (!artifact.startsWith(systemRepository)) {
            throw new IOException("Maven artifact path is outside the Karaf system repository: " + url);
        }
        if (!artifact.toFile().isFile()) {
            throw new FileNotFoundException("Maven artifact " + url + " was not found in the Karaf system repository "
                    + systemRepository);
        }
        return artifact.toFile();
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        return resolve(url);
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        return RetryChance.NEVER;
    }

}
