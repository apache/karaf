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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Wire;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Resolver;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class FelixResolverState implements Resolver.ResolverState
{
    private final Logger m_logger;
    // List of all modules.
    private final List<Module> m_modules;
    // Capability set for modules.
    private final CapabilitySet m_modCapSet;
    // Capability set for packages.
    private final CapabilitySet m_pkgCapSet;
    // Capability set for hosts.
    private final CapabilitySet m_hostCapSet;
    // Maps fragment symbolic names to list of fragment modules sorted by version.
    private final Map<String, List<Module>> m_fragmentMap = new HashMap();
    // Maps singleton symbolic names to list of modules sorted by version.
    private final Map<String, List<Module>> m_singletons = new HashMap();

    public FelixResolverState(Logger logger)
    {
        m_logger = logger;
        m_modules = new ArrayList<Module>();

        List indices = new ArrayList();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_modCapSet = new CapabilitySet(indices);

        indices = new ArrayList();
        indices.add(Capability.PACKAGE_ATTR);
        m_pkgCapSet = new CapabilitySet(indices);

        indices = new ArrayList();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_hostCapSet = new CapabilitySet(indices);
    }

    public synchronized void addModule(Module module)
    {
        if (isSingleton(module))
        {
            // Find the currently selected singleton, which is either the
            // highest version or the resolved one.
            List<Module> modules = m_singletons.get(module.getSymbolicName());
            // Get the highest version.
            Module current = ((modules != null) && !modules.isEmpty()) ? modules.get(0) : null;
            // Now check to see if there is a resolved one instead.
            for (int i = 0; (modules != null) && (i < modules.size()); i++)
            {
                if (modules.get(i).isResolved())
                {
                    current = modules.get(i);
                }
            }

            // Index the new singleton.
            Module highest = indexModule(m_singletons, module);
            // If the currently selected singleton is not resolved and
            // the newly added singleton is a higher version, then select
            // it instead.
            if ((current != null) && !current.isResolved() && (current != highest))
            {
                if (Util.isFragment(current))
                {
                    removeFragment(current);
                }
                else
                {
                    removeHost(current);
                }
            }
            else if (current != null)
            {
                module = null;
            }
        }

        if ((module != null) && Util.isFragment(module))
        {
             addFragment(module);
        }
        else if (module != null)
        {
            addHost(module);
        }

//System.out.println("UNRESOLVED PACKAGES:");
//dumpPackageIndex(m_unresolvedPkgIndex);
//System.out.println("RESOLVED PACKAGES:");
//dumpPackageIndex(m_resolvedPkgIndex);
    }

    public synchronized void removeModule(Module module)
    {
        // If this module is a singleton, then remove it from the
        // singleton map.
        List<Module> modules = m_singletons.get(module.getSymbolicName());
        if (modules != null)
        {
            modules.remove(module);
            if (modules.size() == 0)
            {
                m_singletons.remove(module.getSymbolicName());
            }
        }

        if (Util.isFragment(module))
        {
            removeFragment(module);
        }
        else
        {
            removeHost(module);
        }
    }

    public void detachFragment(Module host, Module fragment)
    {
        List<Module> fragments = ((ModuleImpl) host).getFragments();
        fragments.remove(fragment);
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

    public void checkSingleton(Module module)
    {
        // Check if this module is a singleton.
        List<Module> modules = m_singletons.get(module.getSymbolicName());
        if ((modules != null) && modules.contains(module))
        {
            // If it is, check if there is already a resolved singleton.
            for (int i = 0; (modules != null) && (i < modules.size()); i++)
            {
                if (modules.get(i).isResolved())
                {
                    throw new ResolveException(
                        "Only one singleton can be resolved at a time.", null, null);
                }
            }

            // If not, check to see if it is the selected singleton.
            Module current = (modules.size() > 0) ? modules.get(0) : null;
            if ((current != null) && (current != module))
            {
                // If it is not the selected singleton, remove the selected
                // singleton and select the specified one instead.
                if (Util.isFragment(current))
                {
                    removeFragment(current);
                }
                else
                {
                    removeHost(current);
                }
                if (Util.isFragment(module))
                {
                     addFragment(module);
                }
                else if (module != null)
                {
                    addHost(module);
                }
            }
        }
    }

    private void addFragment(Module fragment)
    {
// TODO: FRAGMENT - This should check to make sure that the host allows fragments.
        Module bestFragment = indexModule(m_fragmentMap, fragment);

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
                Module host = ((Capability) matchingHosts.get(hostIdx)).getModule();

                // Get the fragments currently attached to the host so we
                // can remove the older version of the current fragment, if any.
                List<Module> fragments = ((ModuleImpl) host).getFragments();
                List<Module> fragmentList = new ArrayList();
                for (int fragIdx = 0;
                    (fragments != null) && (fragIdx < fragments.size());
                    fragIdx++)
                {
                    if (!fragments.get(fragIdx).getSymbolicName().equals(
                        bestFragment.getSymbolicName()))
                    {
                        fragmentList.add(fragments.get(fragIdx));
                    }
                }

                // Now add the new fragment in bundle ID order.
                int index = -1;
                for (int listIdx = 0;
                    (index < 0) && (listIdx < fragmentList.size());
                    listIdx++)
                {
                    Module f = fragmentList.get(listIdx);
                    if (bestFragment.getBundle().getBundleId()
                        < f.getBundle().getBundleId())
                    {
                        index = listIdx;
                    }
                }
                fragmentList.add(
                    (index < 0) ? fragmentList.size() : index, bestFragment);

                // Remove host's existing exported packages from index.
                List<Capability> caps = host.getCapabilities();
                for (int i = 0; (caps != null) && (i < caps.size()); i++)
                {
                    if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
                    {
                        m_modCapSet.removeCapability(caps.get(i));
                    }
                    else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                    {
                        m_pkgCapSet.removeCapability(caps.get(i));
                    }
                }

                // Attach the fragments to the host.
                fragments = (fragmentList.size() == 0) ? null : fragmentList;
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
                for (int i = 0; (caps != null) && (i < caps.size()); i++)
                {
                    if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
                    {
                        m_modCapSet.addCapability(caps.get(i));
                    }
                    else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                    {
                        m_pkgCapSet.addCapability(caps.get(i));
                    }
                }
            }
        }
    }

    private void removeFragment(Module fragment)
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
                Module host = ((Capability) matchingHosts.get(hostIdx)).getModule();

                // Check to see if the removed fragment was actually merged with
                // the host, since it might not be if it wasn't the highest version.
                // If it was, recalculate the fragments for the host.
                List<Module> fragments = ((ModuleImpl) host).getFragments();
                if (fragments.contains(fragment))
                {
                    List fragmentList = getMatchingFragments(host);

                    // Remove host's existing exported packages from index.
                    List<Capability> caps = host.getCapabilities();
                    for (int i = 0; (caps != null) && (i < caps.size()); i++)
                    {
                        if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
                        {
                            m_modCapSet.removeCapability(caps.get(i));
                        }
                        else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                        {
                            m_pkgCapSet.removeCapability(caps.get(i));
                        }
                    }

                    // Attach the fragments to the host.
                    try
                    {
                        ((ModuleImpl) host).attachFragments(fragmentList);
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
                    for (int i = 0; (caps != null) && (i < caps.size()); i++)
                    {
                        if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
                        {
                            m_modCapSet.addCapability(caps.get(i));
                        }
                        else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                        {
                            m_pkgCapSet.addCapability(caps.get(i));
                        }
                    }
                }
            }
        }
    }

    public void unmergeFragment(Module fragment)
    {
        if (!Util.isFragment(fragment))
        {
            return;
        }

        removeFragment(fragment);
    }

    private List getMatchingHosts(Module fragment)
    {
        // Find the fragment's host requirement.
        Requirement hostReq = getFragmentHostRequirement(fragment);

        // Create a list of all matching hosts for this fragment.
        List matchingHosts = new ArrayList();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            if (!((BundleProtectionDomain) fragment.getSecurityContext()).impliesDirect(new BundlePermission(
                fragment.getSymbolicName(), BundlePermission.FRAGMENT)))
            {
                return matchingHosts;
            }
        }

        Set<Capability> hostCaps = m_hostCapSet.match(hostReq.getFilter(), true);

        for (Capability hostCap : hostCaps)
        {
            // Only look at unresolved hosts, since we don't support
            // dynamic attachment of fragments.
            if (hostCap.getModule().isResolved()
                || ((BundleImpl) hostCap.getModule().getBundle()).isStale()
                || ((BundleImpl) hostCap.getModule().getBundle()).isRemovalPending())
            {
                continue;
            }

            if (sm != null)
            {
                if (!((BundleProtectionDomain) hostCap.getModule()
                        .getSecurityContext()).impliesDirect(
                            new BundlePermission(hostCap.getModule().getSymbolicName(),
                            BundlePermission.HOST)))
                {
                    continue;
                }
            }

            matchingHosts.add(hostCap);
        }

        return matchingHosts;
    }

    private void addHost(Module host)
    {
        // When a module is added, we first need to pre-merge any potential fragments
        // into the host and then second create an aggregated list of unresolved
        // capabilities to simplify later processing when resolving bundles.
        m_modules.add(host);
        List<Capability> caps = Util.getCapabilityByNamespace(host, Capability.HOST_NAMESPACE);
        if (caps.size() > 0)
        {
            m_hostCapSet.addCapability(caps.get(0));
        }

        //
        // First, merge applicable fragments.
        //

        List<Module> fragments = getMatchingFragments(host);

        // Attach any fragments we found for this host.
        if (fragments.size() > 0)
        {
            // Attach the fragments to the host.
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

        caps = host.getCapabilities();

        // Add exports to unresolved package map.
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
            {
                m_modCapSet.addCapability(caps.get(i));
            }
            else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                m_pkgCapSet.addCapability(caps.get(i));
            }
        }
    }

    private void removeHost(Module host)
    {
        // We need remove the host's exports from the "resolved" and
        // "unresolved" package maps, remove its dependencies on fragments
        // and exporters, and remove it from the module list.
        m_modules.remove(host);
        List<Capability> caps = Util.getCapabilityByNamespace(host, Capability.HOST_NAMESPACE);
        if (caps.size() > 0)
        {
            m_hostCapSet.removeCapability(caps.get(0));
        }

        // Remove exports from package maps.
        caps = host.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
            {
                m_modCapSet.removeCapability(caps.get(i));
            }
            else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                m_pkgCapSet.removeCapability(caps.get(i));
            }
        }

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

    private List getMatchingFragments(Module host)
    {
        // Find the host capability for the current host.
        List<Capability> caps = Util.getCapabilityByNamespace(host, Capability.HOST_NAMESPACE);
        Capability hostCap = (caps.size() == 0) ? null : caps.get(0);

        // If we have a host capability, then loop through all fragments trying to
        // find ones that match.
        List fragmentList = new ArrayList();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            if (!((BundleProtectionDomain) host.getSecurityContext()).impliesDirect(new BundlePermission(host.getSymbolicName(), BundlePermission.HOST)))
            {
                return fragmentList;
            }
        }
        for (Iterator it = m_fragmentMap.entrySet().iterator(); (hostCap != null) && it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            List fragments = (List) entry.getValue();
            Module fragment = null;
            for (int i = 0; (fragment == null) && (i < fragments.size()); i++)
            {
                Module f = (Module) fragments.get(i);
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
            
            if (sm != null)
            {
                if (!((BundleProtectionDomain) fragment.getSecurityContext()).impliesDirect(new BundlePermission(fragment.getSymbolicName(), BundlePermission.FRAGMENT)))
                {
                    continue;
                }
            }
            Requirement hostReq = getFragmentHostRequirement(fragment);

            // If we have a host requirement, then loop through each host and
            // see if it matches the host requirement.
            if ((hostReq != null) && CapabilitySet.matches(hostCap, hostReq.getFilter()))
            {
                // Now add the new fragment in bundle ID order.
                int index = -1;
                for (int listIdx = 0;
                    (index < 0) && (listIdx < fragmentList.size());
                    listIdx++)
                {
                    Module existing = (Module) fragmentList.get(listIdx);
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

    public synchronized Module findHost(Module rootModule) throws ResolveException
    {
        Module newRootModule = rootModule;
        if (Util.isFragment(rootModule))
        {
            List matchingHosts = getMatchingHosts(rootModule);
            Module currentBestHost = null;
            for (int hostIdx = 0; hostIdx < matchingHosts.size(); hostIdx++)
            {
                Module host = ((Capability) matchingHosts.get(hostIdx)).getModule();
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

    private static Requirement getFragmentHostRequirement(Module fragment)
    {
        // Find the fragment's host requirement.
        List<Requirement> reqs = fragment.getRequirements();
        Requirement hostReq = null;
        for (int reqIdx = 0; (hostReq == null) && (reqIdx < reqs.size()); reqIdx++)
        {
            if (reqs.get(reqIdx).getNamespace().equals(Capability.HOST_NAMESPACE))
            {
                hostReq = reqs.get(reqIdx);
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
    synchronized void refreshSystemBundleModule(Module module)
    {
        // The system bundle module should always be resolved, so we only need
        // to update the resolved capability map.
        List<Capability> caps = module.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(Capability.MODULE_NAMESPACE))
            {
                m_modCapSet.addCapability(caps.get(i));
            }
            else if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                m_pkgCapSet.addCapability(caps.get(i));
            }
        }
    }

// TODO: FELIX3 - Try to eliminate this.
    public synchronized List<Module> getModules()
    {
        return m_modules;
    }

    public synchronized void moduleResolved(Module module)
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
            List<Capability> caps = module.getCapabilities();

            // Create a copy of the module's capabilities so we can
            // null out any capabilities that should be ignored.
            List<Capability> capsCopy = (caps == null) ? null : new ArrayList(caps);
            // Loop through the module's capabilities to determine which ones
            // can be ignored by seeing which ones satifies the wire requirements.
// TODO: RB - Bug here because a requirement for a package need not overlap the
//            capability for that package and this assumes it does. This might
//            require us to introduce the notion of a substitutable capability.
            List<Wire> wires = module.getWires();
            for (int capIdx = 0; (capsCopy != null) && (capIdx < caps.size()); capIdx++)
            {
                // Loop through all wires to see if the current capability
                // satisfies any of the wire requirements.
                for (int wireIdx = 0; (wires != null) && (wireIdx < wires.size()); wireIdx++)
                {
                    // If one of the module's capabilities satifies the requirement
                    // for an existing wire, this means the capability was
                    // substituted with another provider by the resolver and
                    // the module's capability was not used. Therefore, we should
                    // null it here so it doesn't get added the list of resolved
                    // capabilities for this module.
                    if (CapabilitySet.matches(
                        caps.get(capIdx), wires.get(wireIdx).getRequirement().getFilter()))
                    {
                        capsCopy.remove(caps.get(capIdx));
                        break;
                    }
                }
            }

            // Now loop through all capabilities and add them to the "resolved"
            // capability and package index maps, ignoring any that were nulled out.
// TODO: FELIX3 - This is actually reversed, we need to remove exports that were imported.
/*
            for (int capIdx = 0; (capsCopy != null) && (capIdx < capsCopy.size()); capIdx++)
            {
                if (capsCopy.get(capIdx).getNamespace().equals(Capability.MODULE_NAMESPACE))
                {
                    m_modCapSet.addCapability(capsCopy.get(capIdx));
                }
                else if (capsCopy.get(capIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                {
                    m_pkgCapSet.addCapability(capsCopy.get(capIdx));
                }
            }
*/
        }
    }

    public Set<Capability> getCandidates(Module module, Requirement req, boolean obeyMandatory)
    {
        Set<Capability> result = new TreeSet(new CandidateComparator());

        if (req.getNamespace().equals(Capability.MODULE_NAMESPACE))
        {
            result.addAll(m_modCapSet.match(req.getFilter(), obeyMandatory));
        }
        else if (req.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            result.addAll(m_pkgCapSet.match(req.getFilter(), obeyMandatory));
        }

        return result;
    }

    //
    // Utility methods.
    //

    /**
     * Returns true if the specified module is a singleton
     * (i.e., directive singleton:=true in Bundle-SymbolicName).
     *
     * @param module the module to check for singleton status.
     * @return true if the module is a singleton, false otherwise.
    **/
    private static boolean isSingleton(Module module)
    {
        final List<Capability> modCaps =
            Util.getCapabilityByNamespace(
                module, Capability.MODULE_NAMESPACE);
        if (modCaps == null || modCaps.size() == 0)
        {
            return false;
        }
        final List<Directive> dirs = modCaps.get(0).getDirectives();
        for (int dirIdx = 0; (dirs != null) && (dirIdx < dirs.size()); dirIdx++)
        {
            if (dirs.get(dirIdx).getName().equalsIgnoreCase(Constants.SINGLETON_DIRECTIVE)
                && Boolean.valueOf((String) dirs.get(dirIdx).getValue()).booleanValue())
            {
                return true;
            }
        }
        return false;
    }

    private static Module indexModule(Map map, Module module)
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
                middleVersion = ((Module) modules.get(middle)).getVersion();
                // Sort in reverse version order.
                int cmp = middleVersion.compareTo(version);
                if (cmp < 0)
                {
                    bottom = middle - 1;
                }
                else if (cmp == 0)
                {
                    // Sort further by ascending bundle ID.
                    long middleId = ((Module) modules.get(middle)).getBundle().getBundleId();
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

        return (Module) modules.get(0);
    }
}