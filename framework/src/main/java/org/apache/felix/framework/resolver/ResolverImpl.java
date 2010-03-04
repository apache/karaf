/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.felix.framework.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.felix.framework.FelixResolverState;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.RequirementImpl;
import org.osgi.framework.Constants;

// 1. Treat hard pkg constraints separately from implied package constraints
// 2. Map pkg constraints to a set of capabilities, not a single capability.
// 3. Uses constraints cannot conflict with other uses constraints, only with hard constraints.
public class ResolverImpl implements Resolver
{
    private final Logger m_logger;

    // Execution environment.
// TODO: FELIX3 - Move EE checking to ResolverState interface.
    private final String m_fwkExecEnvStr;
    private final Set m_fwkExecEnvSet;

    private static final Map<String, Long> m_invokeCounts = new HashMap<String, Long>();
    private static boolean m_isInvokeCount = false;

    // Reusable empty array.
    private static final List<Wire> m_emptyWires = new ArrayList<Wire>(0);

    public ResolverImpl(Logger logger, String fwkExecEnvStr)
    {
//System.out.println("+++ PROTO3 RESOLVER");
        m_logger = logger;
        m_fwkExecEnvStr = (fwkExecEnvStr != null) ? fwkExecEnvStr.trim() : null;
        m_fwkExecEnvSet = parseExecutionEnvironments(fwkExecEnvStr);

        String v = System.getProperty("invoke.count");
        m_isInvokeCount = (v == null) ? false : Boolean.valueOf(v);
    }

    private final List<Map<Requirement, Set<Capability>>> m_candidatePermutations =
        new ArrayList<Map<Requirement, Set<Capability>>>();

    public Map<Module, List<Wire>> resolve(ResolverState state, Module module)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        Map<Module, List<Wire>> wireMap = new HashMap<Module, List<Wire>>();

        Map<Module, Packages> modulePkgMap = new HashMap<Module, Packages>();

        if (!module.isResolved())
        {
            m_candidatePermutations.clear();

//System.out.println("+++ RESOLVING " + module);
            Map<Requirement, Set<Capability>> candidateMap =
                new HashMap<Requirement, Set<Capability>>();

            populateCandidates(state, module, m_fwkExecEnvStr, m_fwkExecEnvSet,
                candidateMap, new HashMap<Module, Object>());
            m_candidatePermutations.add(candidateMap);

            ResolveException rethrow = null;

            do
            {
                rethrow = null;

                candidateMap = m_candidatePermutations.remove(0);
//dumpCandidateMap(state, candidateMap);

                try
                {
                    findConsistentCandidates(
                        module,
                        new ArrayList(),
                        candidateMap,
                        modulePkgMap,
                        new HashMap<Module, Object>());
                }
                catch (ResolveException ex)
                {
                    rethrow = ex;
                    System.out.println("RE: " + ex);
                }
            }
            while ((rethrow != null) && (m_candidatePermutations.size() > 0));

            if (rethrow != null)
            {
                throw rethrow;
            }
//dumpModulePkgMap(modulePkgMap);

            wireMap =
                populateWireMap(module, modulePkgMap, wireMap,
                candidateMap);
        }

        if (m_isInvokeCount)
        {
            System.out.println("INVOKE COUNTS " + m_invokeCounts);
        }

