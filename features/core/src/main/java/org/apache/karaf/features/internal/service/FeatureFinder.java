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
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class FeatureFinder implements ManagedService {

    final Map<String, String> nameToArtifactMap = new HashMap<>();

    public String[] getNames() {
        synchronized (nameToArtifactMap) {
            Set<String> strings = nameToArtifactMap.keySet();
            return strings.toArray(new String[strings.size()]);
        }
    }

    public URI getUriFor(String name, String version) {
        String url;
        synchronized (nameToArtifactMap) {
            url = nameToArtifactMap.get(name);
        }
        if (url == null) {
            return null;
        }
        if (version != null) {
            // replace the version in the URL with the provided one
            url = FeatureFinder.replaceVersion(url, version);
        }
        return URI.create(url);
    }

    @SuppressWarnings("rawtypes")
    public void updated(Dictionary properties) throws ConfigurationException {
        synchronized (nameToArtifactMap) {
            if (properties != null) {
                nameToArtifactMap.clear();
                Enumeration keys = properties.keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    if (!"felix.fileinstall.filename".equals(key) && !"service.pid".equals(key)) {
                        nameToArtifactMap.put(key, (String) properties.get(key));
                    }
                }
            }
        }
    }

    private static String replaceVersion(String url, String version) {
        if (url.startsWith("mvn:")) {
            // mvn:groupId/artifactId/version...
            int index = url.indexOf('/');
            index = url.indexOf('/', index + 1);

            String first = url.substring(0, index);
            index = url.indexOf('/', index + 1);
            String second = url.substring(index + 1);

            return first + "/" + version + "/" + second;
        }
        return url;
    }

}
