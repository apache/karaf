/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.searchpolicy;

import java.net.URL;
import java.util.*;

import org.apache.felix.framework.LogWrapper;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.moduleloader.*;
import org.apache.felix.moduleloader.search.ResolveException;
import org.apache.felix.moduleloader.search.ResolveListener;

public class R4SearchPolicy implements SearchPolicy, ModuleListener
{
    // Array of R4Package.
    public static final String EXPORTS_ATTR = "exports";
    // Array of R4Package.
    public static final String IMPORTS_ATTR = "imports";
    // Array of R4Package.
    public static final String DYNAMICIMPORTS_ATTR = "dynamicimports";
    // Array of R4Wire.
    public static final String WIRING_ATTR = "wiring";
    // Boolean.
    public static final String RESOLVED_ATTR = "resolved";

    private LogWrapper m_logger = null;
    private ModuleManager m_mgr = null;
    private Map m_availPkgMap = new HashMap();
    private Map m_inUsePkgMap = new HashMap();

    // Listener-related instance variables.
    private static final ResolveListener[] m_emptyListeners = new ResolveListener[0];
    private ResolveListener[] m_listeners = m_emptyListeners;

    // Reusable empty arrays.
    public static final Module[] m_emptyModules = new Module[0];
    public static final R4Package[] m_emptyPackages = new R4Package[0];
    public static final R4Wire[] m_emptyWires = new R4Wire[0];

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    public R4SearchPolicy(LogWrapper logger)
    {
        m_logger = logger;
    }

    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException
    {
        if (m_mgr == null)
        {
            m_mgr = mgr;
            m_mgr.addModuleListener(this);
        }
        else
        {
            throw new IllegalStateException("Module manager is already initialized");
        }
    }

    public Object[] definePackage(Module module, String pkgName)
    {
        R4Package pkg = R4SearchPolicy.getExportPackage(module, pkgName);
        if (pkg != null)
        {
            return new Object[]  {
                pkgName, // Spec title.
                pkg.getVersionLow().toString(), // Spec version.
                "", // Spec vendor.
                "", // Impl title.
                "", // Impl version.
                ""  // Impl vendor.
            };
        }
        return null;
    }

    public Class findClassBeforeModule(ClassLoader parent, Module module, String name)
        throws ClassNotFoundException
    {
        // First, try to resolve the originating module.
        try
        {
            resolve(module);
        }
        catch (ResolveException ex)
        {
            // We do not use the resolve exception as the
            // cause of the exception, since this would
            // potentially leak internal module information.
            throw new ClassNotFoundException(
                name + ": cannot resolve package "
                + ex.getPackage() + ".");
        }

        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        // Load all "java.*" classes from parent class loader;
        // these packages cannot be provided by other bundles.
        if (pkgName.startsWith("java."))
        {
            return (parent == null) ? null : parent.loadClass(name);
        }

        // We delegate to the module's wires to find the class.
        R4Wire[] wires = getWiringAttr(module);
        for (int i = 0; i < wires.length; i++)
        {
            // Only check when the package of the class is
            // the same as the import package.
            if (wires[i].m_pkg.getId().equals(pkgName))
            {
                // Before delegating to the module class loader to satisfy
                // the class load, we must check the include/exclude filters
                // from the target package to make sure that the class is
                // actually visible. If the exporting module is the same as
                // the requesting module, then filtering is not performed
                // since a module has complete access to itself.
// TODO: Determine if it is possible to modify Module Loader somehow
// so that this check is done within the target module itself; it
// doesn't totally make sense to do this check in the importing module.
                if (wires[i].m_module != module)
                {
                    if (!wires[i].m_pkg.isIncluded(name))
                    {
                        throw new ClassNotFoundException(name);
                    }
                }

                // Since the class is included, delegate to the exporting module.
                try
                {
                    Class clazz = wires[i].m_module.getClassLoader().loadClassFromModule(name);
                    if (clazz != null)
                    {
                        return clazz;
                    }
                }
                catch (Throwable th)
                {
                    // Not much we can do here.
                }
                throw new ClassNotFoundException(name);
            }
        }

        return null;
    }

    public Class findClassAfterModule(ClassLoader parent, Module module, String name)
        throws ClassNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's own resources. Now we make an attempt to load the
        // class via a dynamic import, if possible.
        String pkgName = Util.getClassPackage(name);
        Module candidate = attemptDynamicImport(module, pkgName);
        // If the dynamic import was successful, then this initial
        // time we must directly return the result from dynamically
        // selected candidate's class loader, but for subsequent
        // requests for classes in the associated package will be
        // processed as part of normal static imports.
        if (candidate != null)
        {
            return candidate.getClassLoader().loadClass(name);
        }

        // At this point, the class could not be found by the bundle's static
        // or dynamic imports, nor its own resources. Before we throw
        // an exception, we will try to determine if the instigator of the
        // class load was a class from a bundle or not. This is necessary
        // because the specification mandates that classes on the class path
        // should be hidden (except for java.*), but it does allow for these
        // classes to be exposed by the system bundle as an export. However,
        // in some situations classes on the class path make the faulty
        // assumption that they can access everything on the class path from
        // every other class loader that they come in contact with. This is
        // not true if the class loader in question is from a bundle. Thus,
        // this code tries to detect that situation. If the class
        // instigating the class load was NOT from a bundle, then we will
        // make the assumption that the caller actually wanted to use the
        // parent class loader and we will delegate to it. If the class was
        // from a bundle, then we will enforce strict class loading rules
        // for the bundle and throw a class not found exception.

