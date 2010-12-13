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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

/**
 * Dump provider which add file named features.txt with informations
 * about installed features and repositories. 
 * 
 * @author ldywicki
 */
public class FeaturesDumpProvider implements DumpProvider {

	/**
	 * Feature service.
	 */
	private final FeaturesService features;

	public FeaturesDumpProvider(FeaturesService features) {
		this.features = features;
	}

	public void createDump(DumpDestination destination) throws Exception {
		writeDump(destination.add("features.txt"));
	}

	private void writeDump(OutputStream outputStream) throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		writer.write("Repositories:\n");

		for (Repository repo : features.listRepositories()) {
			writer.write(repo.getURI() + " (" + repo.getName() + ")\n");
		}

		writer.write("\nfeatures:\n");
		for (Feature feature : features.listFeatures()) {
			writer.write(feature.getName() + " " + feature.getVersion());
			writer.write(" installed: " + features.isInstalled(feature));
			writer.write("\nBundles:\n");
			for (BundleInfo bundle : feature.getBundles()) {
				writer.write("\t" + bundle.getLocation());
				if (bundle.getStartLevel() != 0) {
					writer.write(" start level " + bundle.getStartLevel());
				}
				writer.write("\n");
			}
		}

		// flush & close stream
		writer.close();
	}

}
