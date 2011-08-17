/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.common;

import java.io.OutputStreamWriter;

import org.apache.karaf.diagnostic.core.common.TextDumpProvider;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

/**
 * Dump provider which add file named features.txt with informations
 * about installed features and repositories.
 */
public class FeaturesDumpProvider extends TextDumpProvider {

    /**
     * Feature service.
     */
    private final FeaturesService features;

    /**
     * Creates new dump entry witch contains information about
     * karaf features.
     * 
     * @param features Feature service.
     */
    public FeaturesDumpProvider(FeaturesService features) {
        super("features.txt");
        this.features = features;
    }

    /**
     * {@inheritDoc}
     */
    protected void writeDump(OutputStreamWriter outputStreamWriter) throws Exception {
        // creates header
        outputStreamWriter.write("Repositories:\n");

        // list repositories
        for (Repository repo : features.listRepositories()) {
            outputStreamWriter.write(repo.getURI() + " (" + repo.getName() + ")\n");
        }

        // list features
        outputStreamWriter.write("\nfeatures:\n");
        for (Feature feature : features.listFeatures()) {
            outputStreamWriter.write(feature.getName() + " " + feature.getVersion());
            outputStreamWriter.write(" installed: " + features.isInstalled(feature));
            outputStreamWriter.write("\nBundles:\n");
            for (BundleInfo bundle : feature.getBundles()) {
                outputStreamWriter.write("\t" + bundle.getLocation());
                if (bundle.getStartLevel() != 0) {
                    outputStreamWriter.write(" start level " + bundle.getStartLevel());
                }
                outputStreamWriter.write("\n\n");
            }
        }

        // flush & close stream
        outputStreamWriter.close();
    }

}