        // Get the class context to see the classes on the stack.
        Class[] classes = m_sm.getClassContext();
        // Start from 1 to skip inner class.
        for (int i = 1; i < classes.length; i++)
        {
            // Find the first class on the call stack that is neither
            // a class loader or Class itself, because we want to ignore
            // the calls to ClassLoader.loadClass() and Class.forName().
            if (!ClassLoader.class.isAssignableFrom(classes[i]) &&
                !Class.class.isAssignableFrom(classes[i]))
            {
                // If the instigating class was not from a bundle, then
                // delegate to the parent class loader. Otherwise, break
                // out of loop and throw an exception.
                if (!ModuleClassLoader.class.isInstance(classes[i].getClassLoader()))
                {
                    return parent.loadClass(name);
                }
                break;
            }
        }
        
        throw new ClassNotFoundException(name);
    }

    public URL findResource(ClassLoader parent, Module module, String name)
        throws ResourceNotFoundException
    {
        // First, try to resolve the originating module.
        try
        {
            resolve(module);
        }
        catch (ResolveException ex)
        {
            return null;
        }

        // Get the package of the target resource.
        String pkgName = Util.getResourcePackage(name);

        // Load all "java.*" resources from parent class loader;
        // these packages cannot be provided by other bundles.
        if (pkgName.startsWith("java."))
        {
            return (parent == null) ? null : parent.getResource(name);
        }

        // We delegate to the module's wires to find the resource.
        R4Wire[] wires = getWiringAttr(module);
        for (int i = 0; i < wires.length; i++)
        {
            // Only check when the package of the resource is
            // the same as the import package.
            if (wires[i].m_pkg.getId().equals(pkgName))
            {
                try
                {
                    URL url = wires[i].m_module.getClassLoader().getResourceFromModule(name);
                    if (url != null)
                    {
                        return url;
                    }
                }
                catch (Throwable th)
                {
                    // Not much we can do here.
                }
                throw new ResourceNotFoundException(name);
            }
        }

        // Check dynamic imports.
// TODO: Dynamic imports should be searched after local sources.
        Module candidate = attemptDynamicImport(module, pkgName);
        // This initial time we must directly return the result from
        // the candidate's class loaders, but since the candidate was
        // added to the module's wiring attribute, subsequent class
        // loads from the same package will be handled in the normal
        // fashion for statically imported packaes.
        return (candidate == null)
            ? null : candidate.getClassLoader().getResource(name);
    }

    private Module attemptDynamicImport(Module module, String pkgName)
    {
        Module candidate = null;

        // There is an overriding assumption here that a package is
        // never split across bundles. If a package can be split
        // across bundles, then this will fail.

        try
        {
            // Check the dynamic import specs for a match of
            // the target package.
            R4Package[] dynamics = getDynamicImportsAttr(module);
            R4Package pkgMatch = null;
            for (int i = 0; (pkgMatch == null) && (i < dynamics.length); i++)
            {
                // Star matches everything.
                if (dynamics[i].getId().equals("*"))
                {
                    // Create a package instance without wildcard.
                    pkgMatch = new R4Package(
                        pkgName,
                        dynamics[i].getDirectives(),
                        dynamics[i].getAttributes());
                }
                // Packages ending in ".*" must match starting strings.
                else if (dynamics[i].getId().endsWith(".*"))
                {
                    if (pkgName.regionMatches(
                        0, dynamics[i].getId(), 0, dynamics[i].getId().length() - 2))
                    {
                        // Create a package instance without wildcard.
                        pkgMatch = new R4Package(
                            pkgName,
                            dynamics[i].getDirectives(),
                            dynamics[i].getAttributes());
                    }
                }
                // Or we can have a precise match.
                else
                {
                    if (pkgName.equals(dynamics[i].getId()))
                    {
                        pkgMatch = dynamics[i];
                    }
                }
            }

            // If the target package does not match any dynamically imported
            // packages or if the module is already wired for the target package,
            // then just return null. The module may be already wired to the target
            // package if the class being searched for does not actually exist.
            if ((pkgMatch == null) || (getWire(module, pkgMatch.getId()) != null))
            {
                return null;
            }

            // At this point, the target package has matched a dynamically
            // imported package spec. Now we must try to find a candidate
            // exporter for target package and add it to the module's set
            // of wires.

            // Lock module manager instance to ensure that nothing changes.
            synchronized (m_mgr)
            {
                // Try to add a new entry to the module's import attribute.
                // Select the first candidate that successfully resolves.

                // First check already resolved exports for a match.
                Module[] candidates = getCompatibleExporters(
                    (Module[]) m_inUsePkgMap.get(pkgMatch.getId()), pkgMatch);
                // If there is an "in use" candidate, just take the first one.
                if (candidates.length > 0)
                {
                    candidate = candidates[0];
                }

                // If there were no "in use" candidates, then try "available"
                // candidates.
                if (candidate == null)
                {
                    candidates = getCompatibleExporters(
                        (Module[]) m_availPkgMap.get(pkgMatch.getId()), pkgMatch);
                    for (int candIdx = 0;
                        (candidate == null) && (candIdx < candidates.length);
                        candIdx++)
                    {
                        try
                        {
                            resolve(module);
                            candidate = candidates[candIdx];
                        }
                        catch (ResolveException ex)
                        {
                        }
                    }
                }

                // If we found a candidate, then add it to the module's
                // wiring attribute.
                if (candidate != null)
                {
                    R4Wire[] wires = getWiringAttr(module);
                    R4Wire[] newWires = new R4Wire[wires.length + 1];
                    System.arraycopy(wires, 0, newWires, 0, wires.length);
                    // Find the candidate's export package object and
                    // use that for creating the wire; this is necessary
                    // since it contains "uses" dependency information.
                    newWires[wires.length] = new R4Wire(
                        getExportPackage(candidate, pkgMatch.getId()), candidate);
                    module.setAttribute(WIRING_ATTR, newWires);
m_logger.log(LogWrapper.LOG_DEBUG, "WIRE: [" + module + "] " + newWires[wires.length]);
                }
            }
        }
        catch (Exception ex)
        {
            m_logger.log(LogWrapper.LOG_ERROR, "Unable to dynamically import package.", ex);
        }

        return candidate;
    }

    public Module[] getAvailableExporters(R4Package pkg)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_mgr)
        {
            return getCompatibleExporters((Module[]) m_availPkgMap.get(pkg.getId()), pkg);
        }
    }

    public Module[] getInUseExporters(R4Package pkg)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_mgr)
        {
            return getCompatibleExporters((Module[]) m_inUsePkgMap.get(pkg.getId()), pkg);
        }
    }

    public void resolve(Module rootModule)
        throws ResolveException
    {
        // If the module is already resolved, then we can just return.
        if (getResolvedAttr(rootModule).booleanValue())
        {
            return;
        }

        // This variable maps an unresolved module to a list of resolver
        // nodes, where there is one resolver node for each import that
        // must be resolved. A resolver node contains the potential
        // candidates to resolve the import and the current selected
        // candidate index.
        Map resolverMap = new HashMap();

        // This map will be used to hold the final wires for all
        // resolved modules, which can then be used to fire resolved
        // events outside of the synchronized block.
        Map resolvedModuleWireMap = null;
    
        // Synchronize on the module manager, because we don't want
        // any modules being added or removed while we are in the
        // middle of this operation.
        synchronized (m_mgr)
        {
            // The first step is to populate the resolver map. This
            // will use the target module to populate the resolver map
            // with all potential modules that need to be resolved as a
            // result of resolving the target module. The key of the
            // map is a potential module to be resolved and the value is
            // a list of resolver nodes, one for each of the module's
            // imports, where each resolver node contains the potential
            // candidates for resolving the import. Not all modules in
            // this map will be resolved, only the target module and
            // any candidates selected to resolve its imports and the
            // transitive imports this implies.
            populateResolverMap(resolverMap, rootModule);

//dumpResolverMap();

            // The next step is to use the resolver map to determine if
            // the class space for the root module is consistent. This
            // is an iterative process that transitively walks the "uses"
            // relationships of all currently selected potential candidates
            // for resolving import packages checking for conflicts. If a
            // conflict is found, it "increments" the configuration of
            // currently selected potential candidates and tests them again.
            // If this method returns, then it has found a consistent set
            // of candidates; otherwise, a resolve exception is thrown if
            // it exhausts all possible combinations and could not find a
            // consistent class space.
            findConsistentClassSpace(resolverMap, rootModule);

            // The final step is to create the wires for the root module and
            // transitively all modules that are to be resolved from the
            // selected candidates for resolving the root module's imports.
            // When this call returns, each module's wiring and resolved
            // attributes are set. The resulting wiring map is used below
            // to fire resolved events outside of the synchronized block.
            // The resolved module wire map maps a module to its array of
            // wires.
            resolvedModuleWireMap = createWires(resolverMap, rootModule);
            
//dumpAvailablePackages();
//dumpUsedPackages();

        } // End of synchronized block on module manager.

        // Fire resolved events for all resolved modules;
        // the resolved modules array will only be set if the resolve
        // was successful after the root module was resolved.
        if (resolvedModuleWireMap != null)
        {
            Iterator iter = resolvedModuleWireMap.entrySet().iterator();
            while (iter.hasNext())
            {
                fireModuleResolved((Module) ((Map.Entry) iter.next()).getKey());
            }
        }
    }

    private void populateResolverMap(Map resolverMap, Module module)
        throws ResolveException
    {
        // Detect cycles.
        if (resolverMap.get(module) != null)
        {
            return;
        }

        // Map to hold the bundle's import packages
        // and their respective resolving candidates.
        List nodeList = new ArrayList();

        // Even though the node list is currently emptry, we
        // record it in the resolver map early so we can use
        // it to detect cycles.
        resolverMap.put(module, nodeList);

        // Loop through each import and calculate its resolving
        // set of candidates.
        R4Package[] imports = getImportsAttr(module);
        for (int impIdx = 0; impIdx < imports.length; impIdx++)
        {
            // Get the candidates from the "in use" and "available"
            // package maps. Candidates "in use" have higher priority
            // than "available" ones, so put the "in use" candidates
            // at the front of the list of candidates.
            Module[] inuse = getCompatibleExporters(
                (Module[]) m_inUsePkgMap.get(
                    imports[impIdx].getId()), imports[impIdx]);
            Module[] available = getCompatibleExporters(
                (Module[]) m_availPkgMap.get(
                    imports[impIdx].getId()), imports[impIdx]);
            Module[] candidates = new Module[inuse.length + available.length];
            System.arraycopy(inuse, 0, candidates, 0, inuse.length);
            System.arraycopy(available, 0, candidates, inuse.length, available.length);

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
                        if (!getResolvedAttr(candidates[candIdx]).booleanValue())
                        {
                            populateResolverMap(resolverMap, candidates[candIdx]);
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
                candidates = shrinkModuleArray(candidates);
            }

            // If no candidates exist at this point, then throw a
            // resolve exception unless the import is optional.
            if ((candidates.length == 0) && !imports[impIdx].isOptional())
            {
                // If we have received an exception while trying to populate
                // the resolver map, rethrow that exception since it might
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
                        "Unable to resolve.", module, imports[impIdx]);
                }
            }
            else if (candidates.length > 0)
            {
                nodeList.add(
                    new ResolverNode(module, imports[impIdx], candidates));
            }
        }
    }

    /**
     * <p>
     * This method searches the resolver solution space for a consistent
     * set of modules to resolve all transitive imports that must be resolved
     * as a result of resolving the root module. A consistent set of modules
     * is one where the "uses" relationships of the exported packages for
     * the selected provider modules do not conflict with each other. A
     * conflict can occur when the constraints on two or more modules result
     * in multiple providers for the same package in the same class space.
     * </p>
     * @param resolverMap a map containing all potential modules that may need
     *        to be resolved and the candidates to resolve them.
     * @param rootModule the module that is the root of the resolve operation.
     * @throws ResolveException if no consistent set of candidates can be
     *         found to resolve the root module.
    **/
    private void findConsistentClassSpace(Map resolverMap, Module rootModule)
        throws ResolveException
    {
        List resolverList = null;

        // Test the current set of candidates to determine if they
        // are consistent. Keep looping until we find a consistent
        // set or an exception is thrown.
        Map cycleMap = new HashMap();
        while (!isClassSpaceConsistent(resolverMap, rootModule, cycleMap))
        {
m_logger.log(
    LogWrapper.LOG_DEBUG,
    "Constraint violation detected, will try to repair.");

            // The incrementCandidateConfiguration() method requires a
            // ordered access to the resolver map, so we will create
            // a reusable list once right here.
            if (resolverList == null)
            {
                resolverList = new ArrayList();
                for (Iterator iter = resolverMap.entrySet().iterator();
                    iter.hasNext(); )
                {
                    resolverList.add((List) ((Map.Entry) iter.next()).getValue());
                }
            }

            // Increment the candidate configuration so we can test again.
            incrementCandidateConfiguration(resolverList);

            // Clear the cycle map.
            cycleMap.clear();
        }
    }

    private boolean isClassSpaceConsistent(
        Map resolverMap, Module rootModule, Map cycleMap)
    {
        // We do not need to verify that already resolved modules
        // have consistent class spaces because they should be
        // consistent by definition. Also, if the root module is
        // part of a cycle, then just assume it is true.
        if (getResolvedAttr(rootModule).booleanValue() ||
            (cycleMap.get(rootModule) != null))
        {
            return true;
        }

        // Add to cycle map for future reference.
        cycleMap.put(rootModule, rootModule);

        // Create an implicit "uses" constraint for every exported package
        // of the root module that is not also imported; uses constraints
        // for exported packages that are also imported will be taken
        // care of as part of normal import package processing.
        R4Package[] exports = (R4Package[]) getExportsAttr(rootModule);
        Map usesMap = new HashMap();
        for (int i = 0; i < exports.length; i++)
        {
            // Ignore exports that are also imported, since they
            // will be taken care of when verifying import constraints.
            if (getImportPackage(rootModule, exports[i].getId()) == null)
            {
                usesMap.put(exports[i].getId(), rootModule);
            }
        }

        // Loop through the current candidates for the module's imports
        // (available in the resolver node list of the resolver map) and
        // calculate the uses constraints for each of the currently
        // selected candidates for resolving the imports. Compare each
        // candidate's constraints to the existing constraints to check
        // for conflicts.
        List nodeList = (List) resolverMap.get(rootModule);
        for (int nodeIdx = 0; nodeIdx < nodeList.size(); nodeIdx++)
        {
            // Verify that the current candidate does not violate
            // any "uses" constraints of existing candidates by
            // calculating the candidate's transitive "uses" constraints
            // for the provided package and testing whether they
            // overlap with existing constraints.

            // First, get the resolver node.
            ResolverNode node = (ResolverNode) nodeList.get(nodeIdx);

            // Verify that the current candidate itself has a consistent
            // class space.
            if (!isClassSpaceConsistent(
                resolverMap, node.m_candidates[node.m_idx], cycleMap))
            {
                return false;
            }

            // Get the exported package from the current candidate that
            // will be used to resolve the root module's import.
            R4Package candidatePkg = getExportPackage(
                node.m_candidates[node.m_idx], node.m_pkg.getId());

            // Calculate the "uses" dependencies implied by the candidate's
            // exported package with respect to the currently selected
            // candidates in the resolver map.
            Map candUsesMap = calculateUsesDependencies(
                resolverMap,
                node.m_candidates[node.m_idx],
                candidatePkg,
                new HashMap());
//System.out.println("MODULE " + rootModule + " USES    " + usesMap);
//System.out.println("CANDIDATE " + node.m_candidates[node.m_idx] + " USES " + candUsesMap);

            // Iterate through the root module's current set of transitive
            // "uses" constraints and compare them with the candidate's
            // transitive set of constraints.
            Iterator usesIter = candUsesMap.entrySet().iterator();
            while (usesIter.hasNext())
            {
                // If the candidate's uses constraints overlap with
                // the existing uses constraints, but refer to a
                // different provider, then the class space is not
                // consistent; thus, return false.
                Map.Entry entry = (Map.Entry) usesIter.next();
                if ((usesMap.get(entry.getKey()) != null) &&
                    (usesMap.get(entry.getKey()) != entry.getValue()))
                {
                    return false;
                }
            }

            // Since the current candidate's uses constraints did not
            // conflict with existing constraints, merge all constraints
            // and keep testing the remaining candidates for the other
            // imports of the root module.
            usesMap.putAll(candUsesMap);
        }

        return true;
    }

    private Map calculateUsesDependencies(
        Map resolverMap, Module module, R4Package exportPkg, Map usesMap)
    {
// TODO: CAN THIS BE OPTIMIZED?
// TODO: IS THIS CYCLE CHECK CORRECT??
// TODO: WHAT HAPPENS THERE ARE OVERLAPS WHEN CALCULATING USES??
//       MAKE AN EXAMPLE WHERE TWO DEPENDENCIES PROVIDE SAME PACKAGE.
        // Make sure we are not in a cycle.
        if (usesMap.get(exportPkg.getId()) != null)
        {
            return usesMap;
        }

        // The target package at least uses itself,
        // so add it to the uses map.
        usesMap.put(exportPkg.getId(), module);

        // Get the "uses" constraints for the target export
        // package and calculate the transitive uses constraints
        // of any used packages.
        String[] uses = exportPkg.getUses();
        List nodeList = (List) resolverMap.get(module);

        // We need to walk the transitive closure of "uses" relationships
        // for the current export package to calculate the entire set of
        // "uses" constraints.
        for (int usesIdx = 0; usesIdx < uses.length; usesIdx++)
        {
            // There are two possibilities at this point: 1) we are dealing
            // with an already resolved bundle or 2) we are dealing with a
            // bundle that has not yet been resolved. In case 1, there will
            // be no resolver node in the resolver map, so we just need to
            // examine the bundle directly to determine its exact constraints.
            // In case 2, there will be a resolver node in the resolver map,
            // so we will use that to determine the potential constraints of
            // potential candidate for resolving the import.

            // This is case 1, described in the comment above.
            if (nodeList == null)
            {
                // Get the actual exporter from the wire or if there
                // is no wire, then get the export is from the module
                // itself.
                R4Wire wire = getWire(module, uses[usesIdx]);
                if (wire != null)
                {
                    usesMap = calculateUsesDependencies(
                        resolverMap, wire.m_module, wire.m_pkg, usesMap);
                }
                else
                {
                    exportPkg = getExportPackage(module, uses[usesIdx]);
                    if (exportPkg != null)
                    {
                        usesMap = calculateUsesDependencies(
                            resolverMap, module, exportPkg, usesMap);
                    }
                }
            }
            // This is case 2, described in the comment above.
            else
            {
                // First, get the resolver node for the "used" package.
                ResolverNode node = null;
                for (int nodeIdx = 0;
                    (node == null) && (nodeIdx < nodeList.size());
                    nodeIdx++)
                {
                    node = (ResolverNode) nodeList.get(nodeIdx);
                    if (!node.m_pkg.getId().equals(uses[usesIdx]))
                    {
                        node = null;
                    }
                }
    
                // If there is a resolver node for the "used" package,
                // then this means that the module imports the package
                // and we need to recursively add the constraints of
                // the potential exporting module.
                if (node != null)
                {
                    usesMap = calculateUsesDependencies(
                        resolverMap,
                        node.m_candidates[node.m_idx],
                        getExportPackage(node.m_candidates[node.m_idx], node.m_pkg.getId()),
                        usesMap);
                }
                // If there was no resolver node for the "used" package,
                // then this means that the module exports the package
                // and we need to recursively add the constraints of this
                // other exported package of this module.
                else if (getExportPackage(module, uses[usesIdx]) != null)
                {
                    usesMap = calculateUsesDependencies(
                        resolverMap,
                        module,
                        getExportPackage(module, uses[usesIdx]),
                        usesMap);
                }
            }
        }

        return usesMap;
    }

    /**
     * <p>
     * This method <i>increments</i> the current candidate configuration
     * in the specified resolver list, which contains resolver node lists
     * for all of the candidates for all of the imports that need to be
     * resolved. This method performs its function by treating the current
     * candidate index variable in each resolver node as part of a big
     * counter. In other words, it increments the least significant index.
     * If the index overflows it sets it back to zero and carries the
     * overflow to the next significant index and so on. Using this approach
     * it checks every possible combination for a solution.
     * </p>
     * <p>
     * This method is inefficient and a better approach is necessary. For
     * example, it does not take into account which imports are actually
     * being used, it just increments starting at the beginning of the list.
     * This means that it could be modifying candidates that are not relevant
     * to the current configuration and re-testing even though nothing has
     * really changed. It needs to be smarter.
     * </p>
     * @param resolverList an ordered list of resolver node lists for all
     *        the candidates of the potential imports that need to be
     *        resolved.
     * @throws ResolveException if the increment overflows the entire list,
     *         signifying no consistent configurations exist.
    **/
    private void incrementCandidateConfiguration(List resolverList)
        throws ResolveException
    {
        for (int i = 0; i < resolverList.size(); i++)
        {
            List nodeList = (List) resolverList.get(i);
            for (int j = 0; j < nodeList.size(); j++)
            {
                ResolverNode node = (ResolverNode) nodeList.get(j);
                // See if we can increment the node, without overflowing
                // the candidate array bounds.
                if ((node.m_idx + 1) < node.m_candidates.length)
                {
                    node.m_idx++;
                    return;
                }
                // If the index will overflow the candidate array bounds,
                // then set the index back to zero and try to increment
                // the next candidate.
                else
                {
                    node.m_idx = 0;
                }
            }
        }
        throw new ResolveException(
            "Unable to resolve due to constraint violation.", null, null);
    }

    private Map createWires(Map resolverMap, Module rootModule)
    {
        Map resolvedModuleWireMap =
            populateWireMap(resolverMap, rootModule, new HashMap());
        Iterator iter = resolvedModuleWireMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            Module module = (Module) entry.getKey();
            R4Wire[] wires = (R4Wire[]) entry.getValue();

            // Set the module's resolved and wiring attribute.
            module.setAttribute(RESOLVED_ATTR, Boolean.TRUE);
            // Only add wires attribute if some exist; export
            // only modules may not have wires.
            if (wires.length > 0)
            {
                module.setAttribute(WIRING_ATTR, wires);
            }

            // Remove the wire's exporting module from the "available"
            // package map and put it into the "in use" package map;
            // these steps may be a no-op.
            for (int wireIdx = 0;
                (wires != null) && (wireIdx < wires.length);
                wireIdx++)
            {
m_logger.log(LogWrapper.LOG_DEBUG, "WIRE: [" + module + "] " + wires[wireIdx]);
                // First remove the wire module from "available" package map.
                Module[] modules = (Module[]) m_availPkgMap.get(wires[wireIdx].m_pkg.getId());
                modules = removeModuleFromArray(modules, wires[wireIdx].m_module);
                m_availPkgMap.put(wires[wireIdx].m_pkg.getId(), modules);

                // Also remove any exported packages from the "available"
                // package map that are from the module associated with
                // the current wires where the exported packages were not
                // actually exported; an export may not be exported if
                // the module also imports the same package and was wired
                // to a different module. If the exported package is not
                // actually exported, then we just want to remove it
                // completely, since it cannot be used.
                if (wires[wireIdx].m_module != module)
                {
                    modules = (Module[]) m_availPkgMap.get(wires[wireIdx].m_pkg.getId());
                    modules = removeModuleFromArray(modules, module);
                    m_availPkgMap.put(wires[wireIdx].m_pkg.getId(), modules);
                }

                // Add the module of the wire to the "in use" package map.
                modules = (Module[]) m_inUsePkgMap.get(wires[wireIdx].m_pkg.getId());
                modules = addModuleToArray(modules, wires[wireIdx].m_module);
                m_inUsePkgMap.put(wires[wireIdx].m_pkg.getId(), modules);
            }
        }
        return resolvedModuleWireMap;
    }

    private Map populateWireMap(Map resolverMap, Module module, Map wireMap)
    {
        // If the module is already resolved or it is part of
        // a cycle, then just return the wire map.
        if (getResolvedAttr(module).booleanValue() ||
            (wireMap.get(module) != null))
        {
            return wireMap;
        }

        List nodeList = (List) resolverMap.get(module);
        R4Wire[] wires = new R4Wire[nodeList.size()];

        // Put the module in the wireMap with an empty wire array;
        // we do this early so we can use it to detect cycles.
        wireMap.put(module, wires);

        // Loop through each resolver node and create a wire
        // for the selected candidate for the associated import.
        for (int nodeIdx = 0; nodeIdx < nodeList.size(); nodeIdx++)
        {
            // Get the import's associated resolver node.
            ResolverNode node = (ResolverNode) nodeList.get(nodeIdx);

            // Add the candidate to the list of wires.
            R4Package exportPkg =
                getExportPackage(node.m_candidates[node.m_idx], node.m_pkg.getId());
            wires[nodeIdx] = new R4Wire(exportPkg, node.m_candidates[node.m_idx]);

            // Create the wires for the selected candidate module.
            wireMap = populateWireMap(resolverMap, node.m_candidates[node.m_idx], wireMap);
        }

        return wireMap;
    }

