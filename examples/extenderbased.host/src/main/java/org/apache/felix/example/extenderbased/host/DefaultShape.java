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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.ImageIcon;
import org.apache.felix.example.extenderbased.host.extension.SimpleShape;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * This class is used as a proxy to defer object creation from shape
 * provider bundles and also as a placeholder shape when previously
 * used shapes are no longer available. These two purposes are
 * actually orthogonal, but were combined into a single class to
 * reduce the number of classes in the application. The proxy-related
 * functionality is introduced as a way to lazily create shape
 * objects in an effort to improve performance; this level of
 * indirection could be removed if eager creation of objects is not
 * a concern.
**/
class DefaultShape implements SimpleShape
{
    private SimpleShape m_shape;
    private ImageIcon m_icon;
    private BundleContext m_context;
    private long m_bundleId;
    private String m_className;

    /**
     * This constructs a placeholder shape that draws a default
     * icon. It is used when a previously drawn shape is no longer
     * available.
    **/
    public DefaultShape()
    {
        // Do nothing.
    }

    /**
     * This constructs a proxy shape that lazily instantiates the
     * shape class from the associated extension bundle.
     * @param context The bundle context to use for retrieving the extension bundle.
     * @param bundleId The bundle ID of the extension bundle.
     * @param className The class name of the shape.
    **/
    public DefaultShape(BundleContext context, long bundleId, String className)
    {
        m_context = context;
        m_bundleId = bundleId;
        m_className = className;
    }

    /**
     * Implements the <tt>SimpleShape</tt> interface method. When acting as
     * a proxy, this method lazily loads and instantiates the shape class and
     * then uses it to draw the shape. When acting as a placeholder shape,
     * this method draws the default icon.
     * @param g2 The graphics object used for painting.
     * @param p The position to paint the triangle.
    **/
    public void draw(Graphics2D g2, Point p)
    {
        // If this is a proxy shape, instantiate the shape class
        // and use it to draw the shape.
        if (m_context != null)
        {
            try
            {
                if (m_shape == null)
                {
                    // Get the bundle.
                    Bundle bundle = m_context.getBundle(m_bundleId);
                    // Load the class and instantiate it.
                    Class clazz = bundle.loadClass(m_className);
                    m_shape = (SimpleShape) clazz.newInstance();
                }
                // Draw the shape.
                m_shape.draw(g2, p);
                // If everything was successful, then simply return.
                return;
            }
            catch (Exception ex)
            {
                // This generally should not happen, but if it does then
                // we can just fall through and paint the default icon.
            }
        }

        // If the proxied shape could not be drawn for any reason or if
        // this shape is simply a placeholder, then draw the default icon.
        if (m_icon == null)
        {
            try
            {
                m_icon = new ImageIcon(this.getClass().getResource("underc.png"));
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                g2.setColor(Color.red);
                g2.fillRect(0, 0, 60, 60);
                return;
            }
        }
        g2.drawImage(m_icon.getImage(), 0, 0, null);
    }
}