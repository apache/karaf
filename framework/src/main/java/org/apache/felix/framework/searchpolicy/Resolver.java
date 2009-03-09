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
package org.apache.felix.framework.searchpolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.framework.util.manifestparser.R4Attribute;
import org.apache.felix.framework.util.manifestparser.R4Directive;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.osgi.framework.Constants;

public class Resolver
{
    private final Logger m_logger;

    // Reusable empty array.
    private static final IWire[] m_emptyWires = new IWire[0];
    private static final IModule[] m_emptyModules = new IModule[0];
    private static final PackageSource[] m_emptySources = new PackageSource[0];

    public Resolver(Logger logger)
    {
        m_logger = logger;
    }

    // Returns a map of resolved bundles where the key is the module
    // and the value is an array of wires.
    public Map resolve(ResolverState state, IModule rootModule) throws ResolveException
    {
        // If the module is already resolved, then we can just return.
        if (rootModule.isResolved())
        {
            return null;
        }

        // This variable maps an unresolved module to a list of candidate
        // sets, where there is one candidate set for each requirement that
        // must be resolved. A candidate set contains the potential canidates
        // available to resolve the requirement and the currently selected
        // candidate index.
        Map candidatesMap = new HashMap();

        // The first step is to populate the candidates map. This
        // will use the target module to populate the candidates map
        // with all potential modules that need to be resolved as a
        // result of resolving the target module. The key of the
        // map is a potential module to be resolved and the value is
        // a list of candidate sets, one for each of the module's
        // requirements, where each candidate set contains the potential
        // candidates for resolving the requirement. Not all modules in
        // this map will be resolved, only the target module and
        // any candidates selected to resolve its requirements and the
        // transitive requirements this implies.
        populateCandidatesMap(state, candidatesMap, rootModule);

        // The next step is to use the candidates map to determine if
        // the class space for the root module is consistent. This
        // is an iterative process that transitively walks the "uses"
        // relationships of all packages visible from the root module
        // checking for conflicts. If a conflict is found, it "increments"
        // the configuration of currently selected potential candidates
        // and tests them again. If this method returns, then it has found
        // a consistent set of candidates; otherwise, a resolve exception
        // is thrown if it exhausts all possible combinations and could
        // not find a consistent class space.
        findConsistentClassSpace(state, candidatesMap, rootModule);

        // The final step is to create the wires for the root module and
        // transitively all modules that are to be resolved from the
        // selected candidates for resolving the root module's imports.
        // When this call returns, each module's wiring and resolved
        // attributes are set. The resulting wiring map is used below
        // to fire resolved events outside of the synchronized block.
        // The resolved module wire map maps a module to its array of
        // wires.
        return populateWireMap(state, candidatesMap, rootModule, new HashMap());
    }

    // TODO: RESOLVER - Fix this return type.
    // Return candidate wire in result[0] and wire map in result[1]
    public Object[] resolveDynamicImport(ResolverState state, IModule importer, String pkgName)
        throws ResolveException
    {
        PackageSource candidate = null;
        Map resolvedModuleWireMap = null;

        // We can only search dynamic imports if the bundle
        // doesn't import, export, nor require the package in
        // question. Check these conditions first.
        if (isDynamicImportAllowed(importer, pkgName))
        {
            // Loop through the importer's dynamic requirements to determine if
            // there is a matching one for the package from which we want to
            // load a class.
            IRequirement[] dynamics = importer.getDynamicRequirements();
            for (int dynIdx = 0; (dynamics != null) && (dynIdx < dynamics.length); dynIdx++)
            {
                IRequirement target =
                    createDynamicRequirement(dynamics[dynIdx], pkgName);
                if (target != null)
                {
                    // See if there is a candidate exporter that satisfies the
                    // constrained dynamic requirement.
                    try
                    {
                        // Get "resolved" and "unresolved" candidates and put
                        // the "resolved" candidates first.
                        PackageSource[] resolved = state.getResolvedCandidates(target);
                        PackageSource[] unresolved = state.getUnresolvedCandidates(target);
                        PackageSource[] candidates = new PackageSource[resolved.length + unresolved.length];
                        System.arraycopy(resolved, 0, candidates, 0, resolved.length);
                        System.arraycopy(unresolved, 0, candidates, resolved.length, unresolved.length);

                        // Take the first candidate that can resolve.
                        for (int candIdx = 0;
                            (candidate == null) && (candIdx < candidates.length);
                            candIdx++)
                        {
                            try
                            {
                                // If a map is returned, then the candidate resolved
                                // consistently with the importer.
                                resolvedModuleWireMap =
                                    resolveDynamicImportCandidate(
                                        state, candidates[candIdx].m_module, importer);
                                if (resolvedModuleWireMap != null)
                                {
                                    candidate = candidates[candIdx];
                                }
                            }
                            catch (ResolveException ex)
                            {
                                // Ignore candidates that cannot resolve.
                            }
                        }

                        if (candidate != null)
                        {
                            // Create the wire and add it to the module.
                            Object[] result = new Object[2];
                            result[0] = new R4Wire(
                                importer, dynamics[dynIdx], candidate.m_module, candidate.m_capability);
                            result[1] = resolvedModuleWireMap;
                            return result;
                        }
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(Logger.LOG_ERROR, "Unable to dynamically import package.", ex);
                    }
                }
            }
        }

        return null;
    }

