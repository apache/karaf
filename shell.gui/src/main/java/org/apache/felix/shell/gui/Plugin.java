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
package org.apache.felix.shell.gui;

import java.awt.Component;

/**
 * A simple plugin interface for the GUI shell bundle.
**/
public interface Plugin
{
    /**
     * Returns the name of the plugin.
     * @return the name of the plugin.
    **/
    public String getName();
    
    /**
     * Returns the GUI associated with the plugin; this method should
     * always return the same GUI instance when it is invoked.
     * @return the GUI associated with the plugin.
    **/
    public Component getGUI();
}