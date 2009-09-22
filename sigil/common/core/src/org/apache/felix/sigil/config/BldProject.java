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

package org.apache.felix.sigil.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.core.internal.model.osgi.BundleModelElement;
import org.apache.felix.sigil.core.internal.model.osgi.PackageExport;
import org.apache.felix.sigil.core.internal.model.osgi.PackageImport;
import org.apache.felix.sigil.core.internal.model.osgi.RequiredBundle;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IPackageImport.OSGiImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.osgi.framework.Version;

public class BldProject implements IBldProject, IRepositoryConfig
{
    private static final String OVERRIDE_PREFIX = "sigil.";
    private static final int MAX_HEADER = 10240;
    // cache to avoid loading the same default config for each project
    private static Map<URL, BldConfig> defaultsCache = new HashMap<URL, BldConfig>();
    private static Properties overrides;

    private List<String> sourcePkgs;
    private BldConfig config;
    private BldConverter convert;
    private BundleModelElement requirements;
    private final File baseDir;
    private URI loc;
    private Properties packageDefaults;
    private TreeSet<String> packageWildDefaults;
    private long lastModified;

    /* package */BldProject(URI relLoc)
    {
        config = new BldConfig();
        convert = new BldConverter(config);
        loc = new File(".").toURI().resolve(relLoc).normalize();
        File f = new File(loc);
        lastModified = f.lastModified();
        baseDir = f.getParentFile();
    }

    /* package */void load() throws IOException
    {
        // allow System property overrides, e.g.
        // ANT_OPTS='-Dsigil.option\;addMissingImports=false' ant
        config.merge(getOverrides());

        InputStream in = null;
        try
        {
            in = loc.toURL().openStream();
            BufferedInputStream bis = new BufferedInputStream(in);
            bis.mark(MAX_HEADER);
            readHeader(bis);
            bis.reset();

            Properties p = new Properties();
            p.load(bis);
            config.merge(p);

            Properties unknown = config.getUnknown();
            if (!unknown.isEmpty())
                System.err.println("WARN: unknown keys " + unknown.keySet() + " in "
                    + loc);

            loadDefaults(p);
            requirements = parseRequirements();
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }

        //System.err.println("XXX loc=" + loc + ", BldConfig: " + config);
    }

    /* package */void loadDefaults(Properties p) throws IOException
    {
        BldConfig c = loadDefaults(p, baseDir, null);
        config.setDefault(c);

        Properties options = config.getProps(null, BldConfig.P_OPTION);

        if (!options.containsKey(BldAttr.OPTION_ADD_IMPORTS))
            c.setProp(null, BldConfig.P_OPTION, BldAttr.OPTION_ADD_IMPORTS, "true");

        // default omitUnusedImports option depends on number of bundles...
        // we set it here to avoid it being written by save(),
        // but as this may alter cached defaults, once set we have to reset it
        // for each project.

        boolean omitSet = options.containsKey("__omit_set__");
        boolean multiple = getBundleIds().size() > 1;

        if (multiple || omitSet)
        {
            if (!options.containsKey(BldAttr.OPTION_OMIT_IMPORTS) || omitSet)
            {
                c.setProp(null, BldConfig.P_OPTION, BldAttr.OPTION_OMIT_IMPORTS, multiple
                    + "");
                c.setProp(null, BldConfig.P_OPTION, "__omit_set__", "true");
            }
        }
    }

