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
package org.apache.karaf.features.internal.download.impl;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DownloadManagerHelper {

    private static final String DEFAULT_IGNORED_PROTOCOL_PATTERN = "jar|war|war-i|warref|webbundle|wrap|spring|blueprint";
    private static Pattern ignoredProtocolPattern;

    static {
        setIgnoredProtocolPattern(DEFAULT_IGNORED_PROTOCOL_PATTERN);
    }

    private DownloadManagerHelper() {
        //Utility Class
    }

    public static Pattern getIgnoredProtocolPattern(){
        return ignoredProtocolPattern;
    }

    private static void setIgnoredProtocolPattern(String pattern){
        String defaultPattRegex = "^(" + pattern + "):.*$";
        ignoredProtocolPattern = Pattern.compile(defaultPattRegex);
    }

    public static void setExtraProtocols( Collection<String> protocols ){
        StringBuilder sb = new StringBuilder( DEFAULT_IGNORED_PROTOCOL_PATTERN );
        for (String proto : protocols) {
            sb.append("|").append(proto);
        }

        setIgnoredProtocolPattern(sb.toString());
    }

    /**
     * Strips download urls from wrapper protocols.
     *
     * @param url the given url.
     * @return the stripped URL for the wrapper protocols.
     */
    public static String stripUrl(String url) {
        String strippedUrl = url;
        Matcher matcher = ignoredProtocolPattern.matcher(strippedUrl);
        while (matcher.matches()) {
            String protocol = matcher.group(1);
            strippedUrl = strippedUrl.substring(protocol.length() + 1);
            matcher = ignoredProtocolPattern.matcher(strippedUrl);
        }
        if (strippedUrl.contains("?")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.indexOf('?'));
        }
        if (strippedUrl.contains("$")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.indexOf('$'));
        }
        if (strippedUrl.contains("#")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.indexOf('#'));
        }
        if (strippedUrl.contains(";start-level=")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.indexOf(";start-level="));
        }
        return strippedUrl;
    }
    
    public static String stripStartLevel(String url) {
        String strippedUrl = url;
        if (strippedUrl.contains(";start-level=")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.indexOf(";start-level="));
        }
        return strippedUrl;
    }

    public static String stripInlinedMavenRepositoryUrl(String url) {
        if (url.startsWith("mvn:") && url.contains("!")) {
            return url.substring(4, url.indexOf('!'));
        }
        return null;
    }

    public static String removeInlinedMavenRepositoryUrl(String url) {
        if (url.startsWith("mvn:") && url.contains("!")) {
            return "mvn:" + url.substring(url.indexOf('!') + 1);
        }
        return url;
    }
}
