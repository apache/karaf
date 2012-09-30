package org.apache.karaf.features.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.karaf.features.Feature;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

public class TestBase {
    public Bundle createDummyBundle(long id, final String symbolicName, Dictionary<String,String> headers) {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(id).anyTimes();
        expect(bundle.getSymbolicName()).andReturn(symbolicName);
        expect(bundle.getHeaders()).andReturn(headers).anyTimes();
        BundleStartLevel sl = EasyMock.createMock(BundleStartLevel.class);
        expect(sl.isPersistentlyStarted()).andReturn(true);
        expect(bundle.adapt(BundleStartLevel.class)).andReturn(sl );
        replay(bundle);
        return bundle;
    }
    
    public Dictionary<String, String> headers(String ... keyAndHeader) {
        Hashtable<String, String> headersTable = new Hashtable<String, String>();
        int c=0;
        while (c < keyAndHeader.length) {
            String key = keyAndHeader[c++];
            String value = keyAndHeader[c++];
            headersTable.put(key, value);
        }
        return headersTable;
        
    }
    
    public Map<String, Map<String, Feature>> features(Feature ... features) {
        final Map<String, Map<String, Feature>> featuresMap = new HashMap<String, Map<String,Feature>>();
        for (Feature feature : features) {
            Map<String, Feature> featureVersion = getOrCreate(featuresMap, feature);
            featureVersion.put(feature.getVersion(), feature);
        }
        return featuresMap;
    }
    
    private Map<String, Feature> getOrCreate(final Map<String, Map<String, Feature>> featuresMap, Feature feature) {
        Map<String, Feature> featureVersion = featuresMap.get(feature.getName());
        if (featureVersion == null) {
            featureVersion = new HashMap<String, Feature>();
            featuresMap.put(feature.getName(), featureVersion);
        }
        return featureVersion;
    }

    public Feature feature(String name, String version) {
        return new org.apache.karaf.features.internal.model.Feature(name, version);
    }
}
