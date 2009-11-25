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
package org.apache.felix.framework.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.HashMap;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * This is a utility class to centralize all action that should be performed
 * in a <tt>doPrivileged()</tt> block. To perform a secure action, simply
 * create an instance of this class and use the specific method to perform
 * the desired action. When an instance is created, this class will capture
 * the security context and will then use that context when checking for
 * permission to perform the action. Instances of this class should not be
 * passed around since they may grant the receiver a capability to perform
 * privileged actions.
 * </p>
**/
public class SecureAction
{
    private static final ThreadLocal m_actions = new ThreadLocal()
    {
        public Object initialValue()
        {
            return new Actions();
        }
    };

    protected static transient int BUFSIZE = 4096;

    private AccessControlContext m_acc = null;

    public SecureAction()
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.INITIALIZE_CONTEXT, null);
                m_acc = (AccessControlContext) AccessController.doPrivileged(actions);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            m_acc = AccessController.getContext();
        }
    }

    public String getSystemProperty(String name, String def)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_PROPERTY_ACTION, name, def);
                return (String) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return System.getProperty(name, def);
        }
    }

    public ClassLoader getParentClassLoader(ClassLoader loader)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_PARENT_CLASS_LOADER_ACTION, loader);
                return (ClassLoader) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return loader.getParent();
        }
    }

    public ClassLoader getSystemClassLoader()
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_SYSTEM_CLASS_LOADER_ACTION);
                return (ClassLoader) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return ClassLoader.getSystemClassLoader();
        }
    }
    
    public ClassLoader getClassLoader(Class clazz)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_CLASS_LOADER_ACTION, clazz);
                return (ClassLoader) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return clazz.getClassLoader();
        }
    }

    public Class forName(String name) throws ClassNotFoundException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.FOR_NAME_ACTION, name);
                return (Class) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof ClassNotFoundException)
                {
                    throw (ClassNotFoundException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return Class.forName(name);
        }
    }

    public URL createURL(String protocol, String host,
        int port, String path, URLStreamHandler handler)
        throws MalformedURLException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.CREATE_URL_ACTION, protocol, host,
                    new Integer(port), path, handler);
                return (URL) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof MalformedURLException)
                {
                    throw (MalformedURLException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new URL(protocol, host, port, path, handler);
        }
    }

    public URL createURL(URL context, String spec, URLStreamHandler handler)
        throws MalformedURLException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.CREATE_URL_WITH_CONTEXT_ACTION, context,
                    spec, handler);
                return (URL) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof MalformedURLException)
                {
                    throw (MalformedURLException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new URL(context, spec, handler);
        }
    }

    public Process exec(String command) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.EXEC_ACTION, command);
                return (Process) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return Runtime.getRuntime().exec(command);
        }
    }

    public String getAbsolutePath(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_ABSOLUTE_PATH_ACTION, file);
                return (String) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.getAbsolutePath();
        }
    }

    public boolean fileExists(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.FILE_EXISTS_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.exists();
        }
    }

    public boolean isFileDirectory(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.FILE_IS_DIRECTORY_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.isDirectory();
        }
    }

    public boolean mkdir(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.MAKE_DIRECTORY_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.mkdir();
        }
    }

    public boolean mkdirs(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.MAKE_DIRECTORIES_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.mkdirs();
        }
    }

    public File[] listDirectory(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.LIST_DIRECTORY_ACTION, file);
                return (File[]) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return file.listFiles();
        }
    }

    public boolean renameFile(File oldFile, File newFile)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.RENAME_FILE_ACTION, oldFile, newFile);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return oldFile.renameTo(newFile);
        }
    }

    public InputStream getFileInputStream(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_FILE_INPUT_ACTION, file);
                return (InputStream) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new FileInputStream(file);
        }
    }

    public OutputStream getFileOutputStream(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_FILE_OUTPUT_ACTION, file);
                return (OutputStream) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new FileOutputStream(file);
        }
    }

    public InputStream getURLConnectionInputStream(URLConnection conn)
        throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_URL_INPUT_ACTION, conn);
                return (InputStream) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return conn.getInputStream();
        }
    }

    public boolean deleteFile(File target)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.DELETE_FILE_ACTION, target);
                return ((Boolean) AccessController.doPrivileged(actions, m_acc))
                    .booleanValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return target.delete();
        }
    }

    public File createTempFile(String prefix, String suffix, File dir)
        throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.CREATE_TMPFILE_ACTION, prefix, suffix, dir);
                return (File) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return File.createTempFile(prefix, suffix, dir);
        }
    }

    public URLConnection openURLConnection(URL url) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.OPEN_URLCONNECTION_ACTION, url);
                return (URLConnection) AccessController.doPrivileged(actions,
                    m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return url.openConnection();
        }
    }

    public JarFileX openJAR(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.OPEN_JARX_ACTION, file);
                return (JarFileX) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new JarFileX(file);
        }
    }

    public JarFileX openJAR(File file, boolean verify) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.OPEN_JARX_VERIFY_ACTION, file, (verify ? Boolean.TRUE : Boolean.FALSE));
                return (JarFileX) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof IOException)
                {
                    throw (IOException) ex.getException();
                }
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new JarFileX(file, verify);
        }
    }
