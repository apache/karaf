package org.apache.karaf.bundle.core;

import java.util.List;

import org.osgi.framework.Bundle;

public interface BundleSelector {

    List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles, boolean mayAccessSystemBundle) throws Exception;

}
