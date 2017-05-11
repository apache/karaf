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

package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.repository.RemoteRepository;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Configures a {@link Builder} for an {@link AssemblyMojo}.
 */
class BuilderConfiguration {

    private final Log log;

    private final MavenUriParser mavenUriParser;

    private final ProfileEditsParser profileEditsParser;

    private final ArtifactParser artifactParser;

    BuilderConfiguration(
            final Log log, final MavenUriParser mavenUriParser, final ProfileEditsParser profileEditsParser,
            final ArtifactParser artifactParser
                        ) {
        this.log = log;
        this.mavenUriParser = mavenUriParser;
        this.profileEditsParser = profileEditsParser;
        this.artifactParser = artifactParser;
    }

    void configure(final Builder builder, final AssemblyMojo mojo) throws IOException, XMLStreamException {
        builder.offline(mojo.getMavenSession()
                            .isOffline());
        builder.localRepository(mojo.getLocalRepo()
                                    .getBasedir());
        builder.javase(mojo.getJavase());
        mojo.getConfig()
            .forEach(builder::config);
        mojo.getSystem()
            .forEach(builder::system);
        builder.karafVersion(mojo.getKarafVersion());
        builder.useReferenceUrls(mojo.getUseReferenceUrls());
        builder.defaultAddAll(mojo.getInstallAllFeaturesByDefault());
        builder.ignoreDependencyFlag(mojo.getIgnoreDependencyFlag());
        if (mojo.getProfilesUri() != null) {
            builder.profilesUris(mojo.getProfilesUri());
        }
        profileEditsParser.parse(mojo)
                          .ifPresent(builder::propertyEdits);
        builder.pidsToExtract(mojo.getPidsToExtract());
        builder.blacklistPolicy(mojo.getBlacklistPolicy());
        builder.blacklistBundles(mojo.getBlacklistedBundles());
        builder.blacklistFeatures(mojo.getBlacklistedFeatures());
        builder.blacklistProfiles(mojo.getBlacklistedProfiles());
        builder.blacklistRepositories(mojo.getBlacklistedRepositories());

        final String mavenRepositories = getMavenRepositories(mojo);
        log.info("Using repositories: " + mavenRepositories);
        builder.mavenRepositories(mavenRepositories);
        builder.translatedUrls(mavenUriParser.getTranslatedUris(mojo.getProject(), mojo.getTranslatedUrls()));
        // creating system directory
        log.info("Creating work directory");
        builder.homeDirectory(mojo.getWorkDirectory()
                                  .toPath());
        // Loading kars and features repositories
        log.info("Loading kar and features repositories dependencies");
        artifactParser.parse(mojo);
    }

    private String getMavenRepositories(final AssemblyMojo mojo) {
        return mojo.getProject()
                   .getRemoteProjectRepositories()
                   .stream()
                   .map(this::repositoryToString)
                   .collect(Collectors.joining(","));
    }

    private String repositoryToString(final RemoteRepository repository) {
        final String releasesPolicy = repositoryPolicy(repository, false, "", "@noreleases");
        final String snapshotPolicy = repositoryPolicy(repository, true, "@snapshots", "");
        return repository.getUrl() + "@id=" + repository.getId() + releasesPolicy + snapshotPolicy;
    }

    private String repositoryPolicy(
            final RemoteRepository repository, final boolean snapshot, final String ifEnabled, final String ifDisabled
                                   ) {
        return repository.getPolicy(snapshot)
                         .isEnabled() ? ifEnabled : ifDisabled;
    }

}
