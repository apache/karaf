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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.Requirement;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.IWire;
import org.apache.felix.moduleloader.ResourceNotFoundException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

public class ModuleClassLoader extends SecureClassLoader
{
    private static final Constructor m_dexFileClassConstructor;
    private static final Method m_dexFileClassLoadClass;
    static
    {
        Constructor dexFileClassConstructor = null;
        Method dexFileClassLoadClass = null;
        try
        {
            Class dexFileClass;
            try
            {
                dexFileClass = Class.forName("dalvik.system.DexFile");
            }
            catch (Exception ex)
            {
                dexFileClass = Class.forName("android.dalvik.DexFile");
            }

            dexFileClassConstructor = dexFileClass.getConstructor(
                new Class[] { java.io.File.class });
            dexFileClassLoadClass = dexFileClass.getMethod("loadClass",
                new Class[] { String.class, ClassLoader.class });
        }
        catch (Exception ex)
        {
           dexFileClassConstructor = null;
           dexFileClassLoadClass = null;
        }
        m_dexFileClassConstructor = dexFileClassConstructor;
        m_dexFileClassLoadClass = dexFileClassLoadClass;
    }

    private final ModuleImpl m_module;
    private final ProtectionDomain m_protectionDomain;
    private final Map m_jarContentToDexFile;

    public ModuleClassLoader(ModuleImpl module, ProtectionDomain protectionDomain)
    {
        m_module = module;
        m_protectionDomain = protectionDomain;
        if (m_dexFileClassConstructor != null)
        {
            m_jarContentToDexFile = new HashMap();
        }
        else
        {
            m_jarContentToDexFile = null;
        }
    }

    public IModule getModule()
    {
        return m_module;
    }

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        Class clazz = null;

        // Make sure the class was not already loaded.
        synchronized (this)
        {
            clazz = findLoadedClass(name);
        }

        if (clazz == null)
        {
            try
            {
                return (Class) m_module.findClassOrResourceByDelegation(name, true);
            }
            catch (ResourceNotFoundException ex)
            {
                // This should never happen since we are asking for a class,
                // so just ignore it.
            }
            catch (ClassNotFoundException cnfe)
            {
                ClassNotFoundException ex = cnfe;
                String msg = name;
                if (m_module.getLogger().getLogLevel() >= Logger.LOG_DEBUG)
                {
                    msg = diagnoseClassLoadError(m_module, name);
                    ex = new ClassNotFoundException(msg, cnfe);
                }
                throw ex;
            }
        }

