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
package org.apache.felix.framework;

import java.io.IOException;
import java.net.*;

import org.apache.felix.framework.util.SecureAction;

class URLHandlersBundleStreamHandler extends URLStreamHandler
{
    private final Felix m_framework;
    private final SecureAction m_action;

    public URLHandlersBundleStreamHandler(Felix framework)
    {
        m_framework = framework;
        m_action = null;
    }

    public URLHandlersBundleStreamHandler(SecureAction action)
    {
        m_framework = null;
        m_action = action;
    }

    protected synchronized URLConnection openConnection(URL url) throws IOException
    {
        if (m_framework != null)
        {
            return new URLHandlersBundleURLConnection(url, m_framework);
        }
        
        Object framework = URLHandlers.getFrameworkFromContext();
        
        if (framework != null)
        {
            // TODO: optimize this to not use reflection if not needed
            try
            {
                Class targetClass = framework.getClass().getClassLoader().loadClass(
                    URLHandlersBundleURLConnection.class.getName());
                
                return (URLConnection) m_action.invoke(m_action.getConstructor(targetClass, 
                    new Class[]{URL.class, framework.getClass()}),
                    new Object[]{url, framework});
            }
            catch (Exception ex)
            {
                throw new IOException(ex.getMessage());
            }
        }
        throw new IOException("No framework context found");
    }
}