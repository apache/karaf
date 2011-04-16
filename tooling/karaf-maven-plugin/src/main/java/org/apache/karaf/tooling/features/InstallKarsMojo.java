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


package org.apache.karaf.tooling.features;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 *
 * @version $Revision: 1.1 $
 * @goal install-kars
 * @phase process-resources
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Install kar dependencies
 */
public class InstallKarsMojo extends MojoSupport {

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly"
     * @required
     */
    protected String workDirectory;

    /**
     * features config file.
     *
     * @parameter expression="${project.build.directory}/assembly/etc/org.apache.karaf.features.cfg"
     * @required
     */
    protected File featuresCfgFile;

    /**
     * startup.properties file.
     *
     * @parameter expression="${project.build.directory}/assembly/etc/startup.properties"
     * @required
     */
    protected File startupPropertiesFile;

    /**
     * default start level for bundles in features that dont' specify it
     *
     * @parameter
     */
    protected int defaultStartLevel = 30;

    /**
     * if false, unpack to system and add bundles to startup.properties
     * if true, unpack to local-repo and add feature to features config
     *
//     * @parameter
     */
    protected boolean unpackToLocalRepo;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly/local-repo"
     * @required
     */
    protected String localRepoDirectory;

    /**
     * Directory that resources are copied to during the build.
     *
     * @parameter expression="${project.build.directory}/assembly/system"
     * @required
     */
    protected String systemDirectory;

    /**
     * List of features from runtime-scope features xml and kars to be installed.
     *
     * @parameter
     */
    private List<String> features;