        // Resolve the class and return it.
        if (resolve)
        {
            resolveClass(clazz);
        }
        return clazz;
    }

    protected Class findClass(String name) throws ClassNotFoundException
    {
        // Do a quick check here to see if we can short-circuit this
        // entire process if the class was already loaded.
        Class clazz = null;
        synchronized (this)
        {
            clazz = findLoadedClass(name);
        }

        // Search for class in module.
        if (clazz == null)
        {
            String actual = name.replace('.', '/') + ".class";

            byte[] bytes = null;

            IContent content = null;
            // Check the module class path.
            for (int i = 0;
                (bytes == null) &&
                (i < m_module.getClassPath().length); i++)
            {
                bytes = m_module.getClassPath()[i].getEntryAsBytes(actual);
                content = m_module.getClassPath()[i];
            }

            if (bytes != null)
            {
                // Before we actually attempt to define the class, grab
                // the lock for this class loader and make sure than no
                // other thread has defined this class in the meantime.
                synchronized (this)
                {
                    clazz = findLoadedClass(name);

                    if (clazz == null)
                    {
                        // We need to try to define a Package object for the class
                        // before we call defineClass(). Get the package name and
                        // see if we have already created the package.
                        String pkgName = Util.getClassPackage(name);
                        if (pkgName.length() > 0)
                        {
                            if (getPackage(pkgName) == null)
                            {
                                Object[] params = definePackage(pkgName);
                                if (params != null)
                                {
                                    definePackage(
                                        pkgName,
                                        (String) params[0],
                                        (String) params[1],
                                        (String) params[2],
                                        (String) params[3],
                                        (String) params[4],
                                        (String) params[5],
                                        null);
                                }
                                else
                                {
                                    definePackage(pkgName, null, null,
                                        null, null, null, null, null);
                                }
                            }
                        }

                        // If we can load the class from a dex file do so
                        if (content instanceof JarContent)
                        {
                            try
                            {
                                clazz = getDexFileClass((JarContent) content, name, this);
                            }
                            catch (Exception ex)
                            {
                                // Looks like we can't
                            }
                        }

                        if (clazz == null)
                        {
                            // If we have a security context, then use it to
                            // define the class with it for security purposes,
                            // otherwise define the class without a protection domain.
                            if (m_protectionDomain != null)
                            {
                                clazz = defineClass(name, bytes, 0, bytes.length,
                                    m_protectionDomain);
                            }
                            else
                            {
                                clazz = defineClass(name, bytes, 0, bytes.length);
                            }
                        }
                    }
                }
            }
        }

        return clazz;
    }

    private Object[] definePackage(String pkgName)
    {
        Map headerMap = m_module.getHeaders();
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

    private Class getDexFileClass(JarContent content, String name, ClassLoader loader)
        throws Exception
    {
        if (m_jarContentToDexFile == null)
        {
            return null;
        }

        Object dexFile = null;

        if (!m_jarContentToDexFile.containsKey(content))
        {
            try
            {
                dexFile = m_dexFileClassConstructor.newInstance(
                    new Object[] { content.getFile() });
            }
            finally
            {
                m_jarContentToDexFile.put(content, dexFile);
            }
        }
        else
        {
            dexFile = m_jarContentToDexFile.get(content);
        }

        if (dexFile != null)
        {
            return (Class) m_dexFileClassLoadClass.invoke(dexFile,
                new Object[] { name.replace('.','/'), loader });
        }
        return null;
    }

    public URL getResource(String name)
    {
        try
        {
            return (URL) m_module.findClassOrResourceByDelegation(name, false);
        }
        catch (ClassNotFoundException ex)
        {
            // This should never happen, so just ignore it.
        }
        catch (ResourceNotFoundException ex)
        {
            // Not much we can do here since getResource() does not throw any
            // exceptions, so just ignore it too.
        }
        return null;
    }

    protected URL findResource(String name)
    {
        return m_module.getResourceFromModule(name);
    }

    // This should actually be findResources(), but it can't be for the
    // reason described below for the actual findResources() method.
    Enumeration findResourcesFromModule(String name)
    {
        return m_module.getResourcesFromModule(name);
    }

    // The findResources() method should only look at the module itself, but
    // instead it tries to delegate because in Java version prior to 1.5 the
    // getResources() method was final and could not be overridden. We should
    // override getResources() like getResource() to make it delegate, but we
    // can't. As a workaround, we make findResources() delegate instead.
    protected Enumeration findResources(String name)
    {
        return m_module.getResourcesByDelegation(name);
    }

    protected String findLibrary(String name)
    {
        // Remove leading slash, if present.
        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        R4Library[] libs = m_module.getNativeLibraries();
        for (int i = 0; (libs != null) && (i < libs.length); i++)
        {
            if (libs[i].match(name))
            {
                return m_module.getContent()
                    .getEntryAsNativeLibrary(libs[i].getEntryName());
            }
        }

        return null;
    }

    public String toString()
    {
        return m_module.toString();
    }

    private static String diagnoseClassLoadError(ModuleImpl module, String name)
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
                long expId = Util.getBundleIdFromModuleId(wires[i].getExporter().getId());

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
        IRequirement[] reqs = module.getRequirements();
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
        PackageSource[] exporters =
            module.getResolver().getResolvedCandidates(pkgReq);
        exporters = (exporters.length == 0)
            ? module.getResolver().getUnresolvedCandidates(pkgReq)
            : exporters;
        if (exporters.length > 0)
        {
            boolean classpath = false;
            try
            {
                ModuleClassLoader.class.getClassLoader().loadClass(name);
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
            ModuleClassLoader.class.getClassLoader().loadClass(name);

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