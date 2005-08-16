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
package org.apache.osgi.moduleloader;

import java.net.URL;

/**
 * <p>
 * This interface represents a policy to define the most basic behavior
 * of how classes, resources, and native libraries within a specific instance
 * of <tt>ModuleManager</tt> are found. A <tt>ModuleManager</tt> manages a set of
 * <tt>Module</tt>s, each of which is a potential source of classes, resources,
 * and native libraries. The search policy makes it possible to consult these
 * sources without hard-coding assumptions about application behavior
 * or structure. Applicaitons inject their own specific class loading policy
 * by creating a custom search policy or by selecting a pre-existing search
 * policy that matches their needs.
 * </p>
 * <p>
 * The search policy is used by <tt>ModuleClassLoader</tt>, of which, there
 * is one per <tt>Module</tt> within a given <tt>ModuleManager</tt> instance.
 * The search policy is consulted by the <tt>ModuleClassLoader</tt> whenever
 * there is a request for a class, resource, or native library. The search
 * policy will generally search other modules in an application-specific
 * way in order to find the requested item; for example, an application may
 * use a policy where module's may import from one another. If the search
 * policy provides an answer, then the <tt>ModuleClassLoader</tt> will use
 * this to answer the originating request.
 * </p>
 * <p>
 * <b><i>Important:</i></b> The search policy <i>searches</i> modules in
 * some application-specific manner in order to find a class or resource.
 * This <i>search</i> is instigated, either directly or indirectly, by calls
 * to <tt>ModuleClassLoader.loadClass()</tt> and <tt>ModuleClassLoader.getResource()</tt>,
 * respectively. In order for the search policy to load a class or resource,
 * it must <b>not</b> use <tt>ModuleClassLoader.loadClass()</tt> or
 * <tt>ModuleClassLoader.getResource()</tt> again, because this would result
 * in an infinite loop. Instead, the <tt>ModuleClassLoader</tt> offers the
 * the methods <tt>ModuleClassLoader.loadClassFromModule()</tt> and
 * <tt>ModuleClassLoader.getResourceFromModule()</tt> to search a given module
 * and to avoid an infinite loop.
 * </p>
 * <pre>
 *     ...
 *     public Class findClass(Module module, String name)
 *     {
 *         Module[] modules = m_mgr.getModules();
 *         for (int i = 0; i < modules.length; i++)
 *         {
 *             try {
 *                 Class clazz = modules[i].getClassLoader().loadClassFromModule(name);
 *                 if (clazz != null)
 *                 {
 *                     return clazz;
 *                 }
 *             } catch (Throwable th) {
 *             }
 *         }
 *
 *         return null;
 *     }
 *     ...
 * </pre>
 * <p>
 * In the above code, the search policy "exhaustively" searches every module in the
 * <tt>ModuleManager</tt> to find the requested resource. Note that this policy
 * will also search the module that originated the request, which is not totally
 * necessary since returning <tt>null</tt> will cause the <tt>ModuleClassLoader</tt>
 * to search the originating module's <tt>ResourceSource</tt>s.
 * </p>
**/
public interface SearchPolicy
{
    /**
     * <p>
     * This method is called once by the <tt>ModuleManager</tt> to
     * give the search policy instance a reference to its associated
     * module manager. This method should be implemented such that
     * it cannot be called twice; calling this method a second time
     * should produce an illegal state exception.
     * </p>
     * @param mgr the module manager associated with this search policy.
     * @throws java.lang.IllegalStateException if the method is called
     *         more than once.
    **/
    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException;

    /**
     * <p>
     * The <tt>ModuleClassLoader</tt> calls this method before performing
     * the call to <tt>ClassLoader.defineClass()</tt> to give the search policy
     * an opportunity to define the <tt>Package</tt> object for the specified
     * package. The method should return an array of <tt>String</tt> values for
     * each of the following: specTitle, specVersion, specVendor, implTitle,
     * implVersion, and implVendor. See <tt>ClassLoader.definePackage()</tt>
     * for more details. The returned array may contain <tt>null</tt>s, but
     * the return array must have six elements.
     * </p>
     * @param module the module requesting a class from the package.
     * @param pkgName the package name of the class being requested.
     * @return an array containing values for creating the <tt>Package</tt>
     *         object for the specified package.
    **/
    public Object[] definePackage(Module module, String pkgName);

