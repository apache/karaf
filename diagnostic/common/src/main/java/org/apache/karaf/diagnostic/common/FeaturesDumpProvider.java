package org.apache.karaf.diagnostic.common;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.features.FeaturesService;

public class FeaturesDumpProvider implements DumpProvider {

	private final FeaturesService features;

	public FeaturesDumpProvider(FeaturesService features) {
		this.features = features;
	}

	public void createDump(DumpDestination destination) throws Exception {
		destination.add(new FeaturesDump(features));
	}

}
