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
package org.apache.felix.example.extenderbased.host.extension;

import java.awt.Graphics2D;
import java.awt.Point;

/**
 * This interface defines the <tt>SimpleShape</tt> extension. This extension
 * is used to draw shapes. It is defined by three manifest properties:
 * <ul>
 *   <li>Extension-Name - A <tt>String</tt> name for the shape.
 *   </li>
 *   <li>Extension-Icon - An <tt>Icon</tt> resource for the shape.
 *   </li>
 *   <li>Extension-Class - A <tt>Class</tt> that implements the shape.
 *   </li>
 * </ul>
**/
public interface SimpleShape
{
    /**
     * A property for the name of the shape.
    **/
    public static final String NAME_PROPERTY = "Extension-Name";
    /**
     * A property for the icon of the shape.
    **/
    public static final String ICON_PROPERTY = "Extension-Icon";
    /**
     * A property for the class of the shape.
    **/
    public static final String CLASS_PROPERTY = "Extension-Class";

    /**
     * Method to draw the shape of the extension.
     * @param g2 The graphics object used for painting.
     * @param p The position to paint the triangle.
    **/
    public void draw(Graphics2D g2, Point p);
}