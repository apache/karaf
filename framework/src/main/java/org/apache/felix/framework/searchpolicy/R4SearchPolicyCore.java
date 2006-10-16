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

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.*;

public class R4SearchPolicyCore implements ModuleListener
{
    private Logger m_logger = null;
    private PropertyResolver m_config = null;
    private IModuleFactory m_factory = null;
    private Map m_availPkgMap = new HashMap();
    private Map m_inUsePkgMap = new HashMap();
    private Map m_moduleDataMap = new HashMap();

    // Boot delegation packages.
    private String[] m_bootPkgs = null;
    private boolean[] m_bootPkgWildcards = null;

    // Listener-related instance variables.
    private static final ResolveListener[] m_emptyListeners = new ResolveListener[0];
    private ResolveListener[] m_listeners = m_emptyListeners;

    // Reusable empty array.
    public static final IModule[] m_emptyModules = new IModule[0];

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    public R4SearchPolicyCore(Logger logger, PropertyResolver config)
    {
        m_logger = logger;
        m_config = config;

        // Read the boot delegation property and parse it.
        String s = m_config.get(Constants.FRAMEWORK_BOOTDELEGATION);
        s = (s == null) ? "java.*" : s + ",java.*";
        StringTokenizer st = new StringTokenizer(s, " ,");
        m_bootPkgs = new String[st.countTokens()];
        m_bootPkgWildcards = new boolean[m_bootPkgs.length];
        for (int i = 0; i < m_bootPkgs.length; i++)
        {
            s = st.nextToken();
            if (s.endsWith("*"))
            {
                m_bootPkgWildcards[i] = true;
                s = s.substring(0, s.length() - 1);
            }
            m_bootPkgs[i] = s;
        }
    }

    public IModuleFactory getModuleFactory()
    {
        return m_factory;
    }

    public void setModuleFactory(IModuleFactory factory)
        throws IllegalStateException
    {
        if (m_factory == null)
        {
            m_factory = factory;
            m_factory.addModuleListener(this);
        }
        else
        {
            throw new IllegalStateException(
                "Module manager is already initialized");
        }
    }

    protected synchronized boolean isResolved(IModule module)
    {
        ModuleData data = (ModuleData) m_moduleDataMap.get(module);
        return (data == null) ? false : data.m_resolved;
    }

    protected synchronized void setResolved(IModule module, boolean resolved)
    {
        ModuleData data = (ModuleData) m_moduleDataMap.get(module);
        if (data == null)
        {
            data = new ModuleData(module);
            m_moduleDataMap.put(module, data);
        }
        data.m_resolved = resolved;
    }

    public Object[] definePackage(IModule module, String pkgName)
    {
        R4Package pkg = Util.getExportPackage(module, pkgName);
        if (pkg != null)
        {
            return new Object[] {
                pkgName, // Spec title.
                pkg.getVersion().toString(), // Spec version.
                "", // Spec vendor.
                "", // Impl title.
                "", // Impl version.
                "" // Impl vendor.
            };
        }
        return null;
    }

    public Class findClass(IModule module, String name)
        throws ClassNotFoundException
    {
        try
        {
            return (Class) findClassOrResource(module, name, true);
        }
        catch (ResourceNotFoundException ex)
        {
            // This should never happen, so just ignore it.
        }
        catch (ClassNotFoundException ex)
        {
            String msg = diagnoseClassLoadError(module, name);
            throw new ClassNotFoundException(msg, ex);
        }

        // We should never reach this point.
        return null;
    }

    public URL findResource(IModule module, String name)
        throws ResourceNotFoundException
    {
        try
        {
            return (URL) findClassOrResource(module, name, false);
        }
        catch (ClassNotFoundException ex)
        {
            // This should never happen, so just ignore it.
        }
        catch (ResourceNotFoundException ex)
        {
            throw ex;
        }
    
        // We should never reach this point.
        return null;
    }

