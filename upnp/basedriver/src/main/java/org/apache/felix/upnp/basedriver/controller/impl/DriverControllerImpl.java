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

package org.apache.felix.upnp.basedriver.controller.impl;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.importer.core.MyCtrlPoint;
import org.apache.felix.upnp.basedriver.tool.Logger;
import org.apache.felix.upnp.basedriver.controller.DevicesInfo;
import org.apache.felix.upnp.basedriver.controller.DriverController;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class DriverControllerImpl implements DriverController, DevicesInfo{
    private MyCtrlPoint myCtrl;
    private Logger logger = Activator.logger;
    
    public DriverControllerImpl(MyCtrlPoint myCtrl){
        this.myCtrl = myCtrl;
        
    }
    
    public void setLogLevel(int n) {
        logger.setLogLevel(n);
    }

    public int getLogLevel() {
        return logger.getLogLevel();
    }

    public void setCyberDebug(boolean b) {
        logger.setCyberDebug(b);
    }

    public boolean getCyberDebug() {
        return logger.getCyberDebug();
    }

    public String getLocationURL(String udn) {
    	if (myCtrl == null){
    		logger.WARNING("UPnP Importer is disabled. getLocationURL is not available");
    		return null;
    	}
        if (udn == null || udn.equals(""))  throw new IllegalArgumentException("Invalid udn paramenter");
        Device device = myCtrl.getDevice(udn);
        if (device == null) {
        	logger.WARNING("getLocationURL():: No device data available for UDN:"+udn);
        	return null;
        }
        return device.getLocation();
    }
    
    public String getSCPDURL(String udn, String serviceId) {
    	if (myCtrl == null){
    		logger.WARNING("UPnP Importer is disabled. getSCPDURL() is not available");
    		return null;
    	}
        if (udn == null || udn.equals("") )  throw new IllegalArgumentException("Invalid udn paramenter");
        if (serviceId == null || serviceId.equals("") )  throw new IllegalArgumentException("Invalid serviceId paramenter");
        Device device= myCtrl.getDevice(udn);
        if (device == null) {
            logger.WARNING("getSCPDURL():: No device data available for UDN: "+udn);
            return null;
        }
        Service service = device.getService(serviceId);
        if (service == null) {
            logger.WARNING("getSCPDURL():: No service data available for serviceId:"+serviceId + " of UDN " + udn);
            return null;
        }
        String scpd = service.getSCPDURL().trim();
        return resolveRelativeLink(device,scpd);
    }
    
    public String resolveRelativeUrl(String udn, String link) {
       	if (myCtrl == null){
    		logger.WARNING("UPnP Importer is disabled. resolveRelativeUrl() is not available");
    		return null;
    	}
       if (udn == null || udn.equals(""))  throw new IllegalArgumentException("Invalid udn paramenter");
        Device device = myCtrl.getDevice(udn);
        if (device == null) {
            logger.WARNING("resolveRelativeUrl():: No device data available for UDN: "+udn);
            return null;
        }
        return resolveRelativeLink(device,link);        
    }

    private String resolveRelativeLink(Device device, String link) {
        if ( device == null || link == null) return null;
        if (link.startsWith("http:"))
            return link;
        else {
            String hostname = "";
            String location = "";
            String urlBase = device.getURLBase().trim();
            //TODO Check if is more important URLBase or location
            if (urlBase.equals("")){
                location = device.getLocation().trim();
                int endHostnameIdx = location.indexOf("/",7);
                if (endHostnameIdx!=-1)
                    hostname=location.substring(0,endHostnameIdx);
                else
                    hostname = location;
                if (link.startsWith("/")){
                    return hostname+link;
                }else{
                	//TODO Check for link start with .. or /
                    return location +link;
                }
            }
            else {
               return urlBase+link;
            }             
        }
   }

    public void search(String target) {
       	if (myCtrl == null){
    		logger.WARNING("UPnP Importer is disabled. resolveRelativeUrl() is not available");
    		return ;
    	}
       myCtrl.search(target);       
    }



}
