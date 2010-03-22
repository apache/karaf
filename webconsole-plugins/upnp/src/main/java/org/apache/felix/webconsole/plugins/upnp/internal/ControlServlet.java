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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This class handles requests from the Web Interface. It is separated from
 * the WebConsolePlugin just to improve readability. This servlet actually
 * is not registered in HTTP service.
 */
public class ControlServlet extends HttpServlet implements ServiceTrackerCustomizer
{

    private static final SimpleDateFormat DATA_FORMAT = new SimpleDateFormat(
        "EEE, d MMM yyyy HH:mm:ss Z");

    final HashMap icons = new HashMap(10);
    final HashMap sessions = new HashMap(10);

    private ServiceTracker tracker;
    private final BundleContext bc;

    private static final long LAST_MODIFIED = System.currentTimeMillis();

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        String udn = request.getParameter("icon");

        if (udn != null)
        {
            UPnPIcon icon = (UPnPIcon) icons.get(udn);
            if (icon == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            else
            {
                if (request.getDateHeader("If-Modified-Since") > 0)
                {
                    // if it is already in cache - don't bother to go further
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
                else
                {
                    // enable caching
                    response.setDateHeader("Last-Modified", LAST_MODIFIED);

                    String mime = icon.getMimeType();
                    if (mime != null)
                        response.setContentType(mime);
                    OutputStream out = response.getOutputStream();

                    int size = icon.getSize();
                    if (size > 0)
                        response.setContentLength(size);

                    InputStream in = icon.getInputStream();
                    // can't use buffer, because it's might block if reading byte[]
                    int read;
                    while (-1 != (read = in.read()))
                        out.write(read);
                }
            }
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            JSONObject json = new JSONObject();

            String method = request.getParameter("action");

            if ("listDevices".equals(method))
            {
                getSession(request).unsubscribe();

                ServiceReference[] refs = tracker.getServiceReferences();
                // add root devices only
                for (int i = 0; refs != null && i < refs.length; i++)
                {
                    if (refs[i] != null
                        && refs[i].getProperty(UPnPDevice.PARENT_UDN) == null)
                    {
                        json.append("devices", deviceTreeToJSON(refs[i]));
                    }
                }
            }
            else if ("serviceDetails".equals(method))
            {
                UPnPService service = requireService(request);
                SessionObject session = getSession(request)//
                .subscribe(require("udn", request), service.getId());

                json = serviceToJSON(service, session);
            }
            else if ("invokeAction".equals(method))
            {
                UPnPService service = requireService(request);
                UPnPAction action = service.getAction(require("actionID", request));

                json = invoke(
                    action, //
                    request.getParameterValues("names"),
                    request.getParameterValues("vals"));
            }
            else
            {
                throw new ServletException("Invalid action: " + method);
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(json.toString(2));
        }
        catch (ServletException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException(e.toString());
        }
    }

    private final SessionObject getSession(HttpServletRequest request)
    {
        String sessionID = request.getSession().getId();
        SessionObject ret = (SessionObject) sessions.get(sessionID);
        if (ret == null)
        {
            ret = new SessionObject(bc, sessionID, sessions);
            request.getSession().setAttribute("___upnp.session.object", ret);
        }
        return ret;
    }

    private static final String require(String name, HttpServletRequest request)
        throws ServletException
    {
        String value = request.getParameter(name);
        if (value == null)
            throw new ServletException("missing parameter: " + name);
        return value;
    }

    private final UPnPService requireService(HttpServletRequest request)
        throws ServletException
    {
        String deviceUdn = require("udn", request);
        String serviceUrn = require("urn", request);

        UPnPDevice device = getDevice(deviceUdn);
        return getService(device, serviceUrn);
    }

    private final JSONObject deviceTreeToJSON(ServiceReference ref) throws JSONException
    {
        UPnPDevice device = (UPnPDevice) tracker.getService(ref);
        Object[] refs = tracker.getServiceReferences();

        Object parentUdn = ref.getProperty(UPnPDevice.UDN);
        JSONObject json = deviceToJSON(ref, device);

        // add child devices
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            ref = (ServiceReference) refs[i];

            Object parent = ref.getProperty(UPnPDevice.PARENT_UDN);
            Object currentUDN = ref.getProperty(UPnPDevice.UDN);
            if (parent == null)
            { // no parent
                continue;
            }
            else if (currentUDN != null && currentUDN.equals(parent))
            { // self ?
                continue;
            }
            else if (parentUdn.equals(parent))
            {
                device = (UPnPDevice) tracker.getService(ref);
                json.append("children", deviceTreeToJSON(ref));
            }
        }
        return json;
    }

    private static final JSONObject deviceToJSON(ServiceReference ref, UPnPDevice device)
        throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("icon", device.getIcons(null) != null);

        // add properties
        String[] props = ref.getPropertyKeys();
        JSONObject _props = new JSONObject();
        for (int i = 0; props != null && i < props.length; i++)
        {
            _props.put(props[i], ref.getProperty(props[i]));
        }
        json.put("props", _props);

        UPnPService[] services = device.getServices();
        for (int i = 0; services != null && i < services.length; i++)
        {
            json.append("services", services[i].getType());
        }

        return json;
    }