    public Enumeration findResources(IModule module, String name)
        throws ResourceNotFoundException
    {
        Enumeration urls;
        // First, try to resolve the originating module.
        // TODO: Consider opimizing this call to resolve, since it is called
        // for each class load.
        try
        {
            resolve(module);
        }
        catch (ResolveException ex)
        {
            // The spec states that if the bundle cannot be resolved, then
            // only the local bundle's resources should be searched. So we
            // will ask the module's own class path.
            urls = module.getContentLoader().getResources(name);
            if (urls != null)
            {
                return urls;
            }
            // We need to throw a resource not found exception.
            throw new ResourceNotFoundException(name
                + ": cannot resolve package " + ex.getPackage());
        }

        // Get the package of the target class/resource.
        String pkgName = Util.getResourcePackage(name);

        // Delegate any packages listed in the boot delegation
        // property to the parent class loader.
        // NOTE for the default package:
        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if (pkgName.length() > 0)
        {
            for (int i = 0; i < m_bootPkgs.length; i++)
            {
                // A wildcarded boot delegation package will be in the form of
                // "foo.", so if the package is wildcarded do a startsWith() or a
                // regionMatches() to ignore the trailing "." to determine if the
                // request should be delegated to the parent class loader. If the
                // package is not wildcarded, then simply do an equals() test to
                // see if the request should be delegated to the parent class loader.
                if ((m_bootPkgWildcards[i] &&
                    (pkgName.startsWith(m_bootPkgs[i]) ||
                    m_bootPkgs[i].regionMatches(0, pkgName, 0, pkgName.length())))
                    || (!m_bootPkgWildcards[i] && m_bootPkgs[i].equals(pkgName)))
                {
                    try
                    {
                        urls = getClass().getClassLoader().getResources(name);
                        return urls;
                    }
                    catch (IOException ex)
                    {
                        return null;
                    }
                }
            }
        }

        // Look in the module's imports.
        // We delegate to the module's wires to the resources.
        // If any resources are found, this means that the package of these
        // resources is imported, we must not keep looking since we do not
        // support split-packages.

        // Note that the search may be aborted if this method throws an
        // exception, otherwise it continues if a null is returned.
        IWire[] wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            // If we find the class or resource, then return it.
            urls = wires[i].getResources(name);
            if (urls != null)
            {
                return urls;
            }
        }

        // If not found, try the module's own class path.
        urls = module.getContentLoader().getResources(name);
        if (urls != null)
        {
            return urls;
        }

        // If still not found, then try the module's dynamic imports.
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        IWire wire = attemptDynamicImport(module, pkgName);
        if (wire != null)
        {
            urls = wire.getResources(name);
        }

        if (urls == null)
        {
            throw new ResourceNotFoundException(name);
        }

