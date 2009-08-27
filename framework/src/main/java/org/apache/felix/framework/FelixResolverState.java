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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.felix.framework.searchpolicy.Resolver;
import org.apache.felix.framework.searchpolicy.PackageSource;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Attribute;
import org.apache.felix.framework.util.manifestparser.R4Directive;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.Version;

public class FelixResolverState implements Resolver.ResolverState
{
    private final Logger m_logger;
    // List of all modules.
    private final List m_moduleList = new ArrayList();
    // Map of fragment symbolic names to array of modules sorted version.
    private final Map m_fragmentMap = new HashMap();
    // Maps a package name to an array of modules.
    private final Map m_unresolvedPkgIndexMap = new HashMap();
    // Maps a package name to an array of modules.
    private final Map m_resolvedPkgIndexMap = new HashMap();
    // Maps a module to an array of capabilities.
    private final Map m_resolvedCapMap = new HashMap();

    // Reusable empty array.
    private static final IModule[] m_emptyModules = new IModule[0];
    private static final PackageSource[] m_emptySources = new PackageSource[0];

    public FelixResolverState(Logger logger)
    {
        m_logger = logger;
    }

    public synchronized IModule mergeFragments(IModule rootModule) throws Exception
    {
// TODO: FRAGMENT - This should check to make sure that the host allows fragments.
        IModule newRootModule = rootModule;
        for (int hostIdx = 0; hostIdx < m_moduleList.size(); hostIdx++)
        {
            IModule host = (IModule) m_moduleList.get(hostIdx);
            if (!host.isResolved() && !Util.isFragment(host))
            {
                ICapability[] caps = host.getCapabilities();
                ICapability hostCap = null;
                for (int capIdx = 0; capIdx < caps.length; capIdx++)
                {
                    if (caps[capIdx].getNamespace().equals(ICapability.HOST_NAMESPACE))
                    {
                        hostCap = caps[capIdx];
                        break;
                    }
                }

                // If there is no host capability in the current module,
                // then just ignore it.
                if (hostCap == null)
                {
                    continue;
                }

                // Need to remove any previously attached, but not resolved fragments.
// TODO: FRAGMENT - We need to rethink how we do fragment merging...probably merging
//       as bundles are installed would be better.
                ((ModuleImpl) host).attachFragments(null);

                // Fragments are grouped by symbolic name and descending version.
                // Attach the first matching fragment from each group if possible,
                // since only one version of a given fragment may attach to a host.
                List fragmentList = new ArrayList();
                for (Iterator it = m_fragmentMap.entrySet().iterator(); it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) it.next();
                    IModule[] fragments = (IModule[]) entry.getValue();
                    done: for (int fragIdx = 0; fragIdx < fragments.length; fragIdx++)
                    {
                        IRequirement[] reqs = fragments[fragIdx].getRequirements();
                        for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
                        {
                            if (reqs[reqIdx].getNamespace().equals(ICapability.HOST_NAMESPACE)
                                && reqs[reqIdx].isSatisfied(hostCap)
                                && !((BundleImpl) fragments[fragIdx].getBundle()).isStale()
                                && !((BundleImpl) fragments[fragIdx].getBundle()).isRemovalPending())
                            {
                                // Fragments are attached in bundle ID order.
                                int index = -1;
                                for (int listIdx = 0;
                                    (index < 0) && (listIdx < fragmentList.size());
                                    listIdx++)
                                {
                                    if (fragments[fragIdx].getBundle().getBundleId()
                                        < ((IModule) fragmentList.get(listIdx)).getBundle().getBundleId())
                                    {
                                        index = listIdx;
                                    }
                                }
                                fragmentList.add(
                                    (index < 0) ? fragmentList.size() : index,
                                    fragments[fragIdx]);
                                break done;
                            }
                        }
                    }
                }

                if (fragmentList.size() > 0)
                {
                    // Check if fragment conflicts with existing metadata.
                    checkForConflicts(host, fragmentList);

                    // Attach the fragments to the host.
                    IModule[] fragments = (IModule[]) fragmentList.toArray(new IModule[fragmentList.size()]);
                    ((ModuleImpl) host).attachFragments(fragments);

                    for (int fragIdx = 0; fragIdx < fragments.length; fragIdx++)
                    {
                        // Check to see if the root module is actually a fragment,
                        // if so then we want to return the host for resolving.
                        if (rootModule == fragments[fragIdx])
                        {
                            newRootModule = host;
                        }

                        // Add each fragment capabililty to the resolver state
                        // data structures.
                        ICapability[] fragCaps = fragments[fragIdx].getCapabilities();
                        for (int capIdx = 0; (fragCaps != null) && (capIdx < fragCaps.length); capIdx++)
                        {
                            if (fragCaps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                            {
                                indexPackageCapability(
                                    m_unresolvedPkgIndexMap, host, fragCaps[capIdx]);
                            }
                        }
                    }
                }
            }
        }

        return newRootModule;
    }

