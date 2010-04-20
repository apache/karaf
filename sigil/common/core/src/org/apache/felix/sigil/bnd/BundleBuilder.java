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

package org.apache.felix.sigil.bnd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.config.BldAttr;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.config.IBldProject.IBldBundle;
import org.apache.felix.sigil.core.repository.SystemRepositoryProvider;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.osgi.framework.Version;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;

public class BundleBuilder
{
    private IBldProject project;
    private File[] classpath;
    private String destPattern;
    private Properties env;
    private List<String> errors = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    private Set<String> unused = new HashSet<String>();
    private String lastBundle = null;

    private boolean addMissingImports;
    private boolean omitUnusedImports;
    private Set<String> systemPkgs;

    public interface Log
    {
        void warn(String msg);

        void verbose(String msg);
    }

    /**
     * creates a BundleBuilder.
     * 
     * @param classpath
     * @param destPattern
     *            ivy-like pattern: PATH/[name]-[revision].[ext].
     *            [id] is replaced with the bundle id.
     *            [name] is replaced with the Bundle-SymbolicName
     *            [revision] is replaced with the Bundle-Version
     *            [ext] is replaced with "jar".
     * @param hashtable
     */
    public BundleBuilder(IBldProject project, File[] classpath, String destPattern, Properties env)
    {
        this.project = project;
        this.classpath = classpath;
        this.destPattern = destPattern;
        this.env = env;

        Properties options = project.getOptions();

        addMissingImports = options.containsKey(BldAttr.OPTION_ADD_IMPORTS)
            && Boolean.parseBoolean(options.getProperty(BldAttr.OPTION_ADD_IMPORTS));
        omitUnusedImports = options.containsKey(BldAttr.OPTION_OMIT_IMPORTS)
            && Boolean.parseBoolean(options.getProperty(BldAttr.OPTION_OMIT_IMPORTS));

        for (IBldBundle b : project.getBundles())
        {
            lastBundle = b.getId();
            for (IPackageImport import1 : b.getImports())
            {
                if (import1.getOSGiImport().equals(IPackageImport.OSGiImport.AUTO))
                {
                    unused.add(import1.getPackageName());
                }
            }
        }

        try
        {
            systemPkgs = new HashSet<String>();
            Properties profile = SystemRepositoryProvider.readProfile(null);
            String pkgs = profile.getProperty("org.osgi.framework.system.packages");
            for (String pkg : pkgs.split(",\\s*"))
            {
                systemPkgs.add(pkg);
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<String> errors()
    {
        return errors;
    }

    public List<String> warnings()
    {
        return warnings;
    }

    @SuppressWarnings("unchecked")
    private void convertErrors(String prefix, List messages)
    {
        // TODO: make error mapping more generic
        final String jarEmpty = "The JAR is empty";

        for (Object omsg : messages)
        {
            if (jarEmpty.equals(omsg))
                warnings.add(prefix + omsg);
            else
                errors.add(prefix + omsg);
        }
    }

    @SuppressWarnings("unchecked")
    private void convertWarnings(String prefix, List messages)
    {
        for (Object omsg : messages)
        {
            warnings.add(prefix + omsg);
        }
    }

    public boolean createBundle(IBldBundle bundle, boolean force, Log log)
        throws Exception
    {
        int bracket = destPattern.indexOf('[');
        if (bracket < 0)
        {
            throw new Exception("destPattern MUST contain [id] or [name].");
        }

        String dest = destPattern.replaceFirst("\\[id\\]", bundle.getId());
        dest = dest.replaceFirst("\\[name\\]", bundle.getSymbolicName());
        dest = dest.replaceFirst("\\[revision\\]", bundle.getVersion());
        dest = dest.replaceFirst("\\[ext\\]", "jar");

        bracket = dest.indexOf('[');
        if (bracket >= 0)
        {
            String token = dest.substring(bracket);
            throw new Exception("destPattern: expected [id],  [name] or [revision]: " + token);
        }

        errors.clear();
        warnings.clear();

        Properties spec = getBndSpec(bundle, dest);

        if (log != null)
        {
            log.verbose("BND instructions: " + spec.toString());
            log.verbose("BND classpath: " + Arrays.asList(classpath));
        }

        Builder builder = new Builder();
        builder.setPedantic(true);
        builder.setProperties(spec);
        builder.mergeProperties(env, false);

        builder.setClasspath(classpath);
        // builder.setSourcepath(sourcepath);

        Jar jar = builder.build();

        convertErrors("BND: ", builder.getErrors());
        convertWarnings("BND: ", builder.getWarnings());
        
        Attributes main = jar.getManifest().getMainAttributes();
        String expHeader = main.getValue(Constants.EXPORT_PACKAGE);
        log.verbose("BND exports: " + expHeader);

        augmentImports(builder, jar, bundle);

        if (log != null)
        {
            for (String warn : warnings)
            {
                log.warn(warn);
            }
        }

        if (!errors.isEmpty())
        {
            throw new Exception(errors.toString());
        }

        boolean modified = false;
        File output = new File(dest);

        if (!output.exists() || force || (output.lastModified() <= jar.lastModified())
            || (output.lastModified() <= project.getLastModified()))
        {
            modified = true;
            // jar.write(dest) catches and ignores IOException
            OutputStream out = new FileOutputStream(dest);
            jar.write(out);
            out.close();
            jar.close();
        }

        builder.close();

        return modified;
    }

    private void augmentImports(Builder builder, Jar jar, IBldBundle bundle)
        throws IOException
    {
        Attributes main = jar.getManifest().getMainAttributes();
        String impHeader = main.getValue(Constants.IMPORT_PACKAGE);
        Map<String, Map<String, String>> bndImports = Processor.parseHeader(impHeader,
            builder);

        if (bndImports.isEmpty())
            return;

        ArrayList<String> self = new ArrayList<String>();
        ArrayList<String> missing = new ArrayList<String>();
        ArrayList<String> modified = new ArrayList<String>();
        ArrayList<String> unversioned = new ArrayList<String>();

        String expHeader = main.getValue(Constants.EXPORT_PACKAGE);
        Set<String> bndExports = Processor.parseHeader(expHeader, builder).keySet();

        HashMap<String, IPackageImport> imports = new HashMap<String, IPackageImport>();
        for (IPackageImport pi : getImports(bundle))
        {
            switch (pi.getOSGiImport())
            {
                case NEVER:
                    break;
                case ALWAYS:
                    String pkg = pi.getPackageName();
                    if (!bndImports.containsKey(pkg))
                    {
                        // Bnd doesn't think this import is needed - but we know
                        // better
                        HashMap<String, String> attrs = new HashMap<String, String>();
                        attrs.put(BldAttr.VERSION_ATTRIBUTE, pi.getVersions().toString());
                        bndImports.put(pkg, attrs);
                        modified.add(pkg + ";resolve=runtime");
                    }
                    // fall thru */
                case AUTO:
                    imports.put(pi.getPackageName(), pi);
                    break;
            }
        }

        boolean importDot = false;

        for (String pkg : bndImports.keySet())
        {
            unused.remove(pkg);
            Map<String, String> attrs = bndImports.get(pkg);
            String currentVersion = (String) attrs.get(BldAttr.VERSION_ATTRIBUTE);
            IPackageImport pi = imports.get(pkg);

            if (pi != null)
            {
                VersionRange range = pi.getVersions();
                String version = range.toString();

                if (!version.equals(currentVersion)
                    && !range.equals(VersionRange.ANY_VERSION))
                {
                    attrs.put(BldAttr.VERSION_ATTRIBUTE, version);
                    if (pi.isOptional())
                        attrs.put(BldAttr.RESOLUTION_ATTRIBUTE,
                            BldAttr.RESOLUTION_OPTIONAL);
                    modified.add(pkg + ";version=" + version
                        + (pi.isOptional() ? ";optional" : ""));
                }
                else if ((currentVersion == null) && !systemPkgs.contains(pkg))
                {
                    unversioned.add(pkg);
                }
            }
            else
            {
                // bnd added the import ...
                if (currentVersion == null)
                {
                    String defaultVersion = project.getDefaultPackageVersion(pkg);
                    if (defaultVersion != null)
                    {
                        attrs.put(BldAttr.VERSION_ATTRIBUTE, defaultVersion);
                        currentVersion = defaultVersion;
                    }
                }

                String imp = pkg
                    + (currentVersion == null ? "" : ";version=" + currentVersion);
                if (bndExports.contains(pkg))
                {
                    self.add(imp);
                }
                else
                {
                    if (pkg.equals("."))
                    {
                        warnings.add("Bnd wants to import '.' (ignored)");
                        importDot = true;
                    }
                    else
                    {
                        missing.add(imp);
                    }
                }
            }
        }

        if (!modified.isEmpty() || importDot)
        {
            if (importDot)
                bndImports.remove(".");
            // warnings.add("INFO: sigil modified imports: " + modified);
            main.putValue(Constants.IMPORT_PACKAGE, Processor.printClauses(bndImports,
                "resolution:"));
        }

        if (!self.isEmpty())
        {
            // warnings.add("INFO: added self imports: " + self);
        }

        if (!missing.isEmpty())
        {
            warnings.add("missing imports (added): " + missing);
        }

        if (!unversioned.isEmpty())
        {
            warnings.add("unversioned imports: " + unversioned);
        }

        if (bundle.getId().equals(lastBundle))
        {
            if (!unused.isEmpty())
            {
                warnings.add("unused imports (omitted): " + unused);
            }
        }
    }

    public Properties getBndSpec(IBldBundle bundle, String dest) throws IOException
    {
        Properties spec = new Properties();

        String junkHeaders = Constants.INCLUDE_RESOURCE; // shows local build
        // paths; can be
        // verbose
        junkHeaders += "," + Constants.PRIVATE_PACKAGE; // less useful, as we
        // use it for exported
        // content too.

        spec.setProperty(Constants.REMOVE_HEADERS, junkHeaders);
        spec.setProperty(Constants.NOEXTRAHEADERS, "true"); // Created-By,
        // Bnd-LastModified
        // and Tool
        spec.setProperty(Constants.CREATED_BY, "sigil.felix.apache.org");

        Properties headers = bundle.getHeaders();
        // XXX: catch attempts to set headers that conflict with Bnd
        // instructions we generate?
        spec.putAll(headers);

        String sn = bundle.isSingleton() ? bundle.getSymbolicName() + ";singleton:=true"
            : bundle.getSymbolicName();

        spec.setProperty(Constants.BUNDLE_SYMBOLICNAME, sn);
        spec.setProperty("version", bundle.getVersion());
        spec.setProperty(Constants.BUNDLE_VERSION, "${version}");

        String activator = bundle.getActivator();
        if (activator != null)
            spec.setProperty(Constants.BUNDLE_ACTIVATOR, activator);

        addRequirements(bundle, spec);

        List<String> exports = addExports(bundle, spec);

        addResources(bundle, spec);

        ArrayList<String> contents = new ArrayList<String>();
        contents.addAll(bundle.getContents());

        if (contents.isEmpty())
        {
            if (!project.getSourcePkgs().isEmpty())
            {
                contents.addAll(project.getSourcePkgs());
            }
            else
            {
                contents.addAll(exports);
            }
        }

        addLibs(bundle, dest, spec);

        addContents(contents, spec);

        IRequiredBundle fh = bundle.getFragmentHost();
        if (fh != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(fh.getSymbolicName());
            addVersions(fh.getVersions(), sb);
            spec.setProperty(Constants.FRAGMENT_HOST, sb.toString());
        }

        return spec;
    }

    private void addContents(List<String> contents, Properties spec)
    {
        // add contents
        StringBuilder sb = new StringBuilder();
        for (String pkg : contents)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(pkg);
        }

        if (sb.length() > 0)
            spec.setProperty(Constants.PRIVATE_PACKAGE, sb.toString());
    }

    private void appendProperty(String key, String value, Properties p)
    {
        String list = p.getProperty(key);

        if (list == null)
        {
            list = value;
        }
        else
        {
            list = list + "," + value;
        }

        p.setProperty(key, list);
    }

    private void addLibs(IBldBundle bundle, String dest, Properties spec)
        throws IOException
    {
        // final String cleanVersion =
        // Builder.cleanupVersion(bundle.getVersion());

        Map<String, Map<String, String>> libs = bundle.getLibs();

        for (String jarpath : libs.keySet())
        {
            Map<String, String> attr = libs.get(jarpath);
            String kind = attr.get(BldAttr.KIND_ATTRIBUTE);

            // first find the lib ..
            String path = attr.get(BldAttr.PATH_ATTRIBUTE);
            if (path == null)
                path = jarpath;

            File fsPath = bundle.resolve(path);

            if (!fsPath.exists())
            {
                // try destDir
                File destDir = new File(dest).getParentFile();
                File file = new File(destDir, fsPath.getName());

                if (!file.exists())
                {
                    // try searching classpath
                    file = findInClasspathDir(fsPath.getName());
                }

                if (file != null && file.exists())
                    fsPath = file;
            }

            if (!fsPath.exists())
            {
                // XXX: find external bundle using name and version range?
                // For now just let BND fail when it can't find resource.
            }

            appendProperty(Constants.INCLUDE_RESOURCE, jarpath + "=" + fsPath, spec);

            if ("classpath".equals(kind))
            {
                String bcp = spec.getProperty(Constants.BUNDLE_CLASSPATH);
                if (bcp == null || bcp.length() == 0)
                    spec.setProperty(Constants.BUNDLE_CLASSPATH, ".");
                appendProperty(Constants.BUNDLE_CLASSPATH, jarpath, spec);
            }
        }
    }

    private void addResources(IBldBundle bundle, Properties spec)
    {
        Map<String, String> resources = bundle.getResources();
        StringBuilder sb = new StringBuilder();

        for (String bPath : resources.keySet())
        {
            if (bPath.startsWith("@"))
            {
                handleInlineJar(bundle, sb, bPath);
            }
            else if (bPath.startsWith("{"))
            {
                handlePreprocessedResource(bundle, resources, sb, bPath);
            }
            else
            {
                handleStandardResource(bundle, resources, sb, bPath);
            }
        }

        if (sb.length() > 0)
            spec.setProperty(Constants.INCLUDE_RESOURCE, sb.toString());
    }

    private void handlePreprocessedResource(IBldBundle bundle,
        Map<String, String> resources, StringBuilder sb, String bPath)
    {
        String fsPath = resources.get(bPath);

        bPath = bPath.substring(1, bPath.length() - 1);

        if ("".equals(fsPath))
            fsPath = bPath;

        fsPath = findFileSystemPath(bundle, fsPath);

        if (sb.length() > 0)
            sb.append(",");
        sb.append("{");
        sb.append(bPath);
        sb.append('=');
        sb.append(fsPath);
        sb.append("}");
    }

    private void handleStandardResource(IBldBundle bundle, Map<String, String> resources,
        StringBuilder sb, String bPath)
    {
        String fsPath = resources.get(bPath);
        if ("".equals(fsPath))
            fsPath = bPath;

        fsPath = findFileSystemPath(bundle, fsPath);

        if (sb.length() > 0)
            sb.append(",");
        sb.append(bPath);
        sb.append('=');
        sb.append(fsPath);
    }

    private String findFileSystemPath(IBldBundle bundle, String fsPath)
    {
        File resolved = bundle.resolve(fsPath);

        // fsPath may contain Bnd variable, making path appear to not exist

        if (!resolved.exists())
        {
            // Bnd already looks for classpath jars
            File found = findInClasspathDir(fsPath);
            if (found != null)
            {
                fsPath = found.getPath();
            }
            else
            {
                fsPath = resolved.getAbsolutePath();
            }
        }
        else
        {
            fsPath = resolved.getAbsolutePath();
        }

        return fsPath;
    }

    private void handleInlineJar(IBldBundle bundle, StringBuilder sb, String bPath)
    {
        if (sb.length() > 0)
            sb.append(",");

        File f = bundle.resolve(bPath.substring(1));

        if (f.exists())
        {
            sb.append('@');
            sb.append(f);
        }
        else
            sb.append(bPath);
    }

    private List<IPackageImport> getImports(IBldBundle bundle)
    {
        List<IPackageImport> imports = bundle.getImports();
        Set<String> pkgs = new HashSet<String>();

        for (IPackageImport pi : imports)
        {
            pkgs.add(pi.getPackageName());
        }

        return imports;
    }

    private void addRequirements(IBldBundle bundle, Properties spec)
    {
        StringBuilder sb = new StringBuilder();

        // option;addMissingImports=true
        // Lets Bnd calculate imports (i.e. specify *),
        // which are then examined by augmentImports();

        // option;omitUnusedImports=true (implies addMissingImports=true)
        // When project contains multiple bundles which don't all use all
        // imports,
        // avoids warnings like:
        // "Importing packages that are never referred to by any class on the Bundle-ClassPath"

        if (omitUnusedImports && !addMissingImports)
        {
            warnings.add("omitUnusedImports ignored as addMissingImports=false.");
            omitUnusedImports = false;
        }

        sb.setLength(0);

        // allow existing header;Package-Import to specify ignored packages
        sb.append(spec.getProperty(Constants.IMPORT_PACKAGE, ""));

        buildImports(sb, getImports(bundle));

        if (sb.length() > 0)
        {
            spec.setProperty(Constants.IMPORT_PACKAGE, sb.toString());
        }

        sb.setLength(0);

        buildRequires(sb, bundle.getRequires());

        if (sb.length() > 0)
        {
            spec.setProperty(Constants.REQUIRE_BUNDLE, sb.toString());
        }
    }

    /**
     * @param sb
     * @param list 
     */
    private void buildRequires(StringBuilder sb, List<IRequiredBundle> requires)
    {
        for (IRequiredBundle rb : requires)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(rb.getSymbolicName());
            addVersions(rb.getVersions(), sb);
        }
    }

    /**
     * @param sb
     */
    private void buildImports(StringBuilder sb, List<IPackageImport> imports)
    {
        for (IPackageImport pi : imports)
        {
            switch (pi.getOSGiImport())
            {
                case AUTO:
                    if (omitUnusedImports)
                        continue; // added by Import-Package: * and fixed by
                    // augmentImports()
                    break;
                case NEVER:
                    if (pi.isDependency())
                        continue; // resolve=compile
                    break;
                case ALWAYS:
                    // Bnd will probably whinge that this import is not used.
                    // we omit it here and replace it in augmentImports,
                    // but only if addMissingImports is true;
                    // otherwise, if the import is used, Bnd will fail.
                    if (addMissingImports)
                        continue;
                    break;
            }

            if (sb.length() > 0)
                sb.append(",");

            if (pi.getOSGiImport().equals(IPackageImport.OSGiImport.NEVER))
            {
                sb.append("!");
                sb.append(pi.getPackageName());
            }
            else
            {
                sb.append(pi.getPackageName());
                addVersions(pi.getVersions(), sb);

                if (pi.isOptional())
                {
                    sb.append(";resolution:=optional");
                }
            }
        }

        if (sb.length() > 0)
            sb.append(",");

        if (addMissingImports)
        {
            sb.append("*");
        }
        else
        {
            sb.append("!*");
        }
    }

    private List<String> addExports(IBldBundle bundle, Properties spec)
    {
        List<IPackageExport> exports = bundle.getExports();
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (IPackageExport export : exports)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(export.getPackageName());
            if (!export.getVersion().equals(Version.emptyVersion))
            {
                sb.append(";version=\"");
                sb.append(export.getVersion());
                sb.append("\"");
            }
            list.add(export.getPackageName());
        }

        if (sb.length() > 0)
        {
            // EXPORT_CONTENTS just sets the Export-Package manifest header;
            // it doesn't add contents like EXPORT_PACKAGE does.
            spec.setProperty(Constants.EXPORT_CONTENTS, sb.toString());
        }

        return list;
    }

    private void addVersions(VersionRange range, StringBuilder sb)
    {
        if (!range.equals(VersionRange.ANY_VERSION))
        {
            sb.append(";version=\"");
            sb.append(range);
            sb.append("\"");
        }
    }

    private File findInClasspathDir(String file)
    {
        for (File cp : classpath)
        {
            if (cp.isDirectory())
            {
                File path = new File(cp, file);
                if (path.exists())
                {
                    return path;
                }
            }
        }

        return null;
    }

}