        return wireMap;
    }

    public Map<Module, List<Wire>> resolve(ResolverState state, Module module, String pkgName)
    {
        Capability candidate = null;

        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The specified module is resolved.
        // 2. The package in question is not already imported.
        // 3. The package in question is not accessible via require-bundle.
        // 4. The package in question is not exported by the bundle.
        // 5. The package in question matches a dynamic import of the bundle.
        // The following call checks all of these conditions and returns
        // a matching dynamic requirement if possible.
        Map<Requirement, Set<Capability>> candidateMap =
            new HashMap<Requirement, Set<Capability>>();
        if (isAllowedDynamicImport(state, module, pkgName, candidateMap))
        {
            m_candidatePermutations.clear();

            Map<Module, List<Wire>> wireMap = new HashMap<Module, List<Wire>>();

            Map<Module, Packages> modulePkgMap = new HashMap<Module, Packages>();

//System.out.println("+++ DYNAMICALLY RESOLVING " + module + " - " + pkgName);
            populateDynamicCandidates(state, module,
                m_fwkExecEnvStr, m_fwkExecEnvSet, candidateMap);
            m_candidatePermutations.add(candidateMap);
            ResolveException rethrow = null;

            do
            {
                rethrow = null;

                candidateMap = m_candidatePermutations.remove(0);
//dumpCandidateMap(state, candidateMap);

                try
                {
                    findConsistentDynamicCandidate(
                        module,
                        new ArrayList(),
                        candidateMap,
                        modulePkgMap);
                }
                catch (ResolveException ex)
                {
                    rethrow = ex;
                    System.out.println("RE: " + ex);
                }
            }
            while ((rethrow != null) && (m_candidatePermutations.size() > 0));

            if (rethrow != null)
            {
                throw rethrow;
            }
//dumpModulePkgMap(modulePkgMap);
            wireMap =
                populateDynamicWireMap(
                    module, pkgName, modulePkgMap, wireMap, candidateMap);

//System.out.println("+++ DYNAMIC SUCCESS: " + wireMap.get(module));
            return wireMap;
        }

//System.out.println("+++ DYNAMIC FAILURE");
        return null;
    }

    // TODO: FELIX3 - It would be nice to make this private.
    // TODO: FELIX3 - At a minimum, figure out a different way than passing in the
    //       candidate map.
    public static boolean isAllowedDynamicImport(
        ResolverState state, Module module, String pkgName, Map<Requirement,
        Set<Capability>> candidateMap)
    {
        // Unresolved modules cannot dynamically import, nor can the default
        // package be dynamically imported.
        if (!module.isResolved() || pkgName.length() == 0)
        {
            return false;
        }

        // If any of the module exports this package, then we cannot
        // attempt to dynamically import it.
        List<Capability> caps = module.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                && caps.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
            {
                return false;
            }
        }
        // If any of our wires have this package, then we cannot
        // attempt to dynamically import it.
        List<Wire> wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).hasPackage(pkgName))
            {
                return false;
            }
        }

        // Loop through the importer's dynamic requirements to determine if
        // there is a matching one for the package from which we want to
        // load a class.
        List<Directive> dirs = new ArrayList(0);
        List<Attribute> attrs = new ArrayList(1);
        attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
        Requirement req = new RequirementImpl(Capability.PACKAGE_NAMESPACE, dirs, attrs);
        Set<Capability> candidates = state.getCandidates(module, req, false);
        List<Requirement> dynamics = module.getDynamicRequirements();

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
            
            if (candidates.size() > 0)
            {
                candidateMap.put(dynReq, candidates);
            }
        }
        else
        {
            candidates.clear();
        }

        return !candidates.isEmpty();
    }

    private static void dumpCandidateMap(
        ResolverState state, Map<Requirement, Set<Capability>> candidateMap)
    {
        System.out.println("=== BEGIN CANDIDATE MAP ===");
        for (Module module : ((FelixResolverState) state).getModules())
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
        for (Entry<String, Blame> entry : packages.m_importedPkgs.entrySet())
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

    private static void populateCandidates(
        ResolverState state, Module module, String fwkExecEnvStr, Set fwkExecEnvSet,
        Map<Requirement, Set<Capability>> candidateMap, Map<Module, Object> resultCache)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        // Determine if we've already calculated this module's candidates.
        // The result cache will have one of three values:
        //   1. A resolve exception if we've already attempted to populate the
        //      module's candidates but were unsuccessful.
        //   2. Boolean.TRUE indicating we've already attempted to populate the
        //      module's candidates and were successful.
        //   3. An array containing the cycle count, current map of candidates
        //      for already processed requirements, and a list of remaining
        //      requirements whose candidates still need to be calculated.
        // For case 1, rethrow the exception. For case 3, simply return immediately.
        // For case 3, this means we have a cycle so we should continue to populate
        // the candidate where we left off and not record any results globally
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
            cycleCount = (Integer) ((Object[]) cacheValue)[0];
            ((Object[]) cacheValue)[0] = new Integer(cycleCount.intValue() + 1);
            cycleCount = (Integer) ((Object[]) cacheValue)[0];
            localCandidateMap = (Map) ((Object[]) cacheValue)[1];
            remainingReqs = (List) ((Object[]) cacheValue)[2];
        }

        // If there is no cache value for the current module, then this is
        // the first time we are attempting to populate its candidates, so
        // do some one-time checks and initialization.
        if ((remainingReqs == null) && (localCandidateMap == null))
        {
            // Verify that any required execution environment is satisfied.
            verifyExecutionEnvironment(fwkExecEnvStr, fwkExecEnvSet, module);

            // Verify that any native libraries match the current platform.
            verifyNativeLibraries(module);

            // Record cycle count.
            cycleCount = new Integer(0);

            // Store candidates in a local map first, just in case the module
            // is not resolvable.
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
                            fwkExecEnvStr, fwkExecEnvSet, candidateMap, resultCache);
                    }
                    catch (ResolveException ex)
                    {
System.out.println("RE: Candidate not resolveable: " + ex);
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

    private static void populateDynamicCandidates(
        ResolverState state, Module module,
        String fwkExecEnvStr, Set fwkExecEnvSet,
        Map<Requirement, Set<Capability>> candidateMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

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
                        fwkExecEnvStr, fwkExecEnvSet, candidateMap,
                        new HashMap<Module, Object>());
                }
                catch (ResolveException ex)
                {
System.out.println("RE: Candidate not resolveable: " + ex);
                    itCandCap.remove();
                }
            }
        }

