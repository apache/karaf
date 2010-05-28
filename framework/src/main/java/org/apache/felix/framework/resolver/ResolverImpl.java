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
package org.apache.felix.framework.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.felix.framework.FelixResolverState;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.RequirementImpl;
import org.osgi.framework.Constants;

public class ResolverImpl implements Resolver
{
    private final Logger m_logger;

    // Reusable empty array.
    private static final List<Wire> m_emptyWires = Util.m_emptyList;

    // Holds candidate permutations based on permutating "uses" chains.
    // These permutations are given higher priority.
    private final List<Map<Requirement, Set<Capability>>> m_usesPermutations =
        new ArrayList<Map<Requirement, Set<Capability>>>();
    // Holds candidate permutations based on permutating requirement candidates.
    // These permutations represent backtracking on previous decisions.
    private final List<Map<Requirement, Set<Capability>>> m_importPermutations =
        new ArrayList<Map<Requirement, Set<Capability>>>();

    public ResolverImpl(Logger logger)
    {
        m_logger = logger;

        String v = System.getProperty("invoke.count");
    }

    public Map<Module, List<Wire>> resolve(ResolverState state, Module module)
    {
        Map<Module, List<Wire>> wireMap = new HashMap<Module, List<Wire>>();

        Map<Module, Packages> modulePkgMap = new HashMap<Module, Packages>();

        if (!module.isResolved())
        {
            try
            {
                Map<Requirement, Set<Capability>> candidateMap =
                    new HashMap<Requirement, Set<Capability>>();

                populateCandidates(
                    state, module, candidateMap, new HashMap<Module, Object>());
                m_usesPermutations.add(candidateMap);

                ResolveException rethrow = null;

                Map<Capability, Set<Requirement>> capDepSet = new HashMap();

                do
                {
                    rethrow = null;

                    modulePkgMap.clear();
                    capDepSet.clear();
                    m_packageSourcesCache.clear();

                    candidateMap = (m_usesPermutations.size() > 0)
                        ? m_usesPermutations.remove(0)
                        : m_importPermutations.remove(0);
//dumpCandidateMap(candidateMap);

                    calculatePackageSpaces(
                        module, candidateMap, modulePkgMap,
                        capDepSet, new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpModulePkgMap(modulePkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                    try
                    {
                        checkPackageSpaceConsistency(
                            module, candidateMap, modulePkgMap, capDepSet, new HashMap());
                    }
                    catch (ResolveException ex)
                    {
                        rethrow = ex;
                        System.out.println("RE: " + ex);
                    }
                }
                while ((rethrow != null)
                    && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                if (rethrow != null)
                {
                    throw rethrow;
                }

                wireMap =
                    populateWireMap(module, modulePkgMap, wireMap,
                    candidateMap);
            }
            finally
            {
                // Always clear the state.
                m_usesPermutations.clear();
                m_importPermutations.clear();
            }
        }

        return wireMap;
    }

    public Map<Module, List<Wire>> resolve(ResolverState state, Module module, String pkgName)
    {
        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The specified module is resolved.
        // 2. The package in question is not already imported.
        // 3. The package in question is not accessible via require-bundle.
        // 4. The package in question is not exported by the bundle.
        // 5. The package in question matches a dynamic import of the bundle.
        // The following call checks all of these conditions and returns
        // the associated dynamic import and matching capabilities.
        Map<Requirement, Set<Capability>> candidateMap =
            getDynamicImportCandidates(state, module, pkgName);
        if (candidateMap != null)
        {
            try
            {
                Map<Module, List<Wire>> wireMap = new HashMap();
                Map<Module, Packages> modulePkgMap = new HashMap();

                populateDynamicCandidates(state, module, candidateMap);
                m_usesPermutations.add(candidateMap);

                ResolveException rethrow = null;

                Map<Capability, Set<Requirement>> capDepSet = new HashMap();

                do
                {
                    rethrow = null;

                    modulePkgMap.clear();
                    capDepSet.clear();

                    candidateMap = (m_usesPermutations.size() > 0)
                        ? m_usesPermutations.remove(0)
                        : m_importPermutations.remove(0);

                    calculatePackageSpaces(
                        module, candidateMap, modulePkgMap,
                        capDepSet, new HashMap(), new HashSet());

                    try
                    {
                        checkPackageSpaceConsistency(
                            module, candidateMap, modulePkgMap,
                            capDepSet, new HashMap());
                    }
                    catch (ResolveException ex)
                    {
                        rethrow = ex;
                        System.out.println("RE: " + ex);
                    }
                }
                while ((rethrow != null)
                    && ((m_usesPermutations.size() > 0)
                        || (m_importPermutations.size() > 0)));

                if (rethrow != null)
                {
                    throw rethrow;
                }

//dumpModulePkgMap(modulePkgMap);
                wireMap = populateDynamicWireMap(
                    module, pkgName, modulePkgMap, wireMap, candidateMap);

                return wireMap;
            }
            finally
            {
                // Always clear the state.
                m_usesPermutations.clear();
                m_importPermutations.clear();
            }
        }

        return null;
    }

    private static Map<Requirement, Set<Capability>> getDynamicImportCandidates(
        ResolverState state, Module module, String pkgName)
    {
        // Unresolved modules cannot dynamically import, nor can the default
        // package be dynamically imported.
        if (!module.isResolved() || pkgName.length() == 0)
        {
            return null;
        }

        // If the module doesn't have dynamic imports, then just return
        // immediately.
        List<Requirement> dynamics = module.getDynamicRequirements();
        if ((dynamics == null) || (dynamics.size() == 0))
        {
            return null;
        }

        // If any of the module exports this package, then we cannot
        // attempt to dynamically import it.
        List<Capability> caps = module.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                && caps.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
            {
                return null;
            }
        }
        // If any of our wires have this package, then we cannot
        // attempt to dynamically import it.
        List<Wire> wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).hasPackage(pkgName))
            {
                return null;
            }
        }

