/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features.model;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Repository {

    private URI uri;
    private List<Feature> features;
    private List<String> repositories;
    private Integer defaultStartLevel;

    public Repository(URI uri, Integer defaultStartLevel) {
        this.uri = uri;
        this.defaultStartLevel = defaultStartLevel;
    }

    public URI getURI() {
        return uri;
    }

    public Feature[] getFeatures() {
        if (features == null) {
            loadFeatures();
        }
        return features.toArray(new Feature[features.size()]);
    }

    public String[] getDefinedRepositories() {
        if (repositories == null) {
            loadRepositories();
        }
        return repositories.toArray(new String[repositories.size()]);
    }

    private void loadRepositories() {
        try {
            repositories = new ArrayList<String>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(uri.toURL().openStream());
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (!(node instanceof Element) || !"repository".equals(node.getNodeName())) {
                    continue;
                }
                Element e = (Element) nodes.item(i);
                repositories.add(e.getTextContent().trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading feature descriptors from " + this.uri, e);
        }
    }

    private void loadFeatures() {
        try {
            features = new ArrayList<Feature>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(uri.toURL().openStream());
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (!(node instanceof Element) || !"feature".equals(node.getNodeName())) {
                    continue;
                }
                Element e = (Element) nodes.item(i);
                String name = e.getAttribute("name");
                String version = e.getAttribute("version");
                Feature f = new Feature(name);
                f.setVersion(version);
                NodeList featureNodes = e.getElementsByTagName("feature");
                for (int j = 0; j < featureNodes.getLength(); j++) {
                    Element b = (Element) featureNodes.item(j);
                    f.addDependency(b.getTextContent());
                }
                NodeList configNodes = e.getElementsByTagName("config");
                for (int j = 0; j < configNodes.getLength(); j++) {
                    Element c = (Element) configNodes.item(j);
                    String cfgName = c.getAttribute("name");
                    String data = c.getTextContent();
                    Properties properties = new Properties();
                    properties.load(new ByteArrayInputStream(data.getBytes()));
                    Map<String, String> hashtable = new Hashtable<String, String>();
                    for (Object key : properties.keySet()) {
                        String n = key.toString();
                        hashtable.put(n, properties.getProperty(n));
                    }
                    f.addConfig(cfgName, hashtable);
                }
                NodeList configFileNodes = e.getElementsByTagName("configfile");
                for (int j = 0; j < configFileNodes.getLength(); j++) {
                    Element c = (Element) configFileNodes.item(j);
                    f.addConfigFile(c.getTextContent());
                }
                NodeList bundleNodes = e.getElementsByTagName("bundle");
                for (int j = 0; j < bundleNodes.getLength(); j++) {
                    Element b = (Element) bundleNodes.item(j);
                    Integer startLevel = getInt(b, "start-level", defaultStartLevel);
                    f.addBundle(new BundleRef(b.getTextContent(), startLevel));
                }
                features.add(f);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading features for " + this.uri, e);
        }
    }

    private Integer getInt(Element el, String key, Integer defaultValue) {
        Integer value;
        try {
            value = Integer.parseInt(el.getAttribute(key));
        } catch (Exception e1) {
            value = null;
        }
        return (value == null || value == 0) ? defaultValue : value;
    }

}