// TODO: REMOVE THESE DEBUG METHODS.
    private void dumpResolverMap(Map resolverMap)
    {
        Iterator iter = resolverMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            ResolverNode node = (ResolverNode) entry.getValue();
            System.out.println("MODULE " + node.m_module + " IMPORT " + node.m_pkg);
            for (int i = 0; i < node.m_candidates.length; i++)
            {
                System.out.println("--> " + node.m_candidates[i]);
            }
        }
    }

    private void dumpAvailablePackages()
    {
        synchronized (m_mgr)
        {
            System.out.println("AVAILABLE PACKAGES:");
            for (Iterator i = m_availPkgMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                System.out.println("  " + entry.getKey());
                Module[] modules = (Module[]) entry.getValue();
                for (int j = 0; j < modules.length; j++)
                {
                    System.out.println("    " + modules[j]);
                }
            }
        }
    }

    private void dumpUsedPackages()
    {
        synchronized (m_mgr)
        {
            System.out.println("USED PACKAGES:");
            for (Iterator i = m_inUsePkgMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                System.out.println("  " + entry.getKey());
                Module[] modules = (Module[]) entry.getValue();
                for (int j = 0; j < modules.length; j++)
                {
                    System.out.println("    " + modules[j]);
                }
            }
        }
    }

    /**
     * This method returns a list of modules that have an export
     * that is compatible with the given import identifier and version.
     * @param pkgMap a map of export packages to exporting modules.
     * @param target the target import package.
     * @return an array of modules that have compatible exports or <tt>null</tt>
     *         if none are found.
    **/
    protected Module[] getCompatibleExporters(Module[] modules, R4Package target)
    {
        // Create list of compatible exporters.
        Module[] candidates = null;
        for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
        {
            // Get the modules export package for the target package.
            R4Package exportPkg = getExportPackage(modules[modIdx], target.getId());
            // If compatible, then add the candidate to the list.
            if ((exportPkg != null) && (exportPkg.doesSatisfy(target)))
            {
                candidates = addModuleToArray(candidates, modules[modIdx]);
            }
        }

        if (candidates == null)
        {
            return m_emptyModules;
        }

        return candidates;
    }

    public void moduleAdded(ModuleEvent event)
    {
        // When a module is added to the system, we need to initialize
        // its resolved and wiring attributes and add its exports to
        // the map of available exports.

        // Synchronize on the module manager, since we don't want any
        // bundles to be installed or removed.
        synchronized (m_mgr)
        {
            // Add wiring attribute.
            event.getModule().setAttribute(WIRING_ATTR, null);
            // Add resolved attribute.
            event.getModule().setAttribute(RESOLVED_ATTR, Boolean.FALSE);
            // Add exports to available package map.
            R4Package[] exports = getExportsAttr(event.getModule());
            for (int i = 0; i < exports.length; i++)
            {
                Module[] modules = (Module[]) m_availPkgMap.get(exports[i].getId());

                // We want to add the module into the list of available
                // exporters in sorted order (descending version and
                // ascending bundle identifier). Insert using a simple
                // binary search algorithm.
                if (modules == null)
                {
                    modules = new Module[] { event.getModule() };
                }
                else
                {
                    int top = 0, bottom = modules.length - 1, middle = 0;
                    R4Version middleVersion = null;
                    while (top <= bottom)
                    {
                        middle = (bottom - top) / 2 + top;
                        middleVersion = getExportPackage(
                            modules[middle], exports[i].getId()).getVersionLow();
                        // Sort in reverse version order.
                        int cmp = middleVersion.compareTo(exports[i].getVersionLow());
                        if (cmp < 0)
                        {
                            bottom = middle - 1;
                        }
                        else if (cmp == 0)
                        {
                            // Sort further by ascending bundle ID.
                            long middleId = getBundleIdFromModuleId(modules[middle].getId());
                            long exportId = getBundleIdFromModuleId(event.getModule().getId());
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

                    Module[] newMods = new Module[modules.length + 1];
                    System.arraycopy(modules, 0, newMods, 0, top);
                    System.arraycopy(modules, top, newMods, top + 1, modules.length - top);
                    newMods[top] = event.getModule();
                    modules = newMods;
                }

                m_availPkgMap.put(exports[i].getId(), modules);
            }
        }
    }

    public void moduleReset(ModuleEvent event)
    {
        moduleRemoved(event);
    }

    public void moduleRemoved(ModuleEvent event)
    {
        // When a module is removed from the system, we need remove
        // its exports from the "in use" and "available" package maps.

        // Synchronize on the module manager, since we don't want any
        // bundles to be installed or removed.
        synchronized (m_mgr)
        {
            // Remove exports from package maps.
            R4Package[] pkgs = getExportsAttr(event.getModule());
            for (int i = 0; i < pkgs.length; i++)
            {
                // Remove from "available" package map.
                Module[] modules = (Module[]) m_availPkgMap.get(pkgs[i].getId());
                if (modules != null)
                {
                    modules = removeModuleFromArray(modules, event.getModule());
                    m_availPkgMap.put(pkgs[i].getId(), modules);
                }
                // Remove from "in use" package map.
                modules = (Module[]) m_inUsePkgMap.get(pkgs[i].getId());
                if (modules != null)
                {
                    modules = removeModuleFromArray(modules, event.getModule());
                    m_inUsePkgMap.put(pkgs[i].getId(), modules);
                }
            }
        }
    }

    // This is duplicated from BundleInfo and probably shouldn't be,
    // but its functionality is needed by the moduleAdded() callback.
    protected static long getBundleIdFromModuleId(String id)
    {
        try
        {
            String bundleId = (id.indexOf('.') >= 0)
                ? id.substring(0, id.indexOf('.')) : id;
            return Long.parseLong(bundleId);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    //
    // Event handling methods for validation events.
    //

    /**
     * Adds a resolver listener to the search policy. Resolver
     * listeners are notified when a module is resolve and/or unresolved
     * by the search policy.
     * @param l the resolver listener to add.
    **/
    public void addResolverListener(ResolveListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_noListeners object as a lock.
        synchronized (m_emptyListeners)
        {
            // If we have no listeners, then just add the new listener.
            if (m_listeners == m_emptyListeners)
            {
                m_listeners = new ResolveListener[] { l };
            }
            // Otherwise, we need to do some array copying.
            // Notice, the old array is always valid, so if
            // the dispatch thread is in the middle of a dispatch,
            // then it has a reference to the old listener array
            // and is not affected by the new value.
            else
            {
                ResolveListener[] newList = new ResolveListener[m_listeners.length + 1];
                System.arraycopy(m_listeners, 0, newList, 0, m_listeners.length);
                newList[m_listeners.length] = l;
                m_listeners = newList;
            }
        }
    }

    /**
     * Removes a resolver listener to this search policy.
     * @param l the resolver listener to remove.
    **/
    public void removeResolverListener(ResolveListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_emptyListeners object as a lock.
        synchronized (m_emptyListeners)
        {
            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < m_listeners.length; i++)
            {
                if (m_listeners[i].equals(l))
                {
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if (m_listeners.length == 1)
                {
                    m_listeners = m_emptyListeners;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    ResolveListener[] newList = new ResolveListener[m_listeners.length - 1];
                    System.arraycopy(m_listeners, 0, newList, 0, idx);
                    if (idx < newList.length)
                    {
                        System.arraycopy(m_listeners, idx + 1, newList, idx,
                            newList.length - idx);
                    }
                    m_listeners = newList;
                }
            }
        }
    }

    /**
     * Fires a validation event for the specified module.
     * @param module the module that was resolved.
    **/
    protected void fireModuleResolved(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ResolveListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(m_mgr, module);
            }
            listeners[i].moduleResolved(event);
        }
    }

    /**
     * Fires an unresolved event for the specified module.
     * @param module the module that was unresolved.
    **/
    protected void fireModuleUnresolved(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ResolveListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(m_mgr, module);
            }
            listeners[i].moduleUnresolved(event);
        }
    }

    //
    // Static utility methods.
    //

    public static Boolean getResolvedAttr(Module m)
    {
        Boolean b =
            (Boolean) m.getAttribute(RESOLVED_ATTR);
        if (b == null)
        {
            b = Boolean.FALSE;
        }
        return b;
    }

    public static R4Package[] getExportsAttr(Module m)
    {
        R4Package[] attr =
            (R4Package[]) m.getAttribute(EXPORTS_ATTR);
        return (attr == null) ? m_emptyPackages : attr;
    }

    public static R4Package getExportPackage(Module m, String id)
    {
        R4Package[] pkgs = getExportsAttr(m);
        for (int i = 0; (pkgs != null) && (i < pkgs.length); i++)
        {
            if (pkgs[i].getId().equals(id))
            {
                return pkgs[i];
            }
        }
        return null;
    }

    public static R4Package[] getImportsAttr(Module m)
    {
        R4Package[] attr =
            (R4Package[]) m.getAttribute(IMPORTS_ATTR);
        return (attr == null) ? m_emptyPackages: attr;
    }

    public static R4Package getImportPackage(Module m, String id)
    {
        R4Package[] pkgs = getImportsAttr(m);
        for (int i = 0; (pkgs != null) && (i < pkgs.length); i++)
        {
            if (pkgs[i].getId().equals(id))
            {
                return pkgs[i];
            }
        }
        return null;
    }

    public static R4Package[] getDynamicImportsAttr(Module m)
    {
        R4Package[] attr =
            (R4Package[]) m.getAttribute(DYNAMICIMPORTS_ATTR);
        return (attr == null) ? m_emptyPackages: attr;
    }

    public static R4Package getDynamicImportPackage(Module m, String id)
    {
        R4Package[] pkgs = getDynamicImportsAttr(m);
        for (int i = 0; (pkgs != null) && (i < pkgs.length); i++)
        {
            if (pkgs[i].getId().equals(id))
            {
                return pkgs[i];
            }
        }
        return null;
    }

    public static R4Wire[] getWiringAttr(Module m)
    {
        R4Wire[] attr =
            (R4Wire[]) m.getAttribute(WIRING_ATTR);
        if (attr == null)
        {
            attr = m_emptyWires;
        }
        return attr;
    }

    public static R4Wire getWire(Module m, String id)
    {
        R4Wire[] wires = getWiringAttr(m);
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].m_pkg.getId().equals(id))
            {
                return wires[i];
            }
        }
        return null;
    }

    public static boolean isModuleInArray(Module[] modules, Module m)
    {
        // Verify that the module is not already in the array.
        for (int i = 0; (modules != null) && (i < modules.length); i++)
        {
            if (modules[i] == m)
            {
                return true;
            }
        }
        
        return false;
    }

    public static Module[] addModuleToArray(Module[] modules, Module m)
    {
        // Verify that the module is not already in the array.
        for (int i = 0; (modules != null) && (i < modules.length); i++)
        {
            if (modules[i] == m)
            {
                return modules;
            }
        }

        if (modules != null)
        {
            Module[] newModules = new Module[modules.length + 1];
            System.arraycopy(modules, 0, newModules, 0, modules.length);
            newModules[modules.length] = m;
            modules = newModules;
        }
        else
        {
            modules = new Module[] { m };
        }

        return modules;
    }

    public static Module[] removeModuleFromArray(Module[] modules, Module m)
    {
        if (modules == null)
        {
            return m_emptyModules;
        }

        int idx = -1;
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
                Module[] newModules= new Module[modules.length - 1];
                System.arraycopy(modules, 0, newModules, 0, idx);
                if (idx < newModules.length)
                {
                    System.arraycopy(
                        modules, idx + 1, newModules, idx, newModules.length - idx);
                }
                modules = newModules;
            }
        }
        return modules;
    }