        // Loop through the importer's dynamic requirements to determine if
        // there is a matching one for the package from which we want to
        // load a class.
        List<Directive> dirs = Collections.EMPTY_LIST;
        List<Attribute> attrs = new ArrayList(1);
        attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
        Requirement req = new RequirementImpl(
            module, Capability.PACKAGE_NAMESPACE, dirs, attrs);
        Set<Capability> candidates = state.getCandidates(module, req, false);

        // First find a dynamic requirement that matches the capabilities.
        Requirement dynReq = null;
        for (int dynIdx = 0;
            (candidates.size() > 0) && (dynReq == null) && (dynIdx < dynamics.size());
            dynIdx++)
        {
            for (Iterator<Capability> itCand = candidates.iterator();
                (dynReq == null) && itCand.hasNext(); )
            {
                Capability cap = itCand.next();
                if (CapabilitySet.matches(cap, dynamics.get(dynIdx).getFilter()))
                {
                    dynReq = dynamics.get(dynIdx);
                }
            }
        }

        // If we found a matching dynamic requirement, then filter out
        // any candidates that do not match it.
        if (dynReq != null)
        {
            for (Iterator<Capability> itCand = candidates.iterator(); itCand.hasNext(); )
            {
                Capability cap = itCand.next();
                if (!CapabilitySet.matches(cap, dynReq.getFilter()))
                {
                    itCand.remove();
                }
            }
        }
        else
        {
            candidates.clear();
        }

        if (candidates.size() > 0)
        {
            Map<Requirement, Set<Capability>> candidateMap = new HashMap();
            candidateMap.put(dynReq, candidates);
            return candidateMap;
        }

        return null;
    }

