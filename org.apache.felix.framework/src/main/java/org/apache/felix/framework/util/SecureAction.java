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
package org.apache.felix.framework.util;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.jar.JarFile;

import org.apache.felix.moduleloader.JarFileX;

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
    protected static transient int BUFSIZE = 4096;

    private AccessControlContext m_acc = null;
    private Actions m_actions = new Actions();

    public SecureAction()
    {
        m_acc = AccessController.getContext();
    }

    public synchronized String getSystemProperty(String name, String def)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.GET_PROPERTY_ACTION, name, def);
                return (String) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized Class forName(String name) throws ClassNotFoundException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.FOR_NAME_ACTION, name);
                return (Class) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized URL createURL(String protocol, String host,
        int port, String path, URLStreamHandler handler)
        throws MalformedURLException
    {
        if (System.getSecurityManager() != null)
        {
            try 
            {
                m_actions.set(
                    Actions.CREATE_URL_ACTION, protocol, host, port, path, handler);
                return (URL) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized boolean fileExists(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.FILE_EXISTS_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized boolean isFileDirectory(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.FILE_IS_DIRECTORY_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized boolean mkdir(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.MAKE_DIRECTORY_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized boolean mkdirs(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.MAKE_DIRECTORIES_ACTION, file);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized File[] listDirectory(File file)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.LIST_DIRECTORY_ACTION, file);
                return (File[]) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized boolean renameFile(File oldFile, File newFile)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.RENAME_FILE_ACTION, oldFile, newFile);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized InputStream getFileInputStream(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.GET_FILE_INPUT_ACTION, file);
                return (InputStream) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized OutputStream getFileOutputStream(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.GET_FILE_OUTPUT_ACTION, file);
                return (OutputStream) AccessController.doPrivileged(m_actions, m_acc);
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

    public synchronized boolean deleteFile(File target)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.DELETE_FILE_ACTION, target);
                return ((Boolean) AccessController.doPrivileged(m_actions, m_acc))
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

    public synchronized JarFile openJAR(File file) throws IOException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                m_actions.set(Actions.OPEN_JAR_ACTION, file);
                return (JarFile) AccessController.doPrivileged(m_actions, m_acc);
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

    private class Actions implements PrivilegedExceptionAction
    {
        public static final int GET_PROPERTY_ACTION = 0;
        public static final int FOR_NAME_ACTION = 1;
        public static final int CREATE_URL_ACTION = 2;
        public static final int FILE_EXISTS_ACTION = 3;
        public static final int FILE_IS_DIRECTORY_ACTION = 4;
        public static final int MAKE_DIRECTORY_ACTION = 5;
        public static final int MAKE_DIRECTORIES_ACTION = 6;
        public static final int LIST_DIRECTORY_ACTION = 7;
        public static final int RENAME_FILE_ACTION = 8;
        public static final int GET_FILE_INPUT_ACTION = 9;
        public static final int GET_FILE_OUTPUT_ACTION = 10;
        public static final int DELETE_FILE_ACTION = 11;
        public static final int OPEN_JAR_ACTION = 12;

        private int m_action = -1;
        private Object m_arg1 = null;
        private Object m_arg2 = null;

        private String m_protocol = null;
        private String m_host = null;
        private int m_port = -1;
        private String m_path = null;
        private URLStreamHandler m_handler = null;

        public void set(int action, Object arg1)
        {
            m_action = action;
            m_arg1 = arg1;

            m_arg2 = null;
            m_protocol = null;
            m_host = null;
            m_port = -1;
            m_path = null;
            m_handler = null;
        }

        public void set(int action, Object arg1, Object arg2)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;

            m_protocol = null;
            m_host = null;
            m_port = -1;
            m_path = null;
            m_handler = null;
        }

        public void set(int action, String protocol, String host,
            int port, String path, URLStreamHandler handler)
        {
            m_action = action;
            m_protocol = protocol;
            m_host = host;
            m_port = port;
            m_path = path;
            m_handler = handler;

            m_arg1 = null;
            m_arg2 = null;
        }

        public Object run() throws Exception
        {
            if (m_action == GET_PROPERTY_ACTION)
            {
                return System.getProperty((String) m_arg1, (String) m_arg2);
            }
            else if (m_action == FOR_NAME_ACTION)
            {
                return Class.forName((String) m_arg1);
            }
            else if (m_action == CREATE_URL_ACTION)
            {
                return new URL(m_protocol, m_host, m_port, m_path, m_handler);
            }
            else if (m_action == FILE_EXISTS_ACTION)
            {
                return ((File) m_arg1).exists() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == FILE_IS_DIRECTORY_ACTION)
            {
                return ((File) m_arg1).isDirectory() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == MAKE_DIRECTORY_ACTION)
            {
                return ((File) m_arg1).mkdir() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == MAKE_DIRECTORIES_ACTION)
            {
                return ((File) m_arg1).mkdirs() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == LIST_DIRECTORY_ACTION)
            {
                return ((File) m_arg1).listFiles();
            }
            else if (m_action == RENAME_FILE_ACTION)
            {
                return ((File) m_arg1).renameTo((File) m_arg2) ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == GET_FILE_INPUT_ACTION)
            {
                return new FileInputStream((File) m_arg1);
            }
            else if (m_action == GET_FILE_OUTPUT_ACTION)
            {
                return new FileOutputStream((File) m_arg1);
            }
            else if (m_action == DELETE_FILE_ACTION)
            {
                return ((File) m_arg1).delete() ? Boolean.TRUE : Boolean.FALSE;
            }
            else if (m_action == OPEN_JAR_ACTION)
            {
                return new JarFileX((File) m_arg1);
            }
            return null;
        }
    }
}
