/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class FeatureReader {
    List<BundleInfo> readBundles(URI featureUri, String featureName) throws Exception {
        InputStream is = featureUri.toURL().openStream();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        is.close();
        NodeList features = doc.getElementsByTagName("feature");
        for (int c=0; c<features.getLength(); c++) {
            Element feature = (Element) features.item(c);
            String name = feature.getAttribute("name");
            if (featureName.equals(name)) {
                NodeList bundleNodes = feature.getElementsByTagName("bundle");
                return getBundles(bundleNodes);
            }
        }
        return new ArrayList<BundleInfo>();
    }

    private List<BundleInfo> getBundles(NodeList bundleNodes) throws URISyntaxException {
        ArrayList<BundleInfo> bundles = new ArrayList<BundleInfo>();
        for (int c=0; c<bundleNodes.getLength(); c++) {
            Element bundleNode = (Element)bundleNodes.item(c);
            String startLevel = bundleNode.getAttribute("start-level");
            String uri = bundleNode.getFirstChild().getNodeValue();
            BundleInfo bi = new BundleInfo();
            if (startLevel != null) {
                bi.startLevel = new Integer(startLevel);
            }
            bi.uri = new URI(uri);
            bundles.add(bi);
        }
        return bundles;
    }
}
