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
package org.apache.karaf.features.internal.service;

import java.net.URI;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class FeatureRepoFinder implements ManagedService {
    public static final String FEATURES_REPOS_PID = "org.apache.karaf.features.repos";

    final Map<String, String> nameToArtifactMap = new HashMap<>();

    public static Dictionary<String, Object> getServiceProperties() {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, FeatureRepoFinder.FEATURES_REPOS_PID);
        return props;
    }

    public synchronized String[] getNames() {
        Set<String> strings = nameToArtifactMap.keySet();
        return strings.toArray(new String[strings.size()]);
    }

    public synchronized URI getUriFor(String name, String version) {
        String artifactUri = nameToArtifactMap.get(name);
        if (artifactUri == null) {
            return null;
        }
        if (version != null) {
            artifactUri = FeatureRepoFinder.replaceVersion(artifactUri, version);
        }
        return URI.create(artifactUri);
    }

    @SuppressWarnings("rawtypes")
    public synchronized void updated(Dictionary properties) throws ConfigurationException {
        if (properties != null) {
            nameToArtifactMap.clear();
            Enumeration keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                if (!isSystemKey(key)) {
                    nameToArtifactMap.put(key, (String)properties.get(key));
                }
            }
        }
    }

    private boolean isSystemKey(String key) {
        return "felix.fileinstall.filename".equals(key) || "service.pid".equals(key);
    }

    /**
     * Replace the version in the URL with the provided one. 
     * Only processes mvn urls like mvn:groupId/artifactId/version... 
     * 
     * @param url
     * @param version
     * @return
     */
    private static String replaceVersion(String url, String version) {
        if (url.startsWith("mvn:")) {
            int firstSlash = url.indexOf('/');
            int secondSlash = url.indexOf('/', firstSlash + 1);

            String before = url.substring(0, secondSlash);
            int thirdSlash = url.indexOf('/', secondSlash + 1);
            String after = url.substring(thirdSlash + 1);

            return before + "/" + version + "/" + after;
        }
        return url;
    }

}
