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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.framework.BundleProtectionDomain;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.framework.util.manifestparser.R4Attribute;
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
import org.osgi.framework.Bundle;

public class R4SearchPolicyCore implements ModuleListener
{
    private Logger m_logger = null;
    private Map m_configMap = null;
    private IModuleFactory m_factory = null;
    // Maps a package name to an array of modules.
    private Map m_unresolvedPkgIndexMap = new HashMap();
    // Maps a package name to an array of modules.
    private Map m_resolvedPkgIndexMap = new HashMap();
    // Maps a module to an array of capabilities.
    private Map m_resolvedCapMap = new HashMap();
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
    public static final PackageSource[] m_emptySources = new PackageSource[0];

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

    /**
     * Private utility method to check module resolved state.
     * CONCURRENCY NOTE: This method must be called while holding
     * a lock on m_factory.
    **/
    private boolean isResolved(IModule module)
    {
        ModuleData data = (ModuleData) m_moduleDataMap.get(module);
        return (data == null) ? false : data.m_resolved;
    }

    /**
     * Private utility method to set module resolved state.
     * CONCURRENCY NOTE: This method must be called while holding
     * a lock on m_factory.
    **/
    private void setResolved(IModule module, boolean resolved)
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
        Map headerMap = ((ModuleDefinition) module.getDefinition()).getHeaders();
        String spectitle = (String) headerMap.get("Specification-Title");
        String specversion = (String) headerMap.get("Specification-Version");
        String specvendor = (String) headerMap.get("Specification-Vendor");
        String impltitle = (String) headerMap.get("Implementation-Title");
        String implversion = (String) headerMap.get("Implementation-Version");
        String implvendor = (String) headerMap.get("Implementation-Vendor");
        if ((spectitle != null)
            || (specversion != null)
            || (specvendor != null)
            || (impltitle != null)
            || (implversion != null)
            || (implvendor != null))
        {
            return new Object[] {
                spectitle, specversion, specvendor, impltitle, implversion, implvendor
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
            String msg = name;
            if (m_logger.getLogLevel() >= Logger.LOG_DEBUG)
            {
                msg = diagnoseClassLoadError(module, name);
            }
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
        Enumeration urls = null;
        List enums = new ArrayList();

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
                    }
                    catch (IOException ex)
                    {
                        // This shouldn't happen and even if it does, there
                        // is nothing we can do, so just ignore it.
                    }
                    // If this is a java.* package, then always terminate the
                    // search; otherwise, continue to look locally.
                    if (m_bootPkgs[i].startsWith("java."))
                    {
                        return urls;
                    }

                    enums.add(urls);
                    break;
                }
            }
        }

        // Look in the module's imports.
        // We delegate to the module's wires for the resources.
        // If any resources are found, this means that the package of these
        // resources is imported, we must not keep looking since we do not
        // support split-packages.

        // Note that the search may be aborted if this method throws an
        // exception, otherwise it continues if a null is returned.
        IWire[] wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i] instanceof R4Wire)
            {
                // If we find the class or resource, then return it.
                urls = wires[i].getResources(name);
                if (urls != null)
                {
                    enums.add(urls);
                    return new CompoundEnumeration((Enumeration[])
                        enums.toArray(new Enumeration[enums.size()]));
                }
            }
        }

        // See whether we can get the resource from the required bundles and
        // regardless of whether or not this is the case continue to the next
        // step potentially passing on the result of this search (if any).
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i] instanceof R4WireModule)
            {
                // If we find the class or resource, then add it.
                urls = wires[i].getResources(name);
                if (urls != null)
                {
                    enums.add(urls);
                }
            }
        }

        // Try the module's own class path. If we can find the resource then
        // return it together with the results from the other searches else
        // try to look into the dynamic imports.
        urls = module.getContentLoader().getResources(name);
        if (urls != null)
        {
            enums.add(urls);
        }
        else
        {
            // If not found, then try the module's dynamic imports.
            // At this point, the module's imports were searched and so was the
            // the module's content. Now we make an attempt to load the
            // class/resource via a dynamic import, if possible.
            IWire wire = attemptDynamicImport(module, pkgName);
            if (wire != null)
            {
                urls = wire.getResources(name);

                if (urls != null)
                {
                    enums.add(urls);
                }
            }
        }

        return new CompoundEnumeration((Enumeration[])
            enums.toArray(new Enumeration[enums.size()]));
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
        Object result = null;
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
                    try
                    {
                        result = (isClass)
                            ? (Object) getClass().getClassLoader().loadClass(name)
                            : (Object) getClass().getClassLoader().getResource(name);
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (m_bootPkgs[i].startsWith("java.") || (result != null))
                        {
                            return result;
                        }
                    }
                    catch (ClassNotFoundException ex)
                    {
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (m_bootPkgs[i].startsWith("java."))
                        {
                            throw ex;
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            }
        }

        // Look in the module's imports. Note that the search may
        // be aborted if this method throws an exception, otherwise
        // it continues if a null is returned.
        result = searchImports(module, name, isClass);

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
                // If delegate is true then there are no bundles
                // providing exports for this package and the instigating
                // class was not from a bundle. Therefore,
                // delegate to the parent class loader in case
                // that this is not due to outside code calling a method
                // on the bundle interface (e.g., Bundle.loadClass()).
                if (delegate && !Bundle.class.isInstance(classes[i - 1]))
                {
                    try
                    {
                        // Return the class or resource from the parent class loader.
                        return (isClass)
                            ? (Object) this.getClass().getClassLoader().loadClass(name)
                            : (Object) this.getClass().getClassLoader().getResource(name);
                    }
                    catch (NoClassDefFoundError ex)
                    {
                        // Ignore, will return null
                    }
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

        // We can only search dynamic imports if the bundle
        // doesn't import, export, nor require the package in
        // question. Check these conditions first.
        if (isDynamicImportAllowed(importer, pkgName))
        {
            // Loop through the importer's dynamic requirements to determine if
            // there is a matching one for the package from which we want to
            // load a class.
            IRequirement[] dynamics = importer.getDefinition().getDynamicRequirements();
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
                        // Lock module manager instance to ensure that nothing changes.
                        synchronized (m_factory)
                        {
                            // Get "resolved" and "unresolved" candidates and put
                            // the "resolved" candidates first.
                            PackageSource[] resolved = getResolvedCandidates(target);
                            PackageSource[] unresolved = getUnresolvedCandidates(target);
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
                                IWire[] newWires = null;
                                if (wires == null)
                                {
                                    newWires = new IWire[1];
                                }
                                else
                                {
                                    newWires = new IWire[wires.length + 1];
                                    System.arraycopy(wires, 0, newWires, 0, wires.length);
                                }

                                // Create the wire and add it to the module.
                                wire = new R4Wire(
                                    importer, dynamics[dynIdx], candidate.m_module, candidate.m_capability);
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

    private boolean isDynamicImportAllowed(IModule importer, String pkgName)
    {
        // If any of the module exports this package, then we cannot
        // attempt to dynamically import it.
        ICapability[] caps = importer.getDefinition().getCapabilities();
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

    private IRequirement createDynamicRequirement(IRequirement dynReq, String pkgName)
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
            if (libs[i].match(name))
            {
                return module.getContentLoader().getContent()
                    .getEntryAsNativeLibrary(libs[i].getEntryName());
            }
        }

        return null;
    }

    public PackageSource[] getResolvedCandidates(IRequirement req)
    {
        // Synchronized on the module manager to make sure that no
        // modules are added, removed, or resolved.
        synchronized (m_factory)
        {
            PackageSource[] candidates = m_emptySources;
            if (req.getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
                && (((Requirement) req).getPackageName() != null))
            {
                String pkgName = ((Requirement) req).getPackageName();
                IModule[] modules = (IModule[]) m_resolvedPkgIndexMap.get(pkgName);

                for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
                {
                    ICapability resolvedCap = Util.getSatisfyingCapability(modules[modIdx], req);
                    if (resolvedCap != null)
                    {
// TODO: RB - Is this permission check correct.
                        if ((System.getSecurityManager() != null) &&
                            !((BundleProtectionDomain) modules[modIdx].getContentLoader().getSecurityContext()).impliesDirect(
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
                                !((BundleProtectionDomain) module.getContentLoader().getSecurityContext()).impliesDirect(
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
    }

    public PackageSource[] getUnresolvedCandidates(IRequirement req)
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
                modules = (IModule[]) m_unresolvedPkgIndexMap.get(((Requirement) req).getPackageName());
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
                // If compatible and it is not currently resolved, then add
                // the unresolved candidate to the list.
                if ((cap != null) && !isResolved(modules[modIdx]))
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
        // This map will be used to hold the final wires for all
        // resolved modules, which can then be used to fire resolved
        // events outside of the synchronized block.
        Map resolvedModuleWireMap = null;
        Map fragmentMap = null;

        // Synchronize on the module manager, because we don't want
        // any modules being added or removed while we are in the
        // middle of this operation.
        synchronized (m_factory)
        {
            // If the module is already resolved, then we can just return.
            if (isResolved(rootModule))
            {
                return;
            }

            // The root module is either a host or a fragment. If it is a host,
            // then we want to go ahead and resolve it. If it is a fragment, then
            // we want to select a host and resolve the host instead.
            IModule targetFragment = null;
// TODO: FRAGMENT - Currently we just make a single selection of the available
//       fragments or hosts and try to resolve. In case of failure, we do not
//       backtrack. We will likely want to add backtracking.
            if (Util.isFragment(rootModule))
            {
                targetFragment = rootModule;
                List hostList = getPotentialHosts(targetFragment);
                rootModule = (IModule) hostList.get(0);
            }

            // Get the available fragments for the host.
            fragmentMap = getPotentialFragments(rootModule);

            // If the resolve was for a specific fragment, then
            // eliminate all other potential candidate fragments
            // of the same symbolic name.
            if (targetFragment != null)
            {
                fragmentMap.put(
                    getBundleSymbolicName(targetFragment),
                    new IModule[] { targetFragment });
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

            // Attach fragments to root module.
            if ((fragmentMap != null) && (fragmentMap.size() > 0))
            {
                List list = new ArrayList();
                for (Iterator iter = fragmentMap.entrySet().iterator(); iter.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String symName = (String) entry.getKey();
                    IModule[] fragments = (IModule[]) entry.getValue();
// TODO: FRAGMENT - For now, just attach first candidate.
                    list.add(fragments[0]);
                    setResolved(fragments[0], true);
m_logger.log(Logger.LOG_DEBUG, "(FRAGMENT) WIRE: "
    + rootModule + " -> " + symName + " -> " + fragments[0]);
                }
                try
                {
                    ((ModuleImpl) rootModule).attachFragments(
                        (IModule[]) list.toArray(new IModule[list.size()]));
                }
                catch (Exception ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Unable to attach fragments", ex);
                }
            }
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
            iter = fragmentMap.entrySet().iterator();
            while (iter.hasNext())
            {
                fireModuleResolved(((IModule[]) ((Map.Entry) iter.next()).getValue())[0]);
            }
        }
    }

    // TODO: FRAGMENT - Not very efficient.
    private List getPotentialHosts(IModule fragment)
        throws ResolveException
    {
        List hostList = new ArrayList();

        IRequirement[] reqs = fragment.getDefinition().getRequirements();
        IRequirement hostReq = null;
        for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
        {
            if (reqs[reqIdx].getNamespace().equals(ICapability.HOST_NAMESPACE))
            {
                hostReq = reqs[reqIdx];
                break;
            }
        }

        IModule[] modules = m_factory.getModules();
        for (int modIdx = 0; (hostReq != null) && (modIdx < modules.length); modIdx++)
        {
            if (!fragment.equals(modules[modIdx]) && !isResolved(modules[modIdx]))
            {
                ICapability[] caps = modules[modIdx].getDefinition().getCapabilities();
                for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
                {
                    if (caps[capIdx].getNamespace().equals(ICapability.HOST_NAMESPACE)
                        && hostReq.isSatisfied(caps[capIdx])
                        && !modules[modIdx].isStale())
                    {
                        hostList.add(modules[modIdx]);
                        break;
                    }
                }
            }
        }

        if (hostList.size() == 0)
        {
            throw new ResolveException("Unable to resolve.", fragment, hostReq);
        }

        return hostList;
    }

// TODO: FRAGMENT - Not very efficient.
    private Map getPotentialFragments(IModule host)
    {
// TODO: FRAGMENT - This should check to make sure that the host allows fragments.
        Map fragmentMap = new HashMap();

        ICapability[] caps = host.getDefinition().getCapabilities();
        ICapability bundleCap = null;
        for (int capIdx = 0; capIdx < caps.length; capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(ICapability.HOST_NAMESPACE))
            {
                bundleCap = caps[capIdx];
                break;
            }
        }

        IModule[] modules = m_factory.getModules();
        for (int modIdx = 0; (bundleCap != null) && (modIdx < modules.length); modIdx++)
        {
            if (!host.equals(modules[modIdx]))
            {
                IRequirement[] reqs = modules[modIdx].getDefinition().getRequirements();
                for (int reqIdx = 0; (reqs != null) && (reqIdx < reqs.length); reqIdx++)
                {
                    if (reqs[reqIdx].getNamespace().equals(ICapability.HOST_NAMESPACE)
                        && reqs[reqIdx].isSatisfied(bundleCap)
                        && !modules[modIdx].isStale())
                    {
                        indexFragment(fragmentMap, modules[modIdx]);
                        break;
                    }
                }
            }
        }

        return fragmentMap;
    }

// TODO: FRAGMENT - Not very efficient.
    private static String getBundleSymbolicName(IModule module)
    {
        ICapability[] caps = module.getDefinition().getCapabilities();
        for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                return (String)
                    caps[capIdx].getProperties().get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
            }
        }
        return null;
    }

// TODO: FRAGMENT - Not very efficient.
    private static Version getBundleVersion(IModule module)
    {
        ICapability[] caps = module.getDefinition().getCapabilities();
        for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                return (Version)
                    caps[capIdx].getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            }
        }
        return Version.emptyVersion;
    }

    private void indexFragment(Map map, IModule module)
    {
        String symName = getBundleSymbolicName(module);
        IModule[] modules = (IModule[]) map.get(symName);

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
            Version version = getBundleVersion(module);
            Version middleVersion = null;
            int top = 0, bottom = modules.length - 1, middle = 0;
            while (top <= bottom)
            {
                middle = (bottom - top) / 2 + top;
                middleVersion = getBundleVersion(modules[middle]);
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

        map.put(symName, modules);
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
            // Get the candidates from the "resolved" and "unresolved"
            // package maps. The "resolved" candidates have higher priority
            // than "unresolved" ones, so put the "resolved" candidates
            // at the front of the list of candidates.
            PackageSource[] resolved = getResolvedCandidates(reqs[reqIdx]);
            PackageSource[] unresolved = getUnresolvedCandidates(reqs[reqIdx]);
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

    private void dumpPackageIndexMap(Map pkgIndexMap)
    {
        synchronized (this)
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
        while (!isSingletonConsistent(rootModule, moduleMap, candidatesMap) ||
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
    private boolean isSingletonConsistent(IModule targetModule, Map moduleMap, Map candidatesMap)
    {
        // Create a map of all resolved singleton modules.
        Map singletonMap = new HashMap();
        IModule[] modules = m_factory.getModules();
        for (int i = 0; (modules != null) && (i < modules.length); i++)
        {
            if (isResolved(modules[i]) && isSingleton(modules[i]))
            {
                String symName = getBundleSymbolicName(modules[i]);
                singletonMap.put(symName, symName);
            }
        }

        return areCandidatesSingletonConsistent(
            targetModule, singletonMap, moduleMap, new HashMap(), candidatesMap);
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
        IModule targetModule, Map singletonMap, Map moduleMap, Map cycleMap, Map candidatesMap)
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
        String symName = getBundleSymbolicName(targetModule);
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
                if (!isResolved(ps.m_module))
                {
                    return areCandidatesSingletonConsistent(ps.m_module, singletonMap, moduleMap, cycleMap, candidatesMap);
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
    private boolean isSingleton(IModule module)
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
            }
        }

        return pkgMap;
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
        // Get a map of all modules and their resolved wires.
        Map resolvedModuleWireMap =
            populateWireMap(candidatesMap, rootModule, new HashMap());
        Iterator iter = resolvedModuleWireMap.entrySet().iterator();
        // Iterate over the map to mark the modules as resolved and
        // update our resolver data structures.
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
for (int wireIdx = 0; (wires != null) && (wireIdx < wires.length); wireIdx++)
{
    m_logger.log(Logger.LOG_DEBUG, "WIRE: " + wires[wireIdx]);
}
            }

            // At this point, we need to remove all of the resolved module's
            // capabilities from the "unresolved" package map and put them in
            // in the "resolved" package map, with the exception of any
            // package exports that are also imported. In that case we need
            // to make sure that the import actually points to the resolved
            // module and not another module. If it points to another module
            // then the capability should be ignored, since the framework
            // decided to honor the import and discard the export.
            ICapability[] caps = module.getDefinition().getCapabilities();

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
            for (int capIdx = 0; (capsCopy != null) && (capIdx < capsCopy.length); capIdx++)
            {
                // Loop through all wires to see if the current capability
                // satisfies any of the wire requirements.
                for (int wireIdx = 0; (wires != null) && (wireIdx < wires.length); wireIdx++)
                {
                    // If the wire requirement is satisfied by the current capability,
                    // then check to see if the wire is to the module itself. If it
                    // is to another module, then null the current capability since
                    // it was both providing and requiring the same capability and
                    // the resolve process chose to import rather than provide that
                    // capability, therefore we should ignore it.
                    if (wires[wireIdx].getRequirement().isSatisfied(capsCopy[capIdx]))
                    {
                        if (!wires[wireIdx].getExporter().equals(module))
                        {
                            capsCopy[capIdx] = null;
                        }
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

//System.out.println("UNRESOLVED INDEX:");
//dumpPackageIndexMap(m_unresolvedPkgIndexMap);
//System.out.println("RESOLVED INDEX:");
//dumpPackageIndexMap(m_resolvedPkgIndexMap);
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
            // When a module is added, create an aggregated list of unresolved
            // exports to simplify later processing when resolving bundles.
            IModule module = event.getModule();
            ICapability[] caps = module.getDefinition().getCapabilities();

            // Add exports to unresolved package map.
            for (int i = 0; (caps != null) && (i < caps.length); i++)
            {
                if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    indexPackageCapability(m_unresolvedPkgIndexMap, module, caps[i]);
                }
            }
        }
    }

    public void moduleRemoved(ModuleEvent event)
    {
        // When a module is removed from the system, we need remove
        // its exports from the "resolved" and "unresolved" package maps,
        // remove the module's dependencies on fragments and exporters,
        // and remove the module from the module data map.

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
                    // Remove from "unresolved" package map.
                    IModule[] modules = (IModule[]) m_unresolvedPkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, event.getModule());
                        m_unresolvedPkgIndexMap.put(pkgName, modules);
                    }

                    // Remove from "resolved" package map.
                    modules = (IModule[]) m_resolvedPkgIndexMap.get(pkgName);
                    if (modules != null)
                    {
                        modules = removeModuleFromArray(modules, event.getModule());
                        m_resolvedPkgIndexMap.put(pkgName, modules);
                    }
                }
            }

            // Set fragments to null, which will remove the module from all
            // of its dependent fragment modules.
            try
            {
                ((ModuleImpl) event.getModule()).attachFragments(null);
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Error detaching fragments.", ex);
            }
            // Set wires to null, which will remove the module from all
            // of its dependent modules.
            ((ModuleImpl) event.getModule()).setWires(null);
            // Remove the module from the "resolved" map.
// TODO: RB - Maybe this can be merged with ModuleData.
            m_resolvedCapMap.remove(event.getModule());
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
            // Add exports to unresolved package map.
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
                    // Get package name.
                    String pkgName = (String)
                        caps[i].getProperties().get(ICapability.PACKAGE_PROPERTY);
                    // Add to "resolved" package index.
                    indexPackageCapability(
                        m_resolvedPkgIndexMap,
                        module,
                        caps[i]);
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
        public CandidateSet(IModule module, IRequirement requirement, PackageSource[] candidates)
        {
            m_module = module;
            m_requirement = requirement;
            m_candidates = candidates;
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

            Version thisVersion = null;
            Version version = null;
            if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                thisVersion = ((Capability) m_capability).getPackageVersion();
                version = ((Capability) ps.m_capability).getPackageVersion();
            }
            else if (m_capability.getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                thisVersion = (Version)
                    m_capability.getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                version = (Version)
                    ps.m_capability.getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            }

            if ((thisVersion != null) && (version != null))
            {
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
     * This utility class is a resolved package, which is comprised of a
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
                IModule[] exporters = getResolvedExporters(reqs[i], true);
                exporters = (exporters.length == 0)
                    ? getUnresolvedExporters(reqs[i], true) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getResolvedExporters(pkgReq, true);
                    exporters = (exporters.length == 0)
                        ? getUnresolvedExporters(pkgReq, true) : exporters;
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
        IRequirement[] dynamics = module.getDefinition().getDynamicRequirements();
        for (int dynIdx = 0; dynIdx < dynamics.length; dynIdx++)
        {
            IRequirement target = createDynamicRequirement(dynamics[dynIdx], pkgName);
            if (target != null)
            {
                // Try to see if there is an exporter available.
                PackageSource[] exporters = getResolvedCandidates(target);
                exporters = (exporters.length == 0)
                    ? getUnresolvedCandidates(target) : exporters;

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
                        exporters = getResolvedCandidates(pkgReq);
                        exporters = (exporters.length == 0)
                            ? getUnresolvedCandidates(pkgReq) : exporters;
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
                        if (!target.isSatisfied(
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
        PackageSource[] exporters = getResolvedCandidates(pkgReq);
        exporters = (exporters.length == 0) ? getUnresolvedCandidates(pkgReq) : exporters;
        if (exporters.length > 0)
        {
            boolean classpath = false;
            try
            {
                getClass().getClassLoader().loadClass(name);
                classpath = true;
            }
            catch (NoClassDefFoundError err)
            {
                // Ignore
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
            sb.append(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
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
