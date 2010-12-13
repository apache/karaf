package org.apache.karaf.diagnostic.common;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.apache.karaf.diagnostic.core.Dump;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

public class FeaturesDump implements Dump {

	private final FeaturesService service;

	public FeaturesDump(FeaturesService service) {
		this.service = service;
	}

	public InputStream createResource() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		writer.write("Repositories:\n");

		for (Repository repo : service.listRepositories()) {
			writer.write(repo.getURI() + "(" + repo.getName() + ")\n");
		}

		writer.write("\nInstalled features:\n");
		for (Feature feature : service.listInstalledFeatures()) {
			writer.write(feature.getName() + " " + feature.getVersion());
			writer.write("\tBundles:\n");
			for (BundleInfo bundle : feature.getBundles()) {
				writer.write("\t" + bundle.getLocation());
				if (bundle.getStartLevel() != 0) {
					writer.write(" start level " + bundle.getStartLevel());
				}
				writer.write("\n");
			}
		}
		writer.flush();

		return new ByteArrayInputStream(outputStream.toByteArray());
	}

	public String getName() {
		return "features.txt";
	}

}