// TODO: FELIX3 - Modify to not be recursive.
    private void populateCandidates(
        ResolverState state, Module module,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<Module, Object> resultCache)
    {
        // Determine if we've already calculated this module's candidates.
        // The result cache will have one of three values:
        //   1. A resolve exception if we've already attempted to populate the
        //      module's candidates but were unsuccessful.
        //   2. Boolean.TRUE indicating we've already attempted to populate the
        //      module's candidates and were successful.
        //   3. An array containing the cycle count, current map of candidates
        //      for already processed requirements, and a list of remaining
        //      requirements whose candidates still need to be calculated.
        // For case 1, rethrow the exception. For case 2, simply return immediately.
        // For case 3, this means we have a cycle so we should continue to populate
        // the candidates where we left off and not record any results globally
        // until we've popped completely out of the cycle.

        // Keeps track of the number of times we've reentered this method
        // for the current module.
        Integer cycleCount = null;

        // Keeps track of the candidates we've already calculated for the
        // current module's requirements.
        Map<Requirement, Set<Capability>> localCandidateMap = null;

        // Keeps track of the current module's requirements for which we
        // haven't yet found candidates.
        List<Requirement> remainingReqs = null;

        // Get the cache value for the current module.
        Object cacheValue = resultCache.get(module);

        // This is case 1.
        if (cacheValue instanceof ResolveException)
        {
            throw (ResolveException) cacheValue;
        }
        // This is case 2.
        else if (cacheValue instanceof Boolean)
        {
            return;
        }
        // This is case 3.
        else if (cacheValue != null)
        {
            // Increment and get the cycle count.
            cycleCount = (Integer)
                (((Object[]) cacheValue)[0]
                    = new Integer(((Integer) ((Object[]) cacheValue)[0]).intValue() + 1));
            // Get the already populated candidates.
            localCandidateMap = (Map) ((Object[]) cacheValue)[1];
            // Get the remaining requirements.
            remainingReqs = (List) ((Object[]) cacheValue)[2];
        }

        // If there is no cache value for the current module, then this is
        // the first time we are attempting to populate its candidates, so
        // do some one-time checks and initialization.
        if ((remainingReqs == null) && (localCandidateMap == null))
        {
            // Verify that any required execution environment is satisfied.
            state.checkExecutionEnvironment(module);

            // Verify that any native libraries match the current platform.
            state.checkNativeLibraries(module);

            // Record cycle count.
            cycleCount = new Integer(0);

            // Create a local map for populating candidates first, just in case
            // the module is not resolvable.
            localCandidateMap = new HashMap();

            // Create a modifiable list of the module's requirements.
            remainingReqs = new ArrayList(module.getRequirements());

            // Add these value to the result cache so we know we are
            // in the middle of populating candidates for the current
            // module.
            resultCache.put(module,
                cacheValue = new Object[] { cycleCount, localCandidateMap, remainingReqs });
        }

        // If we have requirements remaining, then find candidates for them.
        while (remainingReqs.size() > 0)
        {
            Requirement req = remainingReqs.remove(0);

            // Get satisfying candidates and populate their candidates if necessary.
            Set<Capability> candidates = state.getCandidates(module, req, true);
            for (Iterator<Capability> itCandCap = candidates.iterator(); itCandCap.hasNext(); )
            {
                Capability candCap = itCandCap.next();
                if (!candCap.getModule().isResolved())
                {
                    try
                    {
                        populateCandidates(state, candCap.getModule(),
                            candidateMap, resultCache);
                    }
                    catch (ResolveException ex)
                    {
                        // Remove the candidate since we weren't able to
                        // populate its candidates.
                        itCandCap.remove();
                    }
                }
            }

            // If there are no candidates for the current requirement
            // and it is not optional, then create, cache, and throw
            // a resolve exception.
            if ((candidates.size() == 0) && !req.isOptional())
            {
                ResolveException ex =
                    new ResolveException("Unable to resolve " + module
                        + ": missing requirement " + req, module, req);
                resultCache.put(module, ex);
                m_logger.log(Logger.LOG_DEBUG, "No viable candidates", ex);
                throw ex;
            }
            // If we actually have candidates for the requirement, then
            // add them to the local candidate map.
            else if (candidates.size() > 0)
            {
                localCandidateMap.put(req, candidates);
            }
        }

        // If we are exiting from a cycle then decrement
        // cycle counter, otherwise record the result.
        if (cycleCount.intValue() > 0)
        {
            ((Object[]) cacheValue)[0] = new Integer(cycleCount.intValue() - 1);
        }
        else if (cycleCount.intValue() == 0)
        {
            // Record that the module was successfully populated.
            resultCache.put(module, Boolean.TRUE);

            // Merge local candidate map into global candidate map.
            if (localCandidateMap.size() > 0)
            {
                candidateMap.putAll(localCandidateMap);
            }
        }
    }

    private void populateDynamicCandidates(
        ResolverState state, Module module,
        Map<Requirement, Set<Capability>> candidateMap)
    {
        // There should be one entry in the candidate map, which are the
        // the candidates for the matching dynamic requirement. Get the
        // matching candidates and populate their candidates if necessary.
        Entry<Requirement, Set<Capability>> entry = candidateMap.entrySet().iterator().next();
        Requirement dynReq = entry.getKey();
        Set<Capability> candidates = entry.getValue();
        for (Iterator<Capability> itCandCap = candidates.iterator(); itCandCap.hasNext(); )
        {
            Capability candCap = itCandCap.next();
            if (!candCap.getModule().isResolved())
            {
                try
                {
                    populateCandidates(state, candCap.getModule(),
                        candidateMap, new HashMap<Module, Object>());
                }
                catch (ResolveException ex)
                {
                    itCandCap.remove();
                }
            }
        }

        if (candidates.size() == 0)
        {
            candidateMap.remove(dynReq);
            throw new ResolveException("Dynamic import failed.", module, dynReq);
        }
    }

    private void calculatePackageSpaces(
        Module module,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<Module, Packages> modulePkgMap,
        Map<Capability, Set<Requirement>> capDepSet,
        Map<Capability, List<Module>> usesCycleMap,
        Set<Module> cycle)
    {
        if (cycle.contains(module))
        {
            return;
        }
        cycle.add(module);

        // Create a map of wires or proposed wired depending on whether
        // the module is resolved or not.
        List<Requirement> reqs = new ArrayList();
        List<Capability> caps = new ArrayList();
        if (module.isResolved())
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (Wire wire : module.getWires())
            {
                reqs.add(wire.getRequirement());
                caps.add(wire.getCapability());
            }

            // Since the module is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            for (Requirement req : module.getDynamicRequirements())
            {
                // Get the candidates for the current requirement.
                Set<Capability> candCaps = candidateMap.get(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                Capability cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
                // Can only dynamically import one at a time, so break
                // out of the loop after the first.
                break;
            }
        }
        else
        {
            for (Requirement req : module.getRequirements())
            {
                // Get the candidates for the current requirement.
                Set<Capability> candCaps = candidateMap.get(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                Capability cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
            }
        }

        // First, add all exported packages to our package space.
        calculateExportedPackages(module, modulePkgMap);
        Packages modulePkgs = modulePkgMap.get(module);

        // Second, add all imported packages to our candidate space.
        for (int i = 0; i < reqs.size(); i++)
        {
            Requirement req = reqs.get(i);
            Capability cap = caps.get(i);
            calculateExportedPackages(cap.getModule(), modulePkgMap);
            mergeCandidatePackages(module, req, cap, modulePkgMap, candidateMap);
            addCapabilityDependency(cap, req, capDepSet);
        }

        // Third, ask our candidates to calculate their package spaces.
        for (int i = 0; i < caps.size(); i++)
        {
            calculatePackageSpaces(
                caps.get(i).getModule(), candidateMap, modulePkgMap,
                capDepSet, usesCycleMap, cycle);
        }

        // Fourth, add all of the uses constraints implied by our imported
        // and required packages.
        for (Entry<String, List<Blame>> entry : modulePkgs.m_importedPkgs.entrySet())
        {
            for (Blame blame : entry.getValue())
            {
                // Ignore modules that import from themselves.
                if (!blame.m_cap.getModule().equals(module))
                {
                    List<Requirement> blameReqs = new ArrayList();
                    blameReqs.add(blame.m_reqs.get(0));

                    mergeUses(
                        module,
                        modulePkgs,
                        blame.m_cap,
                        blameReqs,
                        modulePkgMap,
                        candidateMap,
                        usesCycleMap);
                }
            }
        }
        for (Entry<String, List<Blame>> entry : modulePkgs.m_requiredPkgs.entrySet())
        {
            for (Blame blame : entry.getValue())
            {
                List<Requirement> blameReqs = new ArrayList();
                blameReqs.add(blame.m_reqs.get(0));

                mergeUses(
                    module,
                    modulePkgs,
                    blame.m_cap,
                    blameReqs,
                    modulePkgMap,
                    candidateMap,
                    usesCycleMap);
            }
        }
    }

    private void mergeCandidatePackages(
        Module current, Requirement currentReq, Capability candCap,
        Map<Module, Packages> modulePkgMap, Map<Requirement, Set<Capability>> candidateMap)
    {
        if (candCap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, currentReq, candCap, modulePkgMap);
        }
        else if (candCap.getNamespace().equals(Capability.MODULE_NAMESPACE))
        {
// TODO: FELIX3 - THIS NEXT LINE IS A HACK. IMPROVE HOW/WHEN WE CALCULATE EXPORTS.
            calculateExportedPackages(candCap.getModule(), modulePkgMap);

            // Get the candidate's package space to determine which packages
            // will be visible to the current module.
            Packages candPkgs = modulePkgMap.get(candCap.getModule());

            // We have to merge all exported packages from the candidate,
            // since the current module requires it.
// TODO: FELIX3 - If a module imports its exports, then imported exports should
//       be reexported to requiring bundles.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    currentReq,
                    entry.getValue().m_cap,
                    modulePkgMap);
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            for (Requirement req : candCap.getModule().getRequirements())
            {
                if (req.getNamespace().equals(Capability.MODULE_NAMESPACE))
                {
                    Directive dir = req.getDirective(Constants.VISIBILITY_DIRECTIVE);
                    if ((dir != null) && dir.getValue().equals(Constants.VISIBILITY_REEXPORT)
                        && (candidateMap.get(req) != null))
                    {
                        mergeCandidatePackages(
                            current,
                            currentReq,
                            candidateMap.get(req).iterator().next(),
                            modulePkgMap,
                            candidateMap);
                    }
                }
            }
        }
    }

    private void mergeCandidatePackage(
        Module current, boolean requires,
        Requirement currentReq, Capability candCap,
        Map<Module, Packages> modulePkgMap)
    {
        if (candCap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            String pkgName = (String)
                candCap.getAttribute(Capability.PACKAGE_ATTR).getValue();

            // Since this capability represents a package, it will become
            // a hard constraint on the module's package space, so we need
            // to make sure it doesn't conflict with any other hard constraints
            // or any other uses constraints.

            List blameReqs = new ArrayList();
            blameReqs.add(currentReq);

            //
            // First, check to see if the capability conflicts with
            // any existing hard constraints.
            //

            Packages currentPkgs = modulePkgMap.get(current);

            if (requires)
            {
                List<Blame> currentRequiredBlames = currentPkgs.m_requiredPkgs.get(pkgName);
                if (currentRequiredBlames == null)
                {
                    currentRequiredBlames = new ArrayList<Blame>();
                    currentPkgs.m_requiredPkgs.put(pkgName, currentRequiredBlames);
                }
                currentRequiredBlames.add(new Blame(candCap, blameReqs));
            }
            else
            {
                List<Blame> currentImportedBlames = currentPkgs.m_importedPkgs.get(pkgName);
                if (currentImportedBlames == null)
                {
                    currentImportedBlames = new ArrayList<Blame>();
                    currentPkgs.m_importedPkgs.put(pkgName, currentImportedBlames);
                }
                currentImportedBlames.add(new Blame(candCap, blameReqs));
            }

//dumpModulePkgs(current, currentPkgs);
        }
    }

    private static void addCapabilityDependency(
        Capability cap, Requirement req, Map<Capability, Set<Requirement>> capDepSet)
    {
        Set<Requirement> reqs = capDepSet.get(cap);
        if (reqs == null)
        {
            reqs = new HashSet();
            capDepSet.put(cap, reqs);
        }
        reqs.add(req);
    }

    private void mergeUses(
        Module current, Packages currentPkgs,
        Capability mergeCap, List<Requirement> blameReqs, Map<Module, Packages> modulePkgMap,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<Capability, List<Module>> cycleMap)
    {
        if (!mergeCap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            return;
        }
        // If the candidate module is the same as the current module,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        else if (current == mergeCap.getModule())
        {
            return;
        }

        // Check for cycles.
        List<Module> list = cycleMap.get(mergeCap);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<Module>() : list;
        list.add(current);
        cycleMap.put(mergeCap, list);

        for (Capability candSourceCap : getPackageSources(mergeCap, modulePkgMap))
        {
            for (String usedPkgName : candSourceCap.getUses())
            {
                Packages candSourcePkgs = modulePkgMap.get(candSourceCap.getModule());
                Blame candExportedBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                List<Blame> candSourceBlames = null;
                if (candExportedBlame != null)
                {
                    candSourceBlames = new ArrayList(1);
                    candSourceBlames.add(candExportedBlame);
                }
                else
                {
                    candSourceBlames = candSourcePkgs.m_importedPkgs.get(usedPkgName);
                }

                if (candSourceBlames == null)
                {
                    continue;
                }

                List<Blame> usedCaps = currentPkgs.m_usedPkgs.get(usedPkgName);
                if (usedCaps == null)
                {
                    usedCaps = new ArrayList<Blame>();
                    currentPkgs.m_usedPkgs.put(usedPkgName, usedCaps);
                }
                for (Blame blame : candSourceBlames)
                {
                    if (blame.m_reqs != null)
                    {
                        List<Requirement> blameReqs2 = new ArrayList(blameReqs);
                        blameReqs2.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                        usedCaps.add(new Blame(blame.m_cap, blameReqs2));
                            mergeUses(current, currentPkgs, blame.m_cap, blameReqs2,
                            modulePkgMap, candidateMap, cycleMap);
                    }
                    else
                    {
                        usedCaps.add(new Blame(blame.m_cap, blameReqs));
                        mergeUses(current, currentPkgs, blame.m_cap, blameReqs,
                            modulePkgMap, candidateMap, cycleMap);
                    }
                }
            }
        }
    }

    private void checkPackageSpaceConsistency(
        Module module, Map<Requirement, Set<Capability>> candidateMap,
        Map<Module, Packages> modulePkgMap, Map<Capability, Set<Requirement>> capDepSet,
        Map<Module, Object> resultCache)
    {
        if (module.isResolved())
        {
            return;
        }
        else if (resultCache.containsKey(module))
        {
            return;
        }

        Packages pkgs = modulePkgMap.get(module);

        ResolveException rethrow = null;
        Map<Requirement, Set<Capability>> copyConflict = null;
        Set<Requirement> mutated = null;

        Set<Module> checkModules = new HashSet();

        // Check for conflicting imports from fragments.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                Blame sourceBlame = null;
                for (Blame blame : entry.getValue())
                {
                    if (sourceBlame == null)
                    {
                        sourceBlame = blame;
                    }
                    else if (!sourceBlame.m_cap.equals(blame.m_cap))
                    {
                        // Try to permutate the conflicting requirement.
                        Requirement req = blame.m_reqs.get(0);
                        Set<Capability> candidates = candidateMap.get(req);
                        if (candidates.size() > 1)
                        {
                            Map<Requirement, Set<Capability>> importPerm =
                                copyCandidateMap(candidateMap);
                            candidates = importPerm.get(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            m_importPermutations.add(importPerm);
                        }
                        // Try to permutate the source requirement.
                        req = sourceBlame.m_reqs.get(0);
                        candidates = candidateMap.get(req);
                        if (candidates.size() > 1)
                        {
                            Map<Requirement, Set<Capability>> importPerm =
                                copyCandidateMap(candidateMap);
                            candidates = importPerm.get(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            m_importPermutations.add(importPerm);
                        }
                        ResolveException ex = new ResolveException(
                            "Constraint violation for package '"
                            + entry.getKey() + "' when resolving module "
                            + module + " between an import "
                            + sourceBlame + " and a fragment import "
                            + blame, module, blame.m_reqs.get(0));
                        m_logger.log(Logger.LOG_DEBUG, "Conflicting fragment import", ex);
                        throw ex;
                    }
                }
            }
        }

        for (Entry<String, Blame> entry : pkgs.m_exportedPkgs.entrySet())
        {
            String pkgName = entry.getKey();
            Blame exportBlame = entry.getValue();
            if (!pkgs.m_usedPkgs.containsKey(pkgName))
            {
                continue;
            }
            for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
            {
                if (!isCompatible(exportBlame.m_cap, usedBlame.m_cap, modulePkgMap))
                {
                    // Create a candidate permutation that eliminates any candidates
                    // that conflict with existing selected candidates.
                    copyConflict = (copyConflict != null)
                        ? copyConflict
                        : copyCandidateMap(candidateMap);
                    rethrow = (rethrow != null)
                        ? rethrow
                        : new ResolveException(
                            "Constraint violation for package '"
                            + pkgName + "' when resolving module "
                            + module + " between existing export "
                            + exportBlame + " and uses constraint "
                            + usedBlame, null, null);

                    mutated = (mutated != null)
                        ? mutated
                        : new HashSet();

                    for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                    {
                        Requirement req = usedBlame.m_reqs.get(reqIdx);

                        // If we've already permutated this requirement in another
                        // uses constraint, don't permutate it again just continue
                        // with the next uses constraint.
                        if (mutated.contains(req))
                        {
                            break;
                        }

                        // See if we can permutate the candidates for blamed
                        // requirement; there may be no candidates if the module
                        // associated with the requirement is already resolved.
                        Set<Capability> candidates = copyConflict.get(req);
                        if ((candidates != null) && (candidates.size() > 1))
                        {
                            mutated.add(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            // Continue with the next uses constraint.
                            break;
                        }
                    }
                }
            }

            if (rethrow != null)
            {
                if (mutated.size() > 0)
                {
                    m_usesPermutations.add(copyConflict);
                }
                m_logger.log(Logger.LOG_DEBUG, "Conflict between an export and import", rethrow);
                throw rethrow;
            }
        }

        // Check if there are any conflicts with imported packages.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                if (!module.equals(importBlame.m_cap.getModule()))
                {
                    checkModules.add(importBlame.m_cap.getModule());
                }

                String pkgName = entry.getKey();
                if (!pkgs.m_usedPkgs.containsKey(pkgName))
                {
                    continue;
                }
                for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
                {
                    if (!isCompatible(importBlame.m_cap, usedBlame.m_cap, modulePkgMap))
                    {
                        // Create a candidate permutation that eliminates any candidates
                        // that conflict with existing selected candidates.
                        copyConflict = (copyConflict != null)
                            ? copyConflict
                            : copyCandidateMap(candidateMap);
                        rethrow = (rethrow != null)
                            ? rethrow
                            : new ResolveException(
                                "Constraint violation for package '"
                                + pkgName + "' when resolving module "
                                + module + " between existing import "
                                + importBlame + " and uses constraint "
                                + usedBlame, null, null);

                        mutated = (mutated != null)
                            ? mutated
                            : new HashSet();

                        for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                        {
                            Requirement req = usedBlame.m_reqs.get(reqIdx);

                            // If we've already permutated this requirement in another
                            // uses constraint, don't permutate it again just continue
                            // with the next uses constraint.
                            if (mutated.contains(req))
                            {
                                break;
                            }

                            // See if we can permutate the candidates for blamed
                            // requirement; there may be no candidates if the module
                            // associated with the requirement is already resolved.
                            Set<Capability> candidates = copyConflict.get(req);
                            if ((candidates != null) && (candidates.size() > 1))
                            {
                                mutated.add(req);
                                Iterator it = candidates.iterator();
                                it.next();
                                it.remove();
                                // Continue with the next uses constraint.
                                break;
                            }
                        }
                    }
                }

                if (rethrow != null)
                {
                    // If we couldn't permutate the uses constraints,
                    // then try to permutate the import.
// TODO: FELIX3 - Maybe we will push too many permutations this way, since
//       we will push one on each level as we move down.
                    Requirement req = importBlame.m_reqs.get(0);
                    if (!mutated.contains(req))
                    {
                        Set<Capability> candidates = candidateMap.get(req);
                        if (candidates.size() > 1)
                        {
                            Map<Requirement, Set<Capability>> importPerm =
                                copyCandidateMap(candidateMap);
                            candidates = importPerm.get(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            m_importPermutations.add(importPerm);
                        }
                    }

                    if (mutated.size() > 0)
                    {
                        m_usesPermutations.add(copyConflict);
                    }

                    m_logger.log(Logger.LOG_DEBUG, "Conflict between imports", rethrow);
                    throw rethrow;
                }
            }
        }

        resultCache.put(module, Boolean.TRUE);

        // Now check the consistency of all modules on which the
        // current module depends.
        for (Module m : checkModules)
        {
            checkPackageSpaceConsistency(m, candidateMap, modulePkgMap, capDepSet, resultCache);
        }
    }

    private static void calculateExportedPackages(
        Module module, Map<Module, Packages> modulePkgMap)
    {
        Packages packages = modulePkgMap.get(module);
        if (packages != null)
        {
            return;
        }
        packages = new Packages();

        List<Capability> caps = module.getCapabilities();

        if (caps.size() > 0)
        {
            for (int i = 0; i < caps.size(); i++)
            {
// TODO: FELIX3 - Assume if a module imports the same package it
//       exports that the import will overlap the export.
                if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                    && !hasOverlappingImport(module, caps.get(i)))
                {
                    packages.m_exportedPkgs.put(
                        (String) caps.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue(),
                        new Blame(caps.get(i), null));
                }
            }
        }

        modulePkgMap.put(module, packages);
    }

    private static boolean hasOverlappingImport(Module module, Capability cap)
    {
        List<Requirement> reqs = module.getRequirements();
        for (int i = 0; i < reqs.size(); i++)
        {
            if (reqs.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                && CapabilitySet.matches(cap, reqs.get(i).getFilter()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isCompatible(
        Capability currentCap, Capability candCap, Map<Module, Packages> modulePkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            if (currentCap.equals(candCap))
            {
                return true;
            }

            List<Capability> currentSources =
                getPackageSources(
                    currentCap,
                    modulePkgMap);
            List<Capability> candSources =
                getPackageSources(
                    candCap,
                    modulePkgMap);

            return currentSources.containsAll(candSources) || candSources.containsAll(currentSources);
        }
        return true;
    }

    private Map<Capability, List<Capability>> m_packageSourcesCache = new HashMap();

    private List<Capability> getPackageSources(
        Capability cap, Map<Module, Packages> modulePkgMap)
    {
        if (cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            List<Capability> sources = m_packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(
                    cap, modulePkgMap, new ArrayList(), new HashSet());
                m_packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        return Util.m_emptyList;
    }

    private static List<Capability> getPackageSourcesInternal(
        Capability cap, Map<Module, Packages> modulePkgMap, List<Capability> sources,
        Set<Capability> cycleMap)
    {
        if (cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            if (cycleMap.contains(cap))
            {
                return sources;
            }
            cycleMap.add(cap);

            // Get the package name associated with the capability.
            String pkgName = cap.getAttribute(Capability.PACKAGE_ATTR).getValue().toString();

            // Since a module can export the same package more than once, get
            // all package capabilities for the specified package name.
            List<Capability> caps = cap.getModule().getCapabilities();
            for (int capIdx = 0; capIdx < caps.size(); capIdx++)
            {
                if (caps.get(capIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                    && caps.get(capIdx).getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
                {
                    sources.add(caps.get(capIdx));
                }
            }

            // Then get any addition sources for the package from required bundles.
            Packages pkgs = modulePkgMap.get(cap.getModule());
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSourcesInternal(blame.m_cap, modulePkgMap, sources, cycleMap);
                }
            }
        }

        return sources;
    }

    private static Map<Requirement, Set<Capability>> copyCandidateMap(
        Map<Requirement, Set<Capability>> candidateMap)
    {
        Map<Requirement, Set<Capability>> copy =
            new HashMap<Requirement, Set<Capability>>();
        for (Entry<Requirement, Set<Capability>> entry : candidateMap.entrySet())
        {
            Set<Capability> candidates = new TreeSet(new CandidateComparator());
            candidates.addAll(entry.getValue());
            copy.put(entry.getKey(), candidates);
        }
        return copy;
    }

    private static Map<Module, List<Wire>> populateWireMap(
        Module module, Map<Module, Packages> modulePkgMap,
        Map<Module, List<Wire>> wireMap, Map<Requirement, Set<Capability>> candidateMap)
    {
        if (!module.isResolved() && !wireMap.containsKey(module))
        {
            wireMap.put(module, m_emptyWires);

            List<Wire> packageWires = new ArrayList<Wire>();
            List<Wire> moduleWires = new ArrayList<Wire>();

            for (Requirement req : module.getRequirements())
            {
                Set<Capability> cands = candidateMap.get(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    Capability cand = cands.iterator().next();
                    if (!cand.getModule().isResolved())
                    {
                        populateWireMap(cand.getModule(),
                            modulePkgMap, wireMap, candidateMap);
                    }
                    // Ignore modules that import themselves.
                    if (req.getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                        && !module.equals(cand.getModule()))
                    {
                        packageWires.add(
                            new WireImpl(module,
                                req,
                                cand.getModule(),
                                cand));
                    }
                    else if (req.getNamespace().equals(Capability.MODULE_NAMESPACE))
                    {
                        Packages candPkgs = modulePkgMap.get(cand.getModule());
                        moduleWires.add(
                            new WireModuleImpl(module,
                                req,
                                cand.getModule(),
                                cand,
                                candPkgs.getExportedAndReexportedPackages()));
                    }
                }
            }

            // Combine wires with module wires last.
            packageWires.addAll(moduleWires);
            wireMap.put(module, packageWires);
        }

        return wireMap;
    }

    private static Map<Module, List<Wire>> populateDynamicWireMap(
        Module module, String pkgName, Map<Module, Packages> modulePkgMap,
        Map<Module, List<Wire>> wireMap, Map<Requirement, Set<Capability>> candidateMap)
    {
        wireMap.put(module, m_emptyWires);

        List<Wire> packageWires = new ArrayList<Wire>();

        Packages pkgs = modulePkgMap.get(module);
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame blame : entry.getValue())
            {
                // Ignore modules that import themselves.
                if (!module.equals(blame.m_cap.getModule())
                    && blame.m_cap.getAttribute(
                        Capability.PACKAGE_ATTR).getValue().equals(pkgName))
                {
                    if (!blame.m_cap.getModule().isResolved())
                    {
                        populateWireMap(blame.m_cap.getModule(), modulePkgMap, wireMap,
                            candidateMap);
                    }

                    List<Attribute> attrs = new ArrayList();
                    attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
                    packageWires.add(
                        new WireImpl(
                            module,
                            // We need an unique requirement here or else subsequent
                            // dynamic imports for the same dynamic requirement will
                            // conflict with previous ones.
                            new RequirementImpl(
                                module,
                                Capability.PACKAGE_NAMESPACE,
                                new ArrayList(0),
                                attrs),
                            blame.m_cap.getModule(),
                            blame.m_cap));
                }
            }
        }

        wireMap.put(module, packageWires);

        return wireMap;
    }

    private static void dumpCandidateMap(Map<Requirement, Set<Capability>> candidateMap)
    {
        // Create set of all modules from requirements.
        Set<Module> modules = new HashSet();
        for (Entry<Requirement, Set<Capability>> entry : candidateMap.entrySet())
        {
            modules.add(entry.getKey().getModule());
        }
        // Now dump the modules.
        System.out.println("=== BEGIN CANDIDATE MAP ===");
        for (Module module : modules)
        {
            System.out.println("  " + module
                 + " (" + (module.isResolved() ? "RESOLVED)" : "UNRESOLVED)"));
            for (Requirement req : module.getRequirements())
            {
                Set<Capability> candidates = candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                        System.out.println("    " + req + ": " + candidates);
                }
            }
            for (Requirement req : module.getDynamicRequirements())
            {
                Set<Capability> candidates = candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
        }
        System.out.println("=== END CANDIDATE MAP ===");
    }

    private static void dumpModulePkgMap(Map<Module, Packages> modulePkgMap)
    {
        System.out.println("+++MODULE PKG MAP+++");
        for (Entry<Module, Packages> entry : modulePkgMap.entrySet())
        {
            dumpModulePkgs(entry.getKey(), entry.getValue());
        }
    }

    private static void dumpModulePkgs(Module module, Packages packages)
    {
        System.out.println(module + " (" + (module.isResolved() ? "RESOLVED)" : "UNRESOLVED)"));
        System.out.println("  EXPORTED");
        for (Entry<String, Blame> entry : packages.m_exportedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  IMPORTED");
        for (Entry<String, List<Blame>> entry : packages.m_importedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  REQUIRED");
        for (Entry<String, List<Blame>> entry : packages.m_requiredPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  USED");
        for (Entry<String, List<Blame>> entry : packages.m_usedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
    }

    private static class Packages
    {
        public final Map<String, Blame> m_exportedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_importedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_requiredPkgs = new HashMap();
        public final Map<String, List<Blame>> m_usedPkgs = new HashMap();

        public Packages()
        {
        }

        public Packages(Packages packages)
        {
            m_exportedPkgs.putAll(packages.m_exportedPkgs);
            m_importedPkgs.putAll(packages.m_importedPkgs);
            m_requiredPkgs.putAll(packages.m_requiredPkgs);
            m_usedPkgs.putAll(packages.m_usedPkgs);
        }

        public List<String> getExportedAndReexportedPackages()
        {
            List<String> pkgs = new ArrayList();
            for (Entry<String, Blame> entry : m_exportedPkgs.entrySet())
            {
                pkgs.add((String)
                    entry.getValue().m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue());
            }
            for (Entry<String, List<Blame>> entry : m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    Directive dir = blame.m_reqs.get(
                        blame.m_reqs.size() - 1).getDirective(Constants.VISIBILITY_DIRECTIVE);
                    if ((dir != null)
                        && dir.getValue().equals(Constants.VISIBILITY_REEXPORT))
                    {
                        pkgs.add((String)
                            blame.m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue());
                        break;
                    }
                }
            }
            return pkgs;
        }
    }

    private static class Blame
    {
        public final Capability m_cap;
        public final List<Requirement> m_reqs;

        public Blame(Capability cap, List<Requirement> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        public String toString()
        {
            return m_cap.getModule()
                + "." + m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue()
                + (((m_reqs == null) || (m_reqs.size() == 0))
                    ? " NO BLAME"
                    : " BLAMED ON " + m_reqs);
        }

        public boolean equals(Object o)
        {
            return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
                && m_cap.equals(((Blame) o).m_cap);
        }
    }
}