// TODO: FELIX3 - Since we reuse the same dynamic requirement, is it possible
//       that some sort of cycle could cause us to try to match another set
//       of candidates to the same requirement?
        if (candidates.size() == 0)
        {
            candidateMap.remove(dynReq);
            throw new ResolveException("Dynamic import failed.", module, dynReq);
        }

        // Add existing wires as candidates.
        for (Wire wire : module.getWires())
        {
            Set<Capability> cs = new TreeSet();
            cs.add(wire.getCapability());
            candidateMap.put(wire.getRequirement(), cs);
        }
    }

    private void findConsistentCandidates(
        Module module, List<Requirement> incomingReqs,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<Module, Packages> modulePkgMap,
        Map<Module, Object> cycleMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        Integer cycleCount = null;

        Object o = cycleMap.get(module);

        if (o instanceof Boolean)
        {
            return;
        }
        else if (o == null)
        {
            List list;
            if (module.isResolved())
            {
                list = new ArrayList(module.getWires());
            }
            else
            {
                list = new ArrayList(module.getRequirements());
            }
            cycleMap.put(module, o = new Object[] { cycleCount = new Integer(0), list });
            calculateExportedPackages(module, incomingReqs, modulePkgMap);
        }
        else
        {
            cycleCount = (Integer) ((Object[]) o)[0];
            ((Object[]) o)[0] = new Integer(cycleCount.intValue() + 1);
            cycleCount = (Integer) ((Object[]) o)[0];
        }

//System.out.println("+++ RESOLVING " + module);

        if (module.isResolved())
        {
            List<Wire> wires = (List<Wire>) ((Object[]) o)[1];

            while (wires.size() > 0)
            {
                Wire wire = wires.remove(0);

                // Try to resolve the candidate.
                findConsistentCandidates(
                    wire.getCapability().getModule(),
                    incomingReqs,
                    candidateMap,
                    modulePkgMap,
                    cycleMap);

                // If we are here, the candidate was consistent. Try to
                // merge the candidate into the target module's packages.
                mergeCandidatePackages(
                    module,
                    incomingReqs,
                    wire.getCapability(),
                    modulePkgMap,
                    candidateMap);
            }
        }
        else
        {
            List<Requirement> reqs = (List<Requirement>) ((Object[]) o)[1];

            while (reqs.size() > 0)
            {
                Requirement req = reqs.remove(0);

                // Get the candidates for the current requirement.
                Set<Capability> candCaps = candidateMap.get(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                List<Requirement> outgoingReqs = new ArrayList<Requirement>(incomingReqs);
                outgoingReqs.add(req);

                for (Iterator<Capability> it = candCaps.iterator(); it.hasNext(); )
                {
                    Capability candCap = it.next();
//System.out.println("+++ TRYING CAND " + candCap + " FOR " + req);
                    try
                    {
                        // Try to resolve the candidate.
                        findConsistentCandidates(
                            candCap.getModule(),
                            outgoingReqs,
                            candidateMap,
                            modulePkgMap,
                            cycleMap);

                        // If we are here, the candidate was consistent. Try to
                        // merge the candidate into the target module's packages.
                        mergeCandidatePackages(
                            module,
                            outgoingReqs,
                            candCap,
                            modulePkgMap,
                            candidateMap);

                        // If we are here, we merged the candidate successfully,
                        // so we can continue with the next requirement
                        break;
                    }
                    catch (ResolveException ex)
                    {
System.out.println("RE: " + ex);
ex.printStackTrace();

// TODO: FELIX3 RESOLVER - Is it ok to remove the failed candidate? By removing
//       it we keep the candidateMap up to date with the selected candidate, but
//       theoretically this eliminates some potential combinations. Are those
//       combinations guaranteed to be failures so eliminating them is ok?
                        it.remove();
                        if (!it.hasNext() && !req.isOptional())
                        {
                            throw new ResolveException("Unresolved constraint "
                                + req + " in " + module, module, req);
                        }
                    }
                }
            }
        }

        // If we are exiting from a cycle then decrement
        // cycle counter, otherwise record the result.
        if (cycleCount.intValue() > 0)
        {
            ((Object[]) o)[0] = new Integer(cycleCount.intValue() - 1);
        }
        else if (cycleCount.intValue() == 0)
        {
            // Record that the module was successfully populated.
            cycleMap.put(module, Boolean.TRUE);
        }
    }

    private void findConsistentDynamicCandidate(
        Module module, List<Requirement> incomingReqs,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<Module, Packages> modulePkgMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

//System.out.println("+++ RESOLVING " + module);
        calculateExportedPackages(module, incomingReqs, modulePkgMap);

        List<Requirement> reqs = new ArrayList(module.getRequirements());
        reqs.addAll(module.getDynamicRequirements());
        for (Requirement req : reqs)
        {
            // Get the candidates for the current requirement.
            Set<Capability> candCaps = candidateMap.get(req);
            // Optional requirements may not have any candidates.
            if (candCaps == null)
            {
                continue;
            }

            List<Requirement> outgoingReqs = new ArrayList<Requirement>(incomingReqs);
            outgoingReqs.add(req);

            for (Iterator<Capability> it = candCaps.iterator(); it.hasNext(); )
            {
                Capability candCap = it.next();
//System.out.println("+++ TRYING CAND " + candCap + " FOR " + req);
                try
                {
                    // Try to resolve the candidate.
                    findConsistentCandidates(
                        candCap.getModule(),
                        outgoingReqs,
                        candidateMap,
                        modulePkgMap,
                        new HashMap());

                    // If we are here, the candidate was consistent. Try to
                    // merge the candidate into the target module's packages.
                    mergeCandidatePackages(
                        module,
                        outgoingReqs,
                        candCap,
                        modulePkgMap,
                        candidateMap);

                    // If we are here, we merged the candidate successfully,
                    // so we can continue with the next requirement
                    break;
                }
                catch (ResolveException ex)
                {
System.out.println("RE: " + ex);
ex.printStackTrace();
// TODO: FELIX3 RESOLVER - Is it ok to remove the failed candidate? By removing
//       it we keep the candidateMap up to date with the selected candidate, but
//       theoretically this eliminates some potential combinations. Are those
//       combinations guaranteed to be failures so eliminating them is ok?
                    it.remove();
                    if (!it.hasNext() && !req.isOptional())
                    {
                        throw new ResolveException("Unresolved constraint "
                            + req + " in " + module, module, req);
                    }
                }
            }
        }
    }

    private static void calculateExportedPackages(
        Module module, List<Requirement> incomingReqs, Map<Module, Packages> modulePkgMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        Packages packages = new Packages();

        List<Capability> caps = module.getCapabilities();

        if (caps.size() > 0)
        {
            for (int i = 0; i < caps.size(); i++)
            {
// TODO: PROTO3 RESOLVER - Assume if a module imports the same package it
//       exports that the import will overlap the export.
                if (caps.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                    && !hasOverlappingImport(module, caps.get(i)))
                {
                    packages.m_exportedPkgs.put(
                        (String) caps.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue(),
                        new Blame(incomingReqs, caps.get(i)));
                }
            }
        }

        modulePkgMap.put(module, packages);
    }

    private static boolean hasOverlappingImport(Module module, Capability cap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

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

    private void mergeCandidatePackages(
        Module current, List<Requirement> outgoingReqs,
        Capability candCap, Map<Module, Packages> modulePkgMap,
        Map<Requirement, Set<Capability>> candidateMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        if (candCap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, new Blame(outgoingReqs, candCap), modulePkgMap, candidateMap);
        }
        else if (candCap.getNamespace().equals(Capability.MODULE_NAMESPACE))
        {
            // Get the candidate's package space to determine which packages
            // will be visible to the current module.
            Packages candPkgs = modulePkgMap.get(candCap.getModule());

// TODO: PROTO3 RESOLVER - For now assume only exports, but eventually we also
//       have to support re-exported packages.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    new Blame(outgoingReqs, entry.getValue().m_cap),
                    modulePkgMap,
                    candidateMap);
            }
            for (Entry<String, List<Blame>> entry : candPkgs.m_requiredPkgs.entrySet())
            {
                List<Blame> blames = entry.getValue();
                for (Blame blame : blames)
                {
// TODO: FELIX3 RESOLVER - Since a single module requirement can include many packages,
//       it is likely we call merge too many times for the same module req. If we knew
//       which candidates were being used to resolve this candidate's module dependencies,
//       then we could just try to merge them directly. This info would also help in
//       in creating wires, since we ultimately want to create wires for the selected
//       candidates, which we are trying to deduce from the package space, but if we
//       knew the selected candidates, we'd be done.
                    if (blame.m_cap.getModule().equals(current))
                    {
                        continue;
                    }

                    Directive dir = blame.m_reqs.get(blame.m_reqs.size() - 1)
                        .getDirective(Constants.VISIBILITY_DIRECTIVE);
                    if ((dir != null) && dir.getValue().equals(Constants.VISIBILITY_REEXPORT))
                    {
                        mergeCandidatePackage(
                            current,
                            true,
                            new Blame(outgoingReqs, blame.m_cap),
                            modulePkgMap,
                            candidateMap);
                    }
                }
            }
        }
    }

    private void mergeCandidatePackage(
        Module current, boolean requires,
        Blame candBlame, Map<Module, Packages> modulePkgMap,
        Map<Requirement, Set<Capability>> candidateMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

// TODO: PROTO3 RESOLVER - Check for merging where module imports from itself,
//       then it should be listed as an export for requiring bundles.
        if (candBlame.m_cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
//System.out.println("+++ MERGING " + candBlame.m_cap + " INTO " + current);
            String pkgName = (String)
                candBlame.m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue();

            // Since this capability represents a package, it will become
            // a hard constraint on the module's package space, so we need
            // to make sure it doesn't conflict with any other hard constraints
            // or any other uses constraints.

            //
            // First, check to see if the capability conflicts with
            // any existing hard constraints.
            //

            Packages currentPkgs = modulePkgMap.get(current);
            Blame currentExportedBlame = currentPkgs.m_exportedPkgs.get(pkgName);
            Blame currentImportedBlame = currentPkgs.m_importedPkgs.get(pkgName);
            List<Blame> currentRequiredBlames = currentPkgs.m_requiredPkgs.get(pkgName);

            // We don't need to worry about an import conflicting with a required
            // bundle's export, since imported package wires are terminal the
            // bundle will never see the exported package from the required bundle.
// TODO: FELIX3 - See scenario 21, this seems odd.
            if (!requires &&
                (currentImportedBlame != null) && !currentImportedBlame.m_cap.equals(candBlame.m_cap))
//            if (!requires &&
//                (((currentExportedBlame != null) && !currentExportedBlame.m_cap.equals(candBlame.m_cap))
//                || ((currentImportedBlame != null) && !currentImportedBlame.m_cap.equals(candBlame.m_cap))))
//                || ((currentRequiredBlames != null) && !currentRequiredBlames.contains(candBlame))))
            {
                // Permutate the candidate map and throw a resolve exception.
                // NOTE: This method ALWAYS throws an exception.
                permutateCandidates(
                    current,
                    pkgName,
                    currentImportedBlame,
                    candBlame,
                    candidateMap);
            }

            //
            // Second, check to see if the capability conflicts with
            // any existing uses constraints
            //

            Packages currentPkgsCopy = currentPkgs;

            if (!current.isResolved())
            {
                List<Blame> currentUsedBlames = currentPkgs.m_usedPkgs.get(pkgName);
                checkExistingUsesConstraints(
                    current, pkgName, currentUsedBlames, candBlame, modulePkgMap, candidateMap);

                //
                // Last, check to see if any uses constraints implied by the
                // candidate conflict with any of the existing hard constraints.
                //

                // For now, create a copy of the module's package space and
                // add the current candidate to the imported packages.
                currentPkgsCopy = new Packages(currentPkgs);
            }

            if (requires)
            {
                if (currentRequiredBlames == null)
                {
                    currentRequiredBlames = new ArrayList<Blame>();
                    currentPkgsCopy.m_requiredPkgs.put(pkgName, currentRequiredBlames);
                }
// TODO: PROTO3 RESOLVER - This is potentially modifying the original, we need to modify a copy.
                currentRequiredBlames.add(candBlame);
            }
            else
            {
                currentPkgsCopy.m_importedPkgs.put(pkgName, candBlame);
            }

            // Verify and merge the candidate's transitive uses constraints.
            verifyAndMergeUses(
                current,
                currentPkgsCopy,
                candBlame,
                modulePkgMap,
                candidateMap,
                new HashMap<String, List<Module>>());

            // If we are here, then there were no conflict, so we should update
            // the module's package space.
            if (!current.isResolved())
            {
                currentPkgs.m_exportedPkgs.putAll(currentPkgsCopy.m_exportedPkgs);
                currentPkgs.m_importedPkgs.putAll(currentPkgsCopy.m_importedPkgs);
                currentPkgs.m_requiredPkgs.putAll(currentPkgsCopy.m_requiredPkgs);
                currentPkgs.m_usedPkgs.putAll(currentPkgsCopy.m_usedPkgs);
            }
//dumpModulePkgs(current, currentPkgs);
        }
    }

    private void checkExistingUsesConstraints(
        Module current, String pkgName, List<Blame> currentUsedBlames,
        Blame candBlame, Map<Module, Packages> modulePkgMap,
        Map<Requirement, Set<Capability>> candidateMap)
    {
        for (int i = 0; (currentUsedBlames != null) && (i < currentUsedBlames.size()); i++)
        {
//System.out.println("+++ CHECK " + candBlame + " IN EXISTING " + currentUsedBlames.get(i));
            if (!isCompatible(currentUsedBlames.get(i).m_cap, candBlame.m_cap, modulePkgMap))
            {
                // Permutate the candidate map and throw a resolve exception.
                // NOTE: This method ALWAYS throws an exception.
                permutateCandidates(
                    current,
                    pkgName,
                    currentUsedBlames.get(i),
                    candBlame,
                    candidateMap);
            }
        }
    }