    /**
     * <p>
     * When a module instigates a class load operation, this method is called
     * to find the desired class for the instigating module. This method is
     * called <b>before</b> searching the module's resource sources for the class.
     * How the class is found is dependent upon the search policy implementation.
     * </p>
     * <p>
     * This method may return <tt>null</tt> or throw an exception if the
     * specified class is not found. Whether a specific search policy
     * implementation should do one or the other depends on the details
     * of the specific search policy. The <tt>ModuleClassLoader</tt>
     * first delegates to this method, then to the local resources
     * sources of the module, and then finally to then the
     * <tt>SearchPolicy.findClassAfterModule()</tt> method. If this method
     * returns null, then the search for the class will continue to these
     * latter two steps. On the other hand, if this method returns a class
     * or throws an exception, then the latter two steps will not be searched.
     * </p>
     * <p>
     * <b>Important:</b> If the implementation of this method delegates
     * the class loading to a <tt>ModuleClassLoader</tt> of another module,
     * then it should <b>not</b> use the method <tt>ModuleClassLoader.loadClass()</tt>
     * to load the class; it should use <tt>ModuleClassLoader.loadClassFromModule()</tt>
     * instead. This is necessary to eliminate an infinite loop that would
     * occur otherwise. Also, with respect to the <tt>ModuleLoader</tt> framework,
     * this method will only be called by a single thread at a time and is only
     * intended to be called by <tt>ModuleClassLoader.loadClass()</tt>.
     * </p>
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return the class if found, <tt>null</tt> otherwise.
     * @throws java.lang.ClassNotFoundException if the class could not be
     *         found and the entire search operation should fail.
    **/
    public Class findClassBeforeModule(ClassLoader parent, Module module, String name)
        throws ClassNotFoundException;

    /**
     * <p>
     * When a module instigates a class load operation, this method is called
     * to find the desired class for the instigating module. This method is
     * called <b>after</b> searching the module's resource sources for the class.
     * How the class is found is dependent upon the search policy implementation.
     * </p>
     * <p>
     * The <tt>ModuleClassLoader</tt> first delegates to the
     * <tt>SearchPolicy.findClassBeforeModule() method, then to the local
     * resources sources of the module, and then finally to this method.
     * This method is the last attempt to find the class and if it fails
     * (by either return <tt>null</tt> or throwing an exception), then the
     * result of the entire class load will fail.
     * </p>
     * <p>
     * <b>Important:</b> If the implementation of this method delegates
     * the class loading to a <tt>ModuleClassLoader</tt> of another module,
     * then it should <b>not</b> use the method <tt>ModuleClassLoader.loadClass()</tt>
     * to load the class; it should use <tt>ModuleClassLoader.loadClassFromModule()</tt>
     * instead. This is necessary to eliminate an infinite loop that would
     * occur otherwise. Also, with respect to the <tt>ModuleLoader</tt> framework,
     * this method will only be called by a single thread at a time and is only
     * intended to be called by <tt>ModuleClassLoader.loadClass()</tt>.
     * </p>
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return the class if found, <tt>null</tt> otherwise.
     * @throws java.lang.ClassNotFoundException if the class could not be
     *         found and the entire search operation should fail.
    **/
    public Class findClassAfterModule(ClassLoader parent, Module module, String name)
        throws ClassNotFoundException;

    /**
     * <p>
     * This method tries to find the specified resource for the specified
     * module. How the resource is found or whether it is actually retrieved
     * from the specified module is dependent upon the implementation. The
     * default <tt>ModuleClassLoader.getResource()</tt> method does not do
     * any searching on its own.
     * </p>
     * <p>
     * This method may return <tt>null</tt> or throw an exception if the
     * specified resource is not found. Whether a specific search policy
     * implementation should do one or the other depends on the details
     * of the specific search policy. The <tt>ModuleClassLoader</tt>
     * first delegates to this method and then to the local resource
     * sources of the module. If this method returns null, then the local
     * resource sources will be searched. On the other hand, if this method
     * throws an exception, then the local resource sources will not be
     * searched.
     * </p>
     * <p>
     * <b>Important:</b> If the implementation of this method delegates
     * the resource loading to a <tt>ModuleClassLoader</tt> of another module,
     * then it should not use the method <tt>ModuleClassLoader.getResource()</tt>
     * to get the resource; it should use <tt>ModuleClassLoader.getResourceFromModule()</tt>
     * instead. This is necessary to eliminate an infinite loop that would
     * occur otherwise. Also, with respect to the <tt>ModuleLoader</tt> framework,
     * this method will only be called by a single thread at a time and is not
     * intended to be called directly.
     * </p>
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the resource.
     * @param name the name of the resource being loaded.
     * @return a <tt>URL</tt> to the resource if found, <tt>null</tt> otherwise.
     * @throws org.apache.osgi.moduleloader.ResourceNotFoundException if the
     *         resource could not be found and the entire search operation
     *         should fail.
    **/
    public URL findResource(ClassLoader parent, Module module, String name)
        throws ResourceNotFoundException;
}