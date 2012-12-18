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
package org.apache.karaf.deployer.blueprint;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.karaf.util.DeployerUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.osgi.framework.Constants;

public class BlueprintTransformer {

    static Transformer transformer;
    static DocumentBuilderFactory dbf;
    static TransformerFactory tf;


    public static void transform(URL url, OutputStream os) throws Exception {
        // Build dom document
        Document doc = parse(url);
        // Heuristicly retrieve name and version
        String name = url.getPath();
        int idx = name.lastIndexOf('/');
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        String[] str = DeployerUtils.extractNameVersionType(name);
        // Create manifest
        Manifest m = new Manifest();
        m.getMainAttributes().putValue("Manifest-Version", "2");
        m.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        m.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, str[0]);
        m.getMainAttributes().putValue(Constants.BUNDLE_VERSION, str[1]);
        String importPkgs = getImportPackages(analyze(new DOMSource(doc)));
        if (importPkgs != null && importPkgs.length() > 0) {
            m.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, importPkgs);
        }
        m.getMainAttributes().putValue(Constants.DYNAMICIMPORT_PACKAGE, "*");
        // Extract manifest entries from the DOM
        NodeList l = doc.getElementsByTagName("manifest");
        if (l != null) {
            for (int i = 0; i < l.getLength(); i++) {
                Element e = (Element) l.item(i);
                String text = e.getTextContent();
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(text.trim().getBytes()));
                Enumeration en = props.propertyNames();
                while (en.hasMoreElements()) {
                    String k = (String) en.nextElement();
                    String v = props.getProperty(k);
                    m.getMainAttributes().putValue(k, v);
                }
                e.getParentNode().removeChild(e);
            }
        }

        JarOutputStream out = new JarOutputStream(os);
        ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
        out.putNextEntry(e);
        m.write(out);
        out.closeEntry();
        e = new ZipEntry("OSGI-INF/");
        out.putNextEntry(e);
        e = new ZipEntry("OSGI-INF/blueprint/");
        out.putNextEntry(e);
        out.closeEntry();
        // check .xml file extension
        if( !name.endsWith(".xml")) {
            name +=".xml";
        }
        e = new ZipEntry("OSGI-INF/blueprint/" + name);
        out.putNextEntry(e);
        // Copy the new DOM
        if (tf == null) {
            tf = TransformerFactory.newInstance();
        }
        tf.newTransformer().transform(new DOMSource(doc), new StreamResult(out));
        out.closeEntry();
        out.close();
    }

    public static Set<String> analyze(Source source) throws Exception {
        if (transformer == null) {
            if (tf == null) {
                tf = TransformerFactory.newInstance();
            }
            Source s = new StreamSource(BlueprintTransformer.class.getResourceAsStream("extract.xsl"));
            transformer = tf.newTransformer(s);
        }

        Set<String> refers = new TreeSet<String>();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Result r = new StreamResult(bout);
        transformer.transform(source, r);

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        bout.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(bin));

        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            if (line.length() > 0) {
                String parts[] = line.split("\\s*,\\s*");
                for (int i = 0; i < parts.length; i++) {
                    int n = parts[i].lastIndexOf('.');
                    if (n > 0) {
                        String pkg = parts[i].substring(0, n);
                        if (!pkg.startsWith("java.")) {
                            refers.add(pkg);
                        }
                    }
                }
            }
            line = br.readLine();
        }
        br.close();
        return refers;
    }

    protected static String getImportPackages(Set<String> packages) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(pkg);
        }
        return sb.toString();
    }

    protected static Document parse(URL url) throws Exception {
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(url.toString());
    }

    protected static void copyInputStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[4096];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
    }
}
