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
package org.apache.felix.example.servicebased.host;

import javax.swing.*;

import org.apache.felix.example.servicebased.host.service.SimpleShape;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

/**
 * Extends the <tt>ServiceTracker</tt> to create a tracker for
 * <tt>SimpleShape</tt> services. The tracker is responsible for
 * listener for the arrival/departure of <tt>SimpleShape</tt>
 * services and informing the application about the availability
 * of shapes. This tracker forces all notifications to be processed
 * on the Swing event thread to avoid synchronization and redraw
 * issues.
**/
public class ShapeTracker extends ServiceTracker
{
    // Flag indicating an added shape.
    private static final int ADDED = 1;
    // Flag indicating a modified shape.
    private static final int MODIFIED = 2;
    // Flag indicating a removed shape.
    private static final int REMOVED = 3;
    // The bundle context used for tracking.
    private BundleContext m_context;
    // The application object to notify.
    private DrawingFrame m_frame;

    /**
     * Constructs a tracker that uses the specified bundle context to
     * track services and notifies the specified application object about
     * changes.
     * @param context The bundle context to be used by the tracker.
     * @param frame The application object to notify about service changes.
    **/
    public ShapeTracker(BundleContext context, DrawingFrame frame)
    {
        super(context, SimpleShape.class.getName(), null);
        m_context = context;
        m_frame = frame;
    }

    /**
     * Overrides the <tt>ServiceTracker</tt> functionality to inform
     * the application object about the added service.
     * @param ref The service reference of the added service.
     * @return The service object to be used by the tracker.
    **/
    public Object addingService(ServiceReference ref)
    {
        SimpleShape shape = new DefaultShape(m_context, ref);
        processShapeOnEventThread(ADDED, ref, shape);
        return shape;
    }

    /**
     * Overrides the <tt>ServiceTracker</tt> functionality to inform
     * the application object about the modified service.
     * @param ref The service reference of the modified service.
     * @param svc The service object of the modified service.
    **/
    public void modifiedService(ServiceReference ref, Object svc)
    {
        processShapeOnEventThread(MODIFIED, ref, (SimpleShape) svc);
    }

    /**
     * Overrides the <tt>ServiceTracker</tt> functionality to inform
     * the application object about the removed service.
     * @param ref The service reference of the removed service.
     * @param svc The service object of the removed service.
    **/
    public void removedService(ServiceReference ref, Object svc)
    {
        processShapeOnEventThread(REMOVED, ref, (SimpleShape) svc);
        ((DefaultShape) svc).dispose();
    }

    /**
     * Processes a received service notification from the <tt>ServiceTracker</tt>,
     * forcing the processing of the notification onto the Swing event thread
     * if it is not already on it.
     * @param action The type of action associated with the notification.
     * @param ref The service reference of the corresponding service.
     * @param shape The service object of the corresponding service.
    **/
    private void processShapeOnEventThread(
        int action, ServiceReference ref, SimpleShape shape)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                processShape(action, ref, shape);
            }
            else
            {
                SwingUtilities.invokeAndWait(new ShapeRunnable(action, ref, shape));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Actually performs the processing of the service notification. Invokes
     * the appropriate callback method on the application object depending on
     * the action type of the notification.
     * @param action The type of action associated with the notification.
     * @param ref The service reference of the corresponding service.
     * @param shape The service object of the corresponding service.
    **/
    private void processShape(int action, ServiceReference ref, SimpleShape shape)
    {
        String name = (String) ref.getProperty(SimpleShape.NAME_PROPERTY);

        switch (action)
        {
            case MODIFIED:
                m_frame.removeShape(name);
                // Purposely let this fall through to the 'add' case to
                // reload the service.

            case ADDED:
                Icon icon = (Icon) ref.getProperty(SimpleShape.ICON_PROPERTY);
                m_frame.addShape(name, icon, shape);
                break;

            case REMOVED:
                m_frame.removeShape(name);
                break;
        }
    }

    /**
     * Simple class used to process service notification handling on the
     * Swing event thread.
    **/
    private class ShapeRunnable implements Runnable
    {
        private int m_action;
        private ServiceReference m_ref;
        private SimpleShape m_shape;

        /**
         * Constructs an object with the specified action, service reference,
         * and service object for processing on the Swing event thread.
         * @param action The type of action associated with the notification.
         * @param ref The service reference of the corresponding service.
         * @param shape The service object of the corresponding service.
        **/
        public ShapeRunnable(int action, ServiceReference ref, SimpleShape shape)
        {
            m_action = action;
            m_ref = ref;
            m_shape = shape;
        }

        /**
         * Calls the <tt>processShape()</tt> method.
        **/
        public void run()
        {
            processShape(m_action, m_ref, m_shape);
        }
    }
}