    private static boolean isDynamicImportAllowed(IModule importer, String pkgName)
    {
        // If any of the module exports this package, then we cannot
        // attempt to dynamically import it.
        ICapability[] caps = importer.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
                && caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
            {
                return false;
            }
        }
        // If any of our wires have this package, then we cannot
        // attempt to dynamically import it.
        IWire[] wires = importer.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].hasPackage(pkgName))
            {
                return false;
            }
        }
        // Ok to attempt to dynamically import the package.
        return true;
    }

    private static IRequirement createDynamicRequirement(IRequirement dynReq, String pkgName)
    {
        IRequirement req = null;

        // First check to see if the dynamic requirement matches the
        // package name; this means we have to do wildcard matching.
        String dynPkgName = ((Requirement) dynReq).getPackageName();
        boolean wildcard = (dynPkgName.lastIndexOf(".*") >= 0);
        dynPkgName = (wildcard)
            ? dynPkgName.substring(0, dynPkgName.length() - 2) : dynPkgName;
        // If the dynamic requirement matches the package name, then
        // create a new requirement for the specific package.
        if (dynPkgName.equals("*") ||
            pkgName.equals(dynPkgName) ||
            (wildcard && pkgName.startsWith(dynPkgName + ".")))
        {
            // Create a new requirement based on the dynamic requirement,
            // but substitute the precise package name for which we are
            // looking, because it is not possible to use the potentially
            // wildcarded version in the dynamic requirement.
            R4Directive[] dirs = ((Requirement) dynReq).getDirectives();
            R4Attribute[] attrs = ((Requirement) dynReq).getAttributes();
            R4Attribute[] newAttrs = new R4Attribute[attrs.length];
            System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
            for (int attrIdx = 0; attrIdx < newAttrs.length; attrIdx++)
            {
                if (newAttrs[attrIdx].getName().equals(ICapability.PACKAGE_PROPERTY))
                {
                    newAttrs[attrIdx] = new R4Attribute(
                        ICapability.PACKAGE_PROPERTY, pkgName, false);
                    break;
                }
            }
            req = new Requirement(ICapability.PACKAGE_NAMESPACE, dirs, newAttrs);
        }

        return req;
    }

    private Map resolveDynamicImportCandidate(
        ResolverState state, IModule provider, IModule importer)
        throws ResolveException
    {
        // If the provider of the dynamically imported package is not
        // resolved, then we need to calculate the candidates to resolve
        // it and see if there is a consistent class space for the
        // provider. If there is no consistent class space, then a resolve
        // exception is thrown.
        Map candidatesMap = new HashMap();
        if (!provider.isResolved())
        {
            populateCandidatesMap(state, candidatesMap, provider);
            findConsistentClassSpace(state, candidatesMap, provider);
        }

        // If the provider can be successfully resolved, then verify that
        // its class space is consistent with the existing class space of the
        // module that instigated the dynamic import.
        Map moduleMap = new HashMap();
        Map importerPkgMap = getModulePackages(moduleMap, importer, candidatesMap);

        // Now we need to calculate the "uses" constraints of every package
        // accessible to the provider module based on its current candidates.
        Map usesMap = calculateUsesConstraints(provider, moduleMap, candidatesMap);

        // Verify that none of the provider's implied "uses" constraints
        // in the uses map conflict with anything in the importing module's
        // package map.
        for (Iterator iter = usesMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();

            // For the given "used" package, get that package from the
            // importing module's package map, if present.
            ResolvedPackage rp = (ResolvedPackage) importerPkgMap.get(entry.getKey());

            // If the "used" package is also visible to the importing
            // module, make sure there is no conflicts in the implied
            // "uses" constraints.
            if (rp != null)
            {
                // Clone the resolve package so we can modify it.
                rp = (ResolvedPackage) rp.clone();

                // Loop through all implied "uses" constraints for the current
                // "used" package and verify that all package sources are
                // compatible with the package source of the importing module's
                // package map.
                List constraintList = (List) entry.getValue();
                for (int constIdx = 0; constIdx < constraintList.size(); constIdx++)
                {
                    // Get a specific "uses" constraint for the current "used"
                    // package.
                    ResolvedPackage rpUses = (ResolvedPackage) constraintList.get(constIdx);
                    // Determine if the implied "uses" constraint is compatible with
                    // the improting module's package sources for the given "used"
                    // package. They are compatible if one is the subset of the other.
                    // Retain the union of the two sets if they are compatible.
                    if (rpUses.isSubset(rp))
                    {
                        // Do nothing because we already have the superset.
                    }
                    else if (rp.isSubset(rpUses))
                    {
                        // Keep the superset, i.e., the union.
                        rp.m_sourceList.clear();
                        rp.m_sourceList.addAll(rpUses.m_sourceList);
                    }
                    else
                    {
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "Constraint violation for " + importer
                            + " detected; module can see "
                            + rp + " and " + rpUses);
                        return null;
                    }
                }
            }
        }

        return populateWireMap(state, candidatesMap, provider, new HashMap());
    }

    private static void populateCandidatesMap(
        ResolverState state, Map candidatesMap, IModule targetModule)
        throws ResolveException
    {
        // Detect cycles.
        if (candidatesMap.get(targetModule) != null)
        {
            return;
        }

        // List to hold the resolving candidate sets for the target
        // module's requirements.
        List candSetList = new ArrayList();

        // Even though the candidate set list is currently empty, we
        // record it in the candidates map early so we can use it to
        // detect cycles.
        candidatesMap.put(targetModule, candSetList);

        // Loop through potential fragments and add each set to the
        // candidate set; if the targetModule is a fragment then there
        // will be no potential fragments here.
        Map fragmentMap = state.getPotentialFragments(targetModule);
        for (Iterator it = fragmentMap.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            IModule[] fragments = (IModule[]) entry.getValue();

            // If we have fragment candidates, then we need to recursively populate
            // the resolver map with each of them.
            if (fragments.length > 0)
            {
                for (int fragIdx = 0; fragIdx < fragments.length; fragIdx++)
                {
                    try
                    {
                        // We still need to populate the candidates for the
                        // fragment even if it is resolved, because fragments
                        // can be attached to multiple hosts and each host
                        // may end up with different wires for the fragments
                        // dependencies.
                        populateCandidatesMap(state, candidatesMap, fragments[fragIdx]);
                    }
                    catch (ResolveException ex)
                    {
                        // If we received a resolve exception, then the
                        // current fragment is not resolvable for some
                        // reason and should be removed from the list of
                        // fragments. For now, just null it.
                        fragments[fragIdx] = null;
                    }
                }

                // Remove any nulled fragments to create the final list
                // of potential fragment candidates.
                fragments = shrinkModuleArray(fragments);
            }

            // All fragments, by definition, are optional so we if none of the
            // candidates could be resolved, then just ignore them; otherwise,
            // add the resolvable fragments to the host's candidate set.
            if (fragments.length > 0)
            {
                candSetList.add(
                    new CandidateSet(CandidateSet.FRAGMENT, targetModule, null, fragments));
            }
        }

        // Loop through each requirement and calculate its resolving
        // set of candidates.
        IRequirement[] reqs = targetModule.getRequirements();
        for (int reqIdx = 0; (reqs != null) && (reqIdx < reqs.length); reqIdx++)
        {
            if (reqs[reqIdx].getNamespace().equals(ICapability.HOST_NAMESPACE))
            {
                List hostList = state.getPotentialHosts(targetModule);
                IModule[] hosts = (IModule[]) hostList.toArray(new IModule[hostList.size()]);
                // If we have host candidates, then we need to recursively populate
                // the resolver map with each of them.
                ResolveException rethrow = null;
                if (hosts.length > 0)
                {
                    for (int hostIdx = 0; hostIdx < hosts.length; hostIdx++)
                    {
                        try
                        {
                            // Only populate the resolver map with hosts that
                            // are not already resolved.
                            if (!hosts[hostIdx].isResolved())
                            {
                                populateCandidatesMap(state, candidatesMap, hosts[hostIdx]);
                            }
                        }
                        catch (ResolveException ex)
                        {
                            // If we received a resolve exception, then the
                            // current host is not resolvable for some
                            // reason and should be removed from the list of
                            // hosts. For now, just null it.
                            hosts[hostIdx] = null;
                            rethrow = ex;
                        }
                    }

                    // Remove any nulled hosts to create the final list
                    // of potential host candidates.
                    hosts = shrinkModuleArray(hosts);
                }

                // If no host candidates exist at this point, then throw a
                // resolve exception since all fragments must have a host.
                if (hosts.length == 0)
                {
                    // If we have received an exception while trying to populate
                    // the candidates map, rethrow that exception since it might
                    // be useful. NOTE: This is not necessarily the "only"
                    // correct exception, since it is possible that multiple
                    // candidates were not resolvable, but it is better than
                    // nothing.
                    if (rethrow != null)
                    {
                        throw rethrow;
                    }
                    else
                    {
                        throw new ResolveException(
                            "Unable to find host for fragment.",
                            targetModule, reqs[reqIdx]);
                    }
                }
                candSetList.add(
                    new CandidateSet(CandidateSet.HOST, targetModule, reqs[reqIdx], hosts));
            }
            else
            {
                // Get the candidates from the "resolved" and "unresolved"
                // package maps. The "resolved" candidates have higher priority
                // than "unresolved" ones, so put the "resolved" candidates
                // at the front of the list of candidates.
                PackageSource[] resolved = state.getResolvedCandidates(reqs[reqIdx]);
                PackageSource[] unresolved = state.getUnresolvedCandidates(reqs[reqIdx]);
                PackageSource[] candidates = new PackageSource[resolved.length + unresolved.length];
                System.arraycopy(resolved, 0, candidates, 0, resolved.length);
                System.arraycopy(unresolved, 0, candidates, resolved.length, unresolved.length);

                // If we have candidates, then we need to recursively populate
                // the resolver map with each of them.
                ResolveException rethrow = null;
                if (candidates.length > 0)
                {
                    for (int candIdx = 0; candIdx < candidates.length; candIdx++)
                    {
                        try
                        {
                            // Only populate the resolver map with modules that
                            // are not already resolved.
                            if (!candidates[candIdx].m_module.isResolved())
                            {
                                populateCandidatesMap(state, candidatesMap, candidates[candIdx].m_module);
                            }
                        }
                        catch (ResolveException ex)
                        {
                            // If we received a resolve exception, then the
                            // current candidate is not resolvable for some
                            // reason and should be removed from the list of
                            // candidates. For now, just null it.
                            candidates[candIdx] = null;
                            rethrow = ex;
                        }
                    }

                    // Remove any nulled candidates to create the final list
                    // of available candidates.
                    candidates = shrinkCandidateArray(candidates);
                }

                // If no candidates exist at this point, then throw a
                // resolve exception unless the import is optional.
                if ((candidates.length == 0) && !reqs[reqIdx].isOptional())
                {
                    // Since the target module cannot resolve, remove its
                    // candidates set list from the candidates map, since
                    // it is invalid.
                    candidatesMap.remove(targetModule);

                    // If we have received an exception while trying to populate
                    // the candidates map, rethrow that exception since it might
                    // be useful. NOTE: This is not necessarily the "only"
                    // correct exception, since it is possible that multiple
                    // candidates were not resolvable, but it is better than
                    // nothing.
                    if (rethrow != null)
                    {
                        throw rethrow;
                    }
                    else
                    {
                        throw new ResolveException(
                            "Unable to resolve.", targetModule, reqs[reqIdx]);
                    }
                }
                else if (candidates.length > 0)
                {
                    candSetList.add(
                        new CandidateSet(CandidateSet.NORMAL, targetModule, reqs[reqIdx], candidates));
                }
            }
        }
    }

    // This flag indicates whether candidates have been rotated due to a
    // "uses" constraint conflict. If so, then it is not necessary to perform
    // a permutation, since rotating the candidates selected a new permutation.
    // This part of an attempt to perform smarter permutations.
    private boolean m_candidatesRotated = false;

    private void findConsistentClassSpace(
        ResolverState state, Map candidatesMap, IModule rootModule)
        throws ResolveException
    {
        List candidatesList = null;

        // The reusable module map maps a module to a map of
        // resolved packages that are accessible by the given
        // module. The set of resolved packages is calculated
        // from the current candidates of the candidates map
        // and the module's metadata.
        Map moduleMap = new HashMap();

        // Reusable map used to test for cycles.
        Map cycleMap = new HashMap();

        // Test the current potential candidates to determine if they
        // are consistent. Keep looping until we find a consistent
        // set or an exception is thrown.
        while (!isSingletonConsistent(state, rootModule, moduleMap, candidatesMap) ||
            !isClassSpaceConsistent(rootModule, moduleMap, cycleMap, candidatesMap))
        {
            // The incrementCandidateConfiguration() method requires
            // ordered access to the candidates map, so we will create
            // a reusable list once right here.
            if (candidatesList == null)
            {
                candidatesList = new ArrayList();
                for (Iterator iter = candidatesMap.entrySet().iterator();
                    iter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) iter.next();
                    candidatesList.add(entry.getValue());
                }

                // Sort the bundles candidate sets according to a weighting
                // based on how many multi-candidate requirements each has.
                // The idea is to push bundles with more potential candidate
                // permutations to the front so we can permutate over them
                // more quickly, since they are likely to have more issues.
                Collections.sort(candidatesList, new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        int w1 = calculateWeight((List) o1);
                        int w2 = calculateWeight((List) o2);
                        if (w1 < w2)
                        {
                            return -1;
                        }
                        else if (w1 > w2)
                        {
                            return 1;
                        }
                        return 0;
                    }

                    private int calculateWeight(List candSetList)
                    {
                        int weight = 0;
                        for (int csIdx = 0; csIdx < candSetList.size(); csIdx++)
                        {
                            CandidateSet cs = (CandidateSet) candSetList.get(csIdx);
                            if ((cs.m_candidates != null) && (cs.m_candidates.length > 1))
                            {
                                weight += cs.m_candidates.length;
                            }
                        }
                        return -weight;
                    }
                });
            }

            // Increment the candidate configuration to a new permutation so
            // we can test again, unless some candidates have been rotated.
            // In that case, we re-test the current permutation, since rotating
            // the candidates effectively selects a new permutation.
            if (!m_candidatesRotated)
            {
                incrementCandidateConfiguration(candidatesList);
            }
            else
            {
                m_candidatesRotated = false;
            }

            // Clear the module map.
            moduleMap.clear();

            // Clear the cycle map.
            cycleMap.clear();
        }
    }

    /**
     * This methd checks to see if the target module and any of the candidate
     * modules to resolve its dependencies violate any singleton constraints.
     * Actually, it just creates a map of resolved singleton modules and then
     * delegates all checking to another recursive method.
     *
     * @param targetModule the module that is the root of the tree of modules to check.
     * @param moduleMap a map to cache the package space of each module.
     * @param candidatesMap a map containing the all candidates to resolve all
     *        dependencies for all modules.
     * @return <tt>true</tt> if all candidates are consistent with respect to singletons,
     *         <tt>false</tt> otherwise.
    **/
    private boolean isSingletonConsistent(
        ResolverState state, IModule targetModule, Map moduleMap, Map candidatesMap)
    {
        // Create a map of all resolved singleton modules.
        Map singletonMap = new HashMap();
        IModule[] modules = state.getModules();
        for (int i = 0; (modules != null) && (i < modules.length); i++)
        {
            if (modules[i].isResolved() && isSingleton(modules[i]))
            {
                String symName = modules[i].getSymbolicName();
                singletonMap.put(symName, symName);
            }
        }

        return areCandidatesSingletonConsistent(
            state, targetModule, singletonMap, moduleMap, new HashMap(), candidatesMap);
    }

    /**
     * This method recursive checks the target module and all of its transitive
     * dependency modules to verify that they do not violate a singleton constraint.
     * If the target module is a singleton, then it checks that againts existing
     * singletons. Then it checks all current unresolved candidates recursively.
     *
     * @param targetModule the module that is the root of the tree of modules to check.
     * @param singletonMap the current map of singleton symbolic names.
     * @param moduleMap a map to cache the package space of each module.
     * @param cycleMap a map to detect cycles.
     * @param candidatesMap a map containing the all candidates to resolve all
     *        dependencies for all modules.
     * @return <tt>true</tt> if all candidates are consistent with respect to singletons,
     *         <tt>false</tt> otherwise.
    **/
    private boolean areCandidatesSingletonConsistent(
        ResolverState state, IModule targetModule,
        Map singletonMap, Map moduleMap, Map cycleMap, Map candidatesMap)
    {
        // If we are in a cycle, then assume true for now.
        if (cycleMap.get(targetModule) != null)
        {
            return true;
        }

        // Record the target module in the cycle map.
        cycleMap.put(targetModule, targetModule);

        // Check to see if the targetModule violates a singleton.
        // If not and it is a singleton, then add it to the singleton
        // map since it will constrain other singletons.
        String symName = targetModule.getSymbolicName();
        boolean isSingleton = isSingleton(targetModule);
        if (isSingleton && singletonMap.containsKey(symName))
        {
            return false;
        }
        else if (isSingleton)
        {
            singletonMap.put(symName, symName);
        }

        // Get the package space of the target module.
        Map pkgMap = null;
        try
        {
            pkgMap = getModulePackages(moduleMap, targetModule, candidatesMap);
        }
        catch (ResolveException ex)
        {
            m_logger.log(
                Logger.LOG_DEBUG,
                "Constraint violation for " + targetModule + " detected.",
                ex);
            return false;
        }

        // Loop through all of the target module's accessible packages and
        // verify that all package sources are consistent.
        for (Iterator iter = pkgMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            // Get the resolved package, which contains the set of all
            // package sources for the given package.
            ResolvedPackage rp = (ResolvedPackage) entry.getValue();
            // Loop through each package source and test if it is consistent.
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                // If the module for this package source is not resolved, then
                // we have to see if resolving it would violate a singleton
                // constraint.
                PackageSource ps = (PackageSource) rp.m_sourceList.get(srcIdx);
                if (!ps.m_module.isResolved())
                {
                    return areCandidatesSingletonConsistent(state, ps.m_module, singletonMap, moduleMap, cycleMap, candidatesMap);
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the specified module is a singleton
     * (i.e., directive singleton:=true in Bundle-SymbolicName).
     *
     * @param module the module to check for singleton status.
     * @return true if the module is a singleton, false otherwise.
    **/
    private static boolean isSingleton(IModule module)
    {
        final ICapability[] modCaps = Util.getCapabilityByNamespace(
                module, Capability.MODULE_NAMESPACE);
        if (modCaps == null || modCaps.length == 0)
        {
            // this should never happen?
            return false;
        }
        final R4Directive[] dirs = ((Capability) modCaps[0]).getDirectives();
        for (int dirIdx = 0; (dirs != null) && (dirIdx < dirs.length); dirIdx++)
        {
            if (dirs[dirIdx].getName().equalsIgnoreCase(Constants.SINGLETON_DIRECTIVE)
                && Boolean.valueOf(dirs[dirIdx].getValue()).booleanValue())
            {
                return true;
            }
        }
        return false;
    }

    private boolean isClassSpaceConsistent(
        IModule targetModule, Map moduleMap, Map cycleMap, Map candidatesMap)
    {
//System.out.println("isClassSpaceConsistent("+targetModule+")");
        // If we are in a cycle, then assume true for now.
        if (cycleMap.get(targetModule) != null)
        {
            return true;
        }

        // Record the target module in the cycle map.
        cycleMap.put(targetModule, targetModule);

        // Get the package map for the target module, which is a
        // map of all packages accessible to the module and their
        // associated package sources.
        Map pkgMap = null;
        try
        {
            pkgMap = getModulePackages(moduleMap, targetModule, candidatesMap);
        }
        catch (ResolveException ex)
        {
            m_logger.log(
                Logger.LOG_DEBUG,
                "Constraint violation for " + targetModule + " detected.",
                ex);
            return false;
        }

        // Loop through all of the target module's accessible packages and
        // verify that all package sources are consistent.
        for (Iterator iter = pkgMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            // Get the resolved package, which contains the set of all
            // package sources for the given package.
            ResolvedPackage rp = (ResolvedPackage) entry.getValue();
            // Loop through each package source and test if it is consistent.
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                PackageSource ps = (PackageSource) rp.m_sourceList.get(srcIdx);
                if (!isClassSpaceConsistent(ps.m_module, moduleMap, cycleMap, candidatesMap))
                {
                    return false;
                }
            }
        }

        // Now we need to calculate the "uses" constraints of every package
        // accessible to the target module based on the current candidates.
        Map usesMap = null;
        try
        {
            usesMap = calculateUsesConstraints(targetModule, moduleMap, candidatesMap);
        }
        catch (ResolveException ex)
        {
            m_logger.log(
                Logger.LOG_DEBUG,
                "Constraint violation for " + targetModule + " detected.",
                ex);
            return false;
        }

        // Verify that none of the implied "uses" constraints in the uses map
        // conflict with anything in the target module's package map.
        for (Iterator iter = usesMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();

            // For the given "used" package, get that package from the
            // target module's package map, if present.
            ResolvedPackage rp = (ResolvedPackage) pkgMap.get(entry.getKey());

            // If the "used" package is also visible to the target module,
            // make sure there is no conflicts in the implied "uses"
            // constraints.
            if (rp != null)
            {
                // Clone the resolve package so we can modify it.
                rp = (ResolvedPackage) rp.clone();

                // Loop through all implied "uses" constraints for the current
                // "used" package and verify that all package sources are
                // compatible with the package source of the root module's
                // package map.
                List constraintList = (List) entry.getValue();
                for (int constIdx = 0; constIdx < constraintList.size(); constIdx++)
                {
                    // Get a specific "uses" constraint for the current "used"
                    // package.
                    ResolvedPackage rpUses = (ResolvedPackage) constraintList.get(constIdx);
                    // Determine if the implied "uses" constraint is compatible with
                    // the target module's package sources for the given "used"
                    // package. They are compatible if one is the subset of the other.
                    // Retain the union of the two sets if they are compatible.
                    if (rpUses.isSubset(rp))
                    {
                        // Do nothing because we already have the superset.
                    }
                    else if (rp.isSubset(rpUses))
                    {
                        // Keep the superset, i.e., the union.
                        rp.m_sourceList.clear();
                        rp.m_sourceList.addAll(rpUses.m_sourceList);
                    }
                    else
                    {
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "Constraint violation for " + targetModule
                            + " detected; module can see "
                            + rp + " and " + rpUses);

                        // If the resolved package has a candidate set, then
                        // attempt to directly rotate the candidates to fix the
                        // "uses" constraint conflict. The idea is rather than
                        // blinding incrementing to the next permutation, we will
                        // try to target the permutation to the bundle with a
                        // conflict, which in some cases will be smarter. Only
                        // rotate the candidates if we have more than one and we
                        // haven't already rotated them completely.
                        if ((rp.m_cs != null) && (rp.m_cs.m_candidates.length > 1)
                            && (rp.m_cs.m_rotated < rp.m_cs.m_candidates.length))
                        {
                            // Rotate candidates.
                            PackageSource first = rp.m_cs.m_candidates[0];
                            for (int i = 1; i < rp.m_cs.m_candidates.length; i++)
                            {
                                rp.m_cs.m_candidates[i - 1] = rp.m_cs.m_candidates[i];
                            }
                            rp.m_cs.m_candidates[rp.m_cs.m_candidates.length - 1] = first;
                            rp.m_cs.m_rotated++;
                            m_candidatesRotated = true;
                        }

                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Map calculateUsesConstraints(
        IModule targetModule, Map moduleMap, Map candidatesMap)
        throws ResolveException
    {
//System.out.println("calculateUsesConstraints("+targetModule+")");
        // Map to store calculated uses constraints. This maps a
        // package name to a list of resolved packages, where each
        // resolved package represents a constraint on anyone
        // importing the given package name. This map is returned
        // by this method.
        Map usesMap = new HashMap();

        // Re-usable map to detect cycles.
        Map cycleMap = new HashMap();

        // Get all packages accessible by the target module.
        Map pkgMap = getModulePackages(moduleMap, targetModule, candidatesMap);

        // Each package accessible from the target module is potentially
        // comprised of one or more modules, called package sources. The
        // "uses" constraints implied by all package sources must be
        // calculated and combined to determine the complete set of implied
        // "uses" constraints for each package accessible by the target module.
        for (Iterator iter = pkgMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            ResolvedPackage rp = (ResolvedPackage) entry.getValue();
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                usesMap = calculateUsesConstraints(
                    (PackageSource) rp.m_sourceList.get(srcIdx),
                    moduleMap, usesMap, cycleMap, candidatesMap);
            }
        }
        return usesMap;
    }

    private static Map calculateUsesConstraints(
        PackageSource psTarget, Map moduleMap, Map usesMap,
        Map cycleMap, Map candidatesMap)
        throws ResolveException
    {
//System.out.println("calculateUsesConstraints2("+psTarget.m_module+")");
        // If we are in a cycle, then return for now.
        if (cycleMap.get(psTarget) != null)
        {
            return usesMap;
        }

        // Record the target package source in the cycle map.
        cycleMap.put(psTarget, psTarget);

        // Get all packages accessible from the module of the
        // target package source.
        Map pkgMap = getModulePackages(moduleMap, psTarget.m_module, candidatesMap);

        // Get capability (i.e., package) of the target package source.
        Capability cap = (Capability) psTarget.m_capability;

        // Loop through all "used" packages of the capability.
        for (int i = 0; i < cap.getUses().length; i++)
        {
            // The target package source module should have a resolved package
            // for the "used" package in its set of accessible packages,
            // since it claims to use it, so get the associated resolved
            // package.
            ResolvedPackage rp = (ResolvedPackage) pkgMap.get(cap.getUses()[i]);

            // In general, the resolved package should not be null,
            // but check for safety.
            if (rp != null)
            {
                // First, iterate through all package sources for the resolved
                // package associated with the current "used" package and calculate
                // and combine the "uses" constraints for each package source.
                for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
                {
                    usesMap = calculateUsesConstraints(
                        (PackageSource) rp.m_sourceList.get(srcIdx),
                        moduleMap, usesMap, cycleMap, candidatesMap);
                }

                // Then, add the resolved package for the current "used" package
                // as a "uses" constraint too; add it to an existing constraint
                // list if the current "used" package is already in the uses map.
                List constraintList = (List) usesMap.get(cap.getUses()[i]);
                if (constraintList == null)
                {
                    constraintList = new ArrayList();
                }
                constraintList.add(rp);
                usesMap.put(cap.getUses()[i], constraintList);
            }
        }

        return usesMap;
    }

    private static Map getModulePackages(Map moduleMap, IModule module, Map candidatesMap)
        throws ResolveException
    {
        Map map = (Map) moduleMap.get(module);

        if (map == null)
        {
            map = calculateModulePackages(module, candidatesMap);
            moduleMap.put(module, map);
        }
        return map;
    }

    /**
     * <p>
     * Calculates the module's set of accessible packages and their
     * assocaited package sources. This method uses the current candidates
     * for resolving the module's requirements from the candidate map
     * to calculate the module's accessible packages.
     * </p>
     * @param module the module whose package map is to be calculated.
     * @param candidatesMap the map of potential candidates for resolving
     *        the module's requirements.
     * @return a map of the packages accessible to the specified module where
     *         the key of the map is the package name and the value of the map
     *         is a ResolvedPackage.
    **/
    private static Map calculateModulePackages(IModule module, Map candidatesMap)
        throws ResolveException
    {
//System.out.println("calculateModulePackages("+module+")");
        Map importedPackages = calculateImportedPackages(module, candidatesMap);
        Map exportedPackages = calculateExportedPackages(module);
        Map requiredPackages = calculateRequiredPackages(module, candidatesMap);

        // Merge exported packages into required packages. If a package is both
        // exported and required, then append the exported source to the end of
        // the require package sources; otherwise just add it to the package map.
        for (Iterator i = exportedPackages.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            ResolvedPackage rpReq = (ResolvedPackage) requiredPackages.get(entry.getKey());
            if (rpReq != null)
            {
                // Merge exported and required packages, avoiding duplicate
                // package sources and maintaining ordering.
                ResolvedPackage rpExport = (ResolvedPackage) entry.getValue();
                rpReq.merge(rpExport);
            }
            else
            {
                requiredPackages.put(entry.getKey(), entry.getValue());
            }
        }

        // Merge imported packages into required packages. Imports overwrite
        // any required and/or exported package.
        for (Iterator i = importedPackages.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            requiredPackages.put(entry.getKey(), entry.getValue());
        }

        return requiredPackages;
    }

    private static Map calculateImportedPackages(IModule targetModule, Map candidatesMap)
        throws ResolveException
    {
        return (candidatesMap.get(targetModule) == null)
            ? calculateImportedPackagesResolved(targetModule)
            : calculateImportedPackagesUnresolved(targetModule, candidatesMap);
    }

    private static Map calculateImportedPackagesUnresolved(IModule targetModule, Map candidatesMap)
        throws ResolveException
    {
//System.out.println("calculateImportedPackagesUnresolved("+targetModule+")");
        Map pkgMap = new HashMap();

        // Get the candidate set list to get all candidates for
        // all of the target module's requirements.
        List candSetList = (List) candidatesMap.get(targetModule);

        // Loop through all candidate sets that represent import dependencies
        // for the target module and add the current candidate's package source
        // to the imported package map.
        for (int candSetIdx = 0; (candSetList != null) && (candSetIdx < candSetList.size()); candSetIdx++)
        {
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);
// TODO: FRAGMENT - Eventually fragments will import, so the imports of a fragment
//                  will need to be added to the imports of the host.
            if (cs.m_type == CandidateSet.FRAGMENT)
            {
                continue;
            }
            else if (cs.m_type == CandidateSet.HOST)
            {
                // We can ignore the host candidates, since they will be
                // taken care of when resolving the host module.
                continue;
            }
            PackageSource ps = cs.m_candidates[cs.m_idx];

            if (ps.m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    ps.m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY);

                ResolvedPackage rp = new ResolvedPackage(pkgName, cs);
                rp.m_sourceList.add(ps);
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private static Map calculateImportedPackagesResolved(IModule targetModule)
        throws ResolveException
    {
//System.out.println("calculateImportedPackagesResolved("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through the target module's wires for package
        // dependencies and add the resolved package source to the
        // imported package map.
        IWire[] wires = targetModule.getWires();
        for (int wireIdx = 0; (wires != null) && (wireIdx < wires.length); wireIdx++)
        {
            if (wires[wireIdx].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    wires[wireIdx].getCapability().getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName, null) : rp;
                rp.m_sourceList.add(new PackageSource(wires[wireIdx].getExporter(), wires[wireIdx].getCapability()));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private static Map calculateExportedPackages(IModule targetModule)
    {
//System.out.println("calculateExportedPackages("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through the target module's capabilities that represent
        // exported packages and add them to the exported package map.
        ICapability[] caps = targetModule.getCapabilities();
        for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    caps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName, null) : rp;
                rp.m_sourceList.add(new PackageSource(targetModule, caps[capIdx]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private static Map calculateRequiredPackages(IModule targetModule, Map candidatesMap)
    {
        return (candidatesMap.get(targetModule) == null)
            ? calculateRequiredPackagesResolved(targetModule)
            : calculateRequiredPackagesUnresolved(targetModule, candidatesMap);
    }

    private static Map calculateRequiredPackagesUnresolved(IModule targetModule, Map candidatesMap)
    {
//System.out.println("calculateRequiredPackagesUnresolved("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through target module's candidate list for candidates
        // for its module dependencies and merge re-exported packages.
        List candSetList = (List) candidatesMap.get(targetModule);
        for (int candSetIdx = 0; (candSetList != null) && (candSetIdx < candSetList.size()); candSetIdx++)
        {
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);
// TODO: FRAGMENT - Eventually fragments will require bundles, so the required
//                  packages of the fragment will need to be added to the host.
            if (cs.m_type == CandidateSet.FRAGMENT)
            {
                continue;
            }
            else if (cs.m_type == CandidateSet.HOST)
            {
                // We can ignore the host candidates, since they will be
                // taken care of when resolving the host module.
                continue;
            }
            PackageSource ps = cs.m_candidates[cs.m_idx];

            // If the capabaility is a module dependency, then flatten it to packages.
            if (ps.m_capability.getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                // Calculate transitively required packages.
                Map cycleMap = new HashMap();
                cycleMap.put(targetModule, targetModule);
                Map requireMap =
                    calculateExportedAndReexportedPackages(
                        ps, candidatesMap, cycleMap);

                // Take the flattened required package map for the current
                // module dependency and merge it into the existing map
                // of required packages.
                for (Iterator reqIter = requireMap.entrySet().iterator(); reqIter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) reqIter.next();
                    ResolvedPackage rp = (ResolvedPackage) pkgMap.get(entry.getKey());
                    if (rp != null)
                    {
                        // Merge required packages, avoiding duplicate
                        // package sources and maintaining ordering.
                        ResolvedPackage rpReq = (ResolvedPackage) entry.getValue();
                        rp.merge(rpReq);
                    }
                    else
                    {
                        pkgMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return pkgMap;
    }

    private static Map calculateRequiredPackagesResolved(IModule targetModule)
    {
//System.out.println("calculateRequiredPackagesResolved("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through target module's wires for module dependencies
        // and merge re-exported packages.
        IWire[] wires = targetModule.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            // If the wire is a module dependency, then flatten it to packages.
            if (wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                // Calculate transitively required packages.
                // We can call calculateExportedAndReexportedPackagesResolved()
                // directly, since we know all dependencies have to be resolved
                // because this module itself is resolved.
                Map cycleMap = new HashMap();
                cycleMap.put(targetModule, targetModule);
                Map requireMap =
                    calculateExportedAndReexportedPackagesResolved(
                        wires[i].getExporter(), cycleMap);

                // Take the flattened required package map for the current
                // module dependency and merge it into the existing map
                // of required packages.
                for (Iterator reqIter = requireMap.entrySet().iterator(); reqIter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) reqIter.next();
                    ResolvedPackage rp = (ResolvedPackage) pkgMap.get(entry.getKey());
                    if (rp != null)
                    {
                        // Merge required packages, avoiding duplicate
                        // package sources and maintaining ordering.
                        ResolvedPackage rpReq = (ResolvedPackage) entry.getValue();
                        rp.merge(rpReq);
                    }
                    else
                    {
                        pkgMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return pkgMap;
    }

    private static Map calculateExportedAndReexportedPackages(
        PackageSource psTarget, Map candidatesMap, Map cycleMap)
    {
        return (candidatesMap.get(psTarget.m_module) == null)
            ? calculateExportedAndReexportedPackagesResolved(psTarget.m_module, cycleMap)
            : calculateExportedAndReexportedPackagesUnresolved(psTarget, candidatesMap, cycleMap);
    }

    private static Map calculateExportedAndReexportedPackagesUnresolved(
        PackageSource psTarget, Map candidatesMap, Map cycleMap)
    {
//System.out.println("calculateExportedAndReexportedPackagesUnresolved("+psTarget.m_module+")");
        Map pkgMap = new HashMap();

        if (cycleMap.get(psTarget.m_module) != null)
        {
            return pkgMap;
        }

        cycleMap.put(psTarget.m_module, psTarget.m_module);

        // Loop through all current candidates for target module's dependencies
        // and calculate the module's complete set of required packages (and
        // their associated package sources) and the complete set of required
        // packages to be re-exported.
        Map allRequiredMap = new HashMap();
        Map reexportedPkgMap = new HashMap();
        List candSetList = (List) candidatesMap.get(psTarget.m_module);
        for (int candSetIdx = 0; candSetIdx < candSetList.size(); candSetIdx++)
        {
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);
// TODO: FRAGMENT - Eventually fragments will export, so the exports of the
//                  fragment will need to be added to the host.
            if (cs.m_type == CandidateSet.FRAGMENT)
            {
                continue;
            }
            else if (cs.m_type == CandidateSet.HOST)
            {
                // We can ignore the host candidates, since they will be
                // taken care of when resolving the host module.
                continue;
            }
            PackageSource ps = cs.m_candidates[cs.m_idx];

            // If the candidate is resolving a module dependency, then
            // flatten the required packages if they are re-exported.
            if (ps.m_capability.getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                // Determine if required packages are re-exported.
                boolean reexport = false;
                R4Directive[] dirs =  ((Requirement) cs.m_requirement).getDirectives();
                for (int dirIdx = 0;
                    !reexport && (dirs != null) && (dirIdx < dirs.length); dirIdx++)
                {
                    if (dirs[dirIdx].getName().equals(Constants.VISIBILITY_DIRECTIVE)
                        && dirs[dirIdx].getValue().equals(Constants.VISIBILITY_REEXPORT))
                    {
                        reexport = true;
                    }
                }

                // Recursively calculate the required packages for the
                // current candidate.
                Map requiredMap = calculateExportedAndReexportedPackages(ps, candidatesMap, cycleMap);

                // Merge the candidate's exported and required packages
                // into the complete set of required packages.
                for (Iterator reqIter = requiredMap.entrySet().iterator(); reqIter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) reqIter.next();
                    String pkgName = (String) entry.getKey();

                    // Merge the current set of required packages into
                    // the overall complete set of required packages.
                    // We calculate all the required packages, because
                    // despite the fact that some packages will be required
                    // "privately" and some will be required "reexport", any
                    // re-exported package sources will ultimately need to
                    // be combined with privately required package sources,
                    // if the required packages overlap. This is one of the
                    // bad things about require-bundle behavior, it does not
                    // necessarily obey the visibility rules declared in the
                    // dependency.
                    ResolvedPackage rp = (ResolvedPackage) allRequiredMap.get(pkgName);
                    if (rp != null)
                    {
                        // Create the union of all package sources.
                        ResolvedPackage rpReq = (ResolvedPackage) entry.getValue();
                        rp.merge(rpReq);
                    }
                    else
                    {
                        // Add package to required map.
                        allRequiredMap.put(pkgName, entry.getValue());
                    }

                    // Keep track of all required packages to be re-exported.
                    // All re-exported packages will need to be merged into the
                    // target module's package map and become part of its overall
                    // export signature.
                    if (reexport)
                    {
                        reexportedPkgMap.put(pkgName, pkgName);
                    }
                }
            }
        }

        // For the target module we have now calculated its entire set
        // of required packages and their associated package sources in
        // allRequiredMap and have calculated all packages to be re-exported
        // in reexportedPkgMap. Add all re-exported required packages to the
        // target module's package map since they will be part of its export
        // signature.
        for (Iterator iter = reexportedPkgMap.entrySet().iterator(); iter.hasNext(); )
        {
            String pkgName = (String) ((Map.Entry) iter.next()).getKey();
            pkgMap.put(pkgName, allRequiredMap.get(pkgName));
        }

        // Now loop through the target module's export package capabilities and
        // add the target module as a package source for any exported packages.
        ICapability[] candCaps = psTarget.m_module.getCapabilities();
        for (int capIdx = 0; (candCaps != null) && (capIdx < candCaps.length); capIdx++)
        {
            if (candCaps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    candCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName, null) : rp;
                rp.m_sourceList.add(new PackageSource(psTarget.m_module, candCaps[capIdx]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private static Map calculateExportedAndReexportedPackagesResolved(
        IModule targetModule, Map cycleMap)
    {
//System.out.println("calculateExportedAndRequiredPackagesResolved("+targetModule+")");
        Map pkgMap = new HashMap();

        if (cycleMap.get(targetModule) != null)
        {
            return pkgMap;
        }

        cycleMap.put(targetModule, targetModule);

        // Loop through all wires for the target module's module dependencies
        // and calculate the module's complete set of required packages (and
        // their associated package sources) and the complete set of required
        // packages to be re-exported.
        Map allRequiredMap = new HashMap();
        Map reexportedPkgMap = new HashMap();
        IWire[] wires = targetModule.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            // If the wire is a module dependency, then flatten it to packages.
            if (wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                // Determine if required packages are re-exported.
                boolean reexport = false;
                R4Directive[] dirs =  ((Requirement) wires[i].getRequirement()).getDirectives();
                for (int dirIdx = 0;
                    !reexport && (dirs != null) && (dirIdx < dirs.length); dirIdx++)
                {
                    if (dirs[dirIdx].getName().equals(Constants.VISIBILITY_DIRECTIVE)
                        && dirs[dirIdx].getValue().equals(Constants.VISIBILITY_REEXPORT))
                    {
                        reexport = true;
                    }
                }

                // Recursively calculate the required packages for the
                // wire's exporting module.
                Map requiredMap = calculateExportedAndReexportedPackagesResolved(wires[i].getExporter(), cycleMap);

                // Merge the wires exported and re-exported packages
                // into the complete set of required packages.
                for (Iterator reqIter = requiredMap.entrySet().iterator(); reqIter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) reqIter.next();
                    String pkgName = (String) entry.getKey();

                    // Merge the current set of required packages into
                    // the overall complete set of required packages.
                    // We calculate all the required packages, because
                    // despite the fact that some packages will be required
                    // "privately" and some will be required "reexport", any
                    // re-exported package sources will ultimately need to
                    // be combined with privately required package sources,
                    // if the required packages overlap. This is one of the
                    // bad things about require-bundle behavior, it does not
                    // necessarily obey the visibility rules declared in the
                    // dependency.
                    ResolvedPackage rp = (ResolvedPackage) allRequiredMap.get(pkgName);
                    if (rp != null)
                    {
                        // Create the union of all package sources.
                        ResolvedPackage rpReq = (ResolvedPackage) entry.getValue();
                        rp.merge(rpReq);
                    }
                    else
                    {
                        // Add package to required map.
                        allRequiredMap.put(pkgName, entry.getValue());
                    }

                    // Keep track of all required packages to be re-exported.
                    // All re-exported packages will need to be merged into the
                    // target module's package map and become part of its overall
                    // export signature.
                    if (reexport)
                    {
                        reexportedPkgMap.put(pkgName, pkgName);
                    }
                }
            }
        }

        // For the target module we have now calculated its entire set
        // of required packages and their associated package sources in
        // allRequiredMap and have calculated all packages to be re-exported
        // in reexportedPkgMap. Add all re-exported required packages to the
        // target module's package map since they will be part of its export
        // signature.
        for (Iterator iter = reexportedPkgMap.entrySet().iterator(); iter.hasNext(); )
        {
            String pkgName = (String) ((Map.Entry) iter.next()).getKey();
            pkgMap.put(pkgName, allRequiredMap.get(pkgName));
        }

        // Now loop through the target module's export package capabilities and
        // add the target module as a package source for any exported packages.
        ICapability[] caps = targetModule.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName, null) : rp;
                rp.m_sourceList.add(new PackageSource(targetModule, caps[i]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private static Map calculateCandidateRequiredPackages(IModule module, PackageSource psTarget, Map candidatesMap)
    {
//System.out.println("calculateCandidateRequiredPackages("+module+")");
        Map cycleMap = new HashMap();
        cycleMap.put(module, module);
        return calculateExportedAndReexportedPackages(psTarget, candidatesMap, cycleMap);
    }

    private static void incrementCandidateConfiguration(List resolverList)
        throws ResolveException
    {
        for (int i = 0; i < resolverList.size(); i++)
        {
            List candSetList = (List) resolverList.get(i);
            for (int j = 0; j < candSetList.size(); j++)
            {
                CandidateSet cs = (CandidateSet) candSetList.get(j);
                // See if we can increment the candidate set, without overflowing
                // the candidate array bounds.
                if ((cs.m_idx + 1) < cs.m_candidates.length)
                {
                    cs.m_idx++;
                    return;
                }
                // If the index will overflow the candidate array bounds,
                // then set the index back to zero and try to increment
                // the next candidate.
                else
                {
                    cs.m_idx = 0;
                }
            }
        }
        throw new ResolveException(
            "Unable to resolve due to constraint violation.", null, null);
    }

    private static Map populateWireMap(
        ResolverState state, Map candidatesMap, IModule importer, Map wireMap)
    {
        // If the module is already resolved or it is part of
        // a cycle, then just return the wire map.
        if (importer.isResolved() || (wireMap.get(importer) != null))
        {
            return wireMap;
        }

        // Get the candidate set list for the importer.
        List candSetList = (List) candidatesMap.get(importer);

        // Check if the importer is a fragment, if so we actually want
        // to get the host and add all fragments candidates to it.
        if (Util.isFragment(importer))
        {
            // Get host candidate for the fragment.
            IModule host = null;
            for (int csIdx = 0; (host == null) && (csIdx < candSetList.size()); csIdx++)
            {
                CandidateSet cs = (CandidateSet) candSetList.get(csIdx);
                if (cs.m_type == CandidateSet.HOST)
                {
                    host = cs.m_modules[cs.m_idx];
                }
            }
            // Instead of creating wires for the fragment, we will create them
            // for the host.
            importer = host;

            // Make sure we haven't already seen the host to avoid a cycle.
            if (wireMap.get(importer) != null)
            {
                return wireMap;
            }

            // Now add the fragments candidates to the host.
            candSetList = (List) candidatesMap.get(host);
            for (int csIdx = 0; (host == null) && (csIdx < candSetList.size()); csIdx++)
            {
                CandidateSet cs = (CandidateSet) candSetList.get(csIdx);
                if (cs.m_type == CandidateSet.FRAGMENT)
                {
                    // Add the candidate fragment module to the cycle map
                    // with no wires, since only its host will have wires.
                    wireMap.put(cs.m_modules[cs.m_idx], new IWire[0]);

                    // Get the candidate fragment and loop through its candidates
                    // for resolving its dependencies.
                    List fragmentCandSetList = (List) candidatesMap.get(cs.m_modules[cs.m_idx]);
                    for (int csFragIdx = 0; csFragIdx < fragmentCandSetList.size(); csFragIdx++)
                    {
                        // Add all non-HOST candidates to the host candidate set,
                        // since the resulting wires will be for the host not
                        // the fragment.
                        CandidateSet csFrag = (CandidateSet) fragmentCandSetList.get(csFragIdx);
                        if (csFrag.m_type != CandidateSet.HOST)
                        {
                            candSetList.add(csFrag);
                        }
                    }
                }
            }
        }

        List moduleWires = new ArrayList();
        List packageWires = new ArrayList();

        // Put the module in the wireMap with an empty wire array;
        // we do this early so we can use it to detect cycles.
        wireMap.put(importer, m_emptyWires);

        // Loop through each candidate Set and create a wire
        // for the selected candidate for the associated import.
        for (int candSetIdx = 0; candSetIdx < candSetList.size(); candSetIdx++)
        {
            // Get the current candidate set.
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);

            // Create a wire for the current candidate based on the type
            // of requirement it resolves.
            if (cs.m_type == CandidateSet.FRAGMENT)
            {
                moduleWires.add(new R4WireFragment(importer, cs.m_modules[cs.m_idx]));
            }
            else
            {
                // Create a module wire for module dependencies.
                if (cs.m_requirement.getNamespace().equals(ICapability.MODULE_NAMESPACE))
                {
                    moduleWires.add(new R4WireModule(
                        importer,
                        cs.m_requirement,
                        cs.m_candidates[cs.m_idx].m_module,
                        cs.m_candidates[cs.m_idx].m_capability,
                        calculateCandidateRequiredPackages(importer, cs.m_candidates[cs.m_idx], candidatesMap)));
                }
                // Create a package wire for package dependencies.
                // Filter out the case where a module imports from
                // itself, since the module should simply load from
                // its internal class path in this case.
                else if (importer != cs.m_candidates[cs.m_idx].m_module)
                {
                    // Add wire for imported package.
                    packageWires.add(new R4Wire(
                        importer,
                        cs.m_requirement,
                        cs.m_candidates[cs.m_idx].m_module,
                        cs.m_candidates[cs.m_idx].m_capability));
                }

                // Create any necessary wires for the selected candidate module.
                wireMap = populateWireMap(
                    state, candidatesMap, cs.m_candidates[cs.m_idx].m_module, wireMap);
            }
        }

        packageWires.addAll(moduleWires);
        wireMap.put(importer, packageWires.toArray(new IWire[packageWires.size()]));

        return wireMap;
    }

    //
    // Utility methods.
    //

    private static IModule[] shrinkModuleArray(IModule[] modules)
    {
        if (modules == null)
        {
            return m_emptyModules;
        }

        // Move all non-null values to one end of the array.
        int lower = 0;
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i] != null)
            {
                modules[lower++] = modules[i];
            }
        }

        if (lower == 0)
        {
            return m_emptyModules;
        }

        // Copy non-null values into a new array and return.
        IModule[] newModules = new IModule[lower];
        System.arraycopy(modules, 0, newModules, 0, lower);
        return newModules;
    }

    private static PackageSource[] shrinkCandidateArray(PackageSource[] candidates)
    {
        if (candidates == null)
        {
            return m_emptySources;
        }

        // Move all non-null values to one end of the array.
        int lower = 0;
        for (int i = 0; i < candidates.length; i++)
        {
            if (candidates[i] != null)
            {
                candidates[lower++] = candidates[i];
            }
        }

        if (lower == 0)
        {
            return m_emptySources;
        }

        // Copy non-null values into a new array and return.
        PackageSource[] newCandidates= new PackageSource[lower];
        System.arraycopy(candidates, 0, newCandidates, 0, lower);
        return newCandidates;
    }

    //
    // Inner classes.
    //

    public static interface ResolverState
    {
        IModule[] getModules();
        Map getPotentialFragments(IModule module);
        List getPotentialHosts(IModule module);
        PackageSource[] getResolvedCandidates(IRequirement req);
        PackageSource[] getUnresolvedCandidates(IRequirement req);
    }
}