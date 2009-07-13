package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.help.*;
import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.bnd.test.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.eclipse.*;
import aQute.libg.sed.*;
import aQute.service.scripting.*;

/**
 * This class is NOT threadsafe
 * 
 * @author aqute
 * 
 */
public class Project extends Processor {

    final static String         DEFAULT_ACTIONS = "build; label='Build', test; label='Test', clean; label='Clean', release; label='Release', refreshAll; label=Refresh";
    public final static String  BNDFILE         = "bnd.bnd";
    public final static String  BNDCNF          = "cnf";
    final Workspace             workspace;
    long                        time;
    int                         count;
    boolean                     preparedPaths;
    final Collection<Project>   dependson       = new LinkedHashSet<Project>();
    final Collection<Container> buildpath       = new LinkedHashSet<Container>();
    final Collection<Container> runpath         = new LinkedHashSet<Container>();
    final Collection<File>      sourcepath      = new LinkedHashSet<File>();
    final Collection<File>      allsourcepath   = new LinkedHashSet<File>();
    final Collection<Container> bootclasspath   = new LinkedHashSet<Container>();
    final Collection<Container> runbundles      = new LinkedHashSet<Container>();
    File                        output;
    File                        target;
    boolean                     inPrepare;

    public Project(Workspace workspace, File projectDir, File buildFile)
            throws Exception {
        super(workspace);
        this.workspace = workspace;
        setFileMustExist(false);
        setProperties(buildFile);
        assert workspace != null;
        // For backward compatibility reasons, we also read
        readBuildProperties();
    }

    public Project(Workspace workspace, File buildDir) throws Exception {
        this(workspace, buildDir, new File(buildDir, BNDFILE));
    }

