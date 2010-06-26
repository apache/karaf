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
package org.apache.felix.karaf.features.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.felix.karaf.features.Repository;
import org.apache.felix.karaf.features.Feature;
import org.xml.sax.SAXException;

/**
 * The repository implementation.
 */
public class RepositoryImpl implements Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryImpl.class);
    private int unnamedRepoId = 0;
    private String name;
    private URI uri;
    private List<Feature> features;
    private List<URI> repositories;

    public RepositoryImpl(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public URI getURI() {
        return uri;
    }

    public URI[] getRepositories() throws Exception {
        if (repositories == null) {
            load();
        }
        return repositories.toArray(new URI[repositories.size()]);
    }

    public Feature[] getFeatures() throws Exception {
        if (features == null) {
            load();
        }
        return features.toArray(new Feature[features.size()]);
    }

    public void load() throws IOException {
        try {
            repositories = new ArrayList<URI>();
            features = new ArrayList<Feature>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            URLConnection conn = uri.toURL().openConnection();
            conn.setDefaultUseCaches(false);
            Document doc = factory.newDocumentBuilder().parse(conn.getInputStream());
            String temp = doc.getDocumentElement().getAttribute( "name" );
            if ("".equals(temp)) {
                name = "repo-" + String.valueOf(unnamedRepoId++);
            }
            else {
                name = temp;
            }
            if ( uri.toString().startsWith( "bundle" ) ) {
                name += "*";
            }
            
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                if ("repository".equals(node.getNodeName())) {
                    Element e = (Element) nodes.item(i);
                    try {
                        URI newrepo = new URI(e.getTextContent());
                        repositories.add(newrepo);
                    } catch (URISyntaxException ex) {
                        LOGGER.error("Could not load feature repository: " + ex.getMessage() + " in feature repository " + uri);
                    }
                } else if ("feature".equals(node.getNodeName())) {
                    Element e = (Element) nodes.item(i);
                    String name = e.getAttribute("name");
                    String version = e.getAttribute("version");
                    FeatureImpl f;
                    if (version != null && version.length() > 0) {
                        f = new FeatureImpl(name, version);
                    } else {
                        f = new FeatureImpl(name);
                    }

                    NodeList featureNodes = e.getElementsByTagName("feature");
                    for (int j = 0; j < featureNodes.getLength(); j++) {
                        Element b = (Element) featureNodes.item(j);
                        String dependencyFeatureVersion = b.getAttribute("version");
                        if (dependencyFeatureVersion != null && dependencyFeatureVersion.length() > 0) {
                        	f.addDependency(new FeatureImpl(b.getTextContent(), dependencyFeatureVersion));
                        } else {
                        	f.addDependency(new FeatureImpl(b.getTextContent()));
                        }
                    }
                    NodeList configNodes = e.getElementsByTagName("config");
                    for (int j = 0; j < configNodes.getLength(); j++) {
                        Element c = (Element) configNodes.item(j);
                        String cfgName = c.getAttribute("name");
                        String data = c.getTextContent();
                        Properties properties = new Properties();
                        properties.load(new ByteArrayInputStream(data.getBytes()));
                        interpolation(properties);
                        Map<String, String> hashtable = new Hashtable<String, String>();
                        for (Object key : properties.keySet()) {
                            String n = key.toString();
                            hashtable.put(n, properties.getProperty(n));
                        }
                        f.addConfig(cfgName, hashtable);
                    }
                    NodeList bundleNodes = e.getElementsByTagName("bundle");
                    for (int j = 0; j < bundleNodes.getLength(); j++) {
                        Element b = (Element) bundleNodes.item(j);
                        f.addBundle(b.getTextContent());
                    }
                    features.add(f);
                }
            }
        } catch (SAXException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (ParserConfigurationException e) {
            throw (IOException) new IOException().initCause(e);
        } catch (IllegalArgumentException e) {
            throw (IOException) new IOException(e.getMessage() + " : " + uri).initCause(e);
        } catch (Exception e) {
            throw (IOException) new IOException(e.getMessage() + " : " + uri).initCause(e);
        }
    }

    protected void interpolation(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String val = properties.getProperty(key);
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(val);
            while (matcher.find()) {
                String rep = System.getProperty(matcher.group(1));
                if (rep != null) {
                    val = val.replace(matcher.group(0), rep);
                    matcher.reset(val);
                }
            }
            properties.put(key, val);
        }
    }

}
