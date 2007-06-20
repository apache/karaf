/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.console.ifc;

import java.awt.Component;
import java.beans.PropertyChangeListener;

public interface Plugin extends PropertyChangeListener
{
    public String getName();
    public Component getGUI();

    public void registerServicePlugin();
    public void unregisterServicePlugin();
    public String pluginLocation();

    public static final String NEW_NODE_SELECTED="newNodeSelected";
    public static final String NEW_NODE_READY="newNodeReady";
    public static final String NEW_NODE_CONNECTION="newNodeConnection";
    public static final String EMPTY_NODE="emptyNode";

    public static final String PLUGIN_ADDED="pluggin_added";
    public static final String PLUGIN_REMOVED="pluggin_removed";
    public static final String PLUGIN_ACTIVATED="pluggin_activated";

}