    //Aether support
    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    private String repoPath;
    private CommentProperties startupProperties = new CommentProperties();

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (startupPropertiesFile.exists()) {
            try {
                InputStream in = new FileInputStream(startupPropertiesFile);
                try {
                    startupProperties.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new MojoFailureException("Could not open existing startup.properties file at " + startupPropertiesFile, e);
            }
        } else {
            startupProperties.setHeader(Collections.singletonList("#Bundles to be started on startup, with startlevel"));
            if (!startupPropertiesFile.getParentFile().exists()) {
                startupPropertiesFile.getParentFile().mkdirs();
            }
        }
        KarArtifactInstaller installer = new KarArtifactInstaller();
        installer.setBasePath(workDirectory);
        FeaturesService featuresService = new OfflineFeaturesService();
        installer.setFeaturesService(featuresService);
        installer.init();
        Collection<Artifact> dependencies = project.getDependencyArtifacts();
        StringBuilder buf = new StringBuilder();
        byte[] buffer = new byte[4096];
        for (Artifact artifact: dependencies) {
            unpackToLocalRepo = "runtime".equals(artifact.getScope());
            repoPath = unpackToLocalRepo ? localRepoDirectory : systemDirectory;
            installer.setLocalRepoPath(repoPath);
            if ("kar".equals(artifact.getType()) && acceptScope(artifact)) {
                File file = artifact.getFile();
                try {
                    installer.install(file);
                } catch (Exception e) {
                    buf.append("Could not install kar: ").append(artifact.toString()).append("\n");
                    buf.append(e.getMessage()).append("\n\n");
                }
            }
            if ("features".equals(artifact.getClassifier()) && acceptScope(artifact)) {
                String uri = MvnUrlUtil.artifactToMvn(artifact);

                File source = artifact.getFile();
                DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

                //remove timestamp version
                artifact = factory.createArtifactWithClassifier(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getType(), artifact.getClassifier());
                File target = new File(repoPath + "/" + layout.pathOf(artifact));
                if (!target.exists()) {
                    target.getParentFile().mkdirs();
                    try {
                        InputStream is = new FileInputStream(source);
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
                        int count = 0;
                        while ((count = is.read(buffer)) > 0)
                        {
                            bos.write(buffer, 0, count);
                        }
                        bos.close();
                    } catch (IOException e) {
                        getLog().error("Could not copy features " + uri + " from source file " + source, e);
                    }

                }
                try {
                    featuresService.addRepository(URI.create(uri));
                } catch (Exception e) {
                    buf.append("Could not install feature: ").append(artifact.toString()).append("\n");
                    buf.append(e.getMessage()).append("\n\n");
                }
            }
        }

        //install bundles listed in startup properties that weren't in kars into the system dir
        for (String key: startupProperties.keySet()) {
            String path = MvnUrlUtil.pathFromMaven(key);
            File target = new File(systemDirectory + "/" + path);
            if (!target.exists()) {
                File source = resolve(key);
                target.getParentFile().mkdirs();
                try {
                    InputStream is = new FileInputStream(source);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
                    int count = 0;
                    while ((count = is.read(buffer)) > 0)
                    {
                        bos.write(buffer, 0, count);
                    }
                    bos.close();
                } catch (IOException e) {
                    getLog().error("Could not copy bundle " + key, e);
                }

            }
        }

        try {
            OutputStream out = new FileOutputStream(startupPropertiesFile);
            try {
                startupProperties.save(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new MojoFailureException("Could not write startup.properties file at " + startupPropertiesFile, e);
        }
        if (buf.length() > 0) {
            throw new MojoExecutionException("Could not unpack all dependencies:\n" + buf.toString());
        }
    }

    private boolean acceptScope(Artifact artifact) {
        return "compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope());
    }


    public File resolve(String id) {
        id = MvnUrlUtil.mvnToAether(id);
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(id));
        request.setRepositories(remoteRepos);

        getLog().debug("Resolving artifact " + id +
                " from " + remoteRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            getLog().warn("could not resolve " + id, e);
            return null;
        }

        getLog().debug("Resolved artifact " + id + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());
        return result.getArtifact().getFile();
    }

    private class OfflineFeaturesService implements FeaturesService {
        private static final String FEATURES_REPOSITORIES = "featuresRepositories";

        public void validateRepository(URI uri) throws Exception {
        }

        public void addRepository(URI uri) throws Exception {
            if (unpackToLocalRepo) {
                getLog().info("Adding feature repository to local-repo: " + uri);
                if (featuresCfgFile.exists()) {
                    Properties properties = new Properties();
                    InputStream in = new FileInputStream(featuresCfgFile);
                    try {
                        properties.load(in);
                    } finally {
                        in.close();
                    }
                    String existingFeatureRepos = properties.containsKey(FEATURES_REPOSITORIES)? properties.get(FEATURES_REPOSITORIES) + ",": "";
                    existingFeatureRepos = existingFeatureRepos + uri.toString();
                    properties.put(FEATURES_REPOSITORIES, existingFeatureRepos);
                    FileOutputStream out = new FileOutputStream(featuresCfgFile);
                    try {
                        properties.save(out);
                    } finally {
                        out.close();
                    }
                    Features repo = readFeatures(uri);
                    for (Feature feature: repo.getFeature()) {
                        if (features.contains(feature.getName())) {
                            installFeature(feature, null);
                        }
                    }
                }
            } else {
                getLog().info("Installing feature to system and startup.properties");
                File repoFile;
                Features features = readFeatures(uri);
                for (Feature feature: features.getFeature()) {
                    installFeature(feature, null);
                }


            }
        }

        private Features readFeatures(URI uri) throws XMLStreamException, JAXBException, IOException {
            File repoFile;
            if (uri.toString().startsWith("mvn:")) {
                String featuresPath = repoPath + "/" + MvnUrlUtil.pathFromMaven(uri.toString());
                repoFile = new File(featuresPath);
            } else {
                repoFile = new File(uri);
            }
            InputStream in = new FileInputStream(repoFile);
            Features features;
            try {
                features = JaxbUtil.unmarshal(in, false);
            } finally {
                in.close();
            }
            return features;
        }

        public void removeRepository(URI url) {
        }

        public void restoreRepository(URI url) throws Exception {
        }

        public Repository[] listRepositories() {
            return new Repository[0];
        }

        public void installFeature(String name) throws Exception {
        }

        public void installFeature(String name, String version) throws Exception {
        }

        public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        }

        public void installFeature(org.apache.karaf.features.Feature feature, EnumSet<Option> options) throws Exception {
            List<String> comment = Arrays.asList(new String[] {"", "# feature: " + feature.getName() + " version: " + feature.getVersion()});
            for (BundleInfo bundle: feature.getBundles()) {
                String location = bundle.getLocation();
                String startLevel = Integer.toString(bundle.getStartLevel() == 0? defaultStartLevel: bundle.getStartLevel());
                if (startupProperties.containsKey(location)) {
                    int oldStartLevel = Integer.decode(startupProperties.get(location));
                    if (oldStartLevel > bundle.getStartLevel()) {
                        startupProperties.put(location, startLevel);
                    }
                } else {
                    if (comment == null) {
                        startupProperties.put(location, startLevel);
                    } else {
                        startupProperties.put(location, comment, startLevel);
                        comment = null;
                    }
                }
            }
        }

        public void installFeatures(Set<org.apache.karaf.features.Feature> features, EnumSet<Option> options) throws Exception {
        }

        public void uninstallFeature(String name) throws Exception {
        }

        public void uninstallFeature(String name, String version) throws Exception {
        }

        public Feature[] listFeatures() throws Exception {
            return new Feature[0];
        }

        public Feature[] listInstalledFeatures() {
            return new Feature[0];
        }

        public boolean isInstalled(org.apache.karaf.features.Feature f) {
            return false;
        }

        public org.apache.karaf.features.Feature getFeature(String name, String version) throws Exception {
            return null;
        }

        public org.apache.karaf.features.Feature getFeature(String name) throws Exception {
            return null;
        }
    }

