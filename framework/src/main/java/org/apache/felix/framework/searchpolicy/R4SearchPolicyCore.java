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
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Directive;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IModuleFactory;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.apache.felix.moduleloader.ModuleEvent;
import org.apache.felix.moduleloader.ModuleImpl;
import org.apache.felix.moduleloader.ModuleListener;
import org.apache.felix.moduleloader.ResourceNotFoundException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.Version;

public class R4SearchPolicyCore implements ModuleListener
{
    private Logger m_logger = null;
    private Map m_configMap = null;
    private IModuleFactory m_factory = null;
    // Maps a package name to an array of modules.
    private Map m_availPkgIndexMap = new HashMap();
    // Maps a package name to an array of modules.
    private Map m_inUsePkgIndexMap = new HashMap();
    // Maps a module to an array of capabilities.
    private Map m_inUseCapMap = new HashMap();
    private Map m_moduleDataMap = new HashMap();

    // Boot delegation packages.
    private String[] m_bootPkgs = null;
    private boolean[] m_bootPkgWildcards = null;

    // Listener-related instance variables.
    private static final ResolveListener[] m_emptyListeners = new ResolveListener[0];
    private ResolveListener[] m_listeners = m_emptyListeners;

    // Reusable empty array.
    public static final IModule[] m_emptyModules = new IModule[0];
    public static final ICapability[] m_emptyCapabilities = new ICapability[0];
    public static final PackageSource[] m_emptySources= new PackageSource[0];

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    public R4SearchPolicyCore(Logger logger, Map configMap)
    {
        m_logger = logger;
        m_configMap = configMap;

        // Read the boot delegation property and parse it.
        String s = (String) m_configMap.get(Constants.FRAMEWORK_BOOTDELEGATION);
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
        try
        {
            ICapability cap = Util.getSatisfyingCapability(module,
                new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")"));
            if (cap != null)
            {
                return new Object[] {
                    pkgName, // Spec title.
                    cap.getProperties().get(ICapability.VERSION_PROPERTY).toString(), // Spec version.
                    "", // Spec vendor.
                    "", // Impl title.
                    "", // Impl version.
                    "" // Impl vendor.
                };
            }
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
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
// TODO: FRAMEWORK - Consider opimizing this call to resolve, since it is called
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
                + ": cannot resolve requirement " + ex.getRequirement());
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
// TODO: FRAMEWORK - Consider opimizing this call to resolve, since it is called
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
                    + ex.getRequirement());
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
                    + ex.getRequirement());
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
            // Find the first class on the call stack that is not from
            // the class loader that loaded the Felix classes or is not
            // a class loader or class itself, because we want to ignore
            // calls to ClassLoader.loadClass() and Class.forName() since
            // we are trying to find out who instigated the class load.
            // Also since Felix uses threads for changing the start level
            // and refreshing packages, it is possible that there is no
            // module classes on the call stack; therefore, as soon as we
            // see Thread on the call stack we exit this loop. Other cases
            // where modules actually use threads are not an issue because
            // the module classes will be on the call stack before the
            // Thread class.
