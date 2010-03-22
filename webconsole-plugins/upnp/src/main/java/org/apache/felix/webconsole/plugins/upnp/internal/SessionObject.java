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
package org.apache.felix.webconsole.plugins.upnp.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

/**
 * The reason for having this SessionObject is the strange event delivery in UPnP. It's not possible
 * read a state variable value, but if register a listener, you will get notified when the value has
 * changed.
 */
final class SessionObject implements HttpSessionBindingListener, UPnPEventListener
{

    static final String LISTENER_CLASS = UPnPEventListener.class.getName();

    private final Map vars = new HashMap();
    private final String sessionID;
    private final Map store;
    private String udn, urn;

    private final BundleContext bc;
    private final Hashtable regProps = new Hashtable(3);
    private ServiceRegistration reg;

    SessionObject(BundleContext bc, String sessionID, Map store)
    {
        this.bc = bc;
        this.sessionID = sessionID;
        this.store = store;
    }

    /**
     * @see javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void valueBound(HttpSessionBindingEvent event)
    {
        store.put(sessionID, this);
    }

    /**
     * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)
     */
    public final void valueUnbound(HttpSessionBindingEvent event)
    {
        unsubscribe();
        store.remove(sessionID); // remove from list of sessions
    }

    /**
     * @see org.osgi.service.upnp.UPnPEventListener#notifyUPnPEvent(java.lang.String,
     *      java.lang.String, java.util.Dictionary)
     */
    public final void notifyUPnPEvent(String deviceId, String serviceId, Dictionary events)
    {
        if (sameDevice(deviceId, serviceId))
        {
            for (Enumeration e = events.keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();
                vars.put(key, events.get(key));
            }
        }
    }

    private final boolean sameDevice(String udn, String urn)
    {
        String _udn = this.udn;
        String _urn = this.urn;
        if (_udn == null || _urn == null)
            return false; // not subscribed
        return _udn.equals(udn) && _urn.equals(urn);
    }

    final synchronized SessionObject subscribe(String udn, String urn)
    {
        if (!sameDevice(udn, urn))
        {
            unsubscribe();
            this.udn = udn;
            this.urn = urn;

            try
            {
                regProps.put(UPnPEventListener.UPNP_FILTER, bc.createFilter(//
                "(&(" + UPnPDevice.UDN + '=' + udn + ")(" + //
                    UPnPService.ID + '=' + urn + "))"));
            }
            catch (InvalidSyntaxException e)
            { /* will not happen */
            }
            reg = bc.registerService(LISTENER_CLASS, this, regProps);
        }
        return this;
    }

    final synchronized SessionObject unsubscribe()
    {
        this.udn = this.urn = null;
        vars.clear();
        if (reg != null)
        {
            reg.unregister();
            reg = null;
        }
        return this;
    }

    final Object getValue(String name)
    {
        return vars.get(name);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SessionObject [sessionID=").append(sessionID).append(", udn=").append(
            udn).append(", urn=").append(urn).append(", vars=").append(vars).append("]");
        return buffer.toString();
    }

}
