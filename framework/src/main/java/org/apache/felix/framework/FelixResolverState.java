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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.felix.framework.searchpolicy.ResolveException;
import org.apache.felix.framework.searchpolicy.Resolver;
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
    // Map of fragment symbolic names to list of fragment modules sorted by version.
    private final Map m_fragmentMap = new HashMap();
    // Maps a package name to a list of exporting capabilities.
    private final Map m_unresolvedPkgIndex = new HashMap();
    // Maps a package name to a list of exporting capabilities.
    private final Map m_resolvedPkgIndex = new HashMap();
    // Maps a module to a list of capabilities.
    private final Map m_resolvedCapMap = new HashMap();

    public FelixResolverState(Logger logger)
    {
        m_logger = logger;
    }

    public synchronized void addModule(IModule module)
    {
        if (Util.isFragment(module))
        {
            addFragment(module);
        }
        else
        {
            addHost(module);
        }

//System.out.println("UNRESOLVED PACKAGES:");
//dumpPackageIndex(m_unresolvedPkgIndex);
//System.out.println("RESOLVED PACKAGES:");
//dumpPackageIndex(m_resolvedPkgIndex);
    }

    public synchronized void removeModule(IModule module)
    {
        if (Util.isFragment(module))
        {
            removeFragment(module);
        }
        else
        {
            removeHost(module);
        }
    }

    private void addFragment(IModule fragment)
    {
// TODO: FRAGMENT - This should check to make sure that the host allows fragments.
        IModule bestFragment = indexFragment(m_fragmentMap, fragment);

        // If the newly added fragment is the highest version for
        // its given symbolic name, then try to merge it to any
        // matching unresolved hosts and remove the previous highest
        // version of the fragment.
        if (bestFragment == fragment)
        {

            // If we have any matching hosts, then merge the new fragment while
            // removing any older version of the new fragment. Also remove host's
            // existing capabilities from the package index and reindex its new
            // ones after attaching the fragment.
            List matchingHosts = getMatchingHosts(fragment);
            for (int hostIdx = 0; hostIdx < matchingHosts.size(); hostIdx++)
            {
                IModule host = ((ICapability) matchingHosts.get(hostIdx)).getModule();

                // Get the fragments currently attached to the host so we
                // can remove the older version of the current fragment, if any.
                IModule[] fragments = ((ModuleImpl) host).getFragments();
                List fragmentList = new ArrayList();
                for (int fragIdx = 0;
                    (fragments != null) && (fragIdx < fragments.length);
                    fragIdx++)
                {
                    if (!fragments[fragIdx].getSymbolicName().equals(
                        bestFragment.getSymbolicName()))
                    {
                        fragmentList.add(fragments[fragIdx]);
                    }
                }

                // Now add the new fragment in bundle ID order.
                int index = -1;
                for (int listIdx = 0;
                    (index < 0) && (listIdx < fragmentList.size());
                    listIdx++)
                {
                    IModule f = (IModule) fragmentList.get(listIdx);
                    if (bestFragment.getBundle().getBundleId()
                        < f.getBundle().getBundleId())
                    {
                        index = listIdx;
                    }
                }
                fragmentList.add(
                    (index < 0) ? fragmentList.size() : index, bestFragment);

                // Remove host's existing exported packages from index.
                ICapability[] caps = host.getCapabilities();
                for (int i = 0; (caps != null) && (i < caps.length); i++)
                {
                    if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        // Get package name.
                        String pkgName = (String)
                            caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                        // Remove from "unresolved" package map.
                        List capList = (List) m_unresolvedPkgIndex.get(pkgName);
                        if (capList != null)
                        {
                            capList.remove(caps[i]);
                        }
                    }
                }

                // Check if fragment conflicts with existing metadata.
                checkForConflicts(host, fragmentList);

                // Attach the fragments to the host.
                fragments = (fragmentList.size() == 0)
                    ? null
                    : (IModule[]) fragmentList.toArray(new IModule[fragmentList.size()]);
                try
                {
                    ((ModuleImpl) host).attachFragments(fragments);
                }
                catch (Exception ex)
                {
                    // Try to clean up by removing all fragments.
                    try
                    {
                        ((ModuleImpl) host).attachFragments(null);
                    }
                    catch (Exception ex2)
                    {
                    }
                    m_logger.log(Logger.LOG_ERROR,
                        "Serious error attaching fragments.", ex);
                }

                // Reindex the host's exported packages.
                caps = host.getCapabilities();
                for (int i = 0; (caps != null) && (i < caps.length); i++)
                {
                    if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        indexPackageCapability(m_unresolvedPkgIndex, caps[i]);
                    }
                }
            }
        }
    }

    private void removeFragment(IModule fragment)
    {
        // Get fragment list, which may be null for system bundle fragments.
        List fragList = (List) m_fragmentMap.get(fragment.getSymbolicName());
        if (fragList != null)
        {
            // Remove from fragment map.
            fragList.remove(fragment);
            if (fragList.size() == 0)
            {
                m_fragmentMap.remove(fragment.getSymbolicName());
            }

            // If we have any matching hosts, then remove  fragment while
            // removing any older version of the new fragment. Also remove host's
            // existing capabilities from the package index and reindex its new
            // ones after attaching the fragment.
            List matchingHosts = getMatchingHosts(fragment);
            for (int hostIdx = 0; hostIdx < matchingHosts.size(); hostIdx++)
            {
                IModule host = ((ICapability) matchingHosts.get(hostIdx)).getModule();

                // Check to see if the removed fragment was actually merged with
                // the host, since it might not be if it wasn't the highest version.
                // If it was, recalculate the fragments for the host.
                IModule[] fragments = ((ModuleImpl) host).getFragments();
                for (int fragIdx = 0;
                    (fragments != null) && (fragIdx < fragments.length);
                    fragIdx++)
                {
                    if (!fragments[fragIdx].equals(fragment))
                    {
                        List fragmentList = getMatchingFragments(host);

                        // Remove host's existing exported packages from index.
                        ICapability[] caps = host.getCapabilities();
                        for (int i = 0; (caps != null) && (i < caps.length); i++)
                        {
                            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                            {
                                // Get package name.
                                String pkgName = (String)
                                    caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                                // Remove from "unresolved" package map.
                                List capList = (List) m_unresolvedPkgIndex.get(pkgName);
                                if (capList != null)
                                {
                                    capList.remove(caps[i]);
                                }
                            }
                        }

                        // Check if fragment conflicts with existing metadata.
                        checkForConflicts(host, fragmentList);

                        // Attach the fragments to the host.
                        fragments = (fragmentList.size() == 0)
                            ? null
                            : (IModule[]) fragmentList.toArray(new IModule[fragmentList.size()]);
                        try
                        {
                            ((ModuleImpl) host).attachFragments(fragments);
                        }
                        catch (Exception ex)
                        {
                            // Try to clean up by removing all fragments.
                            try
                            {
                                ((ModuleImpl) host).attachFragments(null);
                            }
                            catch (Exception ex2)
                            {
                            }
                            m_logger.log(Logger.LOG_ERROR,
                                "Serious error attaching fragments.", ex);
                        }

                        // Reindex the host's exported packages.
                        caps = host.getCapabilities();
                        for (int i = 0; (caps != null) && (i < caps.length); i++)
                        {
                            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                            {
                                indexPackageCapability(m_unresolvedPkgIndex, caps[i]);
                            }
                        }
                    }
                }
            }
        }
    }

    private List getMatchingHosts(IModule fragment)
    {
        // Find the fragment's host requirement.
        IRequirement hostReq = getFragmentHostRequirement(fragment);

        // Create a list of all matching hosts for this fragment.
        List matchingHosts = new ArrayList();
        for (int hostIdx = 0; (hostReq != null) && (hostIdx < m_moduleList.size()); hostIdx++)
        {
            IModule host = (IModule) m_moduleList.get(hostIdx);
            // Only look at unresolved hosts, since we don't support
            // dynamic attachment of fragments.
            if (host.isResolved()
                || ((BundleImpl) host.getBundle()).isStale()
                || ((BundleImpl) host.getBundle()).isRemovalPending())
            {
                continue;
            }

            // Find the host capability for the current host.
            ICapability hostCap = Util.getSatisfyingCapability(host, hostReq);

            // If there is no host capability in the current module,
            // then just ignore it.
            if (hostCap == null)
            {
                continue;
            }

            matchingHosts.add(hostCap);
        }

        return matchingHosts;
    }

    private void checkForConflicts(IModule host, List fragmentList)
    {
        if ((fragmentList == null) || (fragmentList.size() == 0))
        {
            return;
        }

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
                    Map mergedReqMap =
                        (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                            ? ipMerged : rbMerged;
                    Map fragmentReqMap =
                        (reqs[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
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

    private void addHost(IModule host)
    {
        // When a module is added, we first need to pre-merge any potential fragments
        // into the host and then second create an aggregated list of unresolved
        // capabilities to simplify later processing when resolving bundles.
        m_moduleList.add(host);

        //
        // First, merge applicable fragments.
        //

        List fragmentList = getMatchingFragments(host);

        // Attach any fragments we found for this host.
        if (fragmentList.size() > 0)
        {
            // Check if fragment conflicts with existing metadata.
            checkForConflicts(host, fragmentList);

            // Attach the fragments to the host.
            IModule[] fragments =
                (IModule[]) fragmentList.toArray(new IModule[fragmentList.size()]);
            try
            {
                ((ModuleImpl) host).attachFragments(fragments);
            }
            catch (Exception ex)
            {
                // Try to clean up by removing all fragments.
                try
                {
                    ((ModuleImpl) host).attachFragments(null);
                }
                catch (Exception ex2)
                {
                }
                m_logger.log(Logger.LOG_ERROR,
                    "Serious error attaching fragments.", ex);
            }
        }

        //
        // Second, index module's capabilities.
        //

        ICapability[] caps = host.getCapabilities();

        // Add exports to unresolved package map.
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                indexPackageCapability(m_unresolvedPkgIndex, caps[i]);
            }
        }
    }

    private void removeHost(IModule host)
    {
        // We need remove the host's exports from the "resolved" and
        // "unresolved" package maps, remove its dependencies on fragments
        // and exporters, and remove it from the module list.
        m_moduleList.remove(host);

        // Remove exports from package maps.
        ICapability[] caps = host.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // Get package name.
                String pkgName = (String)
                    caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                // Remove from "unresolved" package map.
                List capList = (List) m_unresolvedPkgIndex.get(pkgName);
                if (capList != null)
                {
                    capList.remove(caps[i]);
                }

                // Remove from "resolved" package map.
                capList = (List) m_resolvedPkgIndex.get(pkgName);
                if (capList != null)
                {
                    capList.remove(caps[i]);
                }
            }
        }

        // Remove the module from the "resolved" map.
        m_resolvedCapMap.remove(host);

        // Set fragments to null, which will remove the module from all
        // of its dependent fragment modules.
        try
        {
            ((ModuleImpl) host).attachFragments(null);
        }
        catch (Exception ex)
        {
            m_logger.log(Logger.LOG_ERROR, "Error detaching fragments.", ex);
        }
        // Set wires to null, which will remove the module from all
        // of its dependent modules.
        ((ModuleImpl) host).setWires(null);
    }

    private List getMatchingFragments(IModule host)
    {
        // Find the host capability for the current host.
        ICapability[] caps = Util.getCapabilityByNamespace(host, ICapability.HOST_NAMESPACE);
        ICapability hostCap = (caps.length == 0) ? null : caps[0];

        // If we have a host capability, then loop through all fragments trying to
        // find ones that match.
        List fragmentList = new ArrayList();
        for (Iterator it = m_fragmentMap.entrySet().iterator(); (hostCap != null) && it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            List fragments = (List) entry.getValue();
            IModule fragment = null;
            for (int i = 0; (fragment == null) && (i < fragments.size()); i++)
            {
                IModule f = (IModule) fragments.get(i);
                if (!((BundleImpl) f.getBundle()).isStale()
                    && !((BundleImpl) f.getBundle()).isRemovalPending())
                {
                    fragment = f;
                }
            }

            if (fragment == null)
            {
                continue;
            }

            IRequirement hostReq = getFragmentHostRequirement(fragment);

            // If we have a host requirement, then loop through each host and
            // see if it matches the host requirement.
            if ((hostReq != null) && hostReq.isSatisfied(hostCap))
            {
                // Now add the new fragment in bundle ID order.
                int index = -1;
                for (int listIdx = 0;
                    (index < 0) && (listIdx < fragmentList.size());
                    listIdx++)
                {
                    IModule existing = (IModule) fragmentList.get(listIdx);
                    if (fragment.getBundle().getBundleId()
                        < existing.getBundle().getBundleId())
                    {
                        index = listIdx;
                    }
                }
                fragmentList.add(
                    (index < 0) ? fragmentList.size() : index, fragment);
            }
        }

        return fragmentList;
    }

    public synchronized IModule findHost(IModule rootModule) throws ResolveException
    {
        IModule newRootModule = rootModule;
        if (Util.isFragment(rootModule))
        {
            List matchingHosts = getMatchingHosts(rootModule);
            IModule currentBestHost = null;
            for (int hostIdx = 0; hostIdx < matchingHosts.size(); hostIdx++)
            {
                IModule host = ((ICapability) matchingHosts.get(hostIdx)).getModule();
                if (currentBestHost == null)
                {
                    currentBestHost = host;
                }
                else if (currentBestHost.getVersion().compareTo(host.getVersion()) < 0)
                {
                    currentBestHost = host;
                }
            }
            newRootModule = currentBestHost;

            if (newRootModule == null)
            {
                throw new ResolveException(
                    "Unable to find host.", rootModule, getFragmentHostRequirement(rootModule));
            }
        }

        return newRootModule;
    }

    private IRequirement getFragmentHostRequirement(IModule fragment)
    {
        // Find the fragment's host requirement.
        IRequirement[] reqs = fragment.getRequirements();
        IRequirement hostReq = null;
        for (int reqIdx = 0; (hostReq == null) && (reqIdx < reqs.length); reqIdx++)
        {
            if (reqs[reqIdx].getNamespace().equals(ICapability.HOST_NAMESPACE))
            {
                hostReq = reqs[reqIdx];
            }
        }
        return hostReq;
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
            List resolvedCaps = (List) m_resolvedCapMap.get(module);
            if (resolvedCaps == null)
            {
                m_resolvedCapMap.put(module, resolvedCaps = new ArrayList());
            }
            if (!resolvedCaps.contains(caps[i]))
            {
                resolvedCaps.add(caps[i]);
            }

            // If the capability is a package, then add the exporter module
            // of the wire to the "resolved" package index and remove it
            // from the "unresolved" package index.
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // Add to "resolved" package index.
                indexPackageCapability(m_resolvedPkgIndex, caps[i]);
            }
        }
    }

    private void dumpPackageIndex(Map pkgIndex)
    {
        for (Iterator i = pkgIndex.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            List capList = (List) entry.getValue();
            if (capList.size() > 0)
            {
                if (!((capList.size() == 1) && ((ICapability) capList.get(0)).getModule().getId().equals("0")))
                {
                    System.out.println("  " + entry.getKey());
                    for (int j = 0; j < capList.size(); j++)
                    {
                        System.out.println("    " + ((ICapability) capList.get(j)).getModule());
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
                    List capList = (List) m_unresolvedPkgIndex.get(pkgName);
                    capList.remove(caps[capIdx]);
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
                    List resolvedCaps = (List) m_resolvedCapMap.get(module);
                    if (resolvedCaps == null)
                    {
                        m_resolvedCapMap.put(module, resolvedCaps = new ArrayList());
                    }
                    if (!resolvedCaps.contains(capsCopy[capIdx]))
                    {
                        resolvedCaps.add(capsCopy[capIdx]);
                    }

                    // If the capability is a package, then add the exporter module
                    // of the wire to the "resolved" package index and remove it
                    // from the "unresolved" package index.
                    if (capsCopy[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        // Add to "resolved" package index.
                        indexPackageCapability(m_resolvedPkgIndex, capsCopy[capIdx]);
                    }
                }
            }
        }

//System.out.println("UNRESOLVED PACKAGES:");
//dumpPackageIndex(m_unresolvedPkgIndex);
//System.out.println("RESOLVED PACKAGES:");
//dumpPackageIndex(m_resolvedPkgIndex);
    }

    public synchronized List getResolvedCandidates(IRequirement req)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        List candidates = new ArrayList();
        if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
            && (((Requirement) req).getTargetName() != null))
        {
            String pkgName = ((Requirement) req).getTargetName();
            List capList = (List) m_resolvedPkgIndex.get(pkgName);

            for (int capIdx = 0; (capList != null) && (capIdx < capList.size()); capIdx++)
            {
                ICapability cap = (ICapability) capList.get(capIdx);
                if (req.isSatisfied(cap))
                {
// TODO: RB - Is this permission check correct.
                    if ((System.getSecurityManager() != null) &&
                        !((BundleProtectionDomain) cap.getModule().getSecurityContext())
                            .impliesDirect(new PackagePermission(pkgName, PackagePermission.EXPORT)))
                    {
                        m_logger.log(Logger.LOG_DEBUG,
                            "PackagePermission.EXPORT denied for "
                            + pkgName
                            + "from " + cap.getModule().getId());
                    }
                    else
                    {
                        candidates.add(cap);
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
                List caps = (List) entry.getValue();
                for (int capIdx = 0; (caps != null) && (capIdx < caps.size()); capIdx++)
                {
                    ICapability cap = (ICapability) caps.get(capIdx);
                    if (req.isSatisfied(cap))
                    {
// TODO: RB - Is this permission check correct.
                        if (cap.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                            (System.getSecurityManager() != null) &&
                            !((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                new PackagePermission(
                                    (String) cap.getProperties().get(ICapability.PACKAGE_PROPERTY),
                                    PackagePermission.EXPORT)))
                        {
                            m_logger.log(Logger.LOG_DEBUG,
                                "PackagePermission.EXPORT denied for "
                                + cap.getProperties().get(ICapability.PACKAGE_PROPERTY)
                                + "from " + module.getId());
                        }
                        else
                        {
                            candidates.add(cap);
                        }
                    }
                }
            }
        }
        Collections.sort(candidates);
        return candidates;
    }

    public synchronized List getUnresolvedCandidates(IRequirement req)
    {
        // Get all matching unresolved capabilities.
        List candidates = new ArrayList();
        if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            (((Requirement) req).getTargetName() != null))
        {
            List capList = (List) m_unresolvedPkgIndex.get(((Requirement) req).getTargetName());
            for (int capIdx = 0; (capList != null) && (capIdx < capList.size()); capIdx++)
            {
                // If compatible and it is not currently resolved, then add
                // the unresolved candidate to the list.
                if (req.isSatisfied((ICapability) capList.get(capIdx)))
                {
                    candidates.add(capList.get(capIdx));
                }
            }
        }
        else
        {
            IModule[] modules = getModules();
            for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
            {
                // Get the module's export package for the target package.
                ICapability cap = Util.getSatisfyingCapability(modules[modIdx], req);
                // If compatible and it is not currently resolved, then add
                // the unresolved candidate to the list.
                if ((cap != null) && !modules[modIdx].isResolved())
                {
                    candidates.add(cap);
                }
            }
        }

        // Create list of compatible providers.
        Collections.sort(candidates);
        return candidates;
    }

    //
    // Utility methods.
    //

    private void indexPackageCapability(Map map, ICapability capability)
    {
        if (capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            String pkgName = (String)
                capability.getProperties().get(ICapability.PACKAGE_PROPERTY);
            List capList = (List) map.get(pkgName);

            // We want to add the capability into the list of exporters
            // in sorted order (descending version and ascending bundle
            // identifier). Insert using a simple binary search algorithm.
            if (capList == null)
            {
                capList = new ArrayList();
                capList.add(capability);
            }
            else
            {
                Version version = (Version)
                    capability.getProperties().get(ICapability.VERSION_PROPERTY);
                Version middleVersion = null;
                int top = 0, bottom = capList.size() - 1, middle = 0;
                while (top <= bottom)
                {
                    middle = (bottom - top) / 2 + top;
                    middleVersion = (Version)
                        ((ICapability) capList.get(middle))
                            .getProperties().get(ICapability.VERSION_PROPERTY);
                    // Sort in reverse version order.
                    int cmp = middleVersion.compareTo(version);
                    if (cmp < 0)
                    {
                        bottom = middle - 1;
                    }
                    else if (cmp == 0)
                    {
                        // Sort further by ascending bundle ID.
                        long middleId = ((ICapability) capList.get(middle))
                            .getModule().getBundle().getBundleId();
                        long exportId = capability.getModule().getBundle().getBundleId();
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
                if ((top >= capList.size()) || (capList.get(top) != capability))
                {
                    capList.add(top, capability);
                }
            }

            map.put(pkgName, capList);
        }
    }

    private IModule indexFragment(Map map, IModule module)
    {
        List modules = (List) map.get(module.getSymbolicName());

        // We want to add the fragment into the list of matching
        // fragments in sorted order (descending version and
        // ascending bundle identifier). Insert using a simple
        // binary search algorithm.
        if (modules == null)
        {
            modules = new ArrayList();
            modules.add(module);
        }
        else
        {
            Version version = module.getVersion();
            Version middleVersion = null;
            int top = 0, bottom = modules.size() - 1, middle = 0;
            while (top <= bottom)
            {
                middle = (bottom - top) / 2 + top;
                middleVersion = ((IModule) modules.get(middle)).getVersion();
                // Sort in reverse version order.
                int cmp = middleVersion.compareTo(version);
                if (cmp < 0)
                {
                    bottom = middle - 1;
                }
                else if (cmp == 0)
                {
                    // Sort further by ascending bundle ID.
                    long middleId = ((IModule) modules.get(middle)).getBundle().getBundleId();
                    long exportId = module.getBundle().getBundleId();
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
            if ((top >= modules.size()) || (modules.get(top) != module))
            {
                modules.add(top, module);
            }
        }

        map.put(module.getSymbolicName(), modules);

        return (IModule) modules.get(0);
    }
}