/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.shell.remote;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Implements a mediator pattern class for services from the OSGi container.
 */
class ServiceMediator
{
    private BundleContext m_bundleContext;
    private ShellService m_felixShellService;
    private Latch m_felixShellServiceLatch;
    private LogService m_logService;
    private Latch m_logServiceLatch;

    /**
     * Returns a reference to the <tt>ShellService</tt> (Felix).
     *
     * @param wait time in milliseconds to wait for the reference if it isn't available.
     * @return the reference to the <tt>ShellService</tt> as obtained from the OSGi service layer.
     */
    public ShellService getFelixShellService(long wait)
    {
        try
        {
            if (wait < 0)
            {
                m_felixShellServiceLatch.acquire();
            }
            else if (wait > 0)
            {
                m_felixShellServiceLatch.attempt(wait);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }

        return m_felixShellService;
    }//getFelixShellService

    public LogService getLogServiceLatch(long wait)
    {
        try
        {
            if (wait < 0)
            {
                m_logServiceLatch.acquire();
            }
            else if (wait > 0)
            {
                m_logServiceLatch.attempt(wait);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }
        return m_logService;
    }//getLogService

    public void info(String msg)
    {
        if (m_logService != null)
        {
            m_logService.log(LogService.LOG_INFO, msg);
        }
        else
        {
            sysout(msg);
        }
    }//info

    public void error(String msg, Throwable t)
    {
        if (m_logService != null)
        {
            m_logService.log(LogService.LOG_ERROR, msg, t);
        }
        else
        {
            syserr(msg, t);
        }
    }//error

    public void error(String msg)
    {
        if (m_logService != null)
        {
            m_logService.log(LogService.LOG_ERROR, msg);
        }
        else
        {
            syserr(msg, null);
        }
    }//error

    public void debug(String msg)
    {
        if (m_logService != null)
        {
            m_logService.log(LogService.LOG_DEBUG, msg);
        }
        else
        {
            sysout(msg);
        }
    }//debug

    public void warn(String msg)
    {
        if (m_logService != null)
        {
            m_logService.log(LogService.LOG_WARNING, msg);
        }
        else
        {
            syserr(msg, null);
        }
    }//warn

    private void sysout(String msg)
    {
        //Assemble String
        StringBuffer sbuf = new StringBuffer();
        Bundle b = m_bundleContext.getBundle();
        sbuf.append(b.getHeaders().get(Constants.BUNDLE_NAME));
        sbuf.append(" [");
        sbuf.append(b.getBundleId());
        sbuf.append("] ");
        sbuf.append(msg);
        System.out.println(sbuf.toString());
    }//sysout

    private void syserr(String msg, Throwable t)
    {
        //Assemble String
        StringBuffer sbuf = new StringBuffer();
        Bundle b = m_bundleContext.getBundle();
        sbuf.append(b.getHeaders().get(Constants.BUNDLE_NAME));
        sbuf.append(" [");
        sbuf.append(b.getBundleId());
        sbuf.append("] ");
        sbuf.append(msg);
        System.err.println(sbuf.toString());
        if (t != null)
        {
            t.printStackTrace(System.err);
        }
    }//logToSystem

    /**
     * Activates this mediator to start tracking the required services using the
     * OSGi service layer.
     *
     * @param bc the bundle's context.
     * @return true if activated successfully, false otherwise.
     */
    public boolean activate(BundleContext bc)
    {
        //get the context
        m_bundleContext = bc;

        m_felixShellServiceLatch = createWaitLatch();
        m_logServiceLatch = createWaitLatch();

        //prepareDefinitions listener
        ServiceListener serviceListener = new ServiceListenerImpl();

        //prepareDefinitions the filter, ShellService is required,
        //LogService may be missing, in which case we only use the
        // ShellService part of the filter
        String filter = "(objectclass=" + ShellService.class.getName() + ")";
        try
        {
            filter = "(|" + filter + "(objectclass=" + LogService.class.getName() + "))";
        }
        catch (Throwable t)
        {
            // ignore
        }

        try
        {
            //add the listener to the bundle context.
            bc.addServiceListener(serviceListener, filter);

            //ensure that already registered Service instances are registered with
            //the manager
            ServiceReference[] srl = bc.getServiceReferences(null, filter);
            for (int i = 0; srl != null && i < srl.length; i++)
            {
                serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
            }
        }
        catch (InvalidSyntaxException ex)
        {
            ex.printStackTrace(System.err);
            return false;
        }
        return true;
    }//activate

    /**
     * Deactivates this mediator, nulling out all references.
     * If called when the bundle is stopped, the framework should actually take
     * care of unregistering the <tt>ServiceListener</tt>.
     */
    public void deactivate()
    {
        m_felixShellService = null;
        m_felixShellServiceLatch = null;
        m_bundleContext = null;
    }//deactivate

    /**
     * Creates a simple wait latch to be used for the mechanism that allows entities
     * in the bundles to wait for references.
     *
     * @return a new Latch instance.
     */
    private Latch createWaitLatch()
    {
        return new Latch();
    }//createWaitLatch

    /**
     * The <tt>ServiceListener</tt> implementation.
     */
    private class ServiceListenerImpl implements ServiceListener
    {
        public void serviceChanged(ServiceEvent ev)
        {
            ServiceReference sr = ev.getServiceReference();
            Object o = null;
            switch (ev.getType())
            {
                case ServiceEvent.REGISTERED:
                    o = m_bundleContext.getService(sr);
                    if (o == null)
                    {
                        return;
                    }
                    else if (o instanceof ShellService)
                    {
                        m_felixShellService = (ShellService) o;
                        m_felixShellServiceLatch.release();
                    }
                    else if (o instanceof LogService)
                    {
                        m_logService = (LogService) o;
                        m_logServiceLatch.release();
                    }
                    else
                    {
                        m_bundleContext.ungetService(sr);
                    }
                    break;
                case ServiceEvent.UNREGISTERING:
                    o = m_bundleContext.getService(sr);
                    if (o == null)
                    {
                        return;
                    }
                    else if (o instanceof ShellService)
                    {
                        m_felixShellService = null;
                        m_felixShellServiceLatch = createWaitLatch();
                    }
                    else if (o instanceof LogService)
                    {
                        m_logService = null;
                        m_logServiceLatch = createWaitLatch();
                    }
                    else
                    {
                        m_bundleContext.ungetService(sr);
                    }
                    break;
            }
        }
    }//inner class ServiceListenerImpl
    public static long WAIT_UNLIMITED = -1;
    public static long NO_WAIT = 0;
}//class ServiceMediator
