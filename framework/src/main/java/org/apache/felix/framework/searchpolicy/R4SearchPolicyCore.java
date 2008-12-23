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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.framework.Felix.FelixResolver;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.apache.felix.moduleloader.ResourceNotFoundException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Bundle;

public class R4SearchPolicyCore
{
    private final Logger m_logger;
    private final Map m_configMap;
    private final FelixResolver m_resolver;

    // Boot delegation packages.
    private String[] m_bootPkgs = null;
    private boolean[] m_bootPkgWildcards = null;

    // Reusable empty array.
    public static final ICapability[] m_emptyCapabilities = new ICapability[0];

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    public R4SearchPolicyCore(
        Logger logger, Map configMap, FelixResolver resolver)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;

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
            m_resolver.resolve(module);
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
            IWire wire = null;
            try
            {
                wire = m_resolver.resolveDynamicImport(module, pkgName);
            }
            catch (ResolveException ex)
            {
                // Ignore this since it is likely normal.
            }
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
            m_resolver.resolve(module);
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
        IWire wire = null;
        try
        {
            wire = m_resolver.resolveDynamicImport(module, pkgName);
        }
        catch (ResolveException ex)
        {
            // Ignore this since it is likely normal.
        }

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
/* TODO: RESOLVER - We need to figure out what to do with this.
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
*/
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
/* TODO: RESOLVER: Need to fix this too.
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
*/
        IRequirement pkgReq = null;
        try
        {
            pkgReq = new Requirement(ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }
        PackageSource[] exporters = m_resolver.getResolvedCandidates(pkgReq);
        exporters = (exporters.length == 0) ? m_resolver.getUnresolvedCandidates(pkgReq) : exporters;
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