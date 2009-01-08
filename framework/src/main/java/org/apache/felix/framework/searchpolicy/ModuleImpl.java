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

import org.apache.felix.moduleloader.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.felix.framework.Felix.FelixResolver;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class ModuleImpl implements IModule
{
    private final Logger m_logger;

    private Bundle m_bundle = null;

    private final Map m_headerMap;
    private final ICapability[] m_capabilities;
    private final IRequirement[] m_requirements;
    private final IRequirement[] m_dynamicRequirements;
    private final R4Library[] m_nativeLibraries;

    private final String m_id;
    private volatile String m_symbolicName = null;
    private IModule[] m_fragments = null;
    private IWire[] m_wires = null;
    private IModule[] m_dependentHosts = new IModule[0];
    private IModule[] m_dependentImporters = new IModule[0];
    private IModule[] m_dependentRequirers = new IModule[0];
    private volatile boolean m_isResolved = false;

    // TODO: REFACTOR - Fields below are from ContentLoaderImpl
    private final Map m_configMap;
    private final FelixResolver m_resolver;
    private final IContent m_content;
    private IContent[] m_contentPath;
    private IContent[] m_fragmentContents = null;
    private IURLPolicy m_urlPolicy = null;
    private ModuleClassLoader m_classLoader;
    private ProtectionDomain m_protectionDomain = null;
    private static SecureAction m_secureAction = new SecureAction();

    // Boot delegation packages.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

// TODO: REFACTOR - Should the module constructor parse the manifest?
    public ModuleImpl(Logger logger, Map configMap, FelixResolver resolver,
        String id, IContent content, Map headerMap,
        ICapability[] caps, IRequirement[] reqs, IRequirement[] dynReqs,
        R4Library[] nativeLibs)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;
        m_id = id;
        m_content = content;
        m_headerMap = headerMap;
        m_capabilities = caps;
        m_requirements = reqs;
        m_dynamicRequirements = dynReqs;
        m_nativeLibraries = nativeLibs;

        // Read the boot delegation property and parse it.
// TODO: REFACTOR - This used to be per framework, now it is per module
//       which doesn't really make sense.
        String s = (m_configMap == null)
            ? null
            : (String) m_configMap.get(Constants.FRAMEWORK_BOOTDELEGATION);
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

    public synchronized Bundle getBundle()
    {
        return m_bundle;
    }

    public synchronized void setBundle(Bundle bundle)
    {
        if (m_bundle == null)
        {
            m_bundle = bundle;
        }
    }

    public String getId()
    {
        return m_id;
    }

    public String getSymbolicName()
    {
        if (m_symbolicName == null)
        {
            for (int capIdx = 0;
                (m_symbolicName == null) && (m_capabilities != null) && (capIdx < m_capabilities.length);
                capIdx++)
            {
                if (m_capabilities[capIdx].getNamespace().equals(ICapability.MODULE_NAMESPACE))
                {
                    m_symbolicName = (String) m_capabilities[capIdx].getProperties().get(
                        Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
                }
            }
        }
        return m_symbolicName;
    }

    public Map getHeaders()
    {
        return m_headerMap;
    }

    public ICapability[] getCapabilities()
    {
        return m_capabilities;
    }

    public IRequirement[] getRequirements()
    {
        return m_requirements;
    }

    public IRequirement[] getDynamicRequirements()
    {
        return m_dynamicRequirements;
    }

    public R4Library[] getNativeLibraries()
    {
        return m_nativeLibraries;
    }

    public synchronized IModule[] getFragments()
    {
        return m_fragments;
    }

    public synchronized void attachFragments(IModule[] fragments) throws Exception
    {
        // Remove module from old fragment dependencies.
        // We will generally only remove module fragment
        // dependencies when we are uninstalling the module.
        for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
        {
            ((ModuleImpl) m_fragments[i]).removeDependentHost(this);
        }

        // Update the dependencies on the new fragments.
        m_fragments = fragments;

        // We need to add ourself as a dependent of each fragment
        // module. We also need to create an array of fragment contents
        // to attach to our content loader.
        if (m_fragments != null)
        {
            IContent[] fragmentContents = new IContent[m_fragments.length];
            for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
            {
                ((ModuleImpl) m_fragments[i]).addDependentHost(this);
                fragmentContents[i] =
                    m_fragments[i].getContent()
                        .getEntryAsContent(FelixConstants.CLASS_PATH_DOT);
            }
            // Now attach the fragment contents to our content loader.
            attachFragmentContents(fragmentContents);
        }
    }

    private void attachFragmentContents(IContent[] fragmentContents)
        throws Exception
    {
        // Close existing fragment contents.
        if (m_fragmentContents != null)
        {
            for (int i = 0; i < m_fragmentContents.length; i++)
            {
                m_fragmentContents[i].close();
            }
        }
        m_fragmentContents = fragmentContents;

        if (m_contentPath != null)
        {
            for (int i = 0; i < m_contentPath.length; i++)
            {
                m_contentPath[i].close();
            }
        }
        m_contentPath = initializeContentPath();
    }

    public synchronized IWire[] getWires()
    {
        return m_wires;
    }

    public synchronized void setWires(IWire[] wires)
    {
        // Remove module from old wire modules' dependencies,
        // since we are no longer dependent on any the moduels
        // from the old wires.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentImporter(this);
            }
        }

        m_wires = wires;

        // Add ourself as a dependent to the new wires' modules.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentImporter(this);
            }
        }
    }

    public synchronized IModule[] getDependentHosts()
    {
        return m_dependentHosts;
    }

    public synchronized void addDependentHost(IModule module)
    {
        m_dependentHosts = addDependent(m_dependentHosts, module);
    }

    public synchronized void removeDependentHost(IModule module)
    {
        m_dependentHosts = removeDependent(m_dependentHosts, module);
    }

    public synchronized IModule[] getDependentImporters()
    {
        return m_dependentImporters;
    }

    public synchronized void addDependentImporter(IModule module)
    {
        m_dependentImporters = addDependent(m_dependentImporters, module);
    }

    public synchronized void removeDependentImporter(IModule module)
    {
        m_dependentImporters = removeDependent(m_dependentImporters, module);
    }

    public synchronized IModule[] getDependentRequirers()
    {
        return m_dependentRequirers;
    }

    public synchronized void addDependentRequirer(IModule module)
    {
        m_dependentRequirers = addDependent(m_dependentRequirers, module);
    }

    public synchronized void removeDependentRequirer(IModule module)
    {
        m_dependentRequirers = removeDependent(m_dependentRequirers, module);
    }

    public synchronized IModule[] getDependents()
    {
        IModule[] dependents = new IModule[
            m_dependentHosts.length + m_dependentImporters.length + m_dependentRequirers.length];
        System.arraycopy(
            m_dependentHosts,
            0,
            dependents,
            0,
            m_dependentHosts.length);
        System.arraycopy(
            m_dependentImporters,
            0,
            dependents,
            m_dependentHosts.length,
            m_dependentImporters.length);
        System.arraycopy(
            m_dependentRequirers,
            0,
            dependents,
            m_dependentHosts.length + m_dependentImporters.length,
            m_dependentRequirers.length);
        return dependents;
    }

    public Class getClassByDelegation(String name) throws ClassNotFoundException
    {
        try
        {
            return getClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException ex)
        {
// TODO: REFACTOR - Should this log?
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
            throw ex;
        }
    }

    public URL getResourceByDelegation(String name)
    {
        return getClassLoader().getResource(name);
    }

    public Enumeration getResourcesByDelegation(String name)
    {
        Enumeration urls = null;
        List enums = new ArrayList();

        // First, try to resolve the originating module.
// TODO: FRAMEWORK - Consider opimizing this call to resolve, since it is called
// for each class load.
        try
        {
            m_resolver.resolve(this);
        }
        catch (ResolveException ex)
        {
            // The spec states that if the bundle cannot be resolved, then
            // only the local bundle's resources should be searched. So we
            // will ask the module's own class path.
            urls = getResourcesFromModule(name);
            return urls;
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
        IWire[] wires = getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i] instanceof R4Wire)
            {
                try
                {
                    // If we find the class or resource, then return it.
                    urls = wires[i].getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
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
                try
                {
                    // If we find the class or resource, then add it.
                    urls = wires[i].getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    enums.add(urls);
                }
            }
        }

        // Try the module's own class path. If we can find the resource then
        // return it together with the results from the other searches else
        // try to look into the dynamic imports.
        urls = getResourcesFromModule(name);
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
                wire = m_resolver.resolveDynamicImport(this, pkgName);
            }
            catch (ResolveException ex)
            {
                // Ignore this since it is likely normal.
            }
            if (wire != null)
            {
                try
                {
                    urls = wire.getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    enums.add(urls);
                }
            }
        }

        return new CompoundEnumeration((Enumeration[])
            enums.toArray(new Enumeration[enums.size()]));
    }

    public boolean isResolved()
    {
        return m_isResolved;
    }

    public void setResolved()
    {
        m_isResolved = true;
    }

    public String toString()
    {
        return m_id;
    }

    private static IModule[] addDependent(IModule[] modules, IModule module)
    {
        // Make sure the dependent module is not already present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                return modules;
            }
        }
        IModule[] tmp = new IModule[modules.length + 1];
        System.arraycopy(modules, 0, tmp, 0, modules.length);
        tmp[modules.length] = module;
        return tmp;
    }

    private static IModule[] removeDependent(IModule[] modules, IModule module)
    {
        IModule[] tmp = modules;

        // Make sure the dependent module is present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                // If this is the module, then point to empty list.
                if ((modules.length - 1) == 0)
                {
                    tmp = new IModule[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    tmp = new IModule[modules.length - 1];
                    System.arraycopy(modules, 0, tmp, 0, i);
                    if (i < tmp.length)
                    {
                        System.arraycopy(modules, i + 1, tmp, i, tmp.length - i);
                    }
                }
                break;
            }
        }

        return tmp;
    }

    // TODO: REFACTOR - Below are from ContentLoaderImpl

    Logger getLogger()
    {
        return m_logger;
    }

    FelixResolver getResolver()
    {
        return m_resolver;
    }

    public synchronized void close()
    {
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            m_fragmentContents[i].close();
        }
        synchronized (this)
        {
            m_classLoader = null;
        }
    }

    public IContent getContent()
    {
        return m_content;
    }

    synchronized IContent[] getClassPath()
    {
        if (m_contentPath == null)
        {
            try
            {
                m_contentPath = initializeContentPath();
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to get module class path.", ex);
            }
        }
        return m_contentPath;
    }

    public synchronized void setURLPolicy(IURLPolicy urlPolicy)
    {
        m_urlPolicy = urlPolicy;
    }

    public synchronized IURLPolicy getURLPolicy()
    {
        return m_urlPolicy;
    }

    public synchronized void setSecurityContext(Object securityContext)
    {
        m_protectionDomain = (ProtectionDomain) securityContext;
    }

    public synchronized Object getSecurityContext()
    {
        return m_protectionDomain;
    }

    public Class getClassFromModule(String name) throws ClassNotFoundException
    {
        try
        {
            return getClassLoader().findClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
            throw ex;
        }
    }