    private synchronized BldConfig loadDefaults(Properties props, File base,
        BldConfig dflt) throws IOException
    {
        boolean cached = false;
        String defaults = props.getProperty(BldConfig.S_DEFAULTS);

        if (defaults != null)
        {
            defaults = BldUtil.expand(defaults, new BldProperties(base));
        }
        else
        {
            defaults = "-" + IBldProject.PROJECT_DEFAULTS;
        }

        if (base != null && defaults.length() > 0)
        {
            boolean ignore = defaults.startsWith("-");

            if (ignore)
                defaults = defaults.substring(1);

            try
            {
                File file = new File(base, defaults).getCanonicalFile();
                URL url = file.toURL();
                BldProperties bp = new BldProperties(file.getParentFile());

                if (dflt == null)
                {
                    dflt = defaultsCache.get(url);
                    if (dflt != null)
                        return dflt;

                    dflt = new BldConfig();
                    defaultsCache.put(url, dflt);
                    cached = true;
                }

                Properties p = new Properties();
                InputStream stream = url.openStream();
                p.load(stream);
                stream.close();

                // expand variables in defaults
                for (Object k : p.keySet())
                {
                    String key = (String) k;
                    String value = p.getProperty(key);
                    p.setProperty(key, BldUtil.expand(value, bp));
                }

                dflt.merge(p);

                ignore = false;
                loadDefaults(p, file.getParentFile(), dflt);
            }
            catch (IOException e)
            {
                if (!ignore)
                    throw e;
            }
        }

        if (dflt == null)
            return new BldConfig();

        if (cached)
        {
            Properties unknown = dflt.getUnknown();
            if (!unknown.isEmpty())
                System.err.println("WARN: unknown keys " + unknown.keySet()
                    + " in defaults for " + loc);
        }

        return dflt;
    }

    private static Properties getOverrides()
    {
        if (overrides == null)
        {
            overrides = new Properties();
            Properties sysProps = System.getProperties();

            for (Object okey : sysProps.keySet())
            {
                String key = (String) okey;
                if (key.startsWith(OVERRIDE_PREFIX))
                {
                    overrides.setProperty(key.substring(OVERRIDE_PREFIX.length()),
                        sysProps.getProperty(key));
                }
            }
        }

        return overrides;
    }