        return urls;
    }

    private Object findClassOrResource(IModule module, String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // First, try to resolve the originating module.
// TODO: Consider opimizing this call to resolve, since it is called
// for each class load.
        try
        {
            resolve(module);
        }
        catch (ResolveException ex)
        {
            if (isClass)
            {
                // We do not use the resolve exception as the
                // cause of the exception, since this would
                // potentially leak internal module information.
                throw new ClassNotFoundException(
                    name + ": cannot resolve package "
                    + ex.getPackage());
            }
            else
            {
                // The spec states that if the bundle cannot be resolved, then
                // only the local bundle's resources should be searched. So we
                // will ask the module's own class path.
                URL url = module.getContentLoader().getResource(name);
                if (url != null)
                {
                    return url;
                }

                // We need to throw a resource not found exception.
                throw new ResourceNotFoundException(
                    name + ": cannot resolve package "
                    + ex.getPackage());
            }
        }

        // Get the package of the target class/resource.
        String pkgName = (isClass)
            ? Util.getClassPackage(name)
            : Util.getResourcePackage(name);

        // Delegate any packages listed in the boot delegation
        // property to the parent class loader.
        for (int i = 0; i < m_bootPkgs.length; i++)
        {
            // A wildcarded boot delegation package will be in the form of "foo.",
            // so if the package is wildcarded do a startsWith() or a regionMatches()
            // to ignore the trailing "." to determine if the request should be
            // delegated to the parent class loader. If the package is not wildcarded,
            // then simply do an equals() test to see if the request should be
            // delegated to the parent class loader.
            if (pkgName.length() > 0)
            {
                // Only consider delegation if we have a package name, since
                // we don't want to promote the default package. The spec does
                // not take a stand on this issue.
                if ((m_bootPkgWildcards[i] &&
                    (pkgName.startsWith(m_bootPkgs[i]) ||
                    m_bootPkgs[i].regionMatches(0, pkgName, 0, pkgName.length())))
                    || (!m_bootPkgWildcards[i] && m_bootPkgs[i].equals(pkgName)))
                {
                    return (isClass)
                        ? (Object) getClass().getClassLoader().loadClass(name)
                        : (Object) getClass().getClassLoader().getResource(name);
                }
            }
        }

        // Look in the module's imports. Note that the search may
        // be aborted if this method throws an exception, otherwise
        // it continues if a null is returned.
        Object result = searchImports(module, name, isClass);

        // If not found, try the module's own class path.
        if (result == null)
        {
            result = (isClass)
                ? (Object) module.getContentLoader().getClass(name)
                : (Object) module.getContentLoader().getResource(name);

            // If still not found, then try the module's dynamic imports.
            if (result == null)
            {
                result = searchDynamicImports(module, name, pkgName, isClass);
            }
        }

        if (result == null)
        {
            if (isClass)
            {
                throw new ClassNotFoundException(name);
            }
            else
            {
                throw new ResourceNotFoundException(name);
            }
        }

        return result;
    }

    private Object searchImports(IModule module, String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // We delegate to the module's wires to find the class or resource.
        IWire[] wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            // If we find the class or resource, then return it.
            Object result = (isClass)
                ? (Object) wires[i].getClass(name)
                : (Object) wires[i].getResource(name);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    private Object searchDynamicImports(
        IModule module, String name, String pkgName, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        IWire wire = attemptDynamicImport(module, pkgName);

        // If the dynamic import was successful, then this initial
        // time we must directly return the result from dynamically
        // created wire, but subsequent requests for classes/resources
        // in the associated package will be processed as part of
        // normal static imports.
        if (wire != null)
        {
            // Return the class or resource.
            return (isClass)
                ? (Object) wire.getClass(name)
                : (Object) wire.getResource(name);
        }

        // At this point, the class/resource could not be found by the bundle's
        // static or dynamic imports, nor its own content. Before we throw
        // an exception, we will try to determine if the instigator of the
        // class/resource load was a class from a bundle or not. This is necessary
        // because the specification mandates that classes on the class path
        // should be hidden (except for java.*), but it does allow for these
        // classes/resources to be exposed by the system bundle as an export.
        // However, in some situations classes on the class path make the faulty
        // assumption that they can access everything on the class path from
        // every other class loader that they come in contact with. This is
        // not true if the class loader in question is from a bundle. Thus,
        // this code tries to detect that situation. If the class
        // instigating the load request was NOT from a bundle, then we will
        // make the assumption that the caller actually wanted to use the
        // parent class loader and we will delegate to it. If the class was
        // from a bundle, then we will enforce strict class loading rules
        // for the bundle and throw an exception.

        // Get the class context to see the classes on the stack.
        Class[] classes = m_sm.getClassContext();
        // Start from 1 to skip security manager class.
        for (int i = 1; i < classes.length; i++)
        {
            // Find the first class on the call stack that is not one
            // of the R4 search policy classes, nor a class loader or
            // class itself, because we want to ignore the calls to
            // ClassLoader.loadClass() and Class.forName().
            if (!R4SearchPolicyCore.class.equals(classes[i])
                && !R4SearchPolicy.class.equals(classes[i])
                && !ClassLoader.class.isAssignableFrom(classes[i])
                && !Class.class.isAssignableFrom(classes[i]))
            {
                // If the instigating class was not from a bundle, then
                // delegate to the parent class loader. Otherwise, break
                // out of loop and return null.
                if (!ContentClassLoader.class.isInstance(classes[i].getClassLoader()))
                {
                    return this.getClass().getClassLoader().loadClass(name);
                }
                break;
            }
        }

        return null;
    }

    private IWire attemptDynamicImport(IModule module, String pkgName)
    {
        R4Wire wire = null;
        IModule candidate = null;

        // There is an overriding assumption here that a package is
        // never split across bundles. If a package can be split
        // across bundles, then this will fail.

        try
        {
            // Get the matching dynamic import, if any.
            R4Import impMatch = createDynamicImportTarget(module, pkgName);

            // If the target package does not match any dynamically imported
            // packages or if the module is already wired for the target package,
            // then just return null. The module may be already wired to the target
            // package if the class being searched for does not actually exist.
            if ((impMatch == null) || (Util.getWire(module, impMatch.getName()) != null))
            {
                return null;
            }

            // At this point, the target package has matched a dynamically
            // imported package spec. Now we must try to find a candidate
            // exporter for target package and add it to the module's set
            // of wires.

            // Lock module manager instance to ensure that nothing changes.
            synchronized (m_factory)
            {
                // Try to add a new entry to the module's import attribute.
                // Select the first candidate that successfully resolves.

                // First check already resolved exports for a match.
                IModule[] candidates = getInUseExporters(impMatch, false);
                // If there is an "in use" candidate, just take the first one.
                if (candidates.length > 0)
                {
                    candidate = candidates[0];
                }

                // If there were no "in use" candidates, then try "available"
                // candidates and take the first one that can resolve.
                if (candidate == null)
                {
                    candidates = getAvailableExporters(impMatch, false);
                    for (int candIdx = 0;
                        (candidate == null) && (candIdx < candidates.length);
                        candIdx++)
                    {
                        try
                        {
                            resolve(candidates[candIdx]);
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
                    IWire[] wires = module.getWires();
                    R4Wire[] newWires = null;
                    if (wires == null)
                    {
                        newWires = new R4Wire[1];
                    }
                    else
                    {
                        newWires = new R4Wire[wires.length + 1];
                        System.arraycopy(wires, 0, newWires, 0, wires.length);
                    }
                    // Find the candidate's export package object and
                    // use that for creating the wire; this is necessary
                    // since it contains "uses" dependency information.
                    wire = new R4Wire(
                        module, candidate,
                        Util.getExportPackage(candidate, impMatch.getName()));
                    newWires[newWires.length - 1] = wire;
                    ((ModuleImpl) module).setWires(newWires);
m_logger.log(Logger.LOG_DEBUG, "WIRE: " + newWires[newWires.length - 1]);
                }
            }
        }
        catch (Exception ex)
        {
            m_logger.log(Logger.LOG_ERROR, "Unable to dynamically import package.", ex);
        }

        return wire;
    }

    private R4Import createDynamicImportTarget(IModule module, String pkgName)
    {
        // Check the dynamic import specs for a match of
        // the target package.
        R4Import[] dynamics = module.getDefinition().getDynamicImports();
        R4Import impMatch = null;
        for (int i = 0; (impMatch == null) && (dynamics != null)
            && (i < dynamics.length); i++)
        {
            // Star matches everything.
            if (dynamics[i].getName().equals("*"))
            {
                // Create a package instance without wildcard.
                impMatch = new R4Import(pkgName, dynamics[i]
                    .getDirectives(), dynamics[i].getAttributes());
            }
            // Packages ending in ".*" must match starting strings.
            else if (dynamics[i].getName().endsWith(".*"))
            {
                if (pkgName.regionMatches(0, dynamics[i].getName(), 0,
                    dynamics[i].getName().length() - 2))
                {
                    // Create a package instance without wildcard.
                    impMatch = new R4Import(pkgName, dynamics[i]
                        .getDirectives(), dynamics[i].getAttributes());
                }
            }
            // Or we can have a precise match.
            else
            {
                if (pkgName.equals(dynamics[i].getName()))
                {
                    impMatch = dynamics[i];
                }
            }
        }

        return impMatch;
    }

    public String findLibrary(IModule module, String name)
    {
        // Remove leading slash, if present.
        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        R4Library[] libs = module.getDefinition().getLibraries();
        for (int i = 0; (libs != null) && (i < libs.length); i++)
        {
            String lib = libs[i].getPath(name);
            if (lib != null)
            {
                return lib;
            }
        }
        return null;
    }

    public IModule[] getAvailableExporters(R4Import pkg, boolean includeRemovalPending)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_factory)
        {
            IModule[] exporters = getCompatibleExporters(
                (IModule[]) m_availPkgMap.get(pkg.getName()), pkg, includeRemovalPending);

            if ((exporters != null) && (System.getSecurityManager() != null))
            {
                PackagePermission perm = new PackagePermission(pkg.getName(),
                    PackagePermission.EXPORT);

                for (int i = 0; i < exporters.length; i++)
                {
                    if (exporters[i] != null)
                    {
                        if (!((ProtectionDomain) exporters[i].getSecurityContext()).implies(perm))
                        {
                            m_logger.log(Logger.LOG_DEBUG,
                                "PackagePermission.EXPORT denied for " + pkg +
                                "from " + exporters[i].getId());

                            exporters[i] = null;
                        }
                    }
                }

                exporters = shrinkModuleArray(exporters);
            }

            return exporters;
        }
    }

    public IModule[] getInUseExporters(R4Import pkg, boolean includeRemovalPending)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_factory)
        {
            return getCompatibleExporters(
                (IModule[]) m_inUsePkgMap.get(pkg.getName()), pkg, includeRemovalPending);
        }
    }

    public void resolve(IModule rootModule)
        throws ResolveException
    {
        // If the module is already resolved, then we can just return.
        if (isResolved(rootModule))
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
        synchronized (m_factory)
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
                fireModuleResolved((IModule) ((Map.Entry) iter.next()).getKey());
            }
        }
    }

    private void populateResolverMap(Map resolverMap, IModule module)
        throws ResolveException
    {
        // Detect cycles.
        if (resolverMap.get(module) != null)
        {
            return;
        }
        // Map to hold the module's import packages
        // and their respective resolving candidates.
        List nodeList = new ArrayList();

        // Even though the node list is currently empty, we
        // record it in the resolver map early so we can use
        // it to detect cycles.
        resolverMap.put(module, nodeList);

        // Loop through each import and calculate its resolving
        // set of candidates.
        R4Import[] imports = module.getDefinition().getImports();
        for (int impIdx = 0; (imports != null) && (impIdx < imports.length); impIdx++)
        {
            // Get the candidates from the "in use" and "available"
            // package maps. Candidates "in use" have higher priority
            // than "available" ones, so put the "in use" candidates
            // at the front of the list of candidates.
            IModule[] inuse = getInUseExporters(imports[impIdx], false);
            IModule[] available = getAvailableExporters(imports[impIdx], false);
            IModule[] candidates = new IModule[inuse.length + available.length];
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
                        if (!isResolved(candidates[candIdx]))
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
                // Since we are not able to resolve the module, we must
                // remove the module from the resolve map so that subsequent
                // resolves do not think that the module is resolvable due
                // to the cycle check at the beginning of this method.
                resolverMap.remove(module);
                
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

// TODO: REMOVE THESE DEBUG METHODS.
    private void dumpAvailablePackages()
    {
        synchronized (this)
        {
            System.out.println("AVAILABLE PACKAGES:");
            for (Iterator i = m_availPkgMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                IModule[] modules = (IModule[]) entry.getValue();
                if ((modules != null) && (modules.length > 0))
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

    private void dumpUsedPackages()
    {
        synchronized (this)
        {
            System.out.println("USED PACKAGES:");
            for (Iterator i = m_inUsePkgMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                IModule[] modules = (IModule[]) entry.getValue();
                if ((modules != null) && (modules.length > 0))
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

    private IModule[] getCompatibleExporters(
        IModule[] modules, R4Import target, boolean includeRemovalPending)
    {
        // Create list of compatible exporters.
        IModule[] candidates = null;
        for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
        {
            // The spec says that we cannot consider modules that
            // are pending removal, so ignore them.
            if (includeRemovalPending || !modules[modIdx].isRemovalPending())
            {
                // Get the modules export package for the target package.
                R4Export export = Util.getExportPackage(
                    modules[modIdx], target.getName());
                // If compatible, then add the candidate to the list.
                if ((export != null) && (target.isSatisfied(export)))
                {
                    candidates = addModuleToArray(candidates, modules[modIdx]);
                }
            }
        }
        if (candidates == null)
        {
            return m_emptyModules;
        }
        return candidates;
    }

    private void findConsistentClassSpace(Map resolverMap, IModule rootModule)
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
    Logger.LOG_DEBUG,
    "Constraint violation detected, will try to repair.");

            // The incrementCandidateConfiguration() method requires an
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
        Map resolverMap, IModule rootModule, Map cycleMap)
    {
        // We do not need to verify that already resolved modules
        // have consistent class spaces because they should be
        // consistent by definition. Also, if the root module is
        // part of a cycle, then just assume it is true.
        if (isResolved(rootModule) || (cycleMap.get(rootModule) != null))
        {
            return true;
        }

        // Add to cycle map for future reference.
        cycleMap.put(rootModule, rootModule);

        // Create an implicit "uses" constraint for every exported package
        // of the root module that is not also imported; uses constraints
        // for exported packages that are also imported will be taken
        // care of as part of normal import package processing.
        R4Export[] exports = rootModule.getDefinition().getExports();
        Map usesMap = new HashMap();
        for (int i = 0; (exports != null) && (i < exports.length); i++)
        {
            // Ignore exports that are also imported, since they
            // will be taken care of when verifying import constraints.
            if (Util.getImportPackage(rootModule, exports[i].getName()) == null)
            {
                usesMap.put(exports[i].getName(), rootModule);
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
            R4Export candidatePkg = Util.getExportPackage(
                node.m_candidates[node.m_idx], node.m_import.getName());

            // Calculate the "uses" dependencies implied by the candidate's
            // exported package with respect to the currently selected
            // candidates in the resolver map.
            Map candUsesMap = calculateUsesDependencies(
                resolverMap,
                node.m_candidates[node.m_idx],
                candidatePkg,
                new HashMap());

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
        Map resolverMap, IModule module, R4Export export, Map usesMap)
    {
// TODO: CAN THIS BE OPTIMIZED?
// TODO: IS THIS CYCLE CHECK CORRECT??
// TODO: WHAT HAPPENS IF THERE ARE OVERLAPS WHEN CALCULATING USES??
//       MAKE AN EXAMPLE WHERE TWO DEPENDENCIES PROVIDE SAME PACKAGE.
        // Make sure we are not in a cycle.
        if (usesMap.get(export.getName()) != null)
        {
            return usesMap;
        }

        // The target package at least uses itself,
        // so add it to the uses map.
        usesMap.put(export.getName(), module);

        // Get the "uses" constraints for the target export
        // package and calculate the transitive uses constraints
        // of any used packages.
        String[] uses = export.getUses();
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
                IWire wire = Util.getWire(module, uses[usesIdx]);
                if (wire != null)
                {
                    usesMap = calculateUsesDependencies(
                        resolverMap, wire.getExporter(), wire.getExport(), usesMap);
                }
                else
                {
                    export = Util.getExportPackage(module, uses[usesIdx]);
                    if (export != null)
                    {
                        usesMap = calculateUsesDependencies(
                            resolverMap, module, export, usesMap);
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
                    if (!node.m_import.getName().equals(uses[usesIdx]))
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
                        Util.getExportPackage(node.m_candidates[node.m_idx], node.m_import.getName()),
                        usesMap);
                }
                // If there was no resolver node for the "used" package,
                // then this means that the module exports the package
                // and we need to recursively add the constraints of this
                // other exported package of this module.
                else if (Util.getExportPackage(module, uses[usesIdx]) != null)
                {
                    usesMap = calculateUsesDependencies(
                        resolverMap,
                        module,
                        Util.getExportPackage(module, uses[usesIdx]),
                        usesMap);
                }
            }
        }

        return usesMap;
    }

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

    private Map createWires(Map resolverMap, IModule rootModule)
    {
        Map resolvedModuleWireMap =
            populateWireMap(resolverMap, rootModule, new HashMap());
        Iterator iter = resolvedModuleWireMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            IModule module = (IModule) entry.getKey();
            IWire[] wires = (IWire[]) entry.getValue();

            // Set the module's resolved and wiring attribute.
            setResolved(module, true);
            // Only add wires attribute if some exist; export
            // only modules may not have wires.
            if (wires.length > 0)
            {
                ((ModuleImpl) module).setWires(wires);
            }

            // Remove the wire's exporting module from the "available"
            // package map and put it into the "in use" package map;
            // these steps may be a no-op.
            for (int wireIdx = 0;
                (wires != null) && (wireIdx < wires.length);
                wireIdx++)
            {
m_logger.log(Logger.LOG_DEBUG, "WIRE: " + wires[wireIdx]);
                // First remove the wire module from "available" package map.
                IModule[] modules = (IModule[]) m_availPkgMap.get(wires[wireIdx].getExport().getName());
                modules = removeModuleFromArray(modules, wires[wireIdx].getExporter());
                m_availPkgMap.put(wires[wireIdx].getExport().getName(), modules);

                // Also remove any exported packages from the "available"
                // package map that are from the module associated with
                // the current wires where the exported packages were not
                // actually exported; an export may not be exported if
                // the module also imports the same package and was wired
                // to a different module. If the exported package is not
                // actually exported, then we just want to remove it
                // completely, since it cannot be used.
                if (wires[wireIdx].getExporter() != module)
                {
                    modules = (IModule[]) m_availPkgMap.get(wires[wireIdx].getExport().getName());
                    modules = removeModuleFromArray(modules, module);
                    m_availPkgMap.put(wires[wireIdx].getExport().getName(), modules);
                }

                // Add the module of the wire to the "in use" package map.
                modules = (IModule[]) m_inUsePkgMap.get(wires[wireIdx].getExport().getName());
                modules = addModuleToArray(modules, wires[wireIdx].getExporter());
                m_inUsePkgMap.put(wires[wireIdx].getExport().getName(), modules);
            }
        }
        return resolvedModuleWireMap;
    }

    private Map populateWireMap(Map resolverMap, IModule module, Map wireMap)
    {
        // If the module is already resolved or it is part of
        // a cycle, then just return the wire map.
        if (isResolved(module) || (wireMap.get(module) != null))
        {
            return wireMap;
        }

        List nodeList = (List) resolverMap.get(module);
        IWire[] wires = new IWire[nodeList.size()];

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
            R4Export export =
                Util.getExportPackage(node.m_candidates[node.m_idx], node.m_import.getName());
            wires[nodeIdx] = new R4Wire(module, node.m_candidates[node.m_idx], export);

            // Create the wires for the selected candidate module.
            wireMap = populateWireMap(resolverMap, node.m_candidates[node.m_idx], wireMap);
        }

        return wireMap;
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
    private void fireModuleResolved(IModule module)
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
                event = new ModuleEvent(m_factory, module);
            }
            listeners[i].moduleResolved(event);
        }
    }

    /**
     * Fires an unresolved event for the specified module.
     * @param module the module that was unresolved.
    **/
    private void fireModuleUnresolved(IModule module)
    {
// TODO: Call this method where appropriate.
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
                event = new ModuleEvent(m_factory, module);
            }
            listeners[i].moduleUnresolved(event);
        }
    }

    //
    // ModuleListener callback methods.
    //

    public void moduleAdded(ModuleEvent event)
    {
        synchronized (m_factory)
        {
            // When a module is added, create an aggregated list of available
            // exports to simplify later processing when resolving bundles.
            IModule module = event.getModule();
            R4Export[] exports = module.getDefinition().getExports();

            // Add exports to available package map.
            for (int i = 0; (exports != null) && (i < exports.length); i++)
            {
                IModule[] modules = (IModule[]) m_availPkgMap.get(exports[i].getName());

                // We want to add the module into the list of available
                // exporters in sorted order (descending version and
                // ascending bundle identifier). Insert using a simple
                // binary search algorithm.
                if (modules == null)
                {
                    modules = new IModule[] { module };
                }
                else
                {
                    int top = 0, bottom = modules.length - 1, middle = 0;
                    Version middleVersion = null;
                    while (top <= bottom)
                    {
                        middle = (bottom - top) / 2 + top;
                        middleVersion = Util.getExportPackage(
                            modules[middle], exports[i].getName()).getVersion();
                        // Sort in reverse version order.
                        int cmp = middleVersion.compareTo(exports[i].getVersion());
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

                    IModule[] newMods = new IModule[modules.length + 1];
                    System.arraycopy(modules, 0, newMods, 0, top);
                    System.arraycopy(modules, top, newMods, top + 1, modules.length - top);
                    newMods[top] = module;
                    modules = newMods;
                }

                m_availPkgMap.put(exports[i].getName(), modules);
            }
        }
    }

    public void moduleRemoved(ModuleEvent event)
    {
        // When a module is removed from the system, we need remove
        // its exports from the "in use" and "available" package maps
        // as well as remove the module from the module data map.

        // Synchronize on the module manager, since we don't want any
        // bundles to be installed or removed.
        synchronized (m_factory)
        {
            // Remove exports from package maps.
            R4Export[] exports = event.getModule().getDefinition().getExports();
            for (int i = 0; (exports != null) && (i < exports.length); i++)
            {
                // Remove from "available" package map.
                IModule[] modules = (IModule[]) m_availPkgMap.get(exports[i].getName());
                if (modules != null)
                {
                    modules = removeModuleFromArray(modules, event.getModule());
                    m_availPkgMap.put(exports[i].getName(), modules);
                }
                // Remove from "in use" package map.
                modules = (IModule[]) m_inUsePkgMap.get(exports[i].getName());
                if (modules != null)
                {
                    modules = removeModuleFromArray(modules, event.getModule());
                    m_inUsePkgMap.put(exports[i].getName(), modules);
                }
            }

            // Finally, remove module data.
            m_moduleDataMap.remove(event.getModule());
        }
    }

    //
    // Simple utility methods.
    //

    private static IModule[] addModuleToArray(IModule[] modules, IModule m)
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
            IModule[] newModules = new IModule[modules.length + 1];
            System.arraycopy(modules, 0, newModules, 0, modules.length);
            newModules[modules.length] = m;
            modules = newModules;
        }
        else
        {
            modules = new IModule[] { m };
        }

        return modules;
    }

    private static IModule[] removeModuleFromArray(IModule[] modules, IModule m)
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
        return modules;
    }

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

    //
    // Simple utility classes.
    //

    private static class ModuleData
    {
        public IModule m_module = null;
        public boolean m_resolved = false;
        public ModuleData(IModule module)
        {
            m_module = module;
        }
    }

    private class ResolverNode
    {
        public IModule m_module = null;
        public R4Import m_import = null;
        public IModule[] m_candidates = null;
        public int m_idx = 0;
        public boolean m_visited = false;
        public ResolverNode(IModule module, R4Import imp, IModule[] candidates)
        {
            m_module = module;
            m_import = imp;
            m_candidates = candidates;
            if (isResolved(m_module))
            {
                m_visited = true;
            }
        }
    }

    private String diagnoseClassLoadError(IModule module, String name)
    {
        // We will try to do some diagnostics here to help the developer
        // deal with this exception.

        // Get package name.
        String pkgName = Util.getClassPackage(name);

        // First, get the bundle ID of the module doing the class loader.
        long impId = Util.getBundleIdFromModuleId(module.getId());

        // Next, check to see if the module imports the package.
        IWire[] wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].getExport().getName().equals(pkgName))
            {
                long expId = Util.getBundleIdFromModuleId(
                    wires[i].getExporter().getId());

                StringBuffer sb = new StringBuffer("*** Package '");
                sb.append(pkgName);
                sb.append("' is imported by bundle ");
                sb.append(impId);
                sb.append(" from bundle ");
                sb.append(expId);
                sb.append(", but the exported package from bundle ");
                sb.append(expId);
                sb.append(" does not contain the requested class '");
                sb.append(name);
                sb.append("'. Please verify that the class name is correct in the importing bundle ");
                sb.append(impId);
                sb.append(" and/or that the exported package is correctly bundled in ");
                sb.append(expId);
                sb.append(". ***");

                return sb.toString();
            }
        }

        // Next, check to see if the package was optionally imported and
        // whether or not there is an exporter available.
        R4Import[] imports = module.getDefinition().getImports();
        for (int i = 0; (imports != null) && (i < imports.length); i++)
        {
            if (imports[i].getName().equals(pkgName) && imports[i].isOptional())
            {
                // Try to see if there is an exporter available. It may be
                // the case that the package is exported, but the attributes
                // do not match, so check that case too.
                IModule[] exporters = getInUseExporters(imports[i], true);
                exporters = (exporters.length == 0)
                    ? getAvailableExporters(imports[i], true) : exporters;
                exporters = (exporters.length == 0)
                    ? getInUseExporters(new R4Import(pkgName, null, null), true) : exporters;
                exporters = (exporters.length == 0)
                    ? getAvailableExporters(new R4Import(pkgName, null, null), true) : exporters;
                long expId = (exporters.length == 0)
                    ? -1 : Util.getBundleIdFromModuleId(exporters[0].getId());

                StringBuffer sb = new StringBuffer("*** Class '");
                sb.append(name);
                sb.append("' was not found, but this is likely normal since package '");
                sb.append(pkgName);
                sb.append("' is optionally imported by bundle ");
                sb.append(impId);
                sb.append(".");
                if (exporters.length > 0)
                {
                    sb.append(" However, bundle ");
                    sb.append(expId);
                    if (imports[i].isSatisfied(
                        Util.getExportPackage(exporters[0], imports[i].getName())))
                    {
                        sb.append(" does export this package. Bundle ");
                        sb.append(expId);
                        sb.append(" must be installed before bundle ");
                        sb.append(impId);
                        sb.append(" is resolved or else the optional import will be ignored.");
                    }
                    else
                    {
                        sb.append(" does export this package with attributes that do not match.");
                    }
                }
                sb.append(" ***");

                return sb.toString();
            }
        }

        // Next, check to see if the package is dynamically imported by the module.
        R4Import imp = createDynamicImportTarget(module, pkgName);
        if (imp != null)
        {
            // Try to see if there is an exporter available. It may be
            // the case that the package is exported, but the attributes
            // do not match, so check that case too.
            IModule[] exporters = getInUseExporters(imp, true);
            exporters = (exporters.length == 0)
                ? getAvailableExporters(imp, true) : exporters;
            exporters = (exporters.length == 0)
                ? getInUseExporters(new R4Import(pkgName, null, null), true) : exporters;
            exporters = (exporters.length == 0)
                ? getAvailableExporters(new R4Import(pkgName, null, null), true) : exporters;
            long expId = (exporters.length == 0)
                ? -1 : Util.getBundleIdFromModuleId(exporters[0].getId());

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found, but this is likely normal since package '");
            sb.append(pkgName);
            sb.append("' is dynamically imported by bundle ");
            sb.append(impId);
            sb.append(".");
            if (exporters.length > 0)
            {
                if (!imp.isSatisfied(
                    Util.getExportPackage(exporters[0], imp.getName())))
                {
                    sb.append(" However, bundle ");
                    sb.append(expId);
                    sb.append(" does export this package with attributes that do not match.");
                }
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, if the package is not imported by the module, check to
        // see if there is an exporter for the package.
        IModule[] exporters = getInUseExporters(new R4Import(pkgName, null, null), true);
        exporters = (exporters.length == 0)
            ? getAvailableExporters(new R4Import(pkgName, null, null), true) : exporters;
        if (exporters.length > 0)
        {
            boolean classpath = false;
            try
            {
                getClass().getClassLoader().loadClass(name);
                classpath = true;
            }
            catch (Exception ex)
            {
                // Ignore
            }

            long expId = Util.getBundleIdFromModuleId(exporters[0].getId());

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found because bundle ");
            sb.append(impId);
            sb.append(" does not import '");
            sb.append(pkgName);
            sb.append("' even though bundle ");
            sb.append(expId);
            sb.append(" does export it.");
            if (classpath)
            {
                sb.append(" Additionally, the class is also available from the system class loader. There are two fixes: 1) Add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(impId);
                sb.append("; imports are necessary for each class directly touched by bundle code or indirectly touched, such as super classes if their methods are used. ");
                sb.append("2) Add package '");
                sb.append(pkgName);
                sb.append("' to the '");
                sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
                sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            }
            else
            {
                sb.append(" To resolve this issue, add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(impId);
                sb.append(".");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, try to see if the class is available from the system
        // class loader.
        try
        {
            getClass().getClassLoader().loadClass(name);

            StringBuffer sb = new StringBuffer("*** Package '");
            sb.append(pkgName);
            sb.append("' is not imported by bundle ");
            sb.append(impId);
            sb.append(", nor is there any bundle that exports package '");
            sb.append(pkgName);
            sb.append("'. However, the class '");
            sb.append(name);
            sb.append("' is available from the system class loader. There are two fixes: 1) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_SYSTEMPACKAGES);
            sb.append("' property and modify bundle ");
            sb.append(impId);
            sb.append(" to import this package; this causes the system bundle to export class path packages. 2) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
            sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            sb.append(" ***");

            return sb.toString();
        }
        catch (Exception ex2)
        {
        }

        // Finally, if there are no imports or exports for the package
        // and it is not available on the system class path, simply
        // log a message saying so.
        StringBuffer sb = new StringBuffer("*** Class '");
        sb.append(name);
        sb.append("' was not found. Bundle ");
        sb.append(impId);
        sb.append(" does not import package '");
        sb.append(pkgName);
        sb.append("', nor is the package exported by any other bundle or available from the system class loader.");
        sb.append(" ***");

        return sb.toString();
    }
}