// TODO: REFACTOR - This previously didn't cause the class loader to be created.
    public URL getResourceFromModule(String name)
    {
        URL url = null;

        // Remove leading slash, if present, but special case
        // "/" so that it returns a root URL...this isn't very
        // clean or meaninful, but the Spring guys want it.
        if (name.equals("/"))
        {
            // Just pick a class path index since it doesn't really matter.
            url = getURLPolicy().createURL(1, name);
        }
        else if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Check the module class path.
        IContent[] contentPath = getClassPath();
        for (int i = 0;
            (url == null) &&
            (i < contentPath.length); i++)
        {
            if (contentPath[i].hasEntry(name))
            {
                url = getURLPolicy().createURL(i + 1, name);
            }
        }

        return url;
    }

// TODO: REFACTOR - This previously didn't cause the class loader to be created.
    public Enumeration getResourcesFromModule(String name)
    {
        Vector v = new Vector();

        // Special case "/" so that it returns a root URLs for
        // each bundle class path entry...this isn't very
        // clean or meaningful, but the Spring guys want it.
        if (name.equals("/"))
        {
            for (int i = 0; i < getClassPath().length; i++)
            {
                v.addElement(getURLPolicy().createURL(i + 1, name));
            }
        }
        else
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module class path.
            IContent[] contentPath = getClassPath();
            for (int i = 0; i < contentPath.length; i++)
            {
                if (contentPath[i].hasEntry(name))
                {
                    // Use the class path index + 1 for creating the path so
                    // that we can differentiate between module content URLs
                    // (where the path will start with 0) and module class
                    // path URLs.
                    v.addElement(getURLPolicy().createURL(i + 1, name));
                }
            }
        }

        return v.elements();
    }

    // TODO: API: Investigate how to handle this better, perhaps we need
    // multiple URL policies, one for content -- one for class path.
    public URL getResourceFromContent(String name)
    {
        URL url = null;

        // Check for the special case of "/", which represents
        // the root of the bundle according to the spec.
        if (name.equals("/"))
        {
            url = getURLPolicy().createURL(0, "/");
        }

        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module content.
            if (getContent().hasEntry(name))
            {
                // Module content URLs start with 0, whereas module
                // class path URLs start with the index into the class
                // path + 1.
                url = getURLPolicy().createURL(0, name);
            }
        }

        return url;
    }

    public boolean hasInputStream(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.hasEntry(urlPath);
        }
        return getClassPath()[index - 1].hasEntry(urlPath);
    }

    public InputStream getInputStream(int index, String urlPath)
        throws IOException
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsStream(urlPath);
        }
        return getClassPath()[index - 1].getEntryAsStream(urlPath);
    }

    private ModuleClassLoader getClassLoader()
    {
        synchronized (this)
        {
            if (m_classLoader == null)
            {
                m_classLoader = m_secureAction.createModuleClassLoader(
                    this, m_protectionDomain);
            }
        }
        return m_classLoader;
    }

    private IContent[] initializeContentPath() throws Exception
    {
        List contentList = new ArrayList();
        calculateContentPath(m_content, contentList, true);
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            calculateContentPath(m_fragmentContents[i], contentList, false);
        }
        return (IContent[]) contentList.toArray(new IContent[contentList.size()]);
    }

    private List calculateContentPath(IContent content, List contentList, boolean searchFragments)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Create a list to contain the content path for the specified content.
        List localContentList = new ArrayList();

        // Find class path meta-data.
        String classPath = (String) m_headerMap.get(FelixConstants.BUNDLE_CLASSPATH);
        // Parse the class path into strings.
        String[] classPathStrings = ManifestParser.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new String[0];
        }

        // Create the bundles class path.
        for (int i = 0; i < classPathStrings.length; i++)
        {
            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            classPathStrings[i] = (classPathStrings[i].startsWith("/"))
                ? classPathStrings[i].substring(1)
                : classPathStrings[i];

            // Check for the bundle itself on the class path.
            if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
            {
                localContentList.add(content);
            }
            else
            {
                // Try to find the embedded class path entry in the current
                // content.
                IContent embeddedContent = content.getEntryAsContent(classPathStrings[i]);
                // If the embedded class path entry was not found, it might be
                // in one of the fragments if the current content is the bundle,
                // so try to search the fragments if necessary.
                for (int fragIdx = 0;
                    searchFragments && (embeddedContent == null)
                        && (m_fragmentContents != null) && (fragIdx < m_fragmentContents.length);
                    fragIdx++)
                {
                    embeddedContent = m_fragmentContents[fragIdx].getEntryAsContent(classPathStrings[i]);
                }
                // If we found the embedded content, then add it to the
                // class path content list.
                if (embeddedContent != null)
                {
                    localContentList.add(embeddedContent);
                }
                else
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    m_logger.log(Logger.LOG_INFO,
                        "Class path entry not found: "
                        + classPathStrings[i]);
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (localContentList.size() == 0)
        {
            localContentList.add(content);
        }

        // Now add the local contents to the global content list and return it.
        contentList.addAll(localContentList);
        return contentList;
    }

// From ModuleClassLoader

    Object findClassOrResourceByDelegation(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // First, try to resolve the originating module.
// TODO: FRAMEWORK - Consider opimizing this call to resolve, since it is called
// for each class load.
        try
        {
            m_resolver.resolve(this);
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
                URL url = getResourceFromModule(name);
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
        result = searchImports(name, isClass);

        // If not found, try the module's own class path.
        if (result == null)
        {
            result = (isClass)
                ? (Object) getClassFromModule(name)
                : (Object) getResourceFromModule(name);

            // If still not found, then try the module's dynamic imports.
            if (result == null)
            {
                result = searchDynamicImports(name, pkgName, isClass);
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

    private Object searchImports(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // We delegate to the module's wires to find the class or resource.
        IWire[] wires = getWires();
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
        String name, String pkgName, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        IWire wire = null;
        try
        {
            wire = m_resolver.resolveDynamicImport(this, pkgName);
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
                    if (ModuleClassLoader.class.isInstance(cl))
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
}