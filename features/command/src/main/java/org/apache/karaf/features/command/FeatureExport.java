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
package org.apache.karaf.features.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AvailableFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.ops4j.pax.url.mvn.MavenResolver;

/**
 * Simple {@link FeaturesCommandSupport} implementation that allows a user in
 * the karaf shell to export the bundles associated with a given feature to the
 * file system. This is useful for several use cases, such as in the event you
 * need to deploy the functionality offered by a particular feature to an OBR
 * repository.
 * 
 */
@Service
@Command(scope = "feature", name = "export-bundles", description = "Export all of the bundles that make up a specified feature to a directory on the file system.")
public class FeatureExport extends FeaturesCommandSupport {

    /**
     * Inject a {@link MavenResolver} so we can translate from a
     * {@link BundleInfo} in a {@link Feature} into the raw bundle from maven.
     */
    @Reference
    private MavenResolver resolver;

    /**
     * The name of the feature you want to export.
     */
    @Argument(index = 0, name = "featureName", description = "The name of the feature you want to export bundles for", required = true, multiValued = false)
    @Completion(value = AvailableFeatureCompleter.class)
    private String featureName = null;

    /**
     * The location we'll export the bundles to.
     */
    @Argument(index = 1, name = "exportLocation", description = "Where you want to export the bundles", multiValued = false, required = true)
    @Completion(value = FileCompleter.class)
    private String exportLocation;

    /**
     * The version of the feature you want to export.
     */
    @Option(name = "-v", multiValued = false, aliases = {
            "--version" }, description = "The version of the feature you want to export bundles for.  Default is latest", required = false)
    private String featureVersion = null;

    /**
     * Option indicating that only bundles marked as a dependency should be
     * exported.
     */
    @Option(name = "-d", multiValued = false, aliases = {
            "--dependencies-only" }, description = "This flag indicates that only bundles marked as a dependency will be exported.", required = false)
    private boolean onlyDependencies = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doExecute(final FeaturesService featuresService) throws Exception {
        if (resolver == null) {
            throw new IllegalStateException("No maven resolver implementation found.");
        } else {
            final File destination = new File(exportLocation);
            if (!prepareDestination(destination)) {
                System.err.println("Invalid exportLocation specified: " + exportLocation);
            } else {
                final Feature feature = featureVersion != null ? featuresService.getFeature(featureName, featureVersion)
                        : featuresService.getFeature(featureName);
                if (feature == null) {
                    System.err.println("Could not find specified feature: '" + featureName + "' version '" + featureVersion + "'");
                } else {
                    // Save feature content bundles.
                    saveBundles(destination, feature, featuresService);
                }
            }
        }

    }

    /**
     * Prepare the target destination directory.
     * 
     * @param destination
     *            Where we'll save the bundles
     * @return true if it is valid, false otherwise
     */
    private boolean prepareDestination(final File destination) {
        return (destination.isDirectory() || destination.mkdirs());
    }

    /**
     * Save the feature bundles, and all of its transitive dependency bundles.
     * 
     * @param dest
     *            The target directory where we'll save the feature bundles
     * @param feature
     *            The {@link Feature} we're saving
     * @throws Exception
     *             If there is an issue saving the bundles or resolving the
     *             feature
     */
    private void saveBundles(final File dest, final Feature feature, final FeaturesService featuresService) throws Exception {
        // Save this feature's bundles.
        for (final BundleInfo info : feature.getBundles()) {
            if (!onlyDependencies || (onlyDependencies && info.isDependency())) {
                final File resolvedLocation = resolver.resolve(info.getLocation());
                if (copyFileToDirectory(resolvedLocation, dest)) {
                    System.out.println("Exported '" + feature.getName() + "/" + feature.getVersion() + "' bundle: " + info.getLocation());
                } else {
                    System.out.println("Already exported bundle: " + info.getLocation());
                }
            }
        }
        // Save feature's dependency bundles.
        for (final Dependency dependency : feature.getDependencies()) {
            final Feature dFeature = featuresService.getFeature(dependency.getName(), dependency.getVersion());
            if (dFeature != null) {
                saveBundles(dest, dFeature, featuresService);
            } else {
                System.err.println("Unable to resolve dependency feature! '" + dependency.getName() + "' '" + dependency.getVersion() + "'");
                throw new Exception("Unable to resolve dependency feature '" + dependency.getName() + "/" + dependency.getVersion() + "' while exporting '"
                        + featureName + "/" + featureVersion + "'");
            }
        }
    }

    /**
     * Simple method to copy a file to a target destination directory.
     * 
     * @param file
     *            The file to copy
     * @param directory
     *            The directory to copy it to
     * @return true if successful, false if it wasn't
     * @throws FileNotFoundException
     *             If the file specified doesn't exist
     * @throws IOException
     *             If there is an issue performing the copy
     */
    private static boolean copyFileToDirectory(final File file, final File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException("Can't copy to non-directory specified: " + directory.getAbsolutePath());
        } else {
            boolean copied = false;
            final File newFile = new File(directory.getAbsolutePath() + "/" + file.getName());
            if (!newFile.isFile()) {
                try (final FileInputStream fis = new FileInputStream(file)) {
                    try (final FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024 * 8];
                        int read = -1;
                        while ((read = fis.read(buffer)) >= 0) {
                            fos.write(buffer, 0, read);
                        }
                    }
                }
                copied = true;
            }
            return copied;
        }
    }

}
