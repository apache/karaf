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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
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
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class FelixResolverState implements Resolver.ResolverState
{
    private final Logger m_logger;
    // List of all modules.
    private final List<Module> m_modules;
    // Capability sets.
    private final Map<String, CapabilitySet> m_capSets;
    // Maps fragment symbolic names to list of fragment modules sorted by version.
    private final Map<String, List<Module>> m_fragmentMap = new HashMap<String, List<Module>>();
    // Maps singleton symbolic names to list of modules sorted by version.
    private final Map<String, List<Module>> m_singletons = new HashMap<String, List<Module>>();
    // Execution environment.
    private final String m_fwkExecEnvStr;
    // Parsed framework environments
    private final Set<String> m_fwkExecEnvSet;

    public FelixResolverState(Logger logger, String fwkExecEnvStr)
    {
        m_logger = logger;
        m_modules = new ArrayList<Module>();
        m_capSets = new HashMap<String, CapabilitySet>();

        m_fwkExecEnvStr = (fwkExecEnvStr != null) ? fwkExecEnvStr.trim() : null;
        m_fwkExecEnvSet = parseExecutionEnvironments(fwkExecEnvStr);

        List<String> indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(Capability.MODULE_NAMESPACE, new CapabilitySet(indices));

        indices = new ArrayList<String>();
        indices.add(Capability.PACKAGE_ATTR);
        m_capSets.put(Capability.PACKAGE_NAMESPACE, new CapabilitySet(indices));

        indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(Capability.HOST_NAMESPACE,  new CapabilitySet(indices));
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
                // Ignore
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
            for (Module mod : modules)
            {
                if (mod.isResolved())
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
                else
                {
                    addHost(module);
                }
            }
        }
    }

    private void addFragment(Module fragment)
    {
// TODO: FRAGMENT - This should check to make sure that the host allows fragments.
        indexModule(m_fragmentMap, fragment);

        // Loop through all matching hosts seeing if we should attach the
        // new fragment. We should attach the new fragment if the existing
        // unresolved host doesn't currently have a fragment of the same
        // symbolic name attached to it or if the currently attached fragment
        // is a lower version.
        Set<Capability> hostCaps = getMatchingHostCapabilities(fragment);
        for (Capability cap : hostCaps)
        {
            Module host = cap.getModule();

            // Get the fragments currently attached to the host so we
            // can remove the older version of the current fragment, if any.
            List<Module> fragments = ((ModuleImpl) host).getFragments();
            Module attachedFragment = null;
            for (int fragIdx = 0;
                (fragments != null) && (attachedFragment == null) && (fragIdx < fragments.size());
                fragIdx++)
            {
                if (fragments.get(fragIdx).getSymbolicName()
                    .equals(fragment.getSymbolicName()))
                {
                    attachedFragment = fragments.get(fragIdx);
                }
            }

            if ((attachedFragment == null)
                || (attachedFragment.getVersion().compareTo(fragment.getVersion()) <= 0))
            {
                // Create a copy of the fragment list and remove the attached
                // fragment, if necessary.
                List<Module> newFragments = (fragments == null)
                    ? new ArrayList<Module>()
                    : new ArrayList<Module>(fragments);
                if (attachedFragment != null)
                {
                    newFragments.remove(attachedFragment);
                }

                // Now add the new fragment in bundle ID order.
                int index = -1;
                for (int listIdx = 0;
                    (index < 0) && (listIdx < newFragments.size());
                    listIdx++)
                {
                    Module f = newFragments.get(listIdx);
                    if (fragment.getBundle().getBundleId()
                        < f.getBundle().getBundleId())
                    {
                        index = listIdx;
                    }
                }
                newFragments.add(
                    (index < 0) ? newFragments.size() : index, fragment);

                // Remove host's existing exported packages from index.
                List<Capability> caps = host.getCapabilities();
                removeCapabilities(caps);

                // Attach the new fragments to the host.
                fragments = (newFragments.isEmpty()) ? null : newFragments;
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
                        // Ignore
                    }
                    m_logger.log(Logger.LOG_ERROR,
                        "Serious error attaching fragments.", ex);
                }

                // Reindex the host's exported packages.
                caps = host.getCapabilities();
                addCapabilities(caps);
            }
        }
    }

    private void removeFragment(Module fragment)
    {
        // Get fragment list, which may be null for system bundle fragments.
        List<Module> fragList = m_fragmentMap.get(fragment.getSymbolicName());
        if (fragList != null)
        {
            // Remove from fragment map.
            fragList.remove(fragment);
            if (fragList.isEmpty())
            {
                m_fragmentMap.remove(fragment.getSymbolicName());
            }

            // If we have any matching hosts, then attempt to remove the
            // fragment from any merged hosts still in the installed state.
            Set<Capability> hostCaps = getMatchingHostCapabilities(fragment);
            for (Capability hostCap : hostCaps)
            {
                Module host = hostCap.getModule();

                // Check to see if the removed fragment was actually merged with
                // the host, since it might not be if it wasn't the highest version.
                // If it was, recalculate the fragments for the host.
                List<Module> fragments = ((ModuleImpl) host).getFragments();
                if ((fragments != null) && fragments.contains(fragment))
                {
                    List<Module> fragmentList = getMatchingFragments(host);

                    // Remove host's existing exported packages from index.
                    List<Capability> caps = host.getCapabilities();
                    removeCapabilities(caps);

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
                            // Ignore
                        }
                        m_logger.log(Logger.LOG_ERROR,
                            "Serious error attaching fragments.", ex);
                    }

                    // Reindex the host's exported packages.
                    caps = host.getCapabilities();
                    addCapabilities(caps);
                }
            }
        }
    }

    private void addCapabilities(List<Capability> caps)
    {
        if (caps != null)
        {
            for (Capability cap : caps)
            {
                CapabilitySet capSet = m_capSets.get(cap.getNamespace());
                if (capSet == null)
                {
                    capSet = new CapabilitySet(null);
                    m_capSets.put(cap.getNamespace(), capSet);
                }
                capSet.addCapability(cap);
            }
        }
    }

    private void removeCapabilities(List<Capability> caps)
    {
        if (caps != null)
        {
            for (Capability cap : caps)
            {
                CapabilitySet capSet = m_capSets.get(cap.getNamespace());
                if (capSet != null)
                {
                    capSet.removeCapability(cap);
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

    private Set<Capability> getMatchingHostCapabilities(Module fragment)
    {
        // Find the fragment's host requirement.
        Requirement hostReq = getFragmentHostRequirement(fragment);

        // Create a list of all matching hosts for this fragment.
        SecurityManager sm = System.getSecurityManager();
        if ((sm != null) && (fragment.getSymbolicName() != null))
        {
            if (!((BundleProtectionDomain) fragment.getSecurityContext()).impliesDirect(
                new BundlePermission(fragment.getSymbolicName(), BundlePermission.FRAGMENT)))
            {
                return new HashSet<Capability>();
            }
        }

        Set<Capability> hostCaps =
            m_capSets.get(Capability.HOST_NAMESPACE).match(hostReq.getFilter(), true);

        for (Iterator<Capability> it = hostCaps.iterator(); it.hasNext(); )
        {
            Capability hostCap = it.next();

            // Only look at unresolved hosts, since we don't support
            // dynamic attachment of fragments.
// TODO: FELIX3 - This is potentially too narrow, since it won't allow
//       attaching with updated modules.
            if (hostCap.getModule().isResolved()
                || ((BundleImpl) hostCap.getModule().getBundle()).isStale()
                || ((BundleImpl) hostCap.getModule().getBundle()).isRemovalPending())
            {
                it.remove();
            }
            else if ((sm != null) && (hostCap.getModule().getSymbolicName() != null))
            {
                if (!((BundleProtectionDomain) hostCap.getModule()
                    .getSecurityContext()).impliesDirect(
                        new BundlePermission(hostCap.getModule().getSymbolicName(),
                            BundlePermission.HOST)))
                {
                    it.remove();
                }
            }
        }

        return hostCaps;
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
            m_capSets.get(Capability.HOST_NAMESPACE).addCapability(caps.get(0));
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
                    // Ignore
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
        addCapabilities(caps);
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
            m_capSets.get(Capability.HOST_NAMESPACE).removeCapability(caps.get(0));
        }

        // Remove exports from package maps.
        caps = host.getCapabilities();
        removeCapabilities(caps);

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

    private List<Module> getMatchingFragments(Module host)
    {
        // Find the host capability for the current host.
        List<Capability> caps = Util.getCapabilityByNamespace(host, Capability.HOST_NAMESPACE);
        Capability hostCap = (caps.isEmpty()) ? null : caps.get(0);

        // If we have a host capability, then loop through all fragments trying to
        // find ones that match.
        List<Module> fragmentList = new ArrayList<Module>();
        SecurityManager sm = System.getSecurityManager();
        if ((sm != null) && (host.getSymbolicName() != null))
        {
            if (!((BundleProtectionDomain) host.getSecurityContext()).impliesDirect(
                new BundlePermission(host.getSymbolicName(), BundlePermission.HOST)))
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
// TODO: FELIX3 - This is potentially too narrow, since it won't allow
//       attaching with updated modules.
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
            
            if ((sm != null) && (fragment.getSymbolicName() != null))
            {
                if (!((BundleProtectionDomain) fragment.getSecurityContext()).impliesDirect(
                    new BundlePermission(fragment.getSymbolicName(), BundlePermission.FRAGMENT)))
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
                    Module existing = fragmentList.get(listIdx);
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
            Set<Capability> hostCaps = getMatchingHostCapabilities(rootModule);
            Module currentBestHost = null;
            for (Capability hostCap : hostCaps)
            {
                Module host = hostCap.getModule();
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
        addCapabilities(caps);
    }

    public synchronized void moduleResolved(Module module)
    {
        if (module.isResolved())
        {
            // Loop through the module's package wires and determine if any
            // of them overlap any of the packages exported by the module.
            // If so, then the framework must have chosen to have the module
            // import rather than export the package, so we need to remove the
            // corresponding package capability from the package capability set.
            List<Wire> wires = module.getWires();
            List<Capability> caps = module.getCapabilities();
            for (int wireIdx = 0; (wires != null) && (wireIdx < wires.size()); wireIdx++)
            {
                Wire wire = wires.get(wireIdx);
                if (wire.getCapability().getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                {
                    for (int capIdx = 0;
                        (caps != null) && (capIdx < caps.size());
                        capIdx++)
                    {
                        if (caps.get(capIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                            && wire.getCapability().getAttribute(Capability.PACKAGE_ATTR).getValue()
                                .equals(caps.get(capIdx).getAttribute(Capability.PACKAGE_ATTR).getValue()))
                        {
                            m_capSets.get(Capability.PACKAGE_NAMESPACE).removeCapability(caps.get(capIdx));
                            break;
                        }
                    }
                }
            }
        }
    }

    public Set<Capability> getCandidates(Module module, Requirement req, boolean obeyMandatory)
    {
        Set<Capability> result = new TreeSet<Capability>(new CandidateComparator());

        CapabilitySet capSet = m_capSets.get(req.getNamespace());
        if (capSet != null)
        {
            Set<Capability> matches = capSet.match(req.getFilter(), obeyMandatory);
            if (System.getSecurityManager() != null)
            {
                for (Capability cap : matches)
                {
                    if (req.getNamespace().equals(Capability.PACKAGE_NAMESPACE) && (
                        !((BundleProtectionDomain) cap.getModule().getSecurityContext()).impliesDirect(
                            new PackagePermission((String) cap.getAttribute(Capability.PACKAGE_ATTR).getValue(), 
                            PackagePermission.EXPORTONLY)) ||
                            !((module == null) ||
                                ((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                    new PackagePermission((String) cap.getAttribute(Capability.PACKAGE_ATTR).getValue(), 
                                    cap.getModule().getBundle(),PackagePermission.IMPORT))
                            )))
                    {
                        if (module != cap.getModule())
                        {
                            continue;
                        }
                    }
                    if (req.getNamespace().equals(Capability.MODULE_NAMESPACE) && (
                        !((BundleProtectionDomain) cap.getModule().getSecurityContext()).impliesDirect(
                            new BundlePermission(cap.getModule().getSymbolicName(), BundlePermission.PROVIDE)) ||
                            !((module == null) ||
                                ((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                    new BundlePermission(module.getSymbolicName(), BundlePermission.REQUIRE))
                            )))
                    {
                        continue;
                    }

                    result.add(cap);
                }
            }
            else 
            {
                result.addAll(matches);
            }
        }

        return result;
    }

    /**
     * Checks to see if the passed in module's required execution environment
     * is provided by the framework.
     * @param module The module whose required execution environment is to be to verified.
     * @throws ResolveException if the module's required execution environment does
     *         not match the framework's supported execution environment.
    **/
    public void checkExecutionEnvironment(Module module) throws ResolveException
    {
        String bundleExecEnvStr = (String)
            module.getHeaders().get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (bundleExecEnvStr != null)
        {
            bundleExecEnvStr = bundleExecEnvStr.trim();

            // If the bundle has specified an execution environment and the
            // framework has an execution environment specified, then we must
            // check for a match.
            if (!bundleExecEnvStr.equals("")
                && (m_fwkExecEnvStr != null)
                && (m_fwkExecEnvStr.length() > 0))
            {
                StringTokenizer tokens = new StringTokenizer(bundleExecEnvStr, ",");
                boolean found = false;
                while (tokens.hasMoreTokens() && !found)
                {
                    if (m_fwkExecEnvSet.contains(tokens.nextToken().trim()))
                    {
                        found = true;
                    }
                }
                if (!found)
                {
                    throw new ResolveException(
                        "Execution environment not supported: "
                        + bundleExecEnvStr, module, null);
                }
            }
        }
    }

    public void checkNativeLibraries(Module module) throws ResolveException
    {
        // Next, try to resolve any native code, since the module is
        // not resolvable if its native code cannot be loaded.
        List<R4Library> libs = module.getNativeLibraries();
        if (libs != null)
        {
            String msg = null;
            // Verify that all native libraries exist in advance; this will
            // throw an exception if the native library does not exist.
            for (int libIdx = 0; (msg == null) && (libIdx < libs.size()); libIdx++)
            {
                String entryName = libs.get(libIdx).getEntryName();
                if (entryName != null)
                {
                    if (!module.getContent().hasEntry(entryName))
                    {
                        msg = "Native library does not exist: " + entryName;
                    }
                }
            }
            // If we have a zero-length native library array, then
            // this means no native library class could be selected
            // so we should fail to resolve.
            if (libs.isEmpty())
            {
                msg = "No matching native libraries found.";
            }
            if (msg != null)
            {
                throw new ResolveException(msg, module, null);
            }
        }
    }

    //
    // Utility methods.
    //

    /**
     * Updates the framework wide execution environment string and a cached Set of
     * execution environment tokens from the comma delimited list specified by the
     * system variable 'org.osgi.framework.executionenvironment'.
     * @param fwkExecEnvStr Comma delimited string of provided execution environments
     * @return the parsed set of execution environments
    **/
    private static Set<String> parseExecutionEnvironments(String fwkExecEnvStr)
    {
        Set<String> newSet = new HashSet<String>();
        if (fwkExecEnvStr != null)
        {
            StringTokenizer tokens = new StringTokenizer(fwkExecEnvStr, ",");
            while (tokens.hasMoreTokens())
            {
                newSet.add(tokens.nextToken().trim());
            }
        }
        return newSet;
    }

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
        if (modCaps == null || modCaps.isEmpty())
        {
            return false;
        }
        final List<Directive> dirs = modCaps.get(0).getDirectives();
        for (int dirIdx = 0; (dirs != null) && (dirIdx < dirs.size()); dirIdx++)
        {
            if (dirs.get(dirIdx).getName().equalsIgnoreCase(Constants.SINGLETON_DIRECTIVE)
                && Boolean.valueOf((String) dirs.get(dirIdx).getValue()))
            {
                return true;
            }
        }
        return false;
    }

    private static Module indexModule(Map<String, List<Module>> map, Module module)
    {
        List<Module> modules = map.get(module.getSymbolicName());

        // We want to add the fragment into the list of matching
        // fragments in sorted order (descending version and
        // ascending bundle identifier). Insert using a simple
        // binary search algorithm.
        if (modules == null)
        {
            modules = new ArrayList<Module>();
            modules.add(module);
        }
        else
        {
            Version version = module.getVersion();
            int top = 0, bottom = modules.size() - 1;
            while (top <= bottom)
            {
                int middle = (bottom - top) / 2 + top;
                Version middleVersion = modules.get(middle).getVersion();
                // Sort in reverse version order.
                int cmp = middleVersion.compareTo(version);
                if (cmp < 0)
                {
                    bottom = middle - 1;
                }
                else if (cmp == 0)
                {
                    // Sort further by ascending bundle ID.
                    long middleId = modules.get(middle).getBundle().getBundleId();
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

        return modules.get(0);
    }
}