    private void readHeader(InputStream in) throws IOException
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        StringBuffer header = new StringBuffer();
        String line;
        while ((line = r.readLine()) != null)
        {
            if (line.startsWith("#"))
            {
                header.append(line);
                header.append("\n");
            }
            else
            {
                config.setComment(header.toString());
                break;
            }
        }
    }

    public File resolve(String path)
    {
        File file = new File(path);
        if (!file.isAbsolute())
        {
            // can't use loc.resolve(value), as value may not be valid URI.
            file = new File(baseDir, path);
        }
        return file;
    }

    public String getVersion()
    {
        String version = config.getString(null, BldConfig.S_VERSION);
        return version == null ? "0" : version;
    }

    public IBundleModelElement getDependencies()
    {
        IBundleModelElement dependencies = new BundleModelElement();

        for (IModelElement element : getRequirements().children())
        {
            if (element instanceof IPackageImport)
            {
                IPackageImport import1 = (IPackageImport) element;
                if (!import1.isDependency())
                    continue;

                IPackageImport pi = (IPackageImport) (element.clone());
                pi.setParent(null);
                dependencies.addImport(pi);
            }
            else
            {
                IRequiredBundle rb = (IRequiredBundle) (element.clone());
                rb.setParent(null);
                dependencies.addRequiredBundle(rb);
            }
        }

        return dependencies;
    }

    private IBundleModelElement getRequirements()
    {
        return requirements;
    }

    /*
     * private boolean globMatch(String pkg, Set<String> set) { // exact match
     * if (set.contains(pkg)) return true;
     * 
     * // org.foo.bar matches org.foo. for (String glob : set) { if
     * (glob.matches(pkg)) { return true; } }
     * 
     * return false; }
     */

    /**
     * set internal OSGiImport and isDependency flags, based on external
     * resolution= attribute.
     * 
     * OSGiImport: AUTO ALWAYS NEVER dependency: default - compile !dependency:
     * auto runtime ignore
     * 
     */
    private void setResolve(IPackageImport pi, String resolve) throws IOException
    {
        if (pi.isOptional())
            pi.setDependency(false);

        if (BldAttr.RESOLVE_COMPILE.equals(resolve))
        {
            if (pi.isOptional())
                pi.setDependency(true);
            else
                pi.setOSGiImport(OSGiImport.NEVER);
        }
        else if (BldAttr.RESOLVE_RUNTIME.equals(resolve))
        {
            pi.setDependency(false);
            pi.setOSGiImport(OSGiImport.ALWAYS);
        }
        else if (BldAttr.RESOLVE_AUTO.equals(resolve))
        {
            pi.setDependency(false);
        }
        else if (BldAttr.RESOLVE_IGNORE.equals(resolve))
        {
            pi.setDependency(false);
            pi.setOSGiImport(OSGiImport.NEVER);
        }
        else if (resolve != null)
        {
            throw new IOException("Bad attribute value: " + BldAttr.RESOLVE_ATTRIBUTE
                + "=" + resolve);
        }
    }

    /**
     * get external resolve= attribute from internal PackageImport flags. This
     * is called from BldConverter.setBundle().
     */
    public static String getResolve(IPackageImport pi, boolean isDependency)
    {
        OSGiImport osgiImport = pi.getOSGiImport();
        String resolve = null;

        if (isDependency)
        {
            if (osgiImport.equals(OSGiImport.NEVER) || pi.isOptional())
                resolve = BldAttr.RESOLVE_COMPILE;
        }
        else
        {
            switch (osgiImport)
            {
                case ALWAYS:
                    resolve = BldAttr.RESOLVE_RUNTIME;
                    break;
                case AUTO:
                    resolve = BldAttr.RESOLVE_AUTO;
                    break;
                case NEVER:
                    resolve = BldAttr.RESOLVE_IGNORE;
                    break;
            }
        }
        return resolve;
    }

    public String getDefaultPackageVersion(String name)
    {
        if (packageDefaults == null)
        {
            packageDefaults = config.getProps(null, BldConfig.P_PACKAGE_VERSION);
            packageWildDefaults = new TreeSet<String>();

            for (Object key : packageDefaults.keySet())
            {
                String pkg = (String) key;
                if (pkg.endsWith("*"))
                {
                    packageWildDefaults.add(pkg.substring(0, pkg.length() - 1));
                }
            }
        }

        String version = packageDefaults.getProperty(name);

        if (version == null)
        {
            for (String pkg : packageWildDefaults)
            {
                if (name.startsWith(pkg))
                {
                    version = packageDefaults.getProperty(pkg + "*");
                    // break; -- don't break, as we want the longest match
                }
            }
        }

        return version;
    }

    private synchronized BundleModelElement parseRequirements() throws IOException
    {
        BundleModelElement reqs = new BundleModelElement();

        List<String> sourceContents = getSourcePkgs();
        HashSet<String> exports = new HashSet<String>();

        parseExports(reqs, exports);

        parseImports(reqs, sourceContents, exports);

        parseRequires(reqs);

        return reqs;
    }

    /**
     * @param reqs
     * @param exports
     */
    private void parseExports(BundleModelElement reqs, HashSet<String> exports)
    {
        for (IBldBundle bundle : getBundles())
        {
            for (IPackageExport export : bundle.getExports())
            {
                exports.add(export.getPackageName());
            }
        }
    }

    /**
     * @param reqs
     * @throws IOException 
     */
    private void parseRequires(BundleModelElement reqs) throws IOException
    {
        Map<String, Map<String, String>> requires = config.getMap(null,
            BldConfig.M_REQUIRES);
        Properties bundleDefaults = config.getProps(null, BldConfig.P_BUNDLE_VERSION);

        if (requires != null)
        {
            for (String name : requires.keySet())
            {
                Map<String, String> attr = requires.get(name);
                String versions = attr.containsKey(BldAttr.VERSION_ATTRIBUTE) ? attr.get(BldAttr.VERSION_ATTRIBUTE)
                    : bundleDefaults.getProperty(name);
                String resolution = attr.get(BldAttr.RESOLUTION_ATTRIBUTE);

                RequiredBundle rb = new RequiredBundle();
                rb.setSymbolicName(name);
                rb.setVersions(VersionRange.parseVersionRange(versions));

                if (BldAttr.RESOLUTION_OPTIONAL.equals(resolution))
                {
                    rb.setOptional(true);
                }
                else if (resolution != null)
                {
                    throw new IOException("Bad attribute value: "
                        + BldAttr.RESOLUTION_ATTRIBUTE + "=" + resolution);
                }

                reqs.addRequiredBundle(rb);
            }
        }

        for (IBldBundle bundle : getBundles())
        {
            IRequiredBundle fh = bundle.getFragmentHost();
            if (fh != null)
                reqs.addRequiredBundle(fh);
        }
    }

    /**
     * @param reqs 
     * @param exports 
     * @param sourceContents 
     * @throws IOException 
     * 
     */
    private void parseImports(BundleModelElement reqs, List<String> sourceContents,
        HashSet<String> exports) throws IOException
    {
        Map<String, Map<String, String>> imports = config.getMap(null,
            BldConfig.M_IMPORTS);

        for (String name : imports.keySet())
        {
            Map<String, String> attr = imports.get(name);

            String resolve = attr.get(BldAttr.RESOLVE_ATTRIBUTE);
            String resolution = attr.get(BldAttr.RESOLUTION_ATTRIBUTE);
            String versions = attr.containsKey(BldAttr.VERSION_ATTRIBUTE) ? attr.get(BldAttr.VERSION_ATTRIBUTE)
                : getDefaultPackageVersion(name);

            PackageImport pi = new PackageImport();
            pi.setPackageName(name);

            // avoid dependency on self-exports
            // XXX: BldConverter.setBundle contains similar logic
            if (exports.contains(name)
                && (sourceContents.contains(name) || sourceContents.isEmpty()))
            {
                pi.setDependency(false);
                if (versions == null)
                    versions = getVersion();
            }

            if (!checkVersionRange(versions))
            {
                throw new IOException("Failed to parse version range for " + resolve
                    + " missing \"'s around version range?");
            }

            pi.setVersions(VersionRange.parseVersionRange(versions));

            if (BldAttr.RESOLUTION_OPTIONAL.equals(resolution))
            {
                pi.setOptional(true);
            }
            else if (resolution != null)
            {
                throw new IOException("Bad attribute value: "
                    + BldAttr.RESOLUTION_ATTRIBUTE + "=" + resolution);
            }

            setResolve(pi, resolve);

            reqs.addImport(pi);
        }
    }

    private boolean checkVersionRange(String versions)
    {
        if (versions == null || versions.length() == 0)
        {
            return true;
        }
        else
        {
            switch (versions.charAt(0))
            {
                case '(':
                case '[':
                    switch (versions.charAt(versions.length() - 1))
                    {
                        case ')':
                        case ']':
                            return true;
                        default:
                            return false;
                    }
                default:
                    return true;
            }
        }
    }

    public List<String> getBundleIds()
    {
        List<String> ids = config.getList(null, BldConfig.C_BUNDLES);
        if (ids == null)
            return Collections.emptyList();
        return ids;
    }

    public List<IBldBundle> getBundles()
    {
        ArrayList<IBldBundle> list = new ArrayList<IBldBundle>();

        for (String id : getBundleIds())
        {
            list.add(new BldBundle(id));
        }

        return list;
    }

    // Implement IBldConfig: getRepositoryConfig

    public Map<String, Properties> getRepositoryConfig()
    {
        HashMap<String, Properties> map = new HashMap<String, Properties>();
        BldProperties bp = new BldProperties(baseDir);

        for (String name : config.getList(null, BldConfig.C_REPOSITORIES))
        {
            Properties repo = config.getProps(null, name);

            for (Object k : repo.keySet())
            {
                String key = (String) k;
                String value = repo.getProperty(key);
                repo.setProperty(key, BldUtil.expand(value, bp));
            }

            map.put(name, repo);
        }
        return map;
    }

    public Properties getOptions()
    {
        return config.getProps(null, BldConfig.P_OPTION);
    }

    public Properties getDefaultPackageVersions()
    {
        return config.getProps(null, BldConfig.P_PACKAGE_VERSION);
    }

    public ISigilBundle getDefaultBundle()
    {
        List<String> bundles = getBundleIds();
        if (bundles.isEmpty())
            return null;

        String id = bundles.get(0);
        return getSigilBundle(id);
    }

    public ISigilBundle getSigilBundle(String id)
    {
        BldBundle bundle = new BldBundle(id);
        return convert.getBundle(id, bundle);
    }

    public void setDefaultBundle(ISigilBundle bundle)
    {
        setSigilBundle(null, bundle);
    }

    public void setSigilBundle(String id, ISigilBundle bundle)
    {
        List<String> ids = getBundleIds();

        if (ids.isEmpty())
        {
            ArrayList<String> list = new ArrayList<String>();
            list.add(id == null ? bundle.getBundleInfo().getSymbolicName() : id);
            config.setList(null, BldConfig.C_BUNDLES, list);
        }
        else if (id == null)
        {
            id = ids.get(0);
        }
        else if (!ids.contains(id))
        {
            List<String> list = config.getList(null, BldConfig.C_BUNDLES);
            list.add(id);
            config.setList(null, BldConfig.C_BUNDLES, list);
        }

        if (ids.size() == 1)
            id = null; // don't prefix default bundle with id

        convert.setBundle(id, bundle);
    }

    public void save() throws IOException
    {
        saveAs(new File(loc));
    }

    public void saveAs(File path) throws IOException
    {
        File part = new File(path.getPath() + ".part");
        saveTo(new FileOutputStream((part)));

        path.delete();
        if (!part.renameTo(path))
            throw new IOException("failed to rename " + part + " to " + path);
    }

    public void saveTo(OutputStream out)
    {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        config.write(writer);
        writer.close();
    }

    public List<String> getSourceDirs()
    {
        List<String> list = config.getList(null, BldConfig.L_SRC_CONTENTS);
        return list != null ? list : Collections.<String> emptyList();
    }

    public List<String> getSourcePkgs()
    {
        if (sourcePkgs == null)
        {
            sourcePkgs = new ArrayList<String>();
            for (String src : getSourceDirs())
            {
                File dir = resolve(src);
                if (!dir.isDirectory())
                {
                    System.err.println("WARN: sourcedir does not exist: " + dir);
                    continue;
                    // throw new RuntimeException("sourcedir: " + dir +
                    // " : is not a directory.");
                }
                findSrcPkgs(dir, null, sourcePkgs);
            }
        }

        return sourcePkgs;
    }

    private void findSrcPkgs(File dir, String pkg, List<String> result)
    {
        ArrayList<File> dirs = new ArrayList<File>();
        boolean found = false;

        for (String name : dir.list())
        {
            if (name.endsWith(".java"))
            {
                found = true;
            }
            else if (!name.equals(".svn"))
            {
                File d = new File(dir, name);
                if (d.isDirectory())
                    dirs.add(d);
            }
        }

        if (pkg == null)
        {
            pkg = "";
        }
        else if (pkg.equals(""))
        {
            pkg = dir.getName();
        }
        else
        {
            pkg = pkg + "." + dir.getName();
        }

        if (found)
            result.add(pkg);

        for (File d : dirs)
            findSrcPkgs(d, pkg, result);
    }

    /**
     * BldBundle
     * 
     */
    class BldBundle implements IBldBundle
    {
        private String id;

        public BldBundle(String id)
        {
            this.id = id;
        }

        public File resolve(String path)
        {
            return BldProject.this.resolve(path);
        }

        private String getString(String key)
        {
            return config.getString(id, key);
        }

        private boolean getBoolean(String key)
        {
            return Boolean.parseBoolean(getString(key));
        }

        private List<String> getList(String key)
        {
            List<String> list = config.getList(id, key);
            return list != null ? list : Collections.<String> emptyList();
        }

        private Map<String, Map<String, String>> getMap(String key)
        {
            Map<String, Map<String, String>> map = config.getMap(id, key);
            return map != null ? map
                : Collections.<String, Map<String, String>> emptyMap();
        }

        public String getActivator()
        {
            return getString(BldConfig.S_ACTIVATOR);
        }

        public String getId()
        {
            String name = getString("id");
            return name != null ? name : id;
        }

        public String getVersion()
        {
            String ver = getString(BldConfig.S_VERSION);
            if (ver == null)
            {
                ver = BldProject.this.getVersion();
            }
            return ver;
        }

        public String getSymbolicName()
        {
            String name = getString(BldConfig.S_SYM_NAME);
            return name != null ? name : getId();
        }

        public boolean isSingleton()
        {
            return getBoolean(BldConfig.S_SINGLETON);
        }

        public List<IPackageExport> getExports()
        {
            ArrayList<IPackageExport> list = new ArrayList<IPackageExport>();
            Map<String, Map<String, String>> exports = getMap(BldConfig.M_EXPORTS);

            if (exports != null)
            {
                for (String name : exports.keySet())
                {
                    Map<String, String> attrs = exports.get(name);
                    PackageExport pkgExport = new PackageExport();
                    pkgExport.setPackageName(name);

                    String version = attrs.get(BldAttr.VERSION_ATTRIBUTE);
                    // only default export version from local packages
                    if (version == null
                        && (getSourcePkgs().isEmpty() || getSourcePkgs().contains(name)))
                    {
                        version = getVersion();
                    }

                    if (version != null)
                        pkgExport.setVersion(new Version(version));

                    list.add(pkgExport);
                }
            }

            return list;
        }

        public List<IPackageImport> getImports()
        {
            ArrayList<IPackageImport> list = new ArrayList<IPackageImport>();

            for (IPackageImport import1 : getRequirements().childrenOfType(
                IPackageImport.class))
            {
                list.add(import1);
            }

            return list;
        }

        public List<IRequiredBundle> getRequires()
        {
            ArrayList<IRequiredBundle> list = new ArrayList<IRequiredBundle>();
            list.addAll(Arrays.asList(getRequirements().childrenOfType(
                IRequiredBundle.class)));

            for (IBldBundle bundle : getBundles())
            {
                IRequiredBundle fh = bundle.getFragmentHost();
                if (fh != null)
                    list.remove(fh);
            }

            return list;
        }

        public IRequiredBundle getFragmentHost()
        {
            IRequiredBundle fragment = null;
            Map<String, Map<String, String>> fragments = getMap(BldConfig.M_FRAGMENT);
            if (fragments != null)
            {
                for (String name : fragments.keySet())
                {
                    Map<String, String> attr = fragments.get(name);
                    String versions = attr.isEmpty() ? null
                        : attr.get(BldAttr.VERSION_ATTRIBUTE);
                    fragment = new RequiredBundle();
                    fragment.setSymbolicName(name);
                    fragment.setVersions(VersionRange.parseVersionRange(versions));
                    break;
                }
            }

            return fragment;
        }

        public Map<String, Map<String, String>> getLibs()
        {
            Map<String, Map<String, String>> libs = getMap(BldConfig.M_LIBS);
            return (libs != null) ? libs
                : Collections.<String, Map<String, String>> emptyMap();
        }

        public List<String> getContents()
        {
            return getList(BldConfig.L_CONTENTS);
        }

        public Map<String, String> getResources()
        {
            HashMap<String, String> map = new HashMap<String, String>();
            List<String> resources = getList(BldConfig.L_RESOURCES);

            if (resources != null)
            {
                for (String resource : resources)
                {
                    String[] paths = resource.split("=", 2);
                    String fsPath = (paths.length > 1 ? paths[1] : "");
                    map.put(paths[0], fsPath);
                }
            }
            return map;
        }

        public Properties getHeaders()
        {
            Properties headers = config.getProps(id, BldConfig.P_HEADER);
            return headers;
        }

    }

    public long getLastModified()
    {
        return lastModified;
    }

}
