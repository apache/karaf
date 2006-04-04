/*
 DomoWare UPnP Base Driver is an implementation of the OSGi UnP Device Spcifaction
 Copyright (C) 2004  Matteo Demuru, Francesco Furfari, Stefano "Kismet" Lenzi

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 You can contact us at:
 {matte-d, sygent, kismet-sl} [at] users.sourceforge.net
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
