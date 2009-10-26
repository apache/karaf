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

package org.apache.felix.sigil.ivy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.config.IBldProject.IBldBundle;
import org.apache.felix.sigil.core.licence.ILicensePolicy;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

import org.osgi.framework.Version;

public class ProjectRepository extends AbstractBundleRepository
{
    private ArrayList<ISigilBundle> bundles;
    private ArrayList<ISigilBundle> wildBundles;
    private String projectFilePattern;

    /* package */ProjectRepository(String id, String projectFilePattern)
    {
        super(id);
        this.projectFilePattern = projectFilePattern.replaceAll("\\[sigilproject\\]",
            IBldProject.PROJECT_FILE);
    }

    @Override
    public void accept(IRepositoryVisitor visitor, int options)
    {
        for (ISigilBundle b : getBundles())
        {
            if (!visitor.visit(b))
            {
                break;
            }
        }
    }

    // override to provide fuzzy matching for wild-card exports.
    @Override
    public Collection<ISigilBundle> findAllProviders(final IPackageImport pi, int options)
    {
        return findProviders(pi, options, false);
    }

    @Override
    public ISigilBundle findProvider(IPackageImport pi, int options)
    {
        Collection<ISigilBundle> found = findProviders(pi, options, true);
        return found.isEmpty() ? null : found.iterator().next();
    }

    private Collection<ISigilBundle> findProviders(final IPackageImport pi, int options,
        boolean findFirst)
    {
        ArrayList<ISigilBundle> found = new ArrayList<ISigilBundle>();
        ILicensePolicy policy = findPolicy(pi);
        String name = pi.getPackageName();
        VersionRange versions = pi.getVersions();

        // find exact match(es)
        for (ISigilBundle bundle : getBundles())
        {
            if (policy.accept(bundle))
            {
                for (IPackageExport exp : bundle.getBundleInfo().getExports())
                {
                    if (name.equals(exp.getPackageName())
                        && versions.contains(exp.getVersion()))
                    {
                        found.add(bundle);
                        if (findFirst)
                            return found;
                    }
                }
            }
        }

        if (!found.isEmpty())
            return found;

        // find best fuzzy match
        ISigilBundle fuzzyMatch = null;
        int fuzzyLen = 0;

        for (ISigilBundle bundle : getWildBundles())
        {
            if (policy.accept(bundle))
            {
                for (IPackageExport exp : bundle.getBundleInfo().getExports())
                {
                    String export = exp.getPackageName();
                    if (export.endsWith("*"))
                    {
                        String export1 = export.substring(0, export.length() - 1);
                        if ((name.startsWith(export1) || export1.equals(name + "."))
                            && versions.contains(exp.getVersion()))
                        {
                            if (export1.length() > fuzzyLen)
                            {
                                fuzzyLen = export1.length();
                                fuzzyMatch = bundle;
                            }
                        }
                    }
                }
            }
        }

        if (fuzzyMatch != null)
            found.add(fuzzyMatch);

        return found;
    }

    private synchronized void init()
    {
        System.out.println("Sigil: loading Project Repository: " + projectFilePattern);

        ArrayList<File> projects = new ArrayList<File>();

        for (String pattern : projectFilePattern.split("\\s+"))
        {
            try
            {
                Collection<File> files = FindUtil.findFiles(new File(""), pattern);
                if (files.isEmpty())
                {
                    Log.warn("ProjectRepository: no projects match: " + pattern);
                }
                else
                {
                    projects.addAll(files);
                }
            }
            catch (IOException e)
            {
                // pattern root dir does not exist
                Log.error("ProjectRepository: " + pattern + ": " + e.getMessage());
            }
        }

        if (projects.isEmpty())
        {
            throw new IllegalArgumentException(
                "ProjectRepository: no projects found using pattern: "
                    + projectFilePattern + " pwd=" + new File("").getAbsolutePath());
        }

        bundles = new ArrayList<ISigilBundle>();

        for (File proj : projects)
        {
            try
            {
                addBundles(proj, bundles);
            }
            catch (IOException e)
            {
                Log.warn("Skipping project: " + proj + ": " + e.getMessage());
            }
            catch (ParseException e)
            {
                Log.warn("Skipping project: " + proj + ": " + e.getMessage());
            }
        }
    }

    private List<ISigilBundle> getBundles()
    {
        if (bundles == null)
        {
            init();
        }
        return bundles;
    }

    private List<ISigilBundle> getWildBundles()
    {
        if (wildBundles == null)
        {
            wildBundles = new ArrayList<ISigilBundle>();
            for (ISigilBundle bundle : getBundles())
            {
                for (IPackageExport exp : bundle.getBundleInfo().getExports())
                {
                    String export = exp.getPackageName();
                    if (export.endsWith("*"))
                    {
                        wildBundles.add(bundle);
                        break;
                    }
                }
            }
        }
        return wildBundles;
    }

    public void refresh()
    {
        bundles = null;
        wildBundles = null;
        notifyChange();
    }

    private void addBundles(File file, List<ISigilBundle> list) throws IOException,
        ParseException
    {
        URI uri = file.getCanonicalFile().toURI();
        IBldProject project = BldFactory.getProject(uri);

        for (IBldBundle bb : project.getBundles())
        {
            IBundleModelElement info = ModelElementFactory.getInstance().newModelElement(IBundleModelElement.class);

            for (IPackageExport pexport : bb.getExports())
            {
                info.addExport(pexport);
            }

            for (IPackageImport import1 : bb.getImports())
            {
                IPackageImport clone = (IPackageImport) import1.clone();
                clone.setParent(null);
                info.addImport(clone);
            }

            for (IRequiredBundle require : bb.getRequires())
            {
                IRequiredBundle clone = (IRequiredBundle) require.clone();
                clone.setParent(null);
                info.addRequiredBundle(clone);
            }

            info.setSymbolicName(bb.getSymbolicName());

            Version version = new Version(bb.getVersion());
            info.setVersion(version);

            ISigilBundle pb = ModelElementFactory.getInstance().newModelElement(ISigilBundle.class);
            pb.setBundleInfo(info);
            
            Map<Object, Object> meta = new HashMap<Object, Object>();
            ModuleDescriptor md = SigilParser.instance().parseDescriptor(uri.toURL());
            if ( !bb.getId().equals( md.getModuleRevisionId().getName() ) )
            { // non-default artifact
                for ( Artifact a : md.getAllArtifacts() ) {
                    if ( a.getName().equals( bb.getId() ) ) {
                        meta.put(Artifact.class, a);
                        break;
                    }
                }
            }

            meta.put(ModuleDescriptor.class, md);
            pb.setMeta(meta);
            
            list.add(pb);
            Log.debug("ProjectRepository: added " + pb);
            Log.debug("ProjectRepository: exports " + bb.getExports());
        }
    }
}
