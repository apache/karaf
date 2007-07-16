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
package org.apache.felix.example.extenderbased.host;

import java.util.Dictionary;
import javax.swing.*;
import org.apache.felix.example.extenderbased.host.extension.SimpleShape;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Extends the <tt>BundleTracker</tt> to create a tracker for
 * <tt>SimpleShape</tt> extensions. The tracker is responsible for
 * listener for the arrival/departure of <tt>SimpleShape</tt>
 * extensions and informing the application about the availability
 * of shapes. This tracker forces all notifications to be processed
 * on the Swing event thread to avoid synchronization and redraw
 * issues.
**/
public class ShapeTracker extends BundleTracker
{
    // Flag indicating an added shape.
    private static final int ADDED = 1;
    // Flag indicating a removed shape.
    private static final int REMOVED = 2;
    // The application object to notify.
    private DrawingFrame m_frame;

    /**
     * Constructs a tracker that uses the specified bundle context to
     * track extensions and notifies the specified application object about
     * changes.
     * @param context The bundle context to be used by the tracker.
     * @param frame The application object to notify about extension changes.
    **/
    public ShapeTracker(BundleContext context, DrawingFrame frame)
    {
        super(context, null);
        m_frame = frame;
    }

    /**
     * Overrides the <tt>BundleTracker</tt> functionality to inform
     * the application object about the added extensions.
     * @param bundle The activated bundle.
    **/
    public void addedBundle(Bundle bundle)
    {
        processBundleOnEventThread(ADDED, bundle);
    }

    /**
     * Overrides the <tt>BundleTracker</tt> functionality to inform
     * the application object about removed extensions.
     * @param bundle The inactivated bundle.
    **/
    public void removedBundle(Bundle bundle)
    {
        processBundleOnEventThread(REMOVED, bundle);
    }

    /**
     * Processes a received bundle notification from the <tt>BundleTracker</tt>,
     * forcing the processing of the notification onto the Swing event thread
     * if it is not already on it.
     * @param action The type of action associated with the notification.
     * @param bundle The bundle of the corresponding extension.
    **/
    private void processBundleOnEventThread(int action, Bundle bundle)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                processBundle(action, bundle);
            }
            else
            {
                SwingUtilities.invokeAndWait(new BundleRunnable(action, bundle));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Actually performs the processing of the bundle notification. Invokes
     * the appropriate callback method on the application object depending on
     * the action type of the notification.
     * @param action The type of action associated with the notification.
     * @param bundle The bundle of the corresponding extension.
    **/
    private void processBundle(int action, Bundle bundle)
    {
        Dictionary dict = bundle.getHeaders();

        // Try to get the name of the extension.
        String name = (String) dict.get(SimpleShape.NAME_PROPERTY);
        // Return immediately if the bundle is not an extension.
        if (name == null)
        {
            return;
        }

        switch (action)
        {
            case ADDED:
                // Get the icon resource of the extension.
                String iconPath = (String) dict.get(SimpleShape.ICON_PROPERTY);
                Icon icon = new ImageIcon(bundle.getResource(iconPath));
                // Get the class of the extension.
                String classPath = (String) dict.get(SimpleShape.CLASS_PROPERTY);
                try
                {
                    Class clazz = bundle.loadClass(classPath);
                    m_frame.addShape(name, icon, (SimpleShape) clazz.newInstance());
                }
                catch (Exception ex)
                {
                    // This should never happen in this example.
                }
                break;

            case REMOVED:
                m_frame.removeShape(name);
                break;
        }
    }

    /**
     * Simple class used to process bundle notification handling on the
     * Swing event thread.
    **/
    private class BundleRunnable implements Runnable
    {
        private int m_action;
        private Bundle m_bundle;

        /**
         * Constructs an object with the specified action and bundle
         * object for processing on the Swing event thread.
         * @param action The type of action associated with the notification.
         * @param bundle The bundle of the corresponding extension.
        **/
        public BundleRunnable(int action, Bundle bundle)
        {
            m_action = action;
            m_bundle = bundle;
        }

        /**
         * Calls the <tt>processBundle()</tt> method.
        **/
        public void run()
        {
            processBundle(m_action, m_bundle);
        }
    }
}