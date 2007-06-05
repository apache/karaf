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

import java.net.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IURLPolicy;

public class URLPolicyImpl implements IURLPolicy
{
    private Logger m_logger = null;
    private URLStreamHandler m_streamHandler = null;
    private IModule m_module = null;
    private static SecureAction m_secureAction = new SecureAction();

// TODO: ML - IT SUCKS HAVING A URL POLICY OBJECT PER MODULE!
    public URLPolicyImpl(Logger logger, URLStreamHandler streamHandler, IModule module)
    {
        m_logger = logger;
        m_streamHandler = streamHandler;
        m_module = module;
    }

    public URL createURL(int port, String path)
    {
         // Add a slash if there is one already, otherwise
         // the is no slash separating the host from the file
         // in the resulting URL.
         if (!path.startsWith("/"))
         {
             path = "/" + path;
         }

         try
         {
             return m_secureAction.createURL(
                 FelixConstants.BUNDLE_URL_PROTOCOL,
                 m_module.getId(), port, path, m_streamHandler);
         }
         catch (MalformedURLException ex)
         {
             m_logger.log(
                 Logger.LOG_ERROR,
                 "Unable to create resource URL.",
                 ex);
         }
         return null;
    }
}