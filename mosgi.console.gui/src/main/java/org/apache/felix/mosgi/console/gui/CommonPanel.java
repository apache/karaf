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
package org.apache.felix.mosgi.console.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTabbedPane;

import org.apache.felix.mosgi.console.ifc.CommonPlugin;

public class CommonPanel extends JTabbedPane implements PropertyChangeListener {
  Activator beanEventBroadcaster;

  public CommonPanel(Activator activator){
    this.beanEventBroadcaster=activator;
    this.beanEventBroadcaster.addPropertyChangeListener(this);
  }

  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(CommonPlugin.COMMON_PLUGIN_ADDED)) {
      CommonPlugin cp=(CommonPlugin)event.getNewValue();
      this.add(cp.getName(), cp.getGUI());
      this.beanEventBroadcaster.addPropertyChangeListener(cp);
    }
    if (event.getPropertyName().equals(CommonPlugin.COMMON_PLUGIN_REMOVED)){
      CommonPlugin cp=(CommonPlugin)event.getNewValue();
      this.remove(cp.getGUI());
      this.beanEventBroadcaster.removePropertyChangeListener(cp);
    }
  }
}
