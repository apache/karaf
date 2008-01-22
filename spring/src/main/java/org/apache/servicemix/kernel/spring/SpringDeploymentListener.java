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
package org.apache.servicemix.kernel.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.kernel.filemonitor.DeploymentListener;

/**
 * A deployment listener that listens for spring xml applications
 * and creates bundles for these.
 */
public class SpringDeploymentListener implements DeploymentListener {


    private static final Log LOGGER = LogFactory.getLog(SpringDeploymentListener.class);

    private DocumentBuilderFactory dbf;

    public boolean canHandle(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("beans".equals(name) && "http://www.springframework.org/schema/beans".equals(uri)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public File handle(File artifact, File tmpDir) {
        InputStream is = null;
        OutputStream os = null;
        try {
            Document doc = parse(artifact);
            String name = artifact.getName();
            String artifactId = name.substring(0, name.length() - 4);
            String version = "0.0.0";
            File destFile = new File(tmpDir, name + ".jar");
            Manifest m = new Manifest();
            m.getMainAttributes().putValue("Manifest-Version", "2");
            m.getMainAttributes().putValue("Bundle-SymbolicName", artifactId);
            m.getMainAttributes().putValue("Bundle-Version", version);
            m.getMainAttributes().putValue("Spring-Context", "*;publish-context:=true;create-asynchronously:=true");
            String importPkgs = getImportPackages(doc);
            if (importPkgs != null && importPkgs.length() > 0) {
                m.getMainAttributes().putValue("Import-Package", importPkgs);
            }

            os = new FileOutputStream(destFile);
            JarOutputStream out = new JarOutputStream(os);
            ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
            out.putNextEntry(e);
            m.write(out);
            out.closeEntry();
            e = new ZipEntry("META-INF/");
            out.putNextEntry(e);
            e = new ZipEntry("META-INF/spring/");
            out.putNextEntry(e);
            out.closeEntry();
            e = new ZipEntry("META-INF/spring/" + artifact.getName());
            out.putNextEntry(e);
            is = new FileInputStream(artifact);
            copyInputStream(is, out);
            out.closeEntry();
            out.close();
            return destFile;
        } catch (Exception e) {
            LOGGER.info("Unable to build spring application bundle", e);
            return null;
        } finally {
            try {
                is.close();
            } catch (Exception e) { }
            try {
                os.close();
            } catch (Exception e) { }
        }
    }

    protected String getImportPackages(Document doc) {
        Set<String> packages = getBeanPackages(doc);
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(pkg);
        }
        return sb.toString();
    }

    protected Set<String> getBeanPackages(Document doc) {
        Set<String> packages = new HashSet<String>();
        extractBeanPackages(doc, packages);
        return packages;
    }

    private void extractBeanPackages(Node node, Set<String> packages) {
        if (node instanceof Element) {
            Element element = (Element) node;
            String name = element.getLocalName();
            String uri  = element.getNamespaceURI();
            if ("bean".equals(name) && "http://www.springframework.org/schema/beans".equals(uri)) {
                String clazz = element.getAttribute("class");
                if (clazz != null) {
                    String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
                    packages.add(pkg);
                }
            }
        }
        if (node != null) {
            extractBeanPackages(node.getFirstChild(), packages);
            extractBeanPackages(node.getNextSibling(), packages);
        }
    }

    protected Document parse(File artifact) throws Exception {
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(artifact);
    }

    /**
     * Copy in stream to an out stream
     *
     * @param in
     * @param out
     * @throws java.io.IOException
     */
    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
    }

}
