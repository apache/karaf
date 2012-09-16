package org.apache.karaf.features;

import java.util.List;
import java.util.Map;

public interface Conditional {

    List<? extends Dependency> getCondition();

    List<Dependency> getDependencies();

    List<BundleInfo> getBundles();

    Map<String, Map<String, String>> getConfigurations();

    List<ConfigFileInfo> getConfigurationFiles();

    Feature asFeature(String name, String version);
}
