package aQute.bnd.build;

import java.io.*;
import java.util.*;

public class Container {
    public enum TYPE {
        REPO, PROJECT, EXTERNAL, LIBRARY, ERROR
    }

    final File                file;
    final TYPE                type;
    final String              bsn;
    final String              version;
    final String              error;
    final Project             project;
    final Map<String, String> attributes;

    Container(Project project, String bsn, String version, TYPE type,
            File source, String error, Map<String, String> attributes) {
        this.bsn = bsn;
        this.version = version;
        this.type = type;
        this.file = source != null ? source : new File("/" + bsn + ":"
                + version + ":" + type);
        this.project = project;
        this.error = error;
        if (attributes == null || attributes.isEmpty())
            this.attributes = Collections.emptyMap();
        else
            this.attributes = attributes;
    }

    public Container(Project project, File file) {
        this(project, file.getName(), "project", TYPE.PROJECT, file, null, null);
    }

    public Container(File file) {
        this(null, file.getName(), "project", TYPE.EXTERNAL, file, null, null);
    }

    public File getFile() {
        return file;
    }

    public String getBundleSymbolicName() {
        return bsn;
    }

    public String getVersion() {
        return version;
    }

    public TYPE getType() {
        return type;
    }

    public String getError() {
        return error;
    }

    public boolean equals(Object other) {
        if (other instanceof Container)
            return file.equals(((Container) other).file);
        else
            return false;
    }

    public int hashCode() {
        return file.hashCode();
    }

    public Project getProject() {
        return project;
    }

    /**
     * Must show the file name or the error formatted as a file name
     * 
     * @return
     */
    public String toString() {
        if (getError() != null)
            return "/error/" + getError();
        else
            return getFile().getAbsolutePath();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Return the this if this is anything else but a library. If it is a
     * library, return the members. This could work recursively, e.g., libraries
     * can point to libraries.
     * 
     * @return
     * @throws Exception
     */
    public List<Container> getMembers() throws Exception {
        List<Container> result = project.newList();

        // Are ww a library? If no, we are the result
        if (getType() != TYPE.LIBRARY)
            result.add(this);
        else {
            // We are a library, parse the file. This is
            // basically a specification clause per line.
            // I.e. you can do bsn; version, bsn2; version. But also
            // spread it out over lines.
            InputStream in = new FileInputStream(file);
            BufferedReader rd = new BufferedReader(new InputStreamReader(in,
                    "UTF-8"));
            try {
                String line;
                while ((line = rd.readLine()) != null) {
                    line = line.trim();
                    if (!line.startsWith("#") && line.length() > 0) {
                        List<Container> list = project.getBundles(
                                Workspace.STRATEGY_EXACT, line);
                        result.addAll(list);
                    }
                }
            } finally {
                in.close();
            }
        }
        return result;
    }
}