    private void checkForConflicts(IModule host, List fragmentList)
    {
        // Verify the fragments do not have conflicting imports.
        // For now, just check for duplicate imports, but in the
        // future we might want to make this more fine grained.
        // First get the host's imported packages.
        final int MODULE_IDX = 0, REQ_IDX = 1;
        Map ipMerged = new HashMap();
        Map rbMerged = new HashMap();
        IRequirement[] reqs = host.getRequirements();
        for (int reqIdx = 0; (reqs != null) && (reqIdx < reqs.length); reqIdx++)
        {
            if (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ipMerged.put(
                    ((Requirement) reqs[reqIdx]).getTargetName(),
                    new Object[] { host, reqs[reqIdx] });
            }
            else if (reqs[reqIdx].getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                rbMerged.put(
                    ((Requirement) reqs[reqIdx]).getTargetName(),
                    new Object[] { host, reqs[reqIdx] });
            }
        }
        // Loop through each fragment verifying it does no conflict,
        // adding its package and bundle dependencies if they do not
        // conflict and removing the fragment if it does conflict.
        for (Iterator it = fragmentList.iterator(); it.hasNext(); )
        {
            IModule fragment = (IModule) it.next();
            reqs = fragment.getRequirements();
            Map ipFragment = new HashMap();
            Map rbFragment = new HashMap();
            boolean conflicting = false;
            for (int reqIdx = 0;
                !conflicting && (reqs != null) && (reqIdx < reqs.length);
                reqIdx++)
            {
                if (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
                    || reqs[reqIdx].getNamespace().equals(ICapability.MODULE_NAMESPACE))
                {
                    String targetName = ((Requirement) reqs[reqIdx]).getTargetName();
                    Map mergedReqMap = (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                        ? ipMerged : rbMerged;
                    Map fragmentReqMap = (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                        ? ipFragment : rbFragment;
                    Object[] existing = (Object[]) mergedReqMap.get(targetName);
                    if (existing == null)
                    {
                        fragmentReqMap.put(targetName, new Object[] { fragment, reqs[reqIdx] });
                    }
                    else if (isRequirementConflicting(
                        (Requirement) existing[REQ_IDX], (Requirement) reqs[reqIdx]))
                    {
                        conflicting = true;
                    }
                    if (conflicting)
                    {
                        ipFragment.clear();
                        rbFragment.clear();
                        it.remove();
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "Excluding fragment " + fragment.getSymbolicName()
                            + " from " + host.getSymbolicName()
                            + " due to conflict with "
                            + (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
                                ? "imported package " : "required bundle ")
                            + targetName + " from "
                            + ((IModule) existing[MODULE_IDX]).getSymbolicName());
                    }
                }
            }

            // Merge non-conflicting requirements into overall set
            // of requirements and continue checking for conflicts
            // with the next fragment.
            for (Iterator it2 = ipFragment.entrySet().iterator(); it2.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it2.next();
                ipMerged.put(entry.getKey(), entry.getValue());
            }
            for (Iterator it2 = rbFragment.entrySet().iterator(); it2.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it2.next();
                rbMerged.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean isRequirementConflicting(
        Requirement existing, Requirement additional)
    {
        // If the namespace is not the same, then they do NOT conflict.
        if (!existing.getNamespace().equals(additional.getNamespace()))
        {
            return false;
        }
        // If the target name is not the same, then they do NOT conflict.
        if (!existing.getTargetName().equals(additional.getTargetName()))
        {
            return false;
        }
        // If the target version range is not the same, then they conflict.
        if (!existing.getTargetVersionRange().equals(additional.getTargetVersionRange()))
        {
            return true;
        }
        // If optionality is not the same, then they conflict, unless
        // the existing requirement is not optional, then it doesn't matter
        // what subsequent requirements are since non-optional is stronger
        // than optional.
        if (existing.isOptional() && (existing.isOptional() != additional.isOptional()))
        {
            return true;
        }
        // Verify directives are the same.
        // This is sort of ugly, but we need to remove
        // the resolution directive, since it is effectively
        // test above when checking optionality.
        // Put directives in a map, since ordering is arbitrary.
        final R4Directive[] exDirs = (existing.getDirectives() == null)
            ? new R4Directive[0] : existing.getDirectives();
        final Map exDirMap = new HashMap();
        for (int i = 0; i < exDirs.length; i++)
        {
            if (!exDirs[i].getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                exDirMap.put(exDirs[i].getName(), exDirs[i]);
            }
        }
        final R4Directive[] addDirs = (additional.getDirectives() == null)
            ? new R4Directive[0] : additional.getDirectives();
        final Map addDirMap = new HashMap();
        for (int i = 0; i < addDirs.length; i++)
        {
            if (!addDirs[i].getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                addDirMap.put(addDirs[i].getName(), addDirs[i]);
            }
        }
        // If different number of directives, then they conflict.
        if (exDirMap.size() != addDirMap.size())
        {
            return true;
        }
        // If directive values do not match, then they conflict.
        for (Iterator it = addDirMap.entrySet().iterator(); it.hasNext(); )
        {
            final Map.Entry entry = (Map.Entry) it.next();
            final String name = (String) entry.getKey();
            final R4Directive addDir = (R4Directive) entry.getValue();
            final R4Directive exDir = (R4Directive) exDirMap.get(name);
            if ((exDir == null) ||
                !exDir.getValue().equals(addDir.getValue()))
            {
                return true;
            }
        }
        // Verify attributes are the same.
        final R4Attribute[] exAttrs = (existing.getAttributes() == null)
            ? new R4Attribute[0] : existing.getAttributes();
        final R4Attribute[] addAttrs = (additional.getAttributes() == null)
            ? new R4Attribute[0] : additional.getAttributes();
        // If different number of attributes, then they conflict.
        if (exAttrs.length != addAttrs.length)
        {
            return true;
        }
        // Put attributes in a map, since ordering is arbitrary.
        final Map exAttrMap = new HashMap();
        for (int i = 0; i < exAttrs.length; i++)
        {
            exAttrMap.put(exAttrs[i].getName(), exAttrs[i]);
        }
        // If attribute values do not match, then they conflict.
        for (int i = 0; i < addAttrs.length; i++)
        {
            final R4Attribute exAttr = (R4Attribute) exAttrMap.get(addAttrs[i].getName());
            if ((exAttr == null) ||
                !exAttr.getValue().equals(addAttrs[i].getValue()) ||
                (exAttr.isMandatory() != addAttrs[i].isMandatory()))
            {
                return true;
            }
        }
        // They do no conflict.
        return false;
    }

    private void addFragment(IModule module)
    {
        indexFragment(m_fragmentMap, module);
//        System.out.println("+++ BEGIN FRAGMENT DUMP");
//        dumpModuleIndexMap(m_fragmentMap);
//        System.out.println("+++ END FRAGMENT DUMP");
    }

    public synchronized void addModule(IModule module)
    {
        if (Util.isFragment(module))
        {
            addFragment(module);
        }
        else
        {
            // When a module is added, create an aggregated list of unresolved
            // exports to simplify later processing when resolving bundles.
            m_moduleList.add(module);

            ICapability[] caps = module.getCapabilities();

            // Add exports to unresolved package map.
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    indexPackageCapability(m_unresolvedPkgIndexMap, module, caps[i]);
                }
            }
        }

//System.out.println("UNRESOLVED PACKAGES:");
//dumpPackageIndexMap(m_unresolvedPkgIndexMap);
//System.out.println("RESOLVED PACKAGES:");
//dumpPackageIndexMap(m_resolvedPkgIndexMap);
    }

    public synchronized void removeModule(IModule module)
    {
        // Depending on whether the module is a fragment or not,
        // we need to do different things.

        // If module is a fragment, then remove from fragment map.
        if (Util.isFragment(module))
        {
            IModule[] fragments = (IModule[]) m_fragmentMap.get(module.getSymbolicName());
            fragments = removeModuleFromArray(fragments, module);
            if (fragments.length == 0)
            {
                m_fragmentMap.remove(module.getSymbolicName());
            }
            else
            {
                m_fragmentMap.put(module.getSymbolicName(), fragments);
            }
        }
        // If it is not a fragment, then we need remove its exports
        // from the "resolved" and "unresolved" package maps, remove
        // the module's dependencies on fragments and exporters,
        // and remove the module from the module list.
        else
        {
            m_moduleList.remove(module);

            // Remove exports from package maps.
            ICapability[] caps = module.getCapabilities();
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    // Get package name.
                    String pkgName = (String)
                        caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Remove from "unresolved" package map.
                    IModule[] modules = (IModule[]) m_unresolvedPkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, module);
                        m_unresolvedPkgIndexMap.put(pkgName, modules);
                    }

                    // Remove from "resolved" package map.
                    modules = (IModule[]) m_resolvedPkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, module);
                        m_resolvedPkgIndexMap.put(pkgName, modules);
                    }
                }
            }

            // Remove the module from the "resolved" map.
            m_resolvedCapMap.remove(module);
            // Set fragments to null, which will remove the module from all
            // of its dependent fragment modules.
            try
            {
                ((ModuleImpl) module).attachFragments(null);
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Error detaching fragments.", ex);
            }
            // Set wires to null, which will remove the module from all
            // of its dependent modules.
            ((ModuleImpl) module).setWires(null);
        }

        // Close the module's content.
        ((ModuleImpl) module).close();
    }

    /**
     * This method is used for installing system bundle extensions. It actually
     * refreshes the system bundle module's capabilities in the resolver state
     * to capture additional capabilities.
     * @param module The module being refresh, which should always be the system bundle.
    **/
    synchronized void refreshSystemBundleModule(IModule module)
    {
        // The system bundle module should always be resolved, so we only need
        // to update the resolved capability map.
        ICapability[] caps = module.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            ICapability[] resolvedCaps = (ICapability[]) m_resolvedCapMap.get(module);
            resolvedCaps = addCapabilityToArray(resolvedCaps, caps[i]);
            m_resolvedCapMap.put(module, resolvedCaps);

            // If the capability is a package, then add the exporter module
            // of the wire to the "resolved" package index and remove it
            // from the "unresolved" package index.
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // Add to "resolved" package index.
                indexPackageCapability(
                    m_resolvedPkgIndexMap,
                    module,
                    caps[i]);
            }
        }
    }

    private void dumpModuleIndexMap(Map moduleIndexMap)
    {
        for (Iterator i = moduleIndexMap.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            IModule[] modules = (IModule[]) entry.getValue();
            if ((modules != null) && (modules.length > 0))
            {
                if (!((modules.length == 1) && modules[0].getId().equals("0")))
                {
                    System.out.println("  " + entry.getKey());
                    for (int j = 0; j < modules.length; j++)
                    {
                        System.out.println("    " + modules[j]);
                    }
                }
            }
        }
    }

    private void dumpPackageIndexMap(Map pkgIndexMap)
    {
        for (Iterator i = pkgIndexMap.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            IModule[] modules = (IModule[]) entry.getValue();
            if ((modules != null) && (modules.length > 0))
            {
                if (!((modules.length == 1) && modules[0].getId().equals("0")))
                {
                    System.out.println("  " + entry.getKey());
                    for (int j = 0; j < modules.length; j++)
                    {
                        System.out.println("    " + modules[j]);
                    }
                }
            }
        }
    }

    public synchronized IModule[] getModules()
    {
        return (IModule[]) m_moduleList.toArray(new IModule[m_moduleList.size()]);
    }

    public synchronized void moduleResolved(IModule module)
    {
        if (module.isResolved())
        {
            // At this point, we need to remove all of the resolved module's
            // capabilities from the "unresolved" package map and put them in
            // in the "resolved" package map, with the exception of any
            // package exports that are also imported. In that case we need
            // to make sure that the import actually points to the resolved
            // module and not another module. If it points to another module
            // then the capability should be ignored, since the framework
            // decided to honor the import and discard the export.
            ICapability[] caps = module.getCapabilities();

            // First remove all existing capabilities from the "unresolved" map.
            for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
            {
                if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    // Get package name.
                    String pkgName = (String)
                        caps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Remove the module's capability for the package.
                    m_unresolvedPkgIndexMap.put(
                        pkgName,
                        removeModuleFromArray(
                            (IModule[]) m_unresolvedPkgIndexMap.get(pkgName),
                            module));
                }
            }

            // Next create a copy of the module's capabilities so we can
            // null out any capabilities that should be ignored.
            ICapability[] capsCopy = (caps == null) ? null : new ICapability[caps.length];
            if (capsCopy != null)
            {
                System.arraycopy(caps, 0, capsCopy, 0, caps.length);
            }
            // Loop through the module's capabilities to determine which ones
            // can be ignored by seeing which ones satifies the wire requirements.
// TODO: RB - Bug here because a requirement for a package need not overlap the
//            capability for that package and this assumes it does. This might
//            require us to introduce the notion of a substitutable capability.
            IWire[] wires = module.getWires();
            for (int capIdx = 0; (capsCopy != null) && (capIdx < capsCopy.length); capIdx++)
            {
                // Loop through all wires to see if the current capability
                // satisfies any of the wire requirements.
                for (int wireIdx = 0; (wires != null) && (wireIdx < wires.length); wireIdx++)
                {
                    // If one of the module's capabilities satifies the requirement
                    // for an existing wire, this means the capability was
                    // substituted with another provider by the resolver and
                    // the module's capability was not used. Therefore, we should
                    // null it here so it doesn't get added the list of resolved
                    // capabilities for this module.
                    if (wires[wireIdx].getRequirement().isSatisfied(capsCopy[capIdx]))
                    {
                        capsCopy[capIdx] = null;
                        break;
                    }
                }
            }

            // Now loop through all capabilities and add them to the "resolved"
            // capability and package index maps, ignoring any that were nulled out.
            for (int capIdx = 0; (capsCopy != null) && (capIdx < capsCopy.length); capIdx++)
            {
                if (capsCopy[capIdx] != null)
                {
                    ICapability[] resolvedCaps = (ICapability[]) m_resolvedCapMap.get(module);
                    resolvedCaps = addCapabilityToArray(resolvedCaps, capsCopy[capIdx]);
                    m_resolvedCapMap.put(module, resolvedCaps);

                    // If the capability is a package, then add the exporter module
                    // of the wire to the "resolved" package index and remove it
                    // from the "unresolved" package index.
                    if (capsCopy[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        // Add to "resolved" package index.
                        indexPackageCapability(
                            m_resolvedPkgIndexMap,
                            module,
                            capsCopy[capIdx]);
                    }
                }
            }
        }

//System.out.println("UNRESOLVED PACKAGES:");
//dumpPackageIndexMap(m_unresolvedPkgIndexMap);
//System.out.println("RESOLVED PACKAGES:");
//dumpPackageIndexMap(m_resolvedPkgIndexMap);
    }

    public synchronized PackageSource[] getResolvedCandidates(IRequirement req)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        PackageSource[] candidates = m_emptySources;
        if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
            && (((Requirement) req).getTargetName() != null))
        {
            String pkgName = ((Requirement) req).getTargetName();
            IModule[] modules = (IModule[]) m_resolvedPkgIndexMap.get(pkgName);

            for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
            {
                ICapability resolvedCap = Util.getSatisfyingCapability(modules[modIdx], req);
                if (resolvedCap != null)
                {
// TODO: RB - Is this permission check correct.
                    if ((System.getSecurityManager() != null) &&
                        !((BundleProtectionDomain) modules[modIdx].getSecurityContext()).impliesDirect(
                            new PackagePermission(pkgName,
                                PackagePermission.EXPORT)))
                    {
                        m_logger.log(Logger.LOG_DEBUG,
                            "PackagePermission.EXPORT denied for "
                            + pkgName
                            + "from " + modules[modIdx].getId());
                    }
                    else
                    {
                        PackageSource[] tmp = new PackageSource[candidates.length + 1];
                        System.arraycopy(candidates, 0, tmp, 0, candidates.length);
                        tmp[candidates.length] =
                            new PackageSource(modules[modIdx], resolvedCap);
                        candidates = tmp;
                    }
                }
            }
        }
        else
        {
            Iterator i = m_resolvedCapMap.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry) i.next();
                IModule module = (IModule) entry.getKey();
                ICapability[] resolvedCaps = (ICapability[]) entry.getValue();
                for (int capIdx = 0; capIdx < resolvedCaps.length; capIdx++)
                {
                    if (req.isSatisfied(resolvedCaps[capIdx]))
                    {
// TODO: RB - Is this permission check correct.
                        if (resolvedCaps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                            (System.getSecurityManager() != null) &&
                            !((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                new PackagePermission(
                                    (String) resolvedCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY),
                                    PackagePermission.EXPORT)))
                        {
                            m_logger.log(Logger.LOG_DEBUG,
                                "PackagePermission.EXPORT denied for "
                                + resolvedCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY)
                                + "from " + module.getId());
                        }
                        else
                        {
                            PackageSource[] tmp = new PackageSource[candidates.length + 1];
                            System.arraycopy(candidates, 0, tmp, 0, candidates.length);
                            tmp[candidates.length] = new PackageSource(module, resolvedCaps[capIdx]);
                            candidates = tmp;
                        }
                    }
                }
            }
        }
        Arrays.sort(candidates);
        return candidates;
    }

    public synchronized PackageSource[] getUnresolvedCandidates(IRequirement req)
    {
        // Get all modules.
        IModule[] modules = null;
        if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            (((Requirement) req).getTargetName() != null))
        {
            modules = (IModule[]) m_unresolvedPkgIndexMap.get(((Requirement) req).getTargetName());
        }
        else
        {
            modules = getModules();
        }

        // Create list of compatible providers.
        PackageSource[] candidates = m_emptySources;
        for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
        {
            // Get the module's export package for the target package.
            ICapability cap = Util.getSatisfyingCapability(modules[modIdx], req);
            // If compatible and it is not currently resolved, then add
            // the unresolved candidate to the list.
            if ((cap != null) && !modules[modIdx].isResolved())
            {
                PackageSource[] tmp = new PackageSource[candidates.length + 1];
                System.arraycopy(candidates, 0, tmp, 0, candidates.length);
                tmp[candidates.length] = new PackageSource(modules[modIdx], cap);
                candidates = tmp;
            }
        }
        Arrays.sort(candidates);
        return candidates;
    }

    //
    // Utility methods.
    //

    private void indexPackageCapability(Map map, IModule module, ICapability capability)
    {
        if (capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            String pkgName = (String)
                capability.getProperties().get(ICapability.PACKAGE_PROPERTY);
            IModule[] modules = (IModule[]) map.get(pkgName);

            // We want to add the module into the list of exporters
            // in sorted order (descending version and ascending bundle
            // identifier). Insert using a simple binary search algorithm.
            if (modules == null)
            {
                modules = new IModule[] { module };
            }
            else
            {
                Version version = (Version)
                    capability.getProperties().get(ICapability.VERSION_PROPERTY);
                Version middleVersion = null;
                int top = 0, bottom = modules.length - 1, middle = 0;
                while (top <= bottom)
                {
                    middle = (bottom - top) / 2 + top;
                    middleVersion = (Version)
                        getExportPackageCapability(
                            modules[middle], pkgName)
                                .getProperties()
                                    .get(ICapability.VERSION_PROPERTY);
                    // Sort in reverse version order.
                    int cmp = middleVersion.compareTo(version);
                    if (cmp < 0)
                    {
                        bottom = middle - 1;
                    }
                    else if (cmp == 0)
                    {
                        // Sort further by ascending bundle ID.
                        long middleId = Util.getBundleIdFromModuleId(modules[middle].getId());
                        long exportId = Util.getBundleIdFromModuleId(module.getId());
                        if (middleId < exportId)
                        {
                            top = middle + 1;
                        }
                        else
                        {
                            bottom = middle - 1;
                        }
                    }
                    else
                    {
                        top = middle + 1;
                    }
                }

                // Ignore duplicates.
                if ((top >= modules.length) || (modules[top] != module))
                {
                    IModule[] newMods = new IModule[modules.length + 1];
                    System.arraycopy(modules, 0, newMods, 0, top);
                    System.arraycopy(modules, top, newMods, top + 1, modules.length - top);
                    newMods[top] = module;
                    modules = newMods;
                }
            }

            map.put(pkgName, modules);
        }
    }

    private void indexFragment(Map map, IModule module)
    {
        IModule[] modules = (IModule[]) map.get(module.getSymbolicName());

        // We want to add the fragment into the list of matching
        // fragments in sorted order (descending version and
        // ascending bundle identifier). Insert using a simple
        // binary search algorithm.
        if (modules == null)
        {
            modules = new IModule[] { module };
        }
        else
        {
            Version version = module.getVersion();
            Version middleVersion = null;
            int top = 0, bottom = modules.length - 1, middle = 0;
            while (top <= bottom)
            {
                middle = (bottom - top) / 2 + top;
                middleVersion = modules[middle].getVersion();
                // Sort in reverse version order.
                int cmp = middleVersion.compareTo(version);
                if (cmp < 0)
                {
                    bottom = middle - 1;
                }
                else if (cmp == 0)
                {
                    // Sort further by ascending bundle ID.
                    long middleId = Util.getBundleIdFromModuleId(modules[middle].getId());
                    long exportId = Util.getBundleIdFromModuleId(module.getId());
                    if (middleId < exportId)
                    {
                        top = middle + 1;
                    }
                    else
                    {
                        bottom = middle - 1;
                    }
                }
                else
                {
                    top = middle + 1;
                }
            }

            // Ignore duplicates.
            if ((top >= modules.length) || (modules[top] != module))
            {
                IModule[] newMods = new IModule[modules.length + 1];
                System.arraycopy(modules, 0, newMods, 0, top);
                System.arraycopy(modules, top, newMods, top + 1, modules.length - top);
                newMods[top] = module;
                modules = newMods;
            }
        }

        map.put(module.getSymbolicName(), modules);
    }

    private static IModule[] removeModuleFromArray(IModule[] modules, IModule m)
    {
        if (modules == null)
        {
            return m_emptyModules;
        }

        int idx = -1;
        do
        {
            idx = -1;
            for (int i = 0; i < modules.length; i++)
            {
                if (modules[i] == m)
                {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0)
            {
                // If this is the module, then point to empty list.
                if ((modules.length - 1) == 0)
                {
                    modules = m_emptyModules;
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    IModule[] newModules = new IModule[modules.length - 1];
                    System.arraycopy(modules, 0, newModules, 0, idx);
                    if (idx < newModules.length)
                    {
                        System.arraycopy(
                            modules, idx + 1, newModules, idx, newModules.length - idx);
                    }
                    modules = newModules;
                }
            }
        }
        while (idx >= 0);

        return modules;
    }

    public static ICapability getExportPackageCapability(IModule m, String pkgName)
    {
        ICapability[] caps = m.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
            {
                return caps[i];
            }
        }
        return null;
    }

    private static ICapability[] addCapabilityToArray(ICapability[] caps, ICapability cap)
    {
        // Verify that the capability is not already in the array.
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].equals(cap))
            {
                return caps;
            }
        }

        if (caps != null)
        {
            ICapability[] newCaps = new ICapability[caps.length + 1];
            System.arraycopy(caps, 0, newCaps, 0, caps.length);
            newCaps[caps.length] = cap;
            caps = newCaps;
        }
        else
        {
            caps = new ICapability[] { cap };
        }

        return caps;
    }
}