// TODO: PROTO3 RESOLVER - We end up with duplicates in uses constraints,
//       see scenario 2 for an example.
    private void verifyAndMergeUses(
        Module current, Packages currentPkgs,
        Blame candBlame, Map<Module, Packages> modulePkgMap,
        Map<Requirement, Set<Capability>> candidateMap,
        Map<String, List<Module>> cycleMap)
    {
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        // Check for cycles.
        String pkgName = (String)
            candBlame.m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue();
        List<Module> list = cycleMap.get(pkgName);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<Module>() : list;
        list.add(current);
        cycleMap.put(pkgName, list);

//System.out.println("+++ VERIFYING USES " + current + " FOR " + candBlame);
        for (Capability candSourceCap : getPackageSources(
            candBlame.m_cap, modulePkgMap, new ArrayList<Capability>(), new HashSet<Capability>()))
        {
            for (String usedPkgName : candSourceCap.getUses())
            {
                Blame currentExportedBlame = currentPkgs.m_exportedPkgs.get(usedPkgName);
                Blame currentImportedBlame = currentPkgs.m_importedPkgs.get(usedPkgName);
// TODO: PROTO3 RESOLVER - What do we do with required packages?
                List<Blame> currentRequiredBlames = currentPkgs.m_requiredPkgs.get(usedPkgName);

                Packages candSourcePkgs = modulePkgMap.get(candSourceCap.getModule());
//System.out.println("+++ candSourceCap " + candSourceCap);
//System.out.println("+++ candSourceCap.getModule() " + candSourceCap.getModule() + " (" + candSourceCap.getModule().isResolved() + ")");
//System.out.println("+++ candSourcePkgs " + candSourcePkgs);
//System.out.println("+++ candSourcePkgs.m_exportedPkgs " + candSourcePkgs.m_exportedPkgs);
                Blame candSourceBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                candSourceBlame = (candSourceBlame != null)
                    ? candSourceBlame
                    : candSourcePkgs.m_importedPkgs.get(usedPkgName);
//                sourceCap = (sourceCap != null)
//                    ? sourceCap
//                    : sourcePkgs.m_requiredPkgs.get(usedPkgName);

                // If the candidate doesn't actually have a constraint for
                // the used package, then just ignore it since this is likely
                // an error in its metadata.
                if (candSourceBlame == null)
                {
                    return;
                }

                // If there is no current mapping for this package, then
                // we can just return.
                if ((currentExportedBlame == null)
                    && (currentImportedBlame == null)
                    && (currentRequiredBlames == null))
                {
                    List<Blame> usedCaps = currentPkgs.m_usedPkgs.get(usedPkgName);
                    if (usedCaps == null)
                    {
                        usedCaps = new ArrayList<Blame>();
                        currentPkgs.m_usedPkgs.put(usedPkgName, usedCaps);
                    }
//System.out.println("+++ MERGING CB " + candBlame + " SB " + candSourceBlame);
//                    usedCaps.add(new Blame(candBlame.m_reqs, sourceBlame.m_cap));
                    usedCaps.add(candSourceBlame);
//                    return;
                }
                else if (!current.isResolved())
                {
                    if ((currentExportedBlame != null)
                        && !isCompatible(currentExportedBlame.m_cap, candSourceBlame.m_cap, modulePkgMap))
                    {
                        throw new ResolveException(
                            "Constraint violation for package '" + usedPkgName
                            + "' when resolving module " + current
                            + " between existing constraint "
                            + currentExportedBlame
                            + " and candidate constraint "
                            + candSourceBlame, null, null);
                    }
                    else if ((currentImportedBlame != null)
                        && !isCompatible(currentImportedBlame.m_cap, candSourceBlame.m_cap, modulePkgMap))
                    {
//System.out.println("+++ CIB " + currentImportedBlame + " SB " + sourceBlame);
                        // Try to remove the previously selected candidate associated
                        // with the requirement blamed for adding the constraint. This
                        // Permutate the candidate map.
                        if (currentImportedBlame.m_reqs.size() != 0)
                        {
                            // Permutate the candidate map.
                            for (int reqIdx = 0; reqIdx < currentImportedBlame.m_reqs.size(); reqIdx++)
                            {
                                Map<Requirement, Set<Capability>> copy = copyCandidateMap(candidateMap);
                                Set<Capability> candidates =
                                    copy.get(currentImportedBlame.m_reqs.get(reqIdx));
                                Iterator it = candidates.iterator();
                                it.next();
                                it.remove();
// TODO: PROTO3 RESOLVER - We could check before doing the candidate map copy.
                                if (candidates.size() > 0)
                                {
                                    m_candidatePermutations.add(copy);
                                }
                            }
                        }

                        throw new ResolveException(
                            "Constraint violation for package '" + usedPkgName
                            + "' when resolving module " + current
                            + " between existing constraint "
                            + currentImportedBlame
                            + " and candidate constraint "
                            + candSourceBlame, null, null);
                    }
                }

                verifyAndMergeUses(current, currentPkgs, candSourceBlame,
                    modulePkgMap, candidateMap, cycleMap);
            }
        }
    }

    private void permutateCandidates(
        Module current, String pkgName, Blame currentBlame, Blame candBlame,
        Map<Requirement, Set<Capability>> candidateMap)
        throws ResolveException
    {
// TODO: FELIX3 - I think permutation is not as efficient as it could be, since
//       we will end up generating permutations that are subsets of previous
//       permutations as we cycle through candidates. We should check if an
//       existing candidate map already has removed the conflicting candidate.

        // Try to remove the previously selected candidate associated
        // with the requirement blamed for adding the constraint. This
        // blamed requirement may be null if the bundle itself is
        // exports the package imposing the uses constraint.
        if ((currentBlame.m_reqs != null) && (currentBlame.m_reqs.size() != 0))
        {
            // Permutate the candidate map.
            for (int reqIdx = 0; reqIdx < currentBlame.m_reqs.size(); reqIdx++)
            {
                // Verify whether we have more than one candidate to create
                // a permutation.
                Set<Capability> candidates = candidateMap.get(currentBlame.m_reqs.get(reqIdx));
                if (candidates.size() > 1)
                {
                    Map<Requirement, Set<Capability>> copy = copyCandidateMap(candidateMap);
                    candidates = copy.get(currentBlame.m_reqs.get(reqIdx));
                    Iterator it = candidates.iterator();
                    it.next();
                    it.remove();
                    m_candidatePermutations.add(copy);
                }
            }
        }
        throw new ResolveException(
            "Constraint violation for package '"
            + pkgName + "' when resolving module "
            + current + " between existing constraint "
            + currentBlame + " and candidate constraint "
            + candBlame, null, null);
    }

    private static boolean isCompatible(
        Capability currentCap, Capability candCap, Map<Module, Packages> modulePkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            List<Capability> currentSources =
                getPackageSources(
                    currentCap,
                    modulePkgMap,
                    new ArrayList<Capability>(),
                    new HashSet<Capability>());
            List<Capability> candSources =
                getPackageSources(
                    candCap,
                    modulePkgMap,
                    new ArrayList<Capability>(),
                    new HashSet<Capability>());
//System.out.println("+++ currentSources " + currentSources + " - candSources " + candSources);
            return currentSources.containsAll(candSources) || candSources.containsAll(currentSources);
        }
        return true;
    }

    private static List<Capability> getPackageSources(
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

            Packages pkgs = modulePkgMap.get(cap.getModule());
            sources.add(cap);
            String pkgName = cap.getAttribute(Capability.PACKAGE_ATTR).getValue().toString();
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSources(blame.m_cap, modulePkgMap, sources, cycleMap);
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
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

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
        if (m_isInvokeCount)
        {
            String methodName = new Exception().fillInStackTrace().getStackTrace()[0].getMethodName();
            Long count = m_invokeCounts.get(methodName);
            count = (count == null) ? new Long(1) : new Long(count.longValue() + 1);
            m_invokeCounts.put(methodName, count);
        }

        wireMap.put(module, m_emptyWires);

        List<Wire> packageWires = new ArrayList<Wire>();

        Packages pkgs = modulePkgMap.get(module);
        for (Entry<String, Blame> entry : pkgs.m_importedPkgs.entrySet())
        {
            if (!entry.getValue().m_cap.getModule().isResolved())
            {
                populateWireMap(entry.getValue().m_cap.getModule(), modulePkgMap, wireMap,
                    candidateMap);
            }

            // Ignore modules that import themselves.
            if (!module.equals(entry.getValue().m_cap.getModule())
                && entry.getValue().m_cap.getAttribute(
                    Capability.PACKAGE_ATTR).getValue().equals(pkgName))
            {
                List<Attribute> attrs = new ArrayList();
                attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
                packageWires.add(
                    new WireImpl(
                        module,
                        // We need an unique requirement here or else subsequent
                        // dynamic imports for the same dynamic requirement will
                        // conflict with previous ones.
                        new RequirementImpl(Capability.PACKAGE_NAMESPACE, new ArrayList(0), attrs),
                        entry.getValue().m_cap.getModule(),
                        entry.getValue().m_cap));
            }
        }

        wireMap.put(module, packageWires);

        return wireMap;
    }

