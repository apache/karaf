/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi.eclipse;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import aQute.libg.reporter.*;

/**
 * Parse the Eclipse project information for the classpath. Unfortunately, it is
 * impossible to read the variables. They are ignored but that can cause
 * problems.
 * 
 * @version $Revision: 1.1 $
 */
public class EclipseClasspath {
    static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                                                                 .newInstance();
    DocumentBuilder               db;
    File                          project;
    File                          workspace;
    Set<File>                     sources                = new LinkedHashSet<File>();
    Set<File>                     allSources                = new LinkedHashSet<File>();
    
    Set<File>                     classpath              = new LinkedHashSet<File>();
    List<File>                    dependents             = new ArrayList<File>();
    File                          output;
    boolean                       recurse                = true;
    Set<File>                     exports                = new LinkedHashSet<File>();
    Map<String, String>           properties             = new HashMap<String, String>();
    Reporter                      reporter;
    int                           options;
    Set<File>                     bootclasspath          = new LinkedHashSet<File>();

    public final static int       DO_VARIABLES           = 1;

    /**
     * Parse an Eclipse project structure to discover the classpath.
     * 
     * @param workspace
     *            Points to workspace
     * @param project
     *            Points to project
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */

    public EclipseClasspath(Reporter reporter, File workspace, File project,
            int options) throws Exception {
        this.project = project.getCanonicalFile();
        this.workspace = workspace.getCanonicalFile();
        this.reporter = reporter;
        db = documentBuilderFactory.newDocumentBuilder();
        parse(this.project, true);
        db = null;
    }

    public EclipseClasspath(Reporter reporter, File workspace, File project)
            throws Exception {
        this(reporter, workspace, project, 0);
    }

    /**
     * Recursive routine to parse the files. If a sub project is detected, it is
     * parsed before the parsing continues. This should give the right order.
     * 
     * @param project
     *            Project directory
     * @param top
     *            If this is the top project
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    void parse(File project, boolean top) throws ParserConfigurationException,
            SAXException, IOException {
        File file = new File(project, ".classpath");
        if (!file.exists())
            throw new FileNotFoundException(".classpath file not found: "
                    + file.getAbsolutePath());

        Document doc = db.parse(file);
        NodeList nodelist = doc.getDocumentElement().getElementsByTagName(
                "classpathentry");

        if (nodelist == null)
            throw new IllegalArgumentException(
                    "Can not find classpathentry in classpath file");

        for (int i = 0; i < nodelist.getLength(); i++) {
            Node node = nodelist.item(i);
            NamedNodeMap attrs = node.getAttributes();
            String kind = get(attrs, "kind");
            if ("src".equals(kind)) {
                String path = get(attrs, "path");
                // TODO boolean exported = "true".equalsIgnoreCase(get(attrs,
                // "exported"));
                if (path.startsWith("/")) {
                    // We have another project
                    File subProject = getFile(workspace, project, path);
                    if (recurse)
                        parse(subProject, false);
                    dependents.add(subProject.getCanonicalFile());
                } else {
                    File src = getFile(workspace, project, path);
                    allSources.add(src);
                    if (top) {
                        // We only want the sources for our own project
                        // or we'll compile all at once. Not a good idea
                        // because project settings can differ.
                        sources.add(src);
                    }
                }
            } else if ("lib".equals(kind)) {
                String path = get(attrs, "path");
                boolean exported = "true".equalsIgnoreCase(get(attrs,
                        "exported"));
                if (top || exported) {
                    File jar = getFile(workspace, project, path);
                    if (jar.getName().startsWith("ee."))
                        bootclasspath.add(jar);
                    else
                        classpath.add(jar);
                    if (exported)
                        exports.add(jar);
                }
            } else if ("output".equals(kind)) {
                String path = get(attrs, "path");
                path = path.replace('/', File.separatorChar);
                output = getFile(workspace, project, path);
                classpath.add(output);
                exports.add(output);
            } else if ("var".equals(kind)) {
                boolean exported = "true".equalsIgnoreCase(get(attrs,
                        "exported"));
                File lib = replaceVar(get(attrs, "path"));
                File slib = replaceVar(get(attrs, "sourcepath"));
                if (lib != null) {
                    classpath.add(lib);
                    if (exported)
                        exports.add(lib);
                }
                if (slib != null)
                    sources.add(slib);
            } else if ("con".equals(kind)) {
                // Should do something useful ...
            }
        }
    }

    private File getFile(File abs, File relative, String opath) {
        String path = opath.replace('/', File.separatorChar);
        File result = new File(path);
        if (result.isAbsolute() && result.isFile()) {
            return result;
        }
        if (path.startsWith(File.separator)) {
            result = abs;
            path = path.substring(1);
        } else
            result = relative;

        StringTokenizer st = new StringTokenizer(path, File.separator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            result = new File(result, token);
        }

        if (!result.exists())
            System.err.println("File not found: project=" + project
                    + " workspace=" + workspace + " path=" + opath + " file="
                    + result);
        return result;
    }

    static Pattern PATH = Pattern.compile("([A-Z_]+)/(.*)");

    private File replaceVar(String path) {
        if ((options & DO_VARIABLES) == 0)
            return null;

        Matcher m = PATH.matcher(path);
        if (m.matches()) {
            String var = m.group(1);
            String remainder = m.group(2);
            String base = (String) properties.get(var);
            if (base != null) {
                File b = new File(base);
                File f = new File(b, remainder.replace('/', File.separatorChar));
                return f;
            } else
                reporter.error("Can't find replacement variable for: " + path);
        } else
            reporter.error("Cant split variable path: " + path);
        return null;
    }

    private String get(NamedNodeMap map, String name) {
        Node node = map.getNamedItem(name);
        if (node == null)
            return null;

        return node.getNodeValue();
    }

    public Set<File> getClasspath() {
        return classpath;
    }

    public Set<File> getSourcepath() {
        return sources;
    }

    public File getOutput() {
        return output;
    }

    public List<File> getDependents() {
        return dependents;
    }

    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    public Set<File> getExports() {
        return exports;
    }

    public void setProperties(Map<String, String> map) {
        this.properties = map;
    }

    public Set<File> getBootclasspath() {
        return bootclasspath;
    }

    public Set<File> getAllSources() {
        return allSources;
    }

}