    // when FELIX-2887 is ready we can use plain Properties again
    private static class CommentProperties extends Properties {

        private Map<String, Layout> layout;
        private Map<String, String> storage;

        public CommentProperties() {
            this.layout = (Map<String, Layout>) getField("layout");
            storage = (Map<String, String>) getField("storage");
        }

        private Object getField(String fieldName)  {
            try {
                Field l = Properties.class.getDeclaredField(fieldName);
                boolean old = l.isAccessible();
                l.setAccessible(true);
                Object layout = l.get(this);
                l.setAccessible(old);
                return layout;
            } catch (Exception e) {
                throw new RuntimeException("Could not access field " + fieldName, e);
            }
        }

        public String put(String key, List<String> commentLines, List<String> valueLines) {
            commentLines = new ArrayList<String>(commentLines);
            valueLines = new ArrayList<String>(valueLines);
            String escapedKey = escapeKey(key);
            int lastLine = valueLines.size() - 1;
            if (valueLines.isEmpty()) {
                valueLines.add(escapedKey + "=");
            } else if (!valueLines.get(0).trim().startsWith(escapedKey)) {
                valueLines.set(0, escapedKey + " = " + escapeJava(valueLines.get(0)) + (0 < lastLine? "\\": ""));
            }
            for (int i = 1; i < valueLines.size(); i++) {
                valueLines.set(i, escapeJava(valueLines.get(i)) + (i < lastLine? "\\": ""));
            }
            StringBuilder value = new StringBuilder();
            for (String line: valueLines) {
                value.append(line);
            }
            this.layout.put(key, new Layout(commentLines, valueLines));
            return storage.put(key, unescapeJava(value.toString()));
        }

        public String put(String key, List<String> commentLines, String value) {
            commentLines = new ArrayList<String>(commentLines);
            this.layout.put(key, new Layout(commentLines, null));
            return storage.put(key, value);
        }

        public String put(String key, String comment, String value) {
            return put(key, Collections.singletonList(comment), value);
        }

        public List<String> getRaw(String key) {
            if (layout.containsKey(key)) {
                if (layout.get(key).getValueLines() != null) {
                    return new ArrayList<String>(layout.get(key).getValueLines());
                }
            }
            List<String> result = new ArrayList<String>();
            if (storage.containsKey(key)) {
                result.add(storage.get(key));
            }
            return result;
        }

        /** The list of possible key/value separators */
        private static final char[] SEPARATORS = new char[] {'=', ':'};

        /** The white space characters used as key/value separators. */
        private static final char[] WHITE_SPACE = new char[] {' ', '\t', '\f'};
        /**
         * Escape the separators in the key.
         *
         * @param key the key
         * @return the escaped key
         */
        private static String escapeKey(String key)
        {
            StringBuffer newkey = new StringBuffer();

            for (int i = 0; i < key.length(); i++)
            {
                char c = key.charAt(i);

                if (contains(SEPARATORS, c) || contains(WHITE_SPACE, c))
                {
                    // escape the separator
                    newkey.append('\\');
                    newkey.append(c);
                }
                else
                {
                    newkey.append(c);
                }
            }

            return newkey.toString();
        }

    }

}
