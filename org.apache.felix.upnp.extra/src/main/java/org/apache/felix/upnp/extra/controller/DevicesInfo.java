/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.upnp.extra.controller;

/**
 * @author Stefano "Kismet" Lenzi 
 * @author Francesco Furfari
 */
public interface DevicesInfo{
    /**
     * 
     * Allow you to get the URL that poinr to the XML description of
     * a device specified by UUID. 
     * 
     * @param udn the UUID that identify a device
     * @return The String that rappresent the URL that point to the description of the Device
     */
    public String getLocationURL(String udn);
    
    /**
     * 
     * Allow you to get the URL that poinr to the XML description of
     * a service specified by ServiceId and UUID of the device that 
     * contain the service 
     * 
     * @param udn the UUID of the device that contain the service
     * @param serviceId the ServiceId of the service
     * @return The String that rappresent the URL that point to the description of the Service
     */
    public String getSCPDURL(String udn,String serviceId);  
    
    /**
     * Allow you to get the absolue URL of a link that is conatin in a device
     * 
     * @param udn the UUID of the UPnP Device 
     * @param link the relative link that you want to resolve
     * @return The String that rappresent the absolute URL to the resourse specified by link
     */
    public String resolveRelativeUrl(String udn, String link);
    
}
