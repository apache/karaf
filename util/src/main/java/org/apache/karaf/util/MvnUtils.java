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
package org.apache.karaf.util;

import java.net.URL;

public class MvnUtils {

    private MvnUtils() { }


    /**
     * Compute the path of url, expanding it if the url uses the mvn protocol.
     *
     * @param url the url
     * @return the path
     */
    public static String getMvnPath(URL url) {
        if (url.getProtocol().equals("mvn")) {
            String[] parts = url.toExternalForm().substring(4).split("/");
            String groupId;
            String artifactId;
            String version;
            String type;
            String qualifier;
            if (parts.length < 3 || parts.length > 5) {
                return url.getPath();
            }
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
            type = (parts.length >= 4) ?  "." + parts[3] : ".jar";
            qualifier = (parts.length >= 5) ? "-" + parts[4] :  "";
            return groupId.replace('.', '/') + "/" + artifactId + "/"
                    + version + "/" + artifactId + "-" + version + qualifier + type;
        }
        return url.getPath();
    }
}
