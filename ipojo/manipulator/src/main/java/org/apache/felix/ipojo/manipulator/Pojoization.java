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
package org.apache.felix.ipojo.manipulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulation.Manipulator;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.ParseException;
import org.apache.felix.ipojo.xml.parser.XMLMetadataParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Pojoization allows creating an iPOJO bundle from a "normal" bundle.  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Pojoization {

    /**
     * List of component types.
     */
    private List m_components;

    /**
     * Metadata (in internal format).
     */
    private Element[] m_metadata;

    /**
     * Errors occured during the manipulation.
     */
    private List m_errors = new ArrayList();

    /**
     * Warnings occured during the manipulation.
     */
    private List m_warnings = new ArrayList();

    /**
     * Class map (jar entry, byte[]).
     */
    private HashMap m_classes = new HashMap();

    /**
     * Referendec packages by the composite.
     */
    private List m_referredPackages;

    /**
     * Add an error in the error list.
     * @param mes : error message.
     */
    private void error(String mes) {
        System.out.println("An error occurs during the pojoization : " + mes);
        m_errors.add(mes);
    }

    /**
     * Add a warning in the warning list.
     * @param mes : warning message
     */
    public void warn(String mes) {
        System.out.println("An warning occurs during the pojoization : " + mes);
        m_warnings.add(mes);
    }

    public List getErrors() {
        return m_errors;
    }

    /**
     * Manipulate a normal bundle.
     * It will create an iPOJO bundle based on the given metadata file.
     * The original and final bundle must be different.
     * @param in : original bundle.
     * @param out : final bundle.
     * @param metadataFile : iPOJO metadata file (XML). 
     */
    public void pojoization(File in, File out, File metadataFile) {
        // Get the metadata.xml location
        String path = metadataFile.getAbsolutePath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        m_metadata = parseXMLMetadata(path);

        // Get the list of declared component
        m_components = getDeclaredComponents(m_metadata);

        // Start the manipulation
        manipulation(in, out);

        // Check that all declared components are manipulated
        for (int i = 0; i < m_components.size(); i++) {
            ComponentInfo ci = (ComponentInfo) m_components.get(i);
            if (!ci.m_isManipulated) {
                error("The component " + ci.m_classname + " is declared but not in the bundle");
            }
        }
    }

    /**
     * Manipulate the Bundle.
     * @param in : original bundle
     * @param out : final bundle
     */
    private void manipulation(File in, File out) {
        // Get a jar file from the given file
        JarFile inputJar = null;
        try {
            inputJar = new JarFile(in);
        } catch (IOException e) {
            error("Cannot the input file is not a JarFile : " + in.getAbsolutePath());
            return;
        }

        manipulateComponents(inputJar); // Manipulate classes
        Manifest mf = doManifest(inputJar); // Compute the manifest

        // Create a new Jar file
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            fos = new FileOutputStream(out);
            jos = new JarOutputStream(fos, mf);
        } catch (FileNotFoundException e1) {
            error("Cannot manipulate the Jar file : the file " + out.getAbsolutePath() + " not found");
            return;
        } catch (IOException e) {
            error("Cannot manipulate the Jar file : cannot access to " + out.getAbsolutePath());
            return;
        }

        try {
            // Copy classes and resources
            Enumeration entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry curEntry = (JarEntry) entries.nextElement();
                // Check if we need to manipulate the clazz
                if (m_classes.containsKey(curEntry.getName())) {
                    JarEntry je = new JarEntry(curEntry.getName());
                    byte[] outClazz = (byte[]) m_classes.get(curEntry.getName());
                    if (outClazz.length != 0) {
                        jos.putNextEntry(je); // copy the entry header to jos
                        jos.write(outClazz);
                        jos.closeEntry();
                    } else { // The class is already manipulated
                        InputStream currIn = inputJar.getInputStream(curEntry);
                        int c;
                        int i = 0;
                        while ((c = currIn.read()) >= 0) {
                            jos.write(c);
                            i++;
                        }
                        currIn.close();
                        jos.closeEntry();
                    }
                } else {
                    // Do not copy the manifest
                    if (!curEntry.getName().equals("META-INF/MANIFEST.MF")) {
                        // copy the entry header to jos
                        jos.putNextEntry(curEntry);
                        InputStream currIn = inputJar.getInputStream(curEntry);
                        int c;
                        int i = 0;
                        while ((c = currIn.read()) >= 0) {
                            jos.write(c);
                            i++;
                        }
                        currIn.close();
                        jos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            error("Cannot manipulate the Jar file : " + e.getMessage() + " - Cause : " + e.getCause());
            return;
        }

        try {
            inputJar.close();
            jos.close();
            fos.close();
            jos = null;
            fos = null;
        } catch (IOException e) {
            error("Cannot close the new Jar file : " + e.getMessage() + " - Cause : " + e.getCause());
            return;
        }
    }

    /**
     * Manipulate classes of the input Jar.
     * @param inputJar : input bundle.
     */
    private void manipulateComponents(JarFile inputJar) {
        Enumeration entries = inputJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry curEntry = (JarEntry) entries.nextElement();
            // Check if we need to manipulate the clazz
            for (int i = 0; i < m_components.size(); i++) {
                ComponentInfo ci = (ComponentInfo) m_components.get(i);
                if (ci.m_classname.equals(curEntry.getName())) {
                    byte[] outClazz = manipulateComponent(inputJar, curEntry, ci);
                    m_classes.put(curEntry.getName(), outClazz);
                }
            }
        }
    }

    /**
     * Create the manifest.
     * Set the bundle activator, imports, iPOJO-components clauses
     * @param initial : initial Jar file.
     * @return the generated manifest.
     */
    private Manifest doManifest(JarFile initial) {
        Manifest mf = null;
        try {
            mf = initial.getManifest(); // Get the initial manifest
        } catch (IOException e) {
            e.printStackTrace();
        }
        Attributes att = mf.getMainAttributes();
        setImports(att); // Set the imports (add ipojo and handler namespaces
        setPOJOMetadata(att); // Add iPOJO-Component
        setCreatedBy(att); // Add iPOJO to the creators
        return mf;
    }

    /**
     * Manipulate a component class.
     * @param inputJar : input bundle
     * @param je : Jar entry of the classes
     * @param ci : attached component info (containing metadata and manipulation metadata)
     * @return the generated class (byte array)
     */
    private byte[] manipulateComponent(JarFile inputJar, JarEntry je, ComponentInfo ci) {
        Manipulator man = new Manipulator();
        try {
            InputStream currIn = inputJar.getInputStream(je);
            byte[] in = new byte[0];
            int c;
            while ((c = currIn.read()) >= 0) {
                byte[] in2 = new byte[in.length + 1];
                System.arraycopy(in, 0, in2, 0, in.length);
                in2[in.length] = (byte) c;
                in = in2;
            }
            currIn.close();
            byte[] out = man.manipulate(in); // iPOJO manipulation
            // Insert information to metadata
            ci.m_componentMetadata.addElement(man.getManipulationMetadata());
            ci.m_isManipulated = true;
            return out;
        } catch (IOException e) {
            error("Cannot manipulate the class " + je.getName() + " : " + e.getMessage());
            return null;
        }
    }

    /**
     * Return the list of "concrete" component.
     * @param meta : metadata.
     * @return the list of compionent info requiring a manipulation.
     */
    private List getDeclaredComponents(Element[] meta) {
        List componentClazzes = new ArrayList();
        for (int i = 0; i < meta.length; i++) {
            if (meta[i].getName().equalsIgnoreCase("component") && meta[i].containsAttribute("className")) {
                String name = meta[i].getAttribute("classname");
                name = name.replace('.', '/');
                name += ".class";
                componentClazzes.add(new ComponentInfo(name, meta[i]));
            }
        }
        return componentClazzes;
    }

    /**
     * Component Info.
     * Represent a component type to be manipulated or already manipulated.
     * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
     */
    private class ComponentInfo {
        /**
         * Component Type metadata.
         */
        Element m_componentMetadata;

        /**
         * Component Type implementation class.
         */
        String m_classname;

        /**
         * Is the class already manipulated. 
         */
        boolean m_isManipulated;

        /**
         * Constructor.
         * @param cn : class name
         * @param met : component type metadata
         */
        ComponentInfo(String cn, Element met) {
            this.m_classname = cn;
            this.m_componentMetadata = met;
            m_isManipulated = false;
        }
    }

    /**
     * Set the create-by in the manifest.
     * @param att : manifest attribute.
     */
    private void setCreatedBy(Attributes att) {
        String prev = att.getValue("Created-By");
        att.putValue("Created-By", prev + " & iPOJO");
    }

    /**
     * Add imports to the given manifest attribute list. This method add ipojo imports and handler imports (if needed).
     * @param att : the manifest attribute list to modify.
     */
    private void setImports(Attributes att) {
        Map imports = parseHeader(att.getValue("Import-Package"));
        Map ver = new TreeMap();
        ver.put("version", "0.7.3");
        if (!imports.containsKey("org.apache.felix.ipojo")) {
            imports.put("org.apache.felix.ipojo", ver);
        }
        if (!imports.containsKey("org.apache.felix.ipojo.architecture")) {
            imports.put("org.apache.felix.ipojo.architecture", ver);
        }
        if (!imports.containsKey("org.osgi.service.cm")) {
            Map verCM = new TreeMap();
            verCM.put("version", "1.2");
            imports.put("org.osgi.service.cm", verCM);
        }
        if (!imports.containsKey("org.osgi.service.log")) {
            Map verCM = new TreeMap();
            verCM.put("version", "1.3");
            imports.put("org.osgi.service.log", verCM);
        }
        

        // Add handler namespace
        String[][] namespaces = computeHandlerNamespace();
        for (int j = 0; j < namespaces.length; j++) {
            for (int k = 0; k < namespaces[j].length; k++) {
                if (!namespaces[j][k].equals("")) {
                    int lastIndex = namespaces[j][k].lastIndexOf('.');
                    String ns = namespaces[j][k].substring(0, lastIndex);
                    if (!imports.containsKey(ns)) {
                        imports.put(ns, new TreeMap());
                    }
                }
            }
        }

        // Add refered imports from the metadata
        for (int i = 0; i < m_referredPackages.size(); i++) {
            String pack = (String) m_referredPackages.get(i);
            imports.put(pack, new TreeMap());
        }

        // Write imports
        att.putValue("Import-Package", printClauses(imports, "resolution:"));
    }

    /**
     * Add iPOJO-Components to the given manifest attribute list. This method add the iPOJO-Components header and its value (according to the metadata) to the manifest.
     * @param att : the manifest attribute list to modify.
     */
    private void setPOJOMetadata(Attributes att) {
        String meta = "";
        for (int i = 0; i < m_metadata.length; i++) {
            meta += buildManifestMetadata(m_metadata[i], "");
        }
        att.putValue("iPOJO-Components", meta);
    }

    /**
     * Build the list of namespaces used in the metadata. (first-order only). 
     * @return the list of namespaces [array of component [ array of namespace ] ].
     */
    private String[][] computeHandlerNamespace() {
        String[][] ns = new String[m_metadata.length][];
        for (int i = 0; i < m_metadata.length; i++) {
            ns[i] = m_metadata[i].getNamespaces();
        }
        return ns;
    }

    /**
     * Standard OSGi header parser. This parser can handle the format clauses ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '=' value )
     * This is mapped to a Map { name => Map { attr|directive => value } }
     * 
     * @param value : String to parse.
     * @return parsed map.
     */
    public Map parseHeader(String value) {
        if (value == null || value.trim().length() == 0) {
            return new HashMap();
        }

        Map result = new LinkedHashMap();
        QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
        char del;
        do {
            boolean hadAttribute = false;
            Map clause = new HashMap();
            List aliases = new ArrayList();
            aliases.add(qt.nextToken());
            del = qt.getSeparator();
            while (del == ';') {
                String adname = qt.nextToken();
                if ((del = qt.getSeparator()) != '=') {
                    if (hadAttribute) {
                        throw new IllegalArgumentException("Header contains name field after attribute or directive: " + adname + " from " + value);
                    }
                    aliases.add(adname);
                } else {
                    String advalue = qt.nextToken();
                    clause.put(adname, advalue);
                    del = qt.getSeparator();
                    hadAttribute = true;
                }
            }
            for (Iterator i = aliases.iterator(); i.hasNext();) {
                result.put(i.next(), clause);
            }
        } while (del == ',');
        return result;
    }

    /**
     * Print a standard Map based OSGi header.
     * 
     * @param exports : map { name => Map { attribute|directive => value } }
     * @param allowedDirectives : list of allowed directives.
     * @return the clauses
     */
    public String printClauses(Map exports, String allowedDirectives) {
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator i = exports.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            Map map = (Map) exports.get(name);
            sb.append(del);
            sb.append(name);

            for (Iterator j = map.keySet().iterator(); j.hasNext();) {
                String key = (String) j.next();

                // Skip directives we do not recognize
                if (key.endsWith(":") && allowedDirectives.indexOf(key) < 0) {
                    continue;
                }

                String value = (String) map.get(key);
                sb.append(";");
                sb.append(key);
                sb.append("=");
                boolean dirty = value.indexOf(',') >= 0 || value.indexOf(';') >= 0;
                if (dirty) {
                    sb.append("\"");
                }
                sb.append(value);
                if (dirty) {
                    sb.append("\"");
                }
            }
            del = ", ";
        }
        return sb.toString();
    }

    /**
     * Parse XML Metadata.
     * @param path : path of the file to parse.
     * @return the parsed element array.
     */
    private Element[] parseXMLMetadata(String path) {
        File metadata = new File(path);
        URL url;
        Element[] meta = null;
        try {
            url = metadata.toURI().toURL();
            if (url == null) {
                error("Cannot find the metdata file : " + path);
                return null;
            }

            InputStream stream = url.openStream();
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            XMLMetadataParser handler = new XMLMetadataParser();
            parser.setContentHandler(handler);
            InputSource is = new InputSource(stream);
            parser.parse(is);
            meta = handler.getMetadata();
            m_referredPackages = handler.getReferredPackages();
            stream.close();

        } catch (MalformedURLException e) {
            error("Malformed Mtadata URL for " + path);
            return null;
        } catch (IOException e) {
            error("Cannot open the file : " + path);
            return null;
        } catch (ParseException e) {
            error("Parsing Error when parsing the XML file " + path + " : " + e.getMessage());
            return null;
        } catch (SAXException e) {
            error("Parsing Error when parsing (Sax Error) the XML file " + path + " : " + e.getMessage());
            return null;
        }

        if (meta == null || meta.length == 0) {
            warn("Neither component, neither instance in " + path);
        }

        return meta;
    }

    /**
     * Generate manipulation metadata.
     * @param element : actual element. 
     * @param actual : actual manipulation metadata.
     * @return : given amnipulation metadata + manipulation metadata of the given element.
     */
    private String buildManifestMetadata(Element element, String actual) {
        String result = "";
        if (element.getNameSpace().equals("")) {
            result = actual + element.getName() + " { ";
        } else {
            result = actual + element.getNameSpace() + ":" + element.getName() + " { ";
        }

        for (int i = 0; i < element.getAttributes().length; i++) {
            if (element.getAttributes()[i].getNameSpace().equals("")) {
                result = result + "$" + element.getAttributes()[i].getName() + "=\"" + element.getAttributes()[i].getValue() + "\" ";
            } else {
                result = result + "$" + element.getAttributes()[i].getNameSpace() + ":" + element.getAttributes()[i].getName() + "=\"" + element.getAttributes()[i].getValue() + "\" ";
            }
        }

        for (int i = 0; i < element.getElements().length; i++) {
            result = buildManifestMetadata(element.getElements()[i], result);
        }

        return result + "}";
    }

    public List getWarnings() {
        return m_warnings;
    }

}