// TODO: INVESTIGATE GENERIC ARRAY GROWING/SHRINKING.
    private static R4Wire[] shrinkWireArray(R4Wire[] wires)
    {
        if (wires == null)
        {
            return m_emptyWires;
        }

        int count = 0;
        for (int i = 0; i < wires.length; i++)
        {
            if (wires[i] == null)
            {
                count++;
            }
        }

        if (count > 0)
        {
            R4Wire[] newWires = new R4Wire[wires.length - count];
            count = 0;
            for (int i = 0; i < wires.length; i++)
            {
                if (wires[i] != null)
                {
                    newWires[count++] = wires[i];
                }
            }
            wires = newWires;
        }

        return wires;
    }

    private static Module[] shrinkModuleArray(Module[] modules)
    {
        if (modules == null)
        {
            return m_emptyModules;
        }

        int count = 0;
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i] == null)
            {
                count++;
            }
        }

        if (count > 0)
        {
            Module[] newModules = new Module[modules.length - count];
            count = 0;
            for (int i = 0; i < modules.length; i++)
            {
                if (modules[i] != null)
                {
                    newModules[count++] = modules[i];
                }
            }
            modules = newModules;
        }

        return modules;
    }

    private static class ResolverNode
    {
        public Module m_module = null;
        public R4Package m_pkg = null;
        public Module[] m_candidates = null;
        public int m_idx = 0;
        public boolean m_visited = false;
        public ResolverNode(Module module, R4Package pkg, Module[] candidates)
        {
            m_module = module;
            m_pkg = pkg;
            m_candidates = candidates;
            if (getResolvedAttr(m_module).booleanValue())
            {
                m_visited = true;
            }
        }
    }
}