// TODO: FELIX3 - This check should be moved to ResolverState.
    private static void verifyNativeLibraries(Module module)
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
            if (libs.size() == 0)
            {
                msg = "No matching native libraries found.";
            }
            if (msg != null)
            {
                throw new ResolveException(msg, module, null);
            }
        }
    }

    /**
     * Checks to see if the passed in module's required execution environment
     * is provided by the framework.
     * @param fwkExecEvnStr The original property value of the framework's
     *        supported execution environments.
     * @param fwkExecEnvSet Parsed set of framework's supported execution environments.
     * @param module The module whose required execution environment is to be to verified.
     * @throws ResolveException if the module's required execution environment does
     *         not match the framework's supported execution environment.
    **/
    private static void verifyExecutionEnvironment(
        String fwkExecEnvStr, Set fwkExecEnvSet, Module module)
        throws ResolveException
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
                && (fwkExecEnvStr != null)
                && (fwkExecEnvStr.length() > 0))
            {
                StringTokenizer tokens = new StringTokenizer(bundleExecEnvStr, ",");
                boolean found = false;
                while (tokens.hasMoreTokens() && !found)
                {
                    if (fwkExecEnvSet.contains(tokens.nextToken().trim()))
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

    /**
     * Updates the framework wide execution environment string and a cached Set of
     * execution environment tokens from the comma delimited list specified by the
     * system variable 'org.osgi.framework.executionenvironment'.
     * @param frameworkEnvironment Comma delimited string of provided execution environments
    **/
    private static Set parseExecutionEnvironments(String fwkExecEnvStr)
    {
        Set newSet = new HashSet();
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

    private static class Packages
    {
        public final Map<String, Blame> m_exportedPkgs
            = new HashMap<String, Blame>();
        public final Map<String, Blame> m_importedPkgs
            = new HashMap<String, Blame>();
        public final Map<String, List<Blame>> m_requiredPkgs
            = new HashMap<String, List<Blame>>();
        public final Map<String, List<Blame>> m_usedPkgs
            = new HashMap<String, List<Blame>>();

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
        public final List<Requirement> m_reqs;
        public final Capability m_cap;

        public Blame(List<Requirement> reqs, Capability cap)
        {
            m_reqs = reqs;
            m_cap = cap;
        }

        public String toString()
        {
            return m_cap.getModule() + "." + m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue()
                + " BLAMED ON " + m_reqs;
        }

        public boolean equals(Object o)
        {
            return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
                && m_cap.equals(((Blame) o).m_cap);
        }
    }
}