// TODO: FRAMEWORK - This check is a hack and we should see if we can think
// of another way to do it, since it won't necessarily work in all situations.
            if (Thread.class.equals(classes[i]))
            {
                break;
            }
            else if ((this.getClass().getClassLoader() != classes[i].getClassLoader())
                && !ClassLoader.class.isAssignableFrom(classes[i])
                && !Class.class.equals(classes[i])
                && !Proxy.class.equals(classes[i]))
            {
                // If there are no bundles providing exports for this
                // package and if the instigating class was not from a
                // bundle, then delegate to the parent class loader.
                // Otherwise, break out of loop and return null.
                boolean delegate = true;
                for (ClassLoader cl = classes[i].getClassLoader(); cl != null; cl = cl.getClass().getClassLoader())
                {
                    if (ContentClassLoader.class.isInstance(cl))
                    {
                        delegate = false;
                        break;
                    }
                }
                if (delegate)
                {
                    return this.getClass().getClassLoader().loadClass(name);
                }
                break;
            }
        }

        return null;
    }

    private IWire attemptDynamicImport(IModule importer, String pkgName)
    {
        R4Wire wire = null;
        PackageSource candidate = null;

        // There is an overriding assumption here that a package is
        // never split across bundles. If a package can be split
        // across bundles, then this will fail.

        // Only attempt to dynamically import a package if the module does
        // not already have a wire for the package; this may be the case if
        // the class being searched for actually does not exist.
        if (Util.getWire(importer, pkgName) == null)
        {
            // Loop through the importer's dynamic requirements to determine if
            // there is a matching one for the package from which we want to
            // load a class.
            IRequirement[] dynamics = importer.getDefinition().getDynamicRequirements();
            for (int i = 0; (dynamics != null) && (i < dynamics.length); i++)
            {
                // Ignore any dynamic requirements whose packages don't match.
                String dynPkgName = ((Requirement) dynamics[i]).getPackageName();
                boolean wildcard = (dynPkgName.lastIndexOf(".*") >= 0);
                dynPkgName = (wildcard)
                    ? dynPkgName.substring(0, dynPkgName.length() - 2) : dynPkgName;
                if (dynPkgName.equals("*") ||
                    pkgName.equals(dynPkgName) ||
                    (wildcard && pkgName.startsWith(dynPkgName + ".")))
                {
                    // Constrain the current dynamic requirement to include
                    // the precise package name for which we are searching; this
                    // is necessary because we cannot easily determine which
                    // package name a given dynamic requirement matches, since
                    // it is only a filter.

                    IRequirement req = null;
                    try
                    {
                        req = new Requirement(
                            ICapability.PACKAGE_NAMESPACE,
                            "(&" + dynamics[i].getFilter().toString()
                                + "(package=" + pkgName + "))");
                    }
                    catch (InvalidSyntaxException ex)
                    {
                        // This should never happen.
                    }

                    // See if there is a candidate exporter that satisfies the
                    // constrained dynamic requirement.
                    try
                    {
                        // Lock module manager instance to ensure that nothing changes.
                        synchronized (m_factory)
                        {
                            // Get "in use" and "available" candidates and put
                            // the "in use" candidates first.
                            PackageSource[] inuse = getInUseCandidates(req);
                            PackageSource[] available = getUnusedCandidates(req);
                            PackageSource[] candidates = new PackageSource[inuse.length + available.length];
                            System.arraycopy(inuse, 0, candidates, 0, inuse.length);
                            System.arraycopy(available, 0, candidates, inuse.length, available.length);

                            // Take the first candidate that can resolve.
                            for (int candIdx = 0;
                                (candidate == null) && (candIdx < candidates.length);
                                candIdx++)
                            {
                                try
                                {
                                    if (resolveDynamicImportCandidate(
                                        candidates[candIdx].m_module, importer))
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
                                IWire[] wires = importer.getWires();
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

                                // Create the wire and add it to the module.
                                wire = new R4Wire(
                                    importer, dynamics[i], candidate.m_module, candidate.m_capability);
                                newWires[newWires.length - 1] = wire;
                                ((ModuleImpl) importer).setWires(newWires);
m_logger.log(Logger.LOG_DEBUG, "WIRE: " + newWires[newWires.length - 1]);
                                return wire;
                            }
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

    private boolean resolveDynamicImportCandidate(IModule provider, IModule importer)
        throws ResolveException
    {
        // If the provider of the dynamically imported package is not
        // resolved, then we need to calculate the candidates to resolve
        // it and see if there is a consistent class space for the
        // provider. If there is no consistent class space, then a resolve
        // exception is thrown.
        Map candidatesMap = new HashMap();
        if (!isResolved(provider))
        {
            populateCandidatesMap(candidatesMap, provider);
            findConsistentClassSpace(candidatesMap, provider);
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
                        return false;
                    }
                }
            }
        }

        Map resolvedModuleWireMap = createWires(candidatesMap, provider);

        // Fire resolved events for all resolved modules;
        // the resolved modules array will only be set if the resolve
        // was successful.
        if (resolvedModuleWireMap != null)
        {
            Iterator iter = resolvedModuleWireMap.entrySet().iterator();
            while (iter.hasNext())
            {
                fireModuleResolved((IModule) ((Map.Entry) iter.next()).getKey());
            }
        }

        return true;
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

    public PackageSource[] getInUseCandidates(IRequirement req)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_factory)
        {
            PackageSource[] candidates = m_emptySources;
            if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                (((Requirement) req).getPackageName() != null))
            {
                String pkgName = ((Requirement) req).getPackageName();
                IModule[] modules = (IModule[]) m_inUsePkgIndexMap.get(pkgName);

                for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
                {
                    ICapability inUseCap = Util.getSatisfyingCapability(modules[modIdx], req);
                    if (inUseCap != null)
                    {
// TODO: RB - Is this permission check correct.
                        if ((System.getSecurityManager() != null) &&
                            !((ProtectionDomain) modules[modIdx].getSecurityContext()).implies(
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
                                new PackageSource(modules[modIdx], inUseCap);
                            candidates = tmp;
                        }
                    }
                }
            }
            else
            {
                Iterator i = m_inUseCapMap.entrySet().iterator();
                while (i.hasNext())
                {
                    Map.Entry entry = (Map.Entry) i.next();
                    IModule module = (IModule) entry.getKey();
                    ICapability[] inUseCaps = (ICapability[]) entry.getValue();
                    for (int capIdx = 0; capIdx < inUseCaps.length; capIdx++)
                    {
                        if (req.isSatisfied(inUseCaps[capIdx]))
                        {
// TODO: RB - Is this permission check correct.
                            if (inUseCaps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                                (System.getSecurityManager() != null) &&
                                !((ProtectionDomain) module.getSecurityContext()).implies(
                                    new PackagePermission(
                                        (String) inUseCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY),
                                        PackagePermission.EXPORT)))
                            {
                                m_logger.log(Logger.LOG_DEBUG,
                                    "PackagePermission.EXPORT denied for "
                                    + inUseCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY)
                                    + "from " + module.getId());
                            }
                            else
                            {
                                PackageSource[] tmp = new PackageSource[candidates.length + 1];
                                System.arraycopy(candidates, 0, tmp, 0, candidates.length);
                                tmp[candidates.length] = new PackageSource(module, inUseCaps[capIdx]);
                                candidates = tmp;
                            }
                        }
                    }
                }
            }
            Arrays.sort(candidates);
            return candidates;
        }
    }

    private boolean isCapabilityInUse(IModule module, ICapability cap)
    {
        ICapability[] caps = (ICapability[]) m_inUseCapMap.get(module);
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].equals(cap))
            {
                return true;
            }
        }
        return false;
    }

    public PackageSource[] getUnusedCandidates(IRequirement req)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_factory)
        {
            // Get all modules.
            IModule[] modules = null;
            if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                (((Requirement) req).getPackageName() != null))
            {
                modules = (IModule[]) m_availPkgIndexMap.get(((Requirement) req).getPackageName());
            }
            else
            {
                modules = m_factory.getModules();
            }

            // Create list of compatible providers.
            PackageSource[] candidates = m_emptySources;
            for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
            {
                // Get the module's export package for the target package.
                ICapability cap = Util.getSatisfyingCapability(modules[modIdx], req);
                // If compatible and it is not currently used, then add
                // the available candidate to the list.
                if ((cap != null) && !isCapabilityInUse(modules[modIdx], cap))
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
    }

    public void resolve(IModule rootModule)
        throws ResolveException
    {
        // If the module is already resolved, then we can just return.
        if (isResolved(rootModule))
        {
            return;
        }

        // This variable maps an unresolved module to a list of candidate
        // sets, where there is one candidate set for each requirement that
        // must be resolved. A candidate set contains the potential canidates
        // available to resolve the requirement and the currently selected
        // candidate index.
        Map candidatesMap = new HashMap();

        // This map will be used to hold the final wires for all
        // resolved modules, which can then be used to fire resolved
        // events outside of the synchronized block.
        Map resolvedModuleWireMap = null;

        // Synchronize on the module manager, because we don't want
        // any modules being added or removed while we are in the
        // middle of this operation.
        synchronized (m_factory)
        {
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
            populateCandidatesMap(candidatesMap, rootModule);

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
            findConsistentClassSpace(candidatesMap, rootModule);

            // The final step is to create the wires for the root module and
            // transitively all modules that are to be resolved from the
            // selected candidates for resolving the root module's imports.
            // When this call returns, each module's wiring and resolved
            // attributes are set. The resulting wiring map is used below
            // to fire resolved events outside of the synchronized block.
            // The resolved module wire map maps a module to its array of
            // wires.
            resolvedModuleWireMap = createWires(candidatesMap, rootModule);

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

    private void populateCandidatesMap(Map candidatesMap, IModule module)
        throws ResolveException
    {
        // Detect cycles.
        if (candidatesMap.get(module) != null)
        {
            return;
        }

        // List to hold the resolving candidate sets for the module's
        // requirements.
        List candSetList = new ArrayList();

        // Even though the candidate set list is currently empty, we
        // record it in the candidates map early so we can use it to
        // detect cycles.
        candidatesMap.put(module, candSetList);

        // Loop through each requirement and calculate its resolving
        // set of candidates.
        IRequirement[] reqs = module.getDefinition().getRequirements();
        for (int reqIdx = 0; (reqs != null) && (reqIdx < reqs.length); reqIdx++)
        {
            // Get the candidates from the "in use" and "available"
            // package maps. Candidates "in use" have higher priority
            // than "available" ones, so put the "in use" candidates
            // at the front of the list of candidates.
            PackageSource[] inuse = getInUseCandidates(reqs[reqIdx]);
            PackageSource[] available = getUnusedCandidates(reqs[reqIdx]);
            PackageSource[] candidates = new PackageSource[inuse.length + available.length];
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
                        if (!isResolved(candidates[candIdx].m_module))
                        {
                            populateCandidatesMap(candidatesMap, candidates[candIdx].m_module);
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
                        "Unable to resolve.", module, reqs[reqIdx]);
                }
            }
            else if (candidates.length > 0)
            {
                candSetList.add(
                    new CandidateSet(module, reqs[reqIdx], candidates));
            }
        }
    }

    private void dumpUsedPackages()
    {
        synchronized (this)
        {
            System.out.println("PACKAGES IN USE MAP:");
            for (Iterator i = m_inUseCapMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                ICapability[] caps = (ICapability[]) entry.getValue();
                if ((caps != null) && (caps.length > 0))
                {
                    System.out.println("  " + entry.getKey());
                    for (int j = 0; j < caps.length; j++)
                    {
                        System.out.println("    " + caps[j]);
                    }
                }
            }
        }
    }

    private void dumpPackageSources(Map pkgMap)
    {
        for (Iterator i = pkgMap.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            ResolvedPackage rp = (ResolvedPackage) entry.getValue();
            System.out.println(rp);
        }
    }

    private void findConsistentClassSpace(Map candidatesMap, IModule rootModule)
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
        while (!isClassSpaceConsistent(rootModule, moduleMap, cycleMap, candidatesMap))
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
                    candidatesList.add(((Map.Entry) iter.next()).getValue());
                }
            }

            // Increment the candidate configuration so we can test again.
            incrementCandidateConfiguration(candidatesList);

            // Clear the module map.
            moduleMap.clear();

            // Clear the cycle map.
            cycleMap.clear();
        }
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
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Map calculateUsesConstraints(
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

    private Map calculateUsesConstraints(
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

    private Map getModulePackages(Map moduleMap, IModule module, Map candidatesMap)
        throws ResolveException
    {
        Map map = (Map) moduleMap.get(module);

        if (map == null)
        {
            map = calculateModulePackages(module, candidatesMap);
            moduleMap.put(module, map);
//if (!module.getId().equals("0"))
//{
//    System.out.println("PACKAGES FOR " + module.getId() + ":");
//    dumpPackageSources(map);
//}
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
    private Map calculateModulePackages(IModule module, Map candidatesMap)
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

    private Map calculateImportedPackages(IModule targetModule, Map candidatesMap)
        throws ResolveException
    {
        return (candidatesMap.get(targetModule) == null)
            ? calculateImportedPackagesResolved(targetModule)
            : calculateImportedPackagesUnresolved(targetModule, candidatesMap);
    }

    private Map calculateImportedPackagesUnresolved(IModule targetModule, Map candidatesMap)
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
            PackageSource ps = cs.m_candidates[cs.m_idx];

            if (ps.m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    ps.m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY);

                ResolvedPackage rp = new ResolvedPackage(pkgName);
                rp.m_sourceList.add(ps);
                pkgMap.put(rp.m_name, rp);

                // TODO: EXPERIMENTAL - Experimental implicit wire concept to try
                //       to deal with code generation.
                // Get implicitly imported packages as defined by the provider
                // of our imported package, unless we are the provider.
                if (!targetModule.equals(ps.m_module))
                {
                    Map implicitPkgMap = calculateImplicitImportedPackages(
                        ps.m_module, ps.m_capability, candidatesMap, new HashMap());
                    // Merge the implicitly imported packages with our imports and
                    // verify that there is no overlap.
                    for (Iterator i = implicitPkgMap.entrySet().iterator(); i.hasNext(); )
                    {
                        Map.Entry entry = (Map.Entry) i.next();
                        ResolvedPackage implicit = (ResolvedPackage) entry.getValue();
                        ResolvedPackage existing = (ResolvedPackage) pkgMap.get(entry.getKey());
                        if ((existing != null) &&
                            !(existing.isSubset(implicit) && implicit.isSubset(existing)))
                        {
                            throw new ResolveException(
                                "Implicit import of "
                                + entry.getKey()
                                + " from "
                                + implicit
                                + " duplicates an existing import from "
                                + existing,
                                targetModule,
                                cs.m_requirement);
                        }
                        pkgMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return pkgMap;
    }

    private Map calculateImportedPackagesResolved(IModule targetModule)
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
                rp = (rp == null) ? new ResolvedPackage(pkgName) : rp;
                rp.m_sourceList.add(new PackageSource(wires[wireIdx].getExporter(), wires[wireIdx].getCapability()));
                pkgMap.put(rp.m_name, rp);

                // TODO: EXPERIMENTAL - Experimental implicit wire concept to try
                //       to deal with code generation.
                // Get implicitly imported packages as defined by the provider
                // of our imported package, unless we are the provider.
                if (!targetModule.equals(wires[wireIdx].getExporter()))
                {
                    Map implicitPkgMap = calculateImplicitImportedPackagesResolved(
                        wires[wireIdx].getExporter(), wires[wireIdx].getCapability(), new HashMap());
                    // Merge the implicitly imported packages with our imports.
                    // No need to verify overlap since this is resolved and should
                    // be consistent.
                    for (Iterator i = implicitPkgMap.entrySet().iterator(); i.hasNext(); )
                    {
                        Map.Entry entry = (Map.Entry) i.next();
                        pkgMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return pkgMap;
    }

    private Map calculateImplicitImportedPackages(
        IModule targetModule, ICapability targetCapability,
        Map candidatesMap, Map cycleMap)
    {
        return (candidatesMap.get(targetModule) == null)
            ? calculateImplicitImportedPackagesResolved(
                targetModule, targetCapability, cycleMap)
            : calculateImplicitImportedPackagesUnresolved(
                targetModule, targetCapability, candidatesMap, cycleMap);
    }

    // TODO: EXPERIMENTAL - This is currently not defined recursively, but it should be.
    //       Currently, it only assumes that a provider can cause implicit imports for
    //       packages that it exports.
    private Map calculateImplicitImportedPackagesUnresolved(
        IModule targetModule, ICapability targetCapability,
        Map candidatesMap, Map cycleMap)
    {
        Map pkgMap = new HashMap();

        R4Directive[] dirs = ((Capability) targetCapability).getDirectives();
        if (dirs != null)
        {
            for (int dirIdx = 0; dirIdx < dirs.length; dirIdx++)
            {
                if (dirs[dirIdx].getName().equals("x-implicitwire"))
                {
                    String[] pkgs = ManifestParser.parseDelimitedString(dirs[dirIdx].getValue(), ",");
                    for (int pkgIdx = 0; pkgIdx < pkgs.length; pkgIdx++)
                    {
                        ResolvedPackage rp = new ResolvedPackage(pkgs[pkgIdx].trim());
                        rp.m_sourceList.add(
                            new PackageSource(
                                targetModule,
                                getExportPackageCapability(targetModule, pkgs[pkgIdx])));
                        pkgMap.put(rp.m_name, rp);
                    }
                }
            }
        }

        return pkgMap;
    }

    // TODO: EXPERIMENTAL - This is currently not defined recursively, but it should be.
    //       Currently, it only assumes that a provider can cause implicit imports for
    //       packages that it exports.
    private Map calculateImplicitImportedPackagesResolved(
        IModule targetModule, ICapability targetCapability, Map cycleMap)
    {
        Map pkgMap = new HashMap();

        R4Directive[] dirs = ((Capability) targetCapability).getDirectives();
        if (dirs != null)
        {
            for (int dirIdx = 0; dirIdx < dirs.length; dirIdx++)
            {
                if (dirs[dirIdx].getName().equals("x-implicitwire"))
                {
                    String[] pkgs = ManifestParser.parseDelimitedString(dirs[dirIdx].getValue(), ",");
                    for (int pkgIdx = 0; pkgIdx < pkgs.length; pkgIdx++)
                    {
                        ResolvedPackage rp = new ResolvedPackage(pkgs[pkgIdx].trim());
                        rp.m_sourceList.add(
                            new PackageSource(
                                targetModule,
                                getExportPackageCapability(targetModule, pkgs[pkgIdx])));
                        pkgMap.put(rp.m_name, rp);
                    }
                }
            }
        }

        return pkgMap;
    }

    private Map calculateCandidateImplicitImportedPackages(IModule module, PackageSource psTarget, Map candidatesMap)
    {
//System.out.println("calculateCandidateImplicitPackages("+module+")");
        // Cannot implicitly wire to oneself.
        if (!module.equals(psTarget.m_module))
        {
            Map cycleMap = new HashMap();
            cycleMap.put(module, module);
            return calculateImplicitImportedPackages(
                psTarget.m_module, psTarget.m_capability, candidatesMap, cycleMap);
        }

        return null;
    }

    private Map calculateExportedPackages(IModule targetModule)
    {
//System.out.println("calculateExportedPackages("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through the target module's capabilities that represent
        // exported packages and add them to the exported package map.
        ICapability[] caps = targetModule.getDefinition().getCapabilities();
        for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    caps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName) : rp;
                rp.m_sourceList.add(new PackageSource(targetModule, caps[capIdx]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private Map calculateRequiredPackages(IModule targetModule, Map candidatesMap)
    {
        return (candidatesMap.get(targetModule) == null)
            ? calculateRequiredPackagesResolved(targetModule)
            : calculateRequiredPackagesUnresolved(targetModule, candidatesMap);
    }

    private Map calculateRequiredPackagesUnresolved(IModule targetModule, Map candidatesMap)
    {
//System.out.println("calculateRequiredPackagesUnresolved("+targetModule+")");
        Map pkgMap = new HashMap();

        // Loop through target module's candidate list for candidates
        // for its module dependencies and merge re-exported packages.
        List candSetList = (List) candidatesMap.get(targetModule);
        for (int candSetIdx = 0; (candSetList != null) && (candSetIdx < candSetList.size()); candSetIdx++)
        {
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);
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

    private Map calculateRequiredPackagesResolved(IModule targetModule)
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

    private Map calculateExportedAndReexportedPackages(
        PackageSource psTarget, Map candidatesMap, Map cycleMap)
    {
        return (candidatesMap.get(psTarget.m_module) == null)
            ? calculateExportedAndReexportedPackagesResolved(psTarget.m_module, cycleMap)
            : calculateExportedAndReexportedPackagesUnresolved(psTarget, candidatesMap, cycleMap);
    }

    private Map calculateExportedAndReexportedPackagesUnresolved(
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
        ICapability[] candCaps = psTarget.m_module.getDefinition().getCapabilities();
        for (int capIdx = 0; (candCaps != null) && (capIdx < candCaps.length); capIdx++)
        {
            if (candCaps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    candCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName) : rp;
                rp.m_sourceList.add(new PackageSource(psTarget.m_module, candCaps[capIdx]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private Map calculateExportedAndReexportedPackagesResolved(
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
        ICapability[] caps = targetModule.getDefinition().getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                String pkgName = (String)
                    caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                ResolvedPackage rp = (ResolvedPackage) pkgMap.get(pkgName);
                rp = (rp == null) ? new ResolvedPackage(pkgName) : rp;
                rp.m_sourceList.add(new PackageSource(targetModule, caps[i]));
                pkgMap.put(rp.m_name, rp);
            }
        }

        return pkgMap;
    }

    private Map calculateCandidateRequiredPackages(IModule module, PackageSource psTarget, Map candidatesMap)
    {
//System.out.println("calculateCandidateRequiredPackages("+module+")");
        Map cycleMap = new HashMap();
        cycleMap.put(module, module);
        return calculateExportedAndReexportedPackages(psTarget, candidatesMap, cycleMap);
    }

    private void incrementCandidateConfiguration(List resolverList)
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

    private Map createWires(Map candidatesMap, IModule rootModule)
    {
        Map resolvedModuleWireMap =
            populateWireMap(candidatesMap, rootModule, new HashMap());
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
                // Add the exporter module of the wire to the "in use" capability map.
                ICapability[] inUseCaps = (ICapability[]) m_inUseCapMap.get(wires[wireIdx].getExporter());
                inUseCaps = addCapabilityToArray(inUseCaps, wires[wireIdx].getCapability());
                m_inUseCapMap.put(wires[wireIdx].getExporter(), inUseCaps);

                // If the capability is a package, then add the exporter module
                // of the wire to the "in use" package index and remove it
                // from the "available" package index.
                if (wires[wireIdx].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    // Get package name.
                    String pkgName = (String)
                        wires[wireIdx].getCapability().getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Add to "in use" package index.
                    indexPackageCapability(
                        m_inUsePkgIndexMap,
                        wires[wireIdx].getExporter(),
                        wires[wireIdx].getCapability());
                    // Remove from "available" package index.
                    m_availPkgIndexMap.put(
                        pkgName,
                        removeModuleFromArray(
                            (IModule[]) m_availPkgIndexMap.get(pkgName),
                            wires[wireIdx].getExporter()));
                }
            }

            // Also add the module's capabilities to the "in use" map
            // if the capability is not matched by a requirement. If the
            // capability is matched by a requirement, then it is handled
            // above when adding the wired modules to the "in use" map.
// TODO: RB - Bug here because a requirement for a package need not overlap the
//            capability for that package and this assumes it does. This might
//            require us to introduce the notion of a substitutable capability.
            ICapability[] caps = module.getDefinition().getCapabilities();
            IRequirement[] reqs = module.getDefinition().getRequirements();
            for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
            {
                boolean matched = false;
                for (int reqIdx = 0;
                    !matched && (reqs != null) && (reqIdx < reqs.length);
                    reqIdx++)
                {
                    if (reqs[reqIdx].isSatisfied(caps[capIdx]))
                    {
                        matched = true;
                    }
                }
                if (!matched)
                {
                    ICapability[] inUseCaps = (ICapability[]) m_inUseCapMap.get(module);
                    inUseCaps = addCapabilityToArray(inUseCaps, caps[capIdx]);
                    m_inUseCapMap.put(module, inUseCaps);

                    // If the capability is a package, then add the exporter module
                    // of the wire to the "in use" package index and remove it
                    // from the "available" package index.
                    if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        // Get package name.
                        String pkgName = (String)
                            caps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
                        // Add to "in use" package index.
                        indexPackageCapability(
                            m_inUsePkgIndexMap,
                            module,
                            caps[capIdx]);
                        // Remove from "available" package index.
                        m_availPkgIndexMap.put(
                            pkgName,
                            removeModuleFromArray(
                                (IModule[]) m_availPkgIndexMap.get(pkgName),
                                module));
                    }
                }
            }
        }

        return resolvedModuleWireMap;
    }

    private Map populateWireMap(Map candidatesMap, IModule importer, Map wireMap)
    {
        // If the module is already resolved or it is part of
        // a cycle, then just return the wire map.
        if (isResolved(importer) || (wireMap.get(importer) != null))
        {
            return wireMap;
        }

        List candSetList = (List) candidatesMap.get(importer);
        List moduleWires = new ArrayList();
        List packageWires = new ArrayList();
        IWire[] wires = new IWire[candSetList.size()];

        // Put the module in the wireMap with an empty wire array;
        // we do this early so we can use it to detect cycles.
        wireMap.put(importer, wires);

        // Loop through each candidate Set and create a wire
        // for the selected candidate for the associated import.
        for (int candSetIdx = 0; candSetIdx < candSetList.size(); candSetIdx++)
        {
            // Get the current candidate set.
            CandidateSet cs = (CandidateSet) candSetList.get(candSetIdx);

            // Create a wire for the current candidate based on the type
            // of requirement it resolves.
            if (cs.m_requirement.getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                moduleWires.add(new R4WireModule(
                    importer,
                    cs.m_requirement,
                    cs.m_candidates[cs.m_idx].m_module,
                    cs.m_candidates[cs.m_idx].m_capability,
                    calculateCandidateRequiredPackages(importer, cs.m_candidates[cs.m_idx], candidatesMap)));
            }
            else
            {
                // Add wire for imported package.
                packageWires.add(new R4Wire(
                    importer,
                    cs.m_requirement,
                    cs.m_candidates[cs.m_idx].m_module,
                    cs.m_candidates[cs.m_idx].m_capability));

                // TODO: EXPERIMENTAL - The following is part of an experimental
                //       implicit imported wire concept. The above code is how
                //       the wire should normally be created.
                // Add wires for any implicitly imported package from provider.
                Map pkgMap = calculateCandidateImplicitImportedPackages(
                    importer, cs.m_candidates[cs.m_idx], candidatesMap);
                if (pkgMap != null)
                {
                    for (Iterator i = pkgMap.entrySet().iterator(); i.hasNext(); )
                    {
                        Map.Entry entry = (Map.Entry) i.next();
                        ResolvedPackage rp = (ResolvedPackage) entry.getValue();
                        packageWires.add(new R4Wire(
                            importer,
                            cs.m_requirement, // TODO: This is not really correct.
                            ((PackageSource) rp.m_sourceList.get(0)).m_module,
                            ((PackageSource) rp.m_sourceList.get(0)).m_capability));
                    }
                }
            }

            // Create any necessary wires for the selected candidate module.
            wireMap = populateWireMap(
                candidatesMap, cs.m_candidates[cs.m_idx].m_module, wireMap);
        }

        packageWires.addAll(moduleWires);
        wireMap.put(importer, packageWires.toArray(wires));

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
// TODO: FRAMEWORK - Call this method where appropriate.
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
            ICapability[] caps = module.getDefinition().getCapabilities();

            // Add exports to available package map.
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    indexPackageCapability(m_availPkgIndexMap, module, caps[i]);
                }
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
            ICapability[] caps = event.getModule().getDefinition().getCapabilities();
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    // Get package name.
                    String pkgName = (String)
                        caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Remove from "available" package map.
                    IModule[] modules = (IModule[]) m_availPkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, event.getModule());
                        m_availPkgIndexMap.put(pkgName, modules);
                    }

                    // Remove from "in use" package map.
                    modules = (IModule[]) m_inUsePkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, event.getModule());
                        m_inUsePkgIndexMap.put(pkgName, modules);
                    }
                }
            }

            // Set wires to null, which will remove the module from all
            // of its dependent modules.
            ((ModuleImpl) event.getModule()).setWires(null);
            // Remove the module from the "in use" map.
// TODO: RB - Maybe this can be merged with ModuleData.
            m_inUseCapMap.remove(event.getModule());
            // Finally, remove module data.
            m_moduleDataMap.remove(event.getModule());
        }
    }

    /**
     * This is an experimental method that is likely to change or go
     * away - so don't use it for now.
     *
     * Note to self, we need to think about what the implications of
     * this are and whether we are fine with them.
     */
    /*
     * This method is used by the framework to let us know that we need to re-read
     * the system bundle capabilities which have been extended by an extension bundle.
     *
     * For now we assume that capabilities have been added only. We might need to
     * enforce that at one point of time.
     */
    public void moduleRefreshed(ModuleEvent event)
    {
        synchronized (m_factory)
        {
            IModule module = event.getModule();
         // Remove exports from package maps.
            ICapability[] caps = event.getModule().getDefinition().getCapabilities();
            // Add exports to available package map.
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    indexPackageCapability(m_availPkgIndexMap, module, caps[i]);
                }


                ICapability[] inUseCaps = (ICapability[]) m_inUseCapMap.get(module);
                inUseCaps = addCapabilityToArray(inUseCaps, caps[i]);
                m_inUseCapMap.put(module, inUseCaps);

                // If the capability is a package, then add the exporter module
                // of the wire to the "in use" package index and remove it
                // from the "available" package index.
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    // Get package name.
                    String pkgName = (String)
                        caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Add to "in use" package index.
                    indexPackageCapability(
                        m_inUsePkgIndexMap,
                        module,
                        caps[i]);
                    // Remove from "available" package index.
                    m_availPkgIndexMap.put(
                        pkgName,
                        removeModuleFromArray(
                            (IModule[]) m_availPkgIndexMap.get(pkgName),
                            module));
                }
            }
        }
    }

    private void indexPackageCapability(Map map, IModule module, ICapability capability)
    {
        if (capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            String pkgName = (String)
                capability.getProperties().get(ICapability.PACKAGE_PROPERTY);
            IModule[] modules = (IModule[]) map.get(pkgName);

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
                    Version version = (Version)
                        capability.getProperties().get(ICapability.VERSION_PROPERTY);
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

    public static ICapability getExportPackageCapability(IModule m, String pkgName)
    {
        ICapability[] caps = m.getDefinition().getCapabilities();
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

    //
    // Simple utility methods.
    //
/*
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
*/
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
        } while (idx >= 0);
        return modules;
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

    private static ICapability[] addCapabilityToArray(ICapability[] caps, ICapability cap)
    {
        // Verify that the inuse is not already in the array.
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

    private class CandidateSet
    {
        public IModule m_module = null;
        public IRequirement m_requirement = null;
        public PackageSource[] m_candidates = null;
        public int m_idx = 0;
        public boolean m_visited = false;
        public CandidateSet(IModule module, IRequirement requirement, PackageSource[] candidates)
        {
            m_module = module;
            m_requirement = requirement;
            m_candidates = candidates;
            if (isResolved(m_module))
            {
                m_visited = true;
            }
        }
    }

    /**
     * This utility class represents a source for a given package, where
     * the package is indicated by a particular module and the module's
     * capability associated with that package. This class also implements
     * <tt>Comparable</tt> so that two package sources can be compared based
     * on version and bundle identifiers.
    **/
    public class PackageSource implements Comparable
    {
        public IModule m_module = null;
        public ICapability m_capability = null;

        public PackageSource(IModule module, ICapability capability)
        {
            m_module = module;
            m_capability = capability;
        }

        public int compareTo(Object o)
        {
            PackageSource ps = (PackageSource) o;

            if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                Version thisVersion = ((Capability) m_capability).getPackageVersion();
                Version version = ((Capability) ps.m_capability).getPackageVersion();

                // Sort in reverse version order.
                int cmp = thisVersion.compareTo(version);
                if (cmp < 0)
                {
                    return 1;
                }
                else if (cmp > 0)
                {
                    return -1;
                }
                else
                {
                    // Sort further by ascending bundle ID.
                    long thisId = Util.getBundleIdFromModuleId(m_module.getId());
                    long id = Util.getBundleIdFromModuleId(ps.m_module.getId());
                    if (thisId < id)
                    {
                        return -1;
                    }
                    else if (thisId > id)
                    {
                        return 1;
                    }
                    return 0;
                }
            }
            else
            {
                return -1;
            }
        }

        public int hashCode()
        {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((m_capability == null) ? 0 : m_capability.hashCode());
            result = PRIME * result + ((m_module == null) ? 0 : m_module.hashCode());
            return result;
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null)
            {
                return false;
            }
            if (getClass() != o.getClass())
            {
                return false;
            }
            PackageSource ps = (PackageSource) o;
            return (m_module.equals(ps.m_module) && (m_capability == ps.m_capability));
        }
    }

    /**
     * This utility class a resolved package, which is comprised of a
     * set of <tt>PackageSource</tt>s that is calculated by the resolver
     * algorithm. A given resolved package may have a single package source,
     * as is the case with imported packages, or it may have multiple
     * package sources, as is the case with required bundles.
    **/
    protected class ResolvedPackage
    {
        public String m_name = null;
        public List m_sourceList = new ArrayList();

        public ResolvedPackage(String name)
        {
            m_name = name;
        }

        public boolean isSubset(ResolvedPackage rp)
        {
            if (m_sourceList.size() > rp.m_sourceList.size())
            {
                return false;
            }
            else if (!m_name.equals(rp.m_name))
            {
                return false;
            }

            // Determine if the target set of source modules is a subset.
            return rp.m_sourceList.containsAll(m_sourceList);
        }

        public Object clone()
        {
            ResolvedPackage rp = new ResolvedPackage(m_name);
            rp.m_sourceList.addAll(m_sourceList);
            return rp;
        }

        public void merge(ResolvedPackage rp)
        {
            // Merge required packages, avoiding duplicate
            // package sources and maintaining ordering.
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                if (!m_sourceList.contains(rp.m_sourceList.get(srcIdx)))
                {
                    m_sourceList.add(rp.m_sourceList.get(srcIdx));
                }
            }
        }

        public String toString()
        {
            return toString("", new StringBuffer()).toString();
        }

        public StringBuffer toString(String padding, StringBuffer sb)
        {
            sb.append(padding);
            sb.append(m_name);
            sb.append(" from [");
            for (int i = 0; i < m_sourceList.size(); i++)
            {
                PackageSource ps = (PackageSource) m_sourceList.get(i);
                sb.append(ps.m_module);
                if ((i + 1) < m_sourceList.size())
                {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb;
        }
    }

    //
    // Diagnostics.
    //

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
            if (wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                wires[i].getCapability().getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
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
        IRequirement[] reqs = module.getDefinition().getRequirements();
/*
 * TODO: RB - Fix diagnostic message for optional imports.
        for (int i = 0; (reqs != null) && (i < reqs.length); i++)
        {
            if (reqs[i].getName().equals(pkgName) && reqs[i].isOptional())
            {
                // Try to see if there is an exporter available.
                IModule[] exporters = getInUseExporters(reqs[i], true);
                exporters = (exporters.length == 0)
                    ? getAvailableExporters(reqs[i], true) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getInUseExporters(pkgReq, true);
                    exporters = (exporters.length == 0)
                        ? getAvailableExporters(pkgReq, true) : exporters;
                }

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
                    if (reqs[i].isSatisfied(
                        Util.getExportPackage(exporters[0], reqs[i].getName())))
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
*/
        // Next, check to see if the package is dynamically imported by the module.
// TODO: RB - Fix dynamic import diagnostic.
//        IRequirement imp = createDynamicImportTarget(module, pkgName);
        IRequirement imp = null;
        if (imp != null)
        {
            // Try to see if there is an exporter available.
            PackageSource[] exporters = getInUseCandidates(imp);
            exporters = (exporters.length == 0)
                ? getUnusedCandidates(imp) : exporters;

            // An exporter might be available, but it may have attributes
            // that do not match the importer's required attributes, so
            // check that case by simply looking for an exporter of the
            // desired package without any attributes.
            if (exporters.length == 0)
            {
                try
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getInUseCandidates(pkgReq);
                    exporters = (exporters.length == 0)
                        ? getUnusedCandidates(pkgReq) : exporters;
                }
                catch (InvalidSyntaxException ex)
                {
                    // This should never happen.
                }
            }

            long expId = (exporters.length == 0)
                ? -1 : Util.getBundleIdFromModuleId(exporters[0].m_module.getId());

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found, but this is likely normal since package '");
            sb.append(pkgName);
            sb.append("' is dynamically imported by bundle ");
            sb.append(impId);
            sb.append(".");
            if (exporters.length > 0)
            {
                try
                {
                    if (!imp.isSatisfied(
                        Util.getSatisfyingCapability(exporters[0].m_module,
                            new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")"))))
                    {
                        sb.append(" However, bundle ");
                        sb.append(expId);
                        sb.append(" does export this package with attributes that do not match.");
                    }
                }
                catch (InvalidSyntaxException ex)
                {
                    // This should never happen.
                }
            }
            sb.append(" ***");

            return sb.toString();
        }
        IRequirement pkgReq = null;
        try
        {
            pkgReq = new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }
        PackageSource[] exporters = getInUseCandidates(pkgReq);
        exporters = (exporters.length == 0) ? getUnusedCandidates(pkgReq) : exporters;
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

            long expId = Util.getBundleIdFromModuleId(exporters[0].m_module.getId());

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