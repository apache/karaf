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
package org.apache.osgi.framework;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.apache.osgi.framework.util.FelixConstants;
import org.apache.osgi.moduleloader.*;

public class OSGiURLPolicy implements URLPolicy
{
    private Felix m_felix = null;
    private BundleURLStreamHandler m_handler = null;
    private FakeURLStreamHandler m_fakeHandler = null;

    public OSGiURLPolicy(Felix felix)
    {
        m_felix = felix;
    }

    public URL createCodeSourceURL(ModuleManager mgr, Module module)
    {
        URL url = null;
/*
        BundleImpl bundle = null;
        try
        {
            bundle = (BundleImpl)
                m_felix.getBundle(BundleInfo.getBundleIdFromModuleId(module.getId()));
            if (bundle != null)
            {
                url = new URL(bundle.getInfo().getLocation());
            }
        }
        catch (NumberFormatException ex)
        {
            url = null;
        }
        catch (MalformedURLException ex)
        {
            if (m_fakeHandler == null)
            {
                m_fakeHandler = new FakeURLStreamHandler();
            }
            try
            {
                url = new URL(null,
                    FelixConstants.FAKE_URL_PROTOCOL_VALUE
                    + "//" + bundle.getLocation(), m_fakeHandler);
            }
            catch (Exception ex2)
            {
                url = null;
            }
        }
*/
        return url;
    }

    public URL createResourceURL(ModuleManager mgr, Module module, int rsIdx, String name)
    {
        if (m_handler == null)
        {
            m_handler = new BundleURLStreamHandler(mgr);
        }

        // Add a slash if there is one already, otherwise
        // the is no slash separating the host from the file
        // in the resulting URL.
        if (!name.startsWith("/"))
        {
            name = "/" + name;
        }

        try
        {
            if (System.getSecurityManager() != null)
            {
                return (URL) AccessController.doPrivileged(
                    new CreateURLPrivileged(module.getId(), rsIdx, name));
            }
            else
            {
                return new URL(FelixConstants.BUNDLE_URL_PROTOCOL,
                    module.getId(), -1, "/" + rsIdx + name, m_handler);
            }
        }
        catch (Exception ex)
        {
            System.err.println("OSGiURLPolicy: " + ex);
            return null;
        }
    }

    /**
     * This simple class is used to perform the privileged action of
     * creating a URL using the "bundle:" protocol stream handler.
    **/
    private class CreateURLPrivileged implements PrivilegedExceptionAction
    {
        private String m_id = null;
        private int m_rsIdx = 0;
        private String m_name = null;

        public CreateURLPrivileged(String id, int rsIdx, String name)
        {
            m_id = id;
            m_rsIdx = rsIdx;
            m_name = name;
        }

        public Object run() throws Exception
        {
            return new URL("bundle", m_id, -1, "/" + m_rsIdx + m_name, m_handler);
        }
    }
}