    private static final JSONObject serviceToJSON(UPnPService service,
        SessionObject session) throws JSONException
    {
        JSONObject json = new JSONObject();

        // add service properties
        json.put("type", service.getType());
        json.put("id", service.getId());

        // add state variables
        UPnPStateVariable[] vars = service.getStateVariables();
        for (int i = 0; vars != null && i < vars.length; i++)
        {
            Object value = null;
            if (vars[i] instanceof UPnPLocalStateVariable)
            {
                value = ((UPnPLocalStateVariable) vars[i]).getCurrentValue();
            }

            if (value == null)
                value = session.getValue(vars[i].getName());
            if (value == null)
                value = "---";

            json.append("variables", new JSONObject() //
            .put("name", vars[i].getName()) //
            .put("value", value) //
            .put("defalt", vars[i].getDefaultValue()) //
            .put("min", vars[i].getMinimum()) //
            .put("max", vars[i].getMaximum()) //
            .put("step", vars[i].getStep()) //
            .put("allowed", vars[i].getAllowedValues()) //
            .put("sendsEvents", vars[i].sendsEvents()) //
            );
        }

        // add actions
        UPnPAction[] actions = service.getActions();
        for (int i = 0; actions != null && i < actions.length; i++)
        {
            json.append("actions", actionToJSON(actions[i]));
        }

        return json;
    }

    private static final JSONObject actionToJSON(UPnPAction action) throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("name", action.getName());
        String[] names = action.getInputArgumentNames();
        for (int i = 0; names != null && i < names.length; i++)
        {
            UPnPStateVariable variable = action.getStateVariable(names[i]);
            json.append("inVars", new JSONObject()//
            .put("name", names[i])//
            .put("type", variable.getUPnPDataType()));
        }

        return json;

    }

    private static final JSONObject invoke(UPnPAction action, String[] names,
        String[] vals) throws Exception
    {
        JSONObject json = new JSONObject();

        // check input arguments
        Hashtable inputArgs = null;
        if (names != null && vals != null && names.length > 0
            && names.length == vals.length)
        {
            inputArgs = new Hashtable(names.length);
            for (int i = 0; i < names.length; i++)
            {
                UPnPStateVariable var = action.getStateVariable(names[i]);
                Class javaType = var.getJavaDataType();
                Constructor constructor = javaType.getConstructor(new Class[] { String.class });
                Object argObj = constructor.newInstance(new Object[] { vals[i] });

                inputArgs.put(names[i], argObj);
            }
        }

        // invoke
        Dictionary out = action.invoke(inputArgs);

        // prepare output arguments
        if (out != null && out.size() > 0)
        {
            for (Enumeration e = out.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                UPnPStateVariable var = action.getStateVariable(key);

                Object value = out.get(key);
                if (value instanceof Date)
                {
                    synchronized (DATA_FORMAT)
                    {
                        value = DATA_FORMAT.format((Date) value);
                    }
                }
                else if (value instanceof byte[])
                {
                    value = hex((byte[]) value);
                }

                json.append("output", new JSONObject() //
                .put("name", key)//
                .put("type", var.getUPnPDataType()) //
                .put("value", value));
            }
        }
        return json;
    }

    private static final String hex(byte[] data)
    {
        if (data == null)
            return "null";
        StringBuffer sb = new StringBuffer(data.length * 3);
        synchronized (sb)
        {
            for (int i = 0; i < data.length; i++)
            {
                sb.append(Integer.toHexString(data[i] & 0xff)).append('-');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private final UPnPDevice getDevice(String udn)
    {
        ServiceReference[] refs = tracker.getServiceReferences();
        String _udn;
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            _udn = (String) refs[i].getProperty(UPnPDevice.UDN);
            if (_udn != null && _udn.equals(udn))
            {
                return (UPnPDevice) tracker.getService(refs[i]);
            }
        }

        throw new IllegalArgumentException("Device '" + udn + "' not found!");
    }

    private final UPnPService getService(UPnPDevice device, String urn)
    {
        UPnPService[] services = device.getServices();
        for (int i = 0; services != null && i < services.length; i++)
        {
            if (services[i].getType().equals(urn))
            {
                return services[i];
            }
        }

        throw new IllegalArgumentException("Service '" + urn + "' not found!");
    }

    /**
     * Creates new XML-RPC handler.
     * 
     * @param bc the bundle context
     * @param iconServlet the icon servlet.
     */
    ControlServlet(BundleContext bc, ServiceTracker tracker)
    {
        this.bc = bc;
        this.tracker = tracker;
    }

    /**
     * Cancels the scheduled timers
     */
    void close()
    {
        icons.clear();
        for (Iterator i = sessions.values().iterator(); i.hasNext();)
        {
            ((SessionObject) i.next()).unsubscribe();
        }
        sessions.clear();
    }

    /* ---------- BEGIN SERVICE TRACKER */
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void modifiedService(ServiceReference ref, Object serv)
    {/* unused */
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void removedService(ServiceReference ref, Object serv)
    {
        icons.remove(ref.getProperty(UPnPDevice.UDN));
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public final Object addingService(ServiceReference ref)
    {
        UPnPDevice device = (UPnPDevice) bc.getService(ref);

        UPnPIcon[] _icons = device.getIcons(null);
        if (_icons != null && _icons.length > 0)
        {
            icons.put(ref.getProperty(UPnPDevice.UDN), _icons[0]);
        }

        return device;
    }

}
