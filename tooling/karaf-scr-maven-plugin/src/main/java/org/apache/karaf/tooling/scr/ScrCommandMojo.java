package org.apache.karaf.tooling.scr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * The <code>ScrCommandMojo</code> generates a service descriptor file based
 * on annotations found in the sources.
 *
 * @goal scr
 * @phase process-classes
 * @threadSafe
 * @description Build Service Descriptors from Java Source
 * @requiresDependencyResolution compile
 */
public class ScrCommandMojo extends AbstractMojo {

    /**
     * The Maven project.
     *
     * @parameter expression="project"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}/generated-scr-commands"
     */
    private File outputDirectory;

    /**
     * @parameter
     */
    private int ranking;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ClassFinder finder = new ClassFinder(getClassLoader());
            List<String> components = new ArrayList<String>();
            boolean hasCommand = false;
            for (Class<?> clazz : finder.findAnnotatedClasses(Service.class)) {

                Command cmd = clazz.getAnnotation(Command.class);
                if (cmd != null) {
                    System.out.println("\nFound command: " + clazz.getName() + "\n\t" + cmd.scope() + ":" + cmd.name() + "\n");

                    StringBuilder sb = new StringBuilder();
                    sb.append("<?xml version='1.1'?>\n");
                    sb.append("<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0\" name=\"")
                            .append(clazz.getName())
                            .append("\" activate=\"activate\" deactivate=\"deactivate\">\n");
                    sb.append("  <implementation class=\"").append(ScrCommandSupport.class.getName()).append("\"/>\n");
                    sb.append("  <service>\n");
                    sb.append("    <provide interface=\"").append(Function.class.getName()).append("\"/>\n");
                    sb.append("    <provide interface=\"").append(CompletableFunction.class.getName()).append("\"/>\n");
                    sb.append("    <provide interface=\"").append(CommandWithAction.class.getName()).append("\"/>\n");
                    sb.append("  </service>\n");
                    Map<String, Class> refs = getReferences(clazz);
                    for (String key : refs.keySet()) {
                        sb.append("  <reference name=\"").append(key).append("\" cardinality=\"1..1\" interface=\"")
                                .append(refs.get(key).getName()).append("\"/>\n");
                    }
                    sb.append("  <property name=\"hidden.component\" value=\"true\"/>\n");
                    if (ranking != 0) {
                        sb.append("  <property name=\"service.ranking\" value=\"").append(ranking).append("\"/>\n");
                    }
                    sb.append("  <property name=\"").append(CommandProcessor.COMMAND_SCOPE).append("\" value=\"").append(cmd.scope()).append("\"/>\n");
                    sb.append("  <property name=\"").append(CommandProcessor.COMMAND_FUNCTION).append("\" value=\"").append(cmd.name()).append("\"/>\n");
                    sb.append("</scr:component>\n");
                    String component = "OSGI-INF/" + clazz.getName() + ".xml";
                    components.add(component);
                    File file = new File(outputDirectory, component);
                    file.getParentFile().mkdirs();
                    Writer w = new FileWriter(file);
                    w.write(sb.toString());
                    w.close();
                    hasCommand = true;
                } else {
                    System.out.println("\nFound service: " + clazz.getName() + "\n");

                    StringBuilder sb = new StringBuilder();
                    sb.append("<?xml version='1.1'?>\n");
                    sb.append("<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0\" name=\"")
                            .append(clazz.getName())
                            .append("\" activate=\"activate\" deactivate=\"deactivate\">\n");
                    sb.append("  <implementation class=\"").append(clazz.getName()).append("\"/>\n");
                    sb.append("  <service>\n");
                    List<Class> allClasses = new ArrayList<Class>();
                    addAllClasses(allClasses, clazz);
                    for (Class cl : allClasses) {
                        sb.append("    <provide interface=\"").append(cl.getName()).append("\"/>\n");
                    }
                    sb.append("  </service>\n");
                    Map<String, Class> refs = getReferences(clazz);
                    for (String key : refs.keySet()) {
                        sb.append("  <reference name=\"").append(key).append("\" cardinality=\"1..1\" interface=\"")
                                .append(refs.get(key).getName()).append("\"");
                        String[] bind = getBindMethods(clazz, key, refs.get(key));
                        if (bind[0] != null) {
                            sb.append(" bind=\"").append(bind[0]).append("\"");
                        }
                        if (bind[1] != null) {
                            sb.append(" unbind=\"").append(bind[1]).append("\"");
                        }
                        sb.append("/>\n");
                    }
                    sb.append("  <property name=\"hidden.component\" value=\"true\"/>\n");
                    if (ranking != 0) {
                        sb.append("  <property name=\"service.ranking\" value=\"").append(ranking).append("\"/>\n");
                    }
                    sb.append("</scr:component>\n");
                    String component = "OSGI-INF/" + clazz.getName() + ".xml";
                    components.add(component);
                    File file = new File(outputDirectory, component);
                    file.getParentFile().mkdirs();
                    Writer w = new FileWriter(file);
                    w.write(sb.toString());
                    w.close();
                }
            }
            if (!components.isEmpty()) {
                if (hasCommand) {
                    String name = ScrCommandSupport.class.getName().replace('.', '/') + ".class";
                    File file = new File(outputDirectory, name);
                    file.getParentFile().mkdirs();
                    URL url = getClass().getClassLoader().getResource(name);
                    InputStream is = url.openStream();
                    FileOutputStream fos = new FileOutputStream(file);
                    copy(is, fos);
                    is.close();
                    fos.close();
                    name = ScrCommandSupport.class.getName();
                    name = name.substring(0, name.lastIndexOf('.'));
                    setPrivatePackageHeader(name);
                }
                setServiceComponentHeader(components);
                updateProjectResources();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing SCR command scanner", e);
        }
    }

    private String[] getBindMethods(Class<?> clazz, String key, Class type) {
        String cap = key.substring(0, 1).toUpperCase() + key.substring(1);
        Method bind = null;
        Method unbind = null;
        try {
            bind = clazz.getMethod("set" + cap, type);
        } catch (NoSuchMethodException e0) {
            try {
                bind = clazz.getMethod("bind" + cap, type);
            } catch (NoSuchMethodException e1) {
            }
        }
        if (bind != null) {
            try {
                unbind = clazz.getMethod("un" + bind.getName(), type);
            } catch (NoSuchMethodException e0) {
            }
        }
        return new String[] {
                bind != null ? bind.getName() : null,
                unbind != null ? unbind.getName() : null
        };
    }

    private void addAllClasses(List<Class> allClasses, Class<?> clazz) {
        if (clazz != null && clazz != Object.class) {
            if (allClasses.add(clazz)) {
                addAllClasses(allClasses, clazz.getSuperclass());
                for (Class cl : clazz.getInterfaces()) {
                    addAllClasses(allClasses, cl);
                }
            }
        }
    }

    private Map<String, Class> getReferences(Class<?> clazz) {
        Map<String, Class> refs = new HashMap<String, Class>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(Reference.class) != null) {
                    refs.put(field.getName(), field.getType());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return refs;
    }

    private ClassLoader getClassLoader() throws MojoFailureException, DependencyResolutionRequiredException, MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (Object object : project.getCompileClasspathElements()) {
            String path = (String) object;
            urls.add(new File(path).toURL());
        }
        ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[] {}), getClass().getClassLoader());
        return classLoader;
    }

    private void setPrivatePackageHeader(String pkg) {
        String header = project.getProperties().getProperty("Private-Package");
        if (header != null && header.length() > 0) {
            header += "," + pkg;
        } else {
            header = pkg;
        }
        project.getProperties().setProperty("Private-Package", header);
        System.out.println("\nPrivate-Package: " + header + "\n");
    }

    /**
     * Set the service component header based on the scr files.
     */
    private void setServiceComponentHeader(final List<String> files) {
        if ( files != null && files.size() > 0 ) {
            final String svcHeader = project.getProperties().getProperty("Service-Component");
            final Set<String> xmlFiles = new HashSet<String>();
            if ( svcHeader != null ) {
                final StringTokenizer st = new StringTokenizer(svcHeader, ",");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    xmlFiles.add(token.trim());
                }
            }

            for(final String path : files) {
                xmlFiles.add(path);
            }
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(final String entry : xmlFiles) {
                if ( !first ) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(entry);
            }
            project.getProperties().setProperty("Service-Component", sb.toString());
            System.out.println("\nService-Component: " + sb.toString() + "\n");
        }
    }

    /**
     * Update the Maven project resources.
     */
    private void updateProjectResources() {
        // now add the descriptor directory to the maven resources
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        @SuppressWarnings("unchecked")
        final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
        while (!found && rsrcIterator.hasNext()) {
            final Resource rsrc = rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if (!found) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }
    }

    static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int l;
        while ((l = is.read(buffer)) > 0) {
            os.write(buffer, 0, l);
        }
    }

}
