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
package org.apache.felix.framework;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author rickhall
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
**/
public class StartLevelImpl implements StartLevel, Runnable
{
    private Felix m_felix = null;
    private List m_requestList = null;
    // Reusable admin permission.
    private static AdminPermission m_adminPerm = new AdminPermission();
    
    public StartLevelImpl(Felix felix)
    {
        m_felix = felix;
        m_requestList = new ArrayList();

        // Start a thread to perform asynchronous package refreshes.
        Thread t = new Thread(this, "FelixStartLevel");
        t.setDaemon(true);
        t.start();
    }
    
    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getStartLevel()
    **/
    public int getStartLevel()
    {
        return m_felix.getStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setStartLevel(int)
    **/
    public void setStartLevel(int startlevel)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        else if (startlevel <= 0)
        {
            throw new IllegalArgumentException(
                "Start level must be greater than zero.");
        }
        synchronized (m_requestList)
        {
            m_requestList.add(new Integer(startlevel));
            m_requestList.notifyAll();
        }
    }

    /**
     * This method is currently only called by the shutdown thread when the
     * framework is shutting down.
     * @param startlevel
    **/
    /* package */ void setStartLevelAndWait(int startlevel)
    {
        Object request = new Integer(startlevel);
        synchronized (request)
        {
            synchronized (m_requestList)
            {
                m_requestList.add(request);
                m_requestList.notifyAll();
            }

            try
            {
                request.wait();
            }
            catch (InterruptedException ex)
            {
                // Log it and ignore since it won't cause much of an issue.
                m_felix.getLogger().log(
                    Logger.LOG_WARNING,
                    "Wait for start level change during shutdown interrupted.",
                    ex);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getBundleStartLevel(org.osgi.framework.Bundle)
    **/
    public int getBundleStartLevel(Bundle bundle)
    {
        return m_felix.getBundleStartLevel(bundle);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setBundleStartLevel(org.osgi.framework.Bundle, int)
    **/
    public void setBundleStartLevel(Bundle bundle, int startlevel)
    {
        m_felix.setBundleStartLevel(bundle, startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getInitialBundleStartLevel()
    **/
    public int getInitialBundleStartLevel()
    {
        return m_felix.getInitialBundleStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setInitialBundleStartLevel(int)
    **/
    public void setInitialBundleStartLevel(int startlevel)
    {
        m_felix.setInitialBundleStartLevel(startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#isBundlePersistentlyStarted(org.osgi.framework.Bundle)
    **/
    public boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        return m_felix.isBundlePersistentlyStarted(bundle);
    }

    public void run()
    {
        Integer request = null;

        // This thread loops forever, thus it should
        // be a daemon thread.
        while (true)
        {
            synchronized (m_requestList)
            {
                // Wait for a request.
                while (m_requestList.size() == 0)
                {
                    try
                    {
                        m_requestList.wait();
                    }
                    catch (InterruptedException ex)
                    {
                        // Ignore.
                    }
                }
                
                // Get the requested start level.
                request = (Integer) m_requestList.remove(0);
            }

            // Set the new start level.
            m_felix.setFrameworkStartLevel(request.intValue());

            // Notify any waiting thread that this request is done.
            synchronized (request)
            {
                request.notifyAll();
            }
        }
    }
}