// TODO: REFACTOR - SecureAction fix needed.
/*
    public ModuleClassLoader createModuleClassLoader(ModuleImpl impl)
    {
        return createModuleClassLoader(impl, null);
    }

    public ModuleClassLoader createModuleClassLoader(ModuleImpl impl,
        ProtectionDomain protectionDomain)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.CREATE_MODULECLASSLOADER_ACTION, impl, protectionDomain);
                return (ModuleClassLoader) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return new ModuleClassLoader(impl, protectionDomain);
        }
    }
*/
    public void startActivator(BundleActivator activator, BundleContext context)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.START_ACTIVATOR_ACTION, activator, context);
                AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw ex.getException();
            }
        }
        else
        {
            activator.start(context);
        }
    }

    public void stopActivator(BundleActivator activator, BundleContext context)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.STOP_ACTIVATOR_ACTION, activator, context);
                AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw ex.getException();
            }
        }
        else
        {
            activator.stop(context);
        }
    }

    public Policy getPolicy()
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Actions actions = (Actions) m_actions.get();
                actions.set(Actions.GET_POLICY_ACTION, null);
                return (Policy) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException ex)
            {
                throw (RuntimeException) ex.getException();
            }
        }
        else
        {
            return Policy.getPolicy();
        }
    }

    public void addURLToURLClassLoader(URL extension, ClassLoader loader) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.ADD_EXTENSION_URL, extension, loader);
            try
            {
                AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            Method addURL =
                URLClassLoader.class.getDeclaredMethod("addURL",
                new Class[] {URL.class});
            addURL.setAccessible(true);
            addURL.invoke(loader, new Object[]{extension});
        }
    }

    public Constructor getConstructor(Class target, Class[] types) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.GET_CONSTRUCTOR_ACTION, target, types);
            try
            {
                return (Constructor) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return target.getConstructor(types);
        }
    }

    public Constructor getDeclaredConstructor(Class target, Class[] types) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.GET_DECLARED_CONSTRUCTOR_ACTION, target, types);
            try
            {
                return (Constructor) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return target.getDeclaredConstructor(types);
        }
    }

    public Method getMethod(Class target, String method, Class[] types) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.GET_METHOD_ACTION, target, method, types);
            try
            {
                return (Method) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return target.getMethod(method, types);
        }
    }

    public Method getDeclaredMethod(Class target, String method, Class[] types) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.GET_DECLARED_METHOD_ACTION, target, method, types);
            try
            {
                return (Method) AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return target.getDeclaredMethod(method, types);
        }
    }

    public void setAccesssible(AccessibleObject ao)
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.SET_ACCESSIBLE_ACTION, ao);
            try
            {
                AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw (RuntimeException) e.getException();
            }
        }
        else
        {
            ao.setAccessible(true);
        }
    }

    public Object invoke(Method method, Object target, Object[] params) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.INVOKE_METHOD_ACTION, method, target, params);
            try
            {
                return AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            method.setAccessible(true);
            return method.invoke(target, params);
        }
    }

    public Object invokeDirect(Method method, Object target, Object[] params) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.INVOKE_DIRECTMETHOD_ACTION, method, target, params);
            try
            {
                return AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return method.invoke(target, params);
        }
    }

    public Object invoke(Constructor constructor, Object[] params) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.INVOKE_CONSTRUCTOR_ACTION, constructor, params);
            try
            {
                return AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return constructor.newInstance(params);
        }
    }

    public Object getDeclaredField(Class targetClass, String name, Object target)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.GET_FIELD_ACTION, targetClass, name, target);
            try
            {
                return AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            Field field = targetClass.getDeclaredField(name);
            field.setAccessible(true);

            return field.get(target);
        }
    }

    public Object swapStaticFieldIfNotClass(Class targetClazz,
        Class targetType, Class condition, String lockName) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.SWAP_FIELD_ACTION, targetClazz, targetType,
                condition, lockName);
            try
            {
                return AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return _swapStaticFieldIfNotClass(targetClazz, targetType,
                condition, lockName);
        }
    }

    private static Object _swapStaticFieldIfNotClass(Class targetClazz,
        Class targetType, Class condition, String lockName) throws Exception
    {
        Object lock = null;
        if (lockName != null)
        {
            try
            {
                Field lockField =
                    targetClazz.getDeclaredField(lockName);
                lockField.setAccessible(true);
                lock = lockField.get(null);
            }
            catch (NoSuchFieldException ex)
            {
            }
        }
        if (lock == null)
        {
            lock = targetClazz;
        }
        synchronized (lock)
        {
            Field[] fields = targetClazz.getDeclaredFields();

            Object result = null;
            for (int i = 0; (i < fields.length) && (result == null); i++)
            {
                if (Modifier.isStatic(fields[i].getModifiers()) &&
                    (fields[i].getType() == targetType))
                {
                    fields[i].setAccessible(true);

                    result = fields[i].get(null);

                    if (result != null)
                    {
                        if ((condition == null) ||
                            !result.getClass().getName().equals(condition.getName()))
                        {
                            fields[i].set(null, null);
                        }
                    }
                }
            }
            if (result != null)
            {
                if ((condition == null) || !result.getClass().getName().equals(condition.getName()))
                {
                    // reset cache
                    for (int i = 0; i < fields.length; i++)
                    {
                        if (Modifier.isStatic(fields[i].getModifiers()) &&
                            (fields[i].getType() == Hashtable.class))
                        {
                            fields[i].setAccessible(true);
                            Hashtable cache = (Hashtable) fields[i].get(null);
                            if (cache != null)
                            {
                                cache.clear();
                            }
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    public void flush(Class targetClazz, Object lock) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            Actions actions = (Actions) m_actions.get();
            actions.set(Actions.FLUSH_FIELD_ACTION, targetClazz, lock);
            try
            {
                AccessController.doPrivileged(actions, m_acc);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            _flush(targetClazz, lock);
        }
    }

    private static void _flush(Class targetClazz, Object lock) throws Exception
    {
        synchronized (lock) 
        {
            Field[] fields = targetClazz.getDeclaredFields();
            // reset cache
            for (int i = 0; i < fields.length; i++)
            {
                if (Modifier.isStatic(fields[i].getModifiers()) &&
                    ((fields[i].getType() == Hashtable.class) || (fields[i].getType() == HashMap.class)))
                {
                    fields[i].setAccessible(true);
                    if (fields[i].getType() == Hashtable.class)
                    {
                        Hashtable cache = (Hashtable) fields[i].get(null);
                        if (cache != null)
                        {
                            cache.clear();
                        }
                    }
                    else
                    {
                        HashMap cache = (HashMap) fields[i].get(null);
                        if (cache != null)
                        {
                            cache.clear();
                        }
                    }
                }
            }
        }
    }

    private static class Actions implements PrivilegedExceptionAction
    {
        public static final int INITIALIZE_CONTEXT = 0;

        public static final int ADD_EXTENSION_URL = 1;
        public static final int CREATE_MODULECLASSLOADER_ACTION = 2;
        public static final int CREATE_TMPFILE_ACTION = 3;
        public static final int CREATE_URL_ACTION = 4;
        public static final int CREATE_URL_WITH_CONTEXT_ACTION = 5;
        public static final int DELETE_FILE_ACTION = 6;
        public static final int EXEC_ACTION = 7;
        public static final int FILE_EXISTS_ACTION = 8;
        public static final int FILE_IS_DIRECTORY_ACTION = 9;
        public static final int FOR_NAME_ACTION = 10;
        public static final int GET_ABSOLUTE_PATH_ACTION = 11;
        public static final int GET_CONSTRUCTOR_ACTION = 12;
        public static final int GET_DECLARED_CONSTRUCTOR_ACTION = 13;
        public static final int GET_DECLARED_METHOD_ACTION = 14;
        public static final int GET_FIELD_ACTION = 15;
        public static final int GET_FILE_INPUT_ACTION = 16;
        public static final int GET_FILE_OUTPUT_ACTION = 17;
        public static final int GET_METHOD_ACTION = 19;
        public static final int GET_POLICY_ACTION = 20;
        public static final int GET_PROPERTY_ACTION = 21;
        public static final int GET_PARENT_CLASS_LOADER_ACTION = 22;
        public static final int GET_SYSTEM_CLASS_LOADER_ACTION = 23;
        public static final int GET_URL_INPUT_ACTION = 24;
        public static final int INVOKE_CONSTRUCTOR_ACTION = 25;
        public static final int INVOKE_DIRECTMETHOD_ACTION = 26;
        public static final int INVOKE_METHOD_ACTION = 27;
        public static final int LIST_DIRECTORY_ACTION = 28;
        public static final int MAKE_DIRECTORIES_ACTION = 29;
        public static final int MAKE_DIRECTORY_ACTION = 30;
        public static final int OPEN_JARX_ACTION = 31;
        public static final int OPEN_JARX_VERIFY_ACTION = 32;
        public static final int OPEN_URLCONNECTION_ACTION = 33;
        public static final int RENAME_FILE_ACTION = 34;
        public static final int SET_ACCESSIBLE_ACTION = 35;
        public static final int START_ACTIVATOR_ACTION = 36;
        public static final int STOP_ACTIVATOR_ACTION = 37;
        public static final int SWAP_FIELD_ACTION = 38;
        public static final int SYSTEM_EXIT_ACTION = 39;
        public static final int FLUSH_FIELD_ACTION = 40;
        public static final int GET_CLASS_LOADER_ACTION = 41;

        private int m_action = -1;
        private Object m_arg1 = null;
        private Object m_arg2 = null;
        private Object m_arg3 = null;
        private Object m_arg4 = null;
        private Object m_arg5 = null;

        public void set(int action)
        {
            m_action = action;
        }

        public void set(int action, Object arg1)
        {
            m_action = action;
            m_arg1 = arg1;
        }

        public void set(int action, Object arg1, Object arg2)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;
        }

        public void set(int action, Object arg1, Object arg2, Object arg3)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;
            m_arg3 = arg3;
        }

        public void set(int action, Object arg1, Object arg2, Object arg3,
            Object arg4)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;
            m_arg3 = arg3;
            m_arg4 = arg4;
        }

        public void set(int action, Object arg1, Object arg2, Object arg3,
            Object arg4, Object arg5)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;
            m_arg3 = arg3;
            m_arg4 = arg4;
            m_arg5 = arg5;
        }

        private void unset()
        {
            m_action = -1;
            m_arg1 = null;
            m_arg2 = null;
            m_arg3 = null;
            m_arg4 = null;
            m_arg5 = null;
        }

        public Object run() throws Exception
        {
            int action =  m_action;
            Object arg1 = m_arg1;
            Object arg2 = m_arg2;
            Object arg3 = m_arg3;
            Object arg4 = m_arg4;
            Object arg5 = m_arg5;

            unset();

            if (action == INITIALIZE_CONTEXT)
            {
                return AccessController.getContext();
            }
            else if (action == GET_PROPERTY_ACTION)
            {
                return System.getProperty((String) arg1, (String) arg2);
            }
            else if (action == GET_PARENT_CLASS_LOADER_ACTION)
            {
                return ((ClassLoader) arg1).getParent();
            }
            else if (action == GET_SYSTEM_CLASS_LOADER_ACTION)
            {
                return ClassLoader.getSystemClassLoader();
            }
            else if (action == FOR_NAME_ACTION)
            {
                return Class.forName((String) arg1);
            }
            else if (action == CREATE_URL_ACTION)
            {
                return new URL((String) arg1, (String) arg2,
                    ((Integer) arg3).intValue(), (String) arg4,
                    (URLStreamHandler) arg5);
            }
            else if (action == CREATE_URL_WITH_CONTEXT_ACTION)
            {
                return new URL((URL) arg1, (String) arg2,
                    (URLStreamHandler) arg3);
            }
            else if (action == EXEC_ACTION)
            {
                return Runtime.getRuntime().exec((String) arg1);
            }
            else if (action == GET_ABSOLUTE_PATH_ACTION)
            {
                return ((File) arg1).getAbsolutePath();
            }
            else if (action == FILE_EXISTS_ACTION)
            {
                return ((File) arg1).exists() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == FILE_IS_DIRECTORY_ACTION)
            {
                return ((File) arg1).isDirectory() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == MAKE_DIRECTORY_ACTION)
            {
                return ((File) arg1).mkdir() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == MAKE_DIRECTORIES_ACTION)
            {
                return ((File) arg1).mkdirs() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == LIST_DIRECTORY_ACTION)
            {
                return ((File) arg1).listFiles();
            }
            else if (action == RENAME_FILE_ACTION)
            {
                return ((File) arg1).renameTo((File) arg2) ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == GET_FILE_INPUT_ACTION)
            {
                return new FileInputStream((File) arg1);
            }
            else if (action == GET_FILE_OUTPUT_ACTION)
            {
                return new FileOutputStream((File) arg1);
            }
            else if (action == DELETE_FILE_ACTION)
            {
                return ((File) arg1).delete() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (action == OPEN_JARX_ACTION)
            {
                return new JarFileX((File) arg1);
            }
            else if (action == OPEN_JARX_VERIFY_ACTION)
            {
                return new JarFileX((File) arg1, ((Boolean) arg2).booleanValue());
            }
            else if (action == GET_URL_INPUT_ACTION)
            {
                return ((URLConnection) arg1).getInputStream();
            }
            else if (action == START_ACTIVATOR_ACTION)
            {
                ((BundleActivator) arg1).start((BundleContext) arg2);
                return null;
            }
            else if (action == STOP_ACTIVATOR_ACTION)
            {
                ((BundleActivator) arg1).stop((BundleContext) arg2);
                return null;
            }
            else if (action == SYSTEM_EXIT_ACTION)
            {
                System.exit(((Integer) arg1).intValue());
            }
            else if (action == GET_POLICY_ACTION)
            {
                return Policy.getPolicy();
            }
            else if (action == CREATE_TMPFILE_ACTION)
            {
                return File.createTempFile((String) arg1, (String) arg2,
                    (File) arg3);
            }
            else if (action == OPEN_URLCONNECTION_ACTION)
            {
                return ((URL) arg1).openConnection();
            }
            else if (action == ADD_EXTENSION_URL)
            {
                Method addURL =
                    URLClassLoader.class.getDeclaredMethod("addURL",
                    new Class[] {URL.class});
                addURL.setAccessible(true);
                addURL.invoke(arg2, new Object[]{arg1});
            }
            else if (action == GET_CONSTRUCTOR_ACTION)
            {
                return ((Class) arg1).getConstructor((Class[]) arg2);
            }
            else if (action == GET_DECLARED_CONSTRUCTOR_ACTION)
            {
                return ((Class) arg1).getDeclaredConstructor((Class[]) arg2);
            }
            else if (action == GET_METHOD_ACTION)
            {
                return ((Class) arg1).getMethod((String) arg2, (Class[]) arg3);
            }
            else if (action == INVOKE_METHOD_ACTION)
            {
                ((Method) arg1).setAccessible(true);
                return ((Method) arg1).invoke(arg2, (Object[]) arg3);
            }
            else if (action == INVOKE_DIRECTMETHOD_ACTION)
            {
                return ((Method) arg1).invoke(arg2, (Object[]) arg3);
            }
            else if (action == INVOKE_CONSTRUCTOR_ACTION)
            {
                return ((Constructor) arg1).newInstance((Object[]) arg2);
            }
            else if (action == SWAP_FIELD_ACTION)
            {
                return _swapStaticFieldIfNotClass((Class) arg1,
                    (Class) arg2, (Class) arg3, (String) arg4);
            }
            else if (action == GET_FIELD_ACTION)
            {
                Field field = ((Class) arg1).getDeclaredField((String) arg2);
                field.setAccessible(true);
                return field.get(arg3);
            }
            else if (action == GET_DECLARED_METHOD_ACTION)
            {
                return ((Class) arg1).getDeclaredMethod((String) arg2, (Class[]) arg3);
            }
            else if (action == SET_ACCESSIBLE_ACTION)
            {
                ((AccessibleObject) arg1).setAccessible(true);
            }
            else if (action == FLUSH_FIELD_ACTION)
            {
                _flush(((Class) arg1), arg2);
            }
            else if (action == GET_CLASS_LOADER_ACTION)
            {
                return ((Class) arg1).getClassLoader();
            }

            return null;
        }
    }
}