    private void readBuildProperties() throws Exception {
        try {
            File f = getFile("build.properties");
            if (f.isFile()) {
                Properties p = loadProperties(f);
                for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    String newkey = key;
                    if (key.indexOf('$') >= 0) {
                        newkey = getReplacer().process(key);
                    }
                    setProperty(newkey, p.getProperty(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Project getUnparented(File propertiesFile) throws Exception {
        propertiesFile = propertiesFile.getAbsoluteFile();
        Workspace workspace = new Workspace(propertiesFile.getParentFile());
        Project project = new Project(workspace, propertiesFile.getParentFile());
        project.setProperties(propertiesFile);
        project.setFileMustExist(true);
        return project;
    }

    public synchronized boolean isValid() {
        return getBase().isDirectory() && getPropertiesFile().isFile();
    }

    /**
     * Return a new builder that is nicely setup for this project. Please close
     * this builder after use.
     * 
     * @param parent
     *            The project builder to use as parent, use this project if null
     * @return
     * @throws Exception
     */
    public synchronized ProjectBuilder getBuilder(ProjectBuilder parent)
            throws Exception {

        ProjectBuilder builder;

        if (parent == null)
            builder = new ProjectBuilder(this);
        else
            builder = new ProjectBuilder(parent);

        builder.setBase(getBase());

        for (Container file : getBuildpath()) {
            builder.addClasspath(file.getFile());
        }

        for (Container file : getBootclasspath()) {
            builder.addClasspath(file.getFile());
        }

        for (File file : getAllsourcepath()) {
            builder.addSourcepath(file);
        }
        return builder;
    }

    public synchronized int getChanged() {
        return count;
    }

    public synchronized void setChanged() {
        // if (refresh()) {
        preparedPaths = false;
        count++;
        // }
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String toString() {
        return getBase().getName();
    }

    /**
     * Set up all the paths
     */

    public synchronized void prepare() throws Exception {
        if (inPrepare)
            throw new CircularDependencyException(toString());
        if (!preparedPaths) {
            inPrepare = true;
            try {
                dependson.clear();
                buildpath.clear();
                sourcepath.clear();
                allsourcepath.clear();
                bootclasspath.clear();
                runpath.clear();
                runbundles.clear();

                // We use a builder to construct all the properties for
                // use.
                setProperty("basedir", getBase().getAbsolutePath());

                // If a bnd.bnd file exists, we read it.
                // Otherwise, we just do the build properties.
                if (!getPropertiesFile().isFile()
                        && new File(getBase(), ".classpath").isFile()) {
                    // Get our Eclipse info, we might depend on other projects
                    // though ideally this should become empty and void
                    doEclipseClasspath();
                }

                // Calculate our source directory

                File src = new File(getBase(), getProperty("src", "src"));
                if (src.isDirectory()) {
                    sourcepath.add(src);
                    allsourcepath.add(src);
                } else
                    sourcepath.add(getBase());

                // Set default bin directory
                output = getFile(getProperty("bin", "bin")).getAbsoluteFile();
                if (output.isDirectory()) {
                    if (!buildpath.contains(output))
                        buildpath.add(new Container(this, output));
                } else {
                    if (!output.exists())
                        output.mkdirs();
                    if (!output.isDirectory())
                        error("Can not find output directory: " + output);
                }

                // Where we store all our generated stuff.
                target = getFile(getProperty("target", "generated"));

                // We might have some other projects we want build
                // before we do anything, but these projects are not in
                // our path. The -dependson allows you to build them before.

                List<Project> dependencies = new ArrayList<Project>();

                String dp = getProperty(Constants.DEPENDSON);
                Set<String> requiredProjectNames = parseHeader(dp).keySet();
                for (String p : requiredProjectNames) {
                    Project required = getWorkspace().getProject(p);
                    if (required == null)
                        error("No such project " + required + " on "
                                + Constants.DEPENDSON);
                    else {
                        dependencies.add(required);
                    }

                }

                // We have two paths that consists of repo files, projects,
                // or some other stuff. The doPath routine adds them to the
                // path and extracts the projects so we can build them before.

                doPath(buildpath, dependencies, parseBuildpath(), bootclasspath);
                doPath(runpath, dependencies, parseTestpath(), bootclasspath);
                doPath(runbundles, dependencies, parseTestbundles(), null);

                // We now know all dependend projects. But we also depend
                // on whatever those projects depend on. This creates an
                // ordered list without any duplicates. This of course assumes
                // that there is no circularity. However, this is checked
                // by the inPrepare flag, will throw an exception if we
                // are circular.

                Set<Project> done = new HashSet<Project>();
                done.add(this);
                allsourcepath.addAll(sourcepath);

                for (Project project : dependencies)
                    project.traverse(dependson, done);

                for (Project project : dependson) {
                    allsourcepath.addAll(project.getSourcepath());
                }
                if (isOk())
                    preparedPaths = true;
            } finally {
                inPrepare = false;
            }
        }
    }

    private void traverse(Collection<Project> dependencies, Set<Project> visited)
            throws Exception {
        if (visited.contains(this))
            return;

        visited.add(this);

        for (Project project : getDependson())
            project.traverse(dependencies, visited);

        dependencies.add(this);
    }

    /**
     * Iterate over the entries and place the projects on the projects list and
     * all the files of the entries on the resultpath.
     * 
     * @param resultpath
     *            The list that gets all the files
     * @param projects
     *            The list that gets any projects that are entries
     * @param entries
     *            The input list of classpath entries
     */
    private void doPath(Collection<Container> resultpath,
            Collection<Project> projects, Collection<Container> entries,
            Collection<Container> bootclasspath) {
        for (Container cpe : entries) {
            if (cpe.getError() != null)
                error(cpe.getError());
            else {
                if (cpe.getType() == Container.TYPE.PROJECT) {
                    projects.add(cpe.getProject());
                }
                if (bootclasspath != null
                        && cpe.getBundleSymbolicName().startsWith("ee.")
                        || cpe.getAttributes().containsKey("boot"))
                    bootclasspath.add(cpe);
                else
                    resultpath.add(cpe);
            }
        }
    }

    /**
     * Parse the list of bundles that are a prerequisite to this project.
     * 
     * Bundles are listed in repo specific names. So we just let our repo
     * plugins iterate over the list of bundles and we get the highest version
     * from them.
     * 
     * @return
     */

    private List<Container> parseBuildpath() throws Exception {
        return getBundles(Constants.STRATEGY_LOWEST,
                getProperty(Constants.BUILDPATH));
    }

    private List<Container> parseTestpath() throws Exception {
        return getBundles(Constants.STRATEGY_HIGHEST,
                getProperty(Constants.RUNPATH));
    }

    private List<Container> parseTestbundles() throws Exception {
        return getBundles(Constants.STRATEGY_HIGHEST,
                getProperty(Constants.RUNBUNDLES));
    }

    /**
     * Analyze the header and return a list of files that should be on the
     * build, test or some other path. The list is assumed to be a list of bsns
     * with a version specification. The special case of version=project
     * indicates there is a project in the same workspace. The path to the
     * output directory is calculated. The default directory ${bin} can be
     * overridden with the output attribute.
     * 
     * @param strategy
     *            STRATEGY_LOWEST or STRATEGY_HIGHEST
     * @param spec
     *            The header
     * @return
     */
    public List<Container> getBundles(int strategy, String spec)
            throws Exception {
        List<Container> result = new ArrayList<Container>();
        Map<String, Map<String, String>> bundles = parseHeader(spec);

        try {
            for (Iterator<Map.Entry<String, Map<String, String>>> i = bundles
                    .entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Map<String, String>> entry = i.next();
                String bsn = entry.getKey();
                Map<String, String> attrs = entry.getValue();

                Container found = null;

                String versionRange = attrs.get("version");

                if (versionRange != null && versionRange.equals("project")) {
                    Project project = getWorkspace().getProject(bsn);
                    if (project.exists()) {
                        File f = project.getOutput();
                        found = new Container(project, bsn, "project",
                                Container.TYPE.PROJECT, f, null, attrs);
                    } else {
                        error(
                                "Reference to project that does not exist in workspace\n"
                                        + "  Project       %s\n"
                                        + "  Specification %s", bsn, spec);
                        continue;
                    }
                } else if (versionRange != null && versionRange.equals("file")) {
                    File f = getFile(bsn);
                    String error = null;
                    if (!f.exists())
                        error = "File does not exist";
                    if (f.getName().endsWith(".lib")) {
                        found = new Container(this, bsn, "file",
                                Container.TYPE.LIBRARY, f, error, attrs);
                    } else {
                        found = new Container(this, bsn, "file",
                                Container.TYPE.EXTERNAL, f, error, attrs);
                    }
                } else {
                    found = getBundle(bsn, versionRange, strategy, attrs);
                }

                if (found != null) {
                    List<Container> libs = found.getMembers();
                    for (Container cc : libs) {
                        if (result.contains(cc))
                            warning("Multiple bundles with the same final URL: "
                                    + cc);

                        result.add(cc);
                    }
                } else {
                    // Oops, not a bundle in sight :-(
                    Container x = new Container(this, bsn, versionRange,
                            Container.TYPE.ERROR, null, bsn + ";version="
                                    + versionRange + " not found", attrs);
                    result.add(x);
                    warning("Can not find URL for bsn " + bsn);
                }
            }
        } catch (Exception e) {
            error("While tring to get the bundles from " + spec, e);
            e.printStackTrace();
        }
        return result;
    }

    public long getTime() {
        return time;
    }

    public Collection<Project> getDependson() throws Exception {
        prepare();
        return dependson;
    }

    public Collection<Container> getBuildpath() throws Exception {
        prepare();
        return buildpath;
    }

    public Collection<Container> getRunpath() throws Exception {
        prepare();
        return runpath;
    }

    public Collection<Container> getRunbundles() throws Exception {
        prepare();
        return runbundles;
    }

    public Collection<File> getSourcepath() throws Exception {
        prepare();
        return sourcepath;
    }

    public Collection<File> getAllsourcepath() throws Exception {
        prepare();
        return allsourcepath;
    }

    public Collection<Container> getBootclasspath() throws Exception {
        prepare();
        return bootclasspath;
    }

    public File getOutput() throws Exception {
        prepare();
        return output;
    }

    private void doEclipseClasspath() throws Exception {
        EclipseClasspath eclipse = new EclipseClasspath(this, getWorkspace()
                .getBase(), getBase());
        eclipse.setRecurse(false);

        // We get the file directories but in this case we need
        // to tell ant that the project names
        for (File dependent : eclipse.getDependents()) {
            Project required = workspace.getProject(dependent.getName());
            dependson.add(required);
        }
        for (File f : eclipse.getClasspath()) {
            buildpath.add(new Container(f));
        }
        for (File f : eclipse.getBootclasspath()) {
            bootclasspath.add(new Container(f));
        }
        sourcepath.addAll(eclipse.getSourcepath());
        allsourcepath.addAll(eclipse.getAllSources());
        output = eclipse.getOutput();
    }

    public String _p_dependson(String args[]) throws Exception {
        return list(args, toFiles(getDependson()));
    }

    private Collection<?> toFiles(Collection<Project> projects) {
        List<File> files = new ArrayList<File>();
        for (Project p : projects) {
            files.add(p.getBase());
        }
        return files;
    }

    public String _p_buildpath(String args[]) throws Exception {
        return list(args, getBuildpath());
    }

    public String _p_testpath(String args[]) throws Exception {
        return list(args, getRunpath());
    }

    public String _p_sourcepath(String args[]) throws Exception {
        return list(args, getSourcepath());
    }

    public String _p_allsourcepath(String args[]) throws Exception {
        return list(args, getAllsourcepath());
    }

    public String _p_bootclasspath(String args[]) throws Exception {
        return list(args, getBootclasspath());
    }

    public String _p_output(String args[]) throws Exception {
        if (args.length != 1)
            throw new IllegalArgumentException(
                    "${output} should not have arguments");
        return getOutput().getAbsolutePath();
    }

    private String list(String[] args, Collection<?> list) {
        if (args.length > 3)
            throw new IllegalArgumentException(
                    "${"
                            + args[0]
                            + "[;<separator>]} can only take a separator as argument, has "
                            + Arrays.toString(args));

        String separator = ",";
        if (args.length == 2) {
            separator = args[1];
        }

        return join(list, separator);
    }

    protected Object[] getMacroDomains() {
        return new Object[] { workspace };
    }

    public File release(Jar jar) throws Exception {
    	String name = getProperty(Constants.RELEASEREPO);
    	return release(name, jar);
    }

    /**
     * Release
     * @param name The repository name
     * @param jar
     * @return
     * @throws Exception
     */
    public File release(String name, Jar jar) throws Exception {
        List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
        RepositoryPlugin rp = null;
        for (RepositoryPlugin plugin : plugins) {
        	if (!plugin.canWrite()) {
        		continue;
        	}
            if (name == null) {
        		rp = plugin;
        		break;
            } else if (name.equals(plugin.getName())){
        		rp = plugin;
        		break;
            }
        }

        if (rp != null) {
            try {
                return rp.put(jar);
            } catch (Exception e) {
                error("Deploying " + jar.getName() + " on " + rp.getName(), e);
            } finally {
                jar.close();
            }
        }
        return null;
   	
    }
    
    public void release(boolean test) throws Exception {
    	String name = getProperty(Constants.RELEASEREPO);
    	release(name, test);
    }
    
    /**
     * Release
     * @param name The respository name
     * @param test Run testcases
     * @throws Exception
     */
    public void release(String name, boolean test) throws Exception {
        File[] jars = build(test);
        // If build fails jars will be null
        if (jars == null) {
        	return;
        }
        for (File jar : jars) {
            Jar j = new Jar(jar);
            release(name, j);
            j.close();
        }
    	
    }
    
    /**
     * Get a bundle from one of the plugin repositories.
     * 
     * @param bsn
     *            The bundle symbolic name
     * @param range
     *            The version range
     * @param lowest
     *            set to LOWEST or HIGHEST
     * @return the file object that points to the bundle or null if not found
     * @throws Exception
     *             when something goes wrong
     */
    public Container getBundle(String bsn, String range, int strategy,
            Map<String, String> attrs) throws Exception {
        List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);

        // If someone really wants the latest, lets give it to them.
        // regardless of they asked for a lowest strategy
        if (range != null && range.equals("latest"))
            strategy = STRATEGY_HIGHEST;

        for (RepositoryPlugin plugin : plugins) {
            File[] results = plugin.get(bsn, range);
            if (results != null && results.length > 0) {
                File f = results[strategy == STRATEGY_LOWEST ? 0
                        : results.length - 1];

                if (f.getName().endsWith("lib"))
                    return new Container(this, bsn, range,
                            Container.TYPE.LIBRARY, f, null, attrs);
                else
                    return new Container(this, bsn, range, Container.TYPE.REPO,
                            f, null, attrs);
            }
        }

        return new Container(this, bsn, range, Container.TYPE.ERROR, null, bsn
                + ";version=" + range + " Not found in " + plugins, null);
    }

    /**
     * Deploy the file (which must be a bundle) into the repository.
     * 
     * @param name The repository name
     * @param file
     *            bundle
     */
    public void deploy(String name, File file) throws Exception {
        List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
        RepositoryPlugin rp = null;
        for (RepositoryPlugin plugin : plugins) {
        	if (!plugin.canWrite()) {
        		continue;
        	}
            if (name == null) {
        		rp = plugin;
        		break;
            } else if (name.equals(plugin.getName())){
        		rp = plugin;
        		break;
            }
        }

        if (rp != null) {
            Jar jar = new Jar(file);
            try {
                rp.put(jar);
                return;
            } catch (Exception e) {
                error("Deploying " + file + " on " + rp.getName(), e);
            } finally {
                jar.close();
            }
            return;
        }
        trace("No repo found " + file);
        throw new IllegalArgumentException("No repository found for " + file);
    }

    /**
     * Deploy the file (which must be a bundle) into the repository.
     * 
     * @param file
     *            bundle
     */
    public void deploy(File file) throws Exception {
    	String name = getProperty(Constants.DEPLOYREPO);
    	deploy(name, file);
    }
    /**
     * Macro access to the repository
     * 
     * ${repo;<bsn>[;<version>[;<low|high>]]}
     */

    public String _repo(String args[]) throws Exception {
        if (args.length < 2)
            throw new IllegalArgumentException(
                    "Too few arguments for repo, syntax=: ${repo ';'<bsn> [ ; <version> ]}");

        String bsns = args[1];
        String version = null;
        int strategy = Constants.STRATEGY_HIGHEST;

        if (args.length > 2) {
            version = args[2];
            if (args.length == 4) {
                if (args[3].equalsIgnoreCase("HIGHEST"))
                    strategy = Constants.STRATEGY_HIGHEST;
                else if (args[3].equalsIgnoreCase("LOWEST"))
                    strategy = STRATEGY_LOWEST;
                else
                    error("${repo;<bsn>;<version>;<'highest'|'lowest'>} macro requires a strategy of 'highest' or 'lowest', and is "
                            + args[3]);
            }
        }

        Collection<String> parts = split(bsns);
        List<String> paths = new ArrayList<String>();

        for (String bsn : parts) {
            Container jar = getBundle(bsn, version, strategy, null);
            if (jar.getError() == null) {
                paths.add(jar.getFile().getAbsolutePath());
            } else {
                error("The ${repo} macro could not find " + bsn
                        + " in the repo, because " + jar.getError() + "\n"
                        + "Repositories     : "
                        + getPlugins(RepositoryPlugin.class) + "\n"
                        + "Strategy         : " + strategy + "\n"
                        + "Bsn              : " + bsn + ";version=" + version);
            }
        }
        return join(paths);
    }

    public File getTarget() throws Exception {
        prepare();
        return target;
    }

    public File[] build(boolean underTest) throws Exception {
        ProjectBuilder builder = getBuilder(null);
        if (underTest)
            builder.setProperty(Constants.UNDERTEST, "true");
        Jar jars[] = builder.builds();
        File files[] = new File[jars.length];

        File target = getTarget();
        target.mkdirs();

        for (int i = 0; i < jars.length; i++) {
            Jar jar = jars[i];
            try {
                String bsn = jar.getName();
                files[i] = new File(target, bsn + ".jar");
                String msg = "";
                if (!files[i].exists()
                        || files[i].lastModified() < jar.lastModified()) {
                    reportNewer(files[i].lastModified(), jar);
                    files[i].delete();
                    jar.write(files[i]);
                } else {
                    msg = "(not modified since "
                            + new Date(files[i].lastModified()) + ")";
                }
                trace(jar.getName() + " (" + files[i].getName() + ") "
                        + jar.getResources().size() + " " + msg);
            } finally {
                jar.close();
            }
        }
        getInfo(builder);
        builder.close();
        if (isOk())
            return files;
        else
            return null;
    }

    private void reportNewer(long lastModified, Jar jar) {
        if (isTrue(getProperty(Constants.REPORTNEWER))) {
            StringBuilder sb = new StringBuilder();
            String del = "Newer than " + new Date(lastModified);
            for (Map.Entry<String, Resource> entry : jar.getResources()
                    .entrySet()) {
                if (entry.getValue().lastModified() > lastModified) {
                    sb.append(del);
                    del = ", \n     ";
                    sb.append(entry.getKey());
                }
            }
            if (sb.length() > 0)
                warning(sb.toString());
        }
    }

    /**
     * Refresh if we are based on stale data. This also implies our workspace.
     */
    public boolean refresh() {
        boolean changed = false;
        if (isCnf()) {
            changed = workspace.refresh();
        }
        return super.refresh() || changed;
    }

    public boolean isCnf() {
        return getBase().getName().equals(Workspace.CNFDIR);
    }

    public void propertiesChanged() {
        super.propertiesChanged();
        preparedPaths = false;
    }

    public String getName() {
        return getBase().getName();
    }

    public Map<String, Action> getActions() {
        Map<String, Action> all = newMap();
        Map<String, Action> actions = newMap();
        fillActions(all);
        getWorkspace().fillActions(all);

        for (Map.Entry<String, Action> action : all.entrySet()) {
            String key = getReplacer().process(action.getKey());
            if (key != null && key.trim().length() != 0)
                actions.put(key, action.getValue());
        }
        return actions;
    }

    public void fillActions(Map<String, Action> all) {
        Map<String, Map<String, String>> actions = parseHeader(getProperty(
                "-actions", DEFAULT_ACTIONS));
        for (Map.Entry<String, Map<String, String>> entry : actions.entrySet()) {
            String key = Processor.removeDuplicateMarker(entry.getKey());
            Action action;

            if (entry.getValue().get("script") != null) {
                // TODO check for the type
                action = new ScriptAction(entry.getValue().get("type"), entry
                        .getValue().get("script"));
            } else {
                action = new ReflectAction(key);
            }
            String label = entry.getValue().get("label");
            all.put(label, action);
        }
    }

    public void release() throws Exception {
        release(false);
    }

    /**
     * Release.
     * @param name The repository name
     * @throws Exception
     */
    public void release(String name) throws Exception {
        release(name, false);
    }

    public void clean() throws Exception {
        File target = getTarget();
        if (target.isDirectory() && target.getParentFile() != null) {
            delete(target);
        }
    }

    public File[] build() throws Exception {
        return build(false);
    }

    public boolean test() throws Exception {
        boolean ok = true;
        String testbundles = getProperty(TESTBUNDLES);

        if (testbundles == null) {
            File jars[] = build(true);
            for (File jar : jars)
                ok &= test(jar);

        } else {
            List<Container> containers = getBundles(STRATEGY_HIGHEST,
                    testbundles);
            for (Container container : containers) {
                if (container.getError() == null) {
                    File jar = container.getFile();
                    ok &= test(jar);
                } else
                    error(container.getError());
            }
        }
        return ok;
    }

    public boolean test(File f) throws Exception {
        ProjectLauncher pl = new ProjectLauncher(this);
        pl.setReport(getProperty("target") + "/" + f.getName().replace(".jar", ".xml"));
        int errors = pl.run(f);
        getInfo(pl);
        if (errors == 0) {
            trace("ok");
            return true;
        } else {
            error("Failed: " + normalize(f) + ", " + errors + " test"
                    + (errors > 1 ? "s" : "") + " failures, see "
                    + normalize(pl.getTestreport()));
            return false;
        }
    }

    private void delete(File target) {
        if (target.getParentFile() == null)
            throw new IllegalArgumentException("Can not delete root!");
        if (!target.exists())
            return;

        if (target.isDirectory()) {
            File sub[] = target.listFiles();
            for (File s : sub)
                delete(s);
        }
        target.delete();
    }

    /**
     * This methods attempts to turn any jar into a valid jar. If this is a
     * bundle with manifest, a manifest is added based on defaults. If it is a
     * bundle, but not r4, we try to add the r4 headers.
     * 
     * @param name
     * @param in
     * @return
     * @throws Exception
     */
    public Jar getValidJar(File f) throws Exception {
        Jar jar = new Jar(f);
        Manifest manifest = jar.getManifest();
        if (manifest == null) {
            trace("Wrapping with all defaults");
            Builder b = new Builder(this);
            b.addClasspath(jar);
            b.setProperty("Bnd-Message", "Wrapped from " + f.getAbsolutePath()
                    + "because lacked manifest");
            b.setProperty(Constants.EXPORT_PACKAGE, "*");
            b.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");
            jar = b.build();
        } else if (manifest.getMainAttributes().getValue(
                Constants.BUNDLE_MANIFESTVERSION) == null) {
            trace("Not a release 4 bundle, wrapping with manifest as source");
            Builder b = new Builder(this);
            b.addClasspath(jar);
            b.setProperty(Constants.PRIVATE_PACKAGE, "*");
            b.mergeManifest(manifest);
            String imprts = manifest.getMainAttributes().getValue(
                    Constants.IMPORT_PACKAGE);
            if (imprts == null)
                imprts = "";
            else
                imprts += ",";
            imprts += "*;resolution=optional";

            b.setProperty(Constants.IMPORT_PACKAGE, imprts);
            b.setProperty("Bnd-Message", "Wrapped from " + f.getAbsolutePath()
                    + "because had incomplete manifest");
            jar = b.build();
        }
        return jar;
    }

    public String _project(String args[]) {
        return getBase().getAbsolutePath();
    }

    public void bump(String mask) throws IOException {
        Sed sed = new Sed(getReplacer(), getPropertiesFile());
        sed
                .replace(
                        "(Bundle-Version\\s*(:|=)\\s*)(([0-9]+(\\.[0-9]+(\\.[0-9]+)?)?))",
                        "$1${version;" + mask + ";$3}");
        sed.doIt();
        refresh();
    }

    public void bump() throws IOException {
        bump(getProperty(BUMPPOLICY, "=+0"));
    }

    public void action(String command) throws Exception {
        Action a = new ReflectAction(command);
        a.execute(this, command);
    }

    public String _findfile(String args[]) {
        File f = getFile(args[1]);
        List<String> files = new ArrayList<String>();
        tree(files, f, "", Instruction.getPattern(args[2]));
        return join(files);
    }

    void tree(List<String> list, File current, String path, Instruction instr) {
        if (path.length() > 0)
            path = path + "/";

        String subs[] = current.list();
        for (String sub : subs) {
            File f = new File(current, sub);
            if (f.isFile()) {
                if (instr.matches(sub) && !instr.isNegated())
                    list.add(path + sub);
            } else
                tree(list, f, path + sub, instr);
        }
    }

    public void refreshAll() {
        workspace.refresh();
        refresh();
    }

    @SuppressWarnings("unchecked")
    public void script(String type, String script) throws Exception {
        // TODO check tyiping
        List<Scripter> scripters = getPlugins(Scripter.class);
        if (scripters.isEmpty()) {
            error(
                    "Can not execute script because there are no scripters registered: %s",
                    script);
            return;
        }
        Map x = (Map) getProperties();
        scripters.get(0)
                .eval((Map<String, Object>) x, new StringReader(script));
    }
    
    public String _repos(String args[]) throws Exception {
        List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
        List<String> names = new ArrayList<String>();
        for ( RepositoryPlugin rp : repos )
            names.add(rp.getName());
        return join(names,", ");        
    }
    
    public String _help(String args[]) throws Exception {
        if ( args.length == 1)
            return "Specify the option or header you want information for";
        
        Syntax syntax = Syntax.HELP.get(args[1]);
        if  (syntax == null )
            return "No help for " + args[1];
        
        String what = null;
        if ( args.length> 2)
            what = args[2];
     
        if ( what == null || what.equals("lead"))
            return syntax.getLead();
        if ( what == null || what.equals("example"))
            return syntax.getExample();
        if ( what == null || what.equals("pattern"))
            return syntax.getPattern();
        if ( what == null || what.equals("values"))
            return syntax.getValues();
        
        return "Invalid type specified for help: lead, example, pattern, values";
    }
}
