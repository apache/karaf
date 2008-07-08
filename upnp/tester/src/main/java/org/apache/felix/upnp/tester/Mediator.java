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

package org.apache.felix.upnp.tester;


import javax.swing.JFrame;
import javax.swing.JTree;

import org.apache.felix.upnp.tester.discovery.DriverProxy;
import org.apache.felix.upnp.tester.discovery.RootDeviceListener;
import org.apache.felix.upnp.tester.gui.PropertiesViewer;
import org.apache.felix.upnp.tester.gui.TreeViewer;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public  class Mediator {
	static PropertiesViewer props;
	static JTree tree;
    static JFrame frame; 
    static DriverProxy driverProxy;
    static RootDeviceListener rootDeviceListener; 
    static ControlPoint controlPoint; 
    static TreeViewer treeViewer; 
	

	public static void setPropertiesViewer(PropertiesViewer props){
		Mediator.props=props;
	}
	public static PropertiesViewer getPropertiesViewer(){
		return props;
	}
	
    public static void setUPnPDeviceTree(JTree tree){
        Mediator.tree=tree;
    }
    public static JTree getUPnPDeviceTree(){
        return tree;
    }
    
    public static void setTreeViewer(TreeViewer treeViewer){
        Mediator.treeViewer=treeViewer;
    }
    public static TreeViewer getTreeViewer(){
        return treeViewer;
    }
	
    public static void setMainFrame(JFrame frame){
        Mediator.frame=frame;
    }
    public static JFrame getMainFrame(){
        return frame;
    }
    public static void setControlPoint(ControlPoint controlPoint){
        Mediator.controlPoint=controlPoint;
    }
    public static ControlPoint getControlPoint(){
        return controlPoint;
    }
    
    public static void setDriverProxy(DriverProxy driverProxy) {
        Mediator.driverProxy = driverProxy;        
    }
    
    public static DriverProxy getDriverProxy(){
        return driverProxy;
    }
    
    public static void setRootDeviceListener(RootDeviceListener listener) {
        Mediator.rootDeviceListener = listener;        
    }
    
    public static RootDeviceListener getRootDeviceListener(){
        return rootDeviceListener;
    }
}
