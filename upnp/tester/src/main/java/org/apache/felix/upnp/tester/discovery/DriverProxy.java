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

package org.apache.felix.upnp.tester.discovery;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import org.apache.felix.upnp.basedriver.controller.DevicesInfo;
import org.apache.felix.upnp.basedriver.controller.DriverController;
import org.apache.felix.upnp.tester.Activator;
import org.apache.felix.upnp.tester.Mediator;
/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class DriverProxy implements ServiceListener {
    private DevicesInfo devicesInfo;
    private DriverController drvController;
    public DriverProxy(){
        ServiceReference sr = Activator.context.getServiceReference(DevicesInfo.class.getName());
        if (sr != null){
            devicesInfo = (DevicesInfo)Activator.context.getService(sr);
            drvController = (DriverController) devicesInfo;
            Mediator.getControlPoint().enableMenus(true,getLogLevel(),getCyberDebug());
            Mediator.getTreeViewer().setPopupMenuEnabled(true);
        }
        String filter =  "(" + Constants.OBJECTCLASS + "=" + DevicesInfo.class.getName() + ")" ;
        try {
            Activator.context.addServiceListener(this,filter);  
        } catch (Exception ignored){};
     }
    
    public boolean isDriverAvailable(){
        return (drvController != null);
    }
    
    public String getDeviceDescriptionURI(String udn){
        if (devicesInfo != null)
            return devicesInfo.getLocationURL(udn);
        return "";
    }
    
    public String getServiceDescriptionURI(String udn,String serviceId){
        if (devicesInfo != null)
            return devicesInfo.getSCPDURL(udn,serviceId);
        return null;
    }
    
    public String resolveRelativeUrl(String udn,String link){
        if (devicesInfo != null)
            return devicesInfo.resolveRelativeUrl(udn,link);
        return null;
    }
    
    public boolean getCyberDebug(){
        if (drvController != null)
            return drvController.getCyberDebug();
        return false;
    }
    public void setCyberDebug(boolean b){
        if (drvController != null)
            drvController.setCyberDebug(b);
    }
    public int getLogLevel(){
        if (drvController != null)
            return drvController.getLogLevel();
        return 0;
    }
    
    public void setLogLevel(int value){
        if (drvController != null)
            drvController.setLogLevel(value);
    }
    public void doSearch(String target){
        if (drvController != null)
            drvController.search(target);
    }
    
    
    public void serviceChanged(ServiceEvent e) {
        switch(e.getType()){
            case ServiceEvent.REGISTERED:{
                Object service = Activator.context.getService(e.getServiceReference());
                if (service != null){
                devicesInfo = (DevicesInfo) service;
                drvController = (DriverController) devicesInfo;
                Mediator.getControlPoint().enableMenus(true,getLogLevel(),getCyberDebug());
                Mediator.getTreeViewer().setPopupMenuEnabled(true);
                }
            };break;
            case ServiceEvent.UNREGISTERING:{   
                devicesInfo = null;
                drvController =null;
                Mediator.getControlPoint().enableMenus(false,0,false);
                Mediator.getTreeViewer().setPopupMenuEnabled(false);
            };break;
        }       
    }
    
    public void close(){
        Activator.context.removeServiceListener(this);           
    }


}
