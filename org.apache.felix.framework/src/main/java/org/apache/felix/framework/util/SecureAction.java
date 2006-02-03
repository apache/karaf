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

import java.security.*;

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
    private AccessControlContext m_acc = null;

    public SecureAction()
    {
        m_acc = AccessController.getContext();
    }

    public String getProperty(String name, String def)
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                return (String) AccessController.doPrivileged(
                    new Actions(Actions.GET_PROPERTY_ACTION, name, def), m_acc);
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

    public Class forName(String name) throws ClassNotFoundException
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                return (Class) AccessController.doPrivileged(
                    new Actions(Actions.FOR_NAME_ACTION, name), m_acc);
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

    private static class Actions implements PrivilegedExceptionAction
    {
        public static final int GET_PROPERTY_ACTION = 0;
        public static final int FOR_NAME_ACTION = 1;

        private int m_action = -1;
        private Object m_arg1 = null;
        private Object m_arg2 = null;

        public Actions(int action, Object arg1)
        {
            m_action = action;
            m_arg1 = arg1;
        }

        public Actions(int action, Object arg1, Object arg2)
        {
            m_action = action;
            m_arg1 = arg1;
            m_arg2 = arg2;
        }

        public Object run() throws Exception
        {
            if (m_action == GET_PROPERTY_ACTION)
            {
                return System.getProperty((String) m_arg1, (String) m_arg2);
            }
            else if (m_action ==FOR_NAME_ACTION)
            {
                return Class.forName((String) m_arg1);
            }
            return null;
        }
    }
}