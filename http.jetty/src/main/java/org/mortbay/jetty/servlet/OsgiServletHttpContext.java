/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.mortbay.jetty.servlet;


import org.mortbay.util.Code;

public class OsgiServletHttpContext
        extends
                ServletHttpContext
{
    protected org.osgi.service.http.HttpContext     m_osgiHttpContext;
    
    public OsgiServletHttpContext(
            org.osgi.service.http.HttpContext osgiHttpContext)
    {
        m_osgiHttpContext = osgiHttpContext;
    }
    
    // intercept to ensure OSGi context is used first for servlet calls to 
    // getMimeType()
    public String getMimeByExtension(String filename)
    { 
        Code.debug("OSGi servlet context: get mime type");
        String encoding = m_osgiHttpContext.getMimeType(filename);

        if (encoding == null)
        {
            encoding = super.getMimeByExtension(filename);
        }
        
        return encoding;
    }
    
}
