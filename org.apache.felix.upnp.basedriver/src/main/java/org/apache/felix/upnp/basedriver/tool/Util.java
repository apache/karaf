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

package org.apache.felix.upnp.basedriver.tool;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/
public class Util {
	
	public static boolean isUPnPDevice(ServiceReference sr){
		String[] aux = (String[]) sr.getProperty(Constants.OBJECTCLASS);
		String val=UPnPDevice.class.getName();
		int i;
		for (i = 0; i < aux.length; i++) {
			if(aux[i].equals(val)) break;
		}
		if(i==aux.length) return false;
		aux = (String[]) sr.getProperty(org.osgi.service.device.Constants.DEVICE_CATEGORY);
		val=UPnPDevice.DEVICE_CATEGORY;
		for (i = 0; i < aux.length; i++) {
			if(aux[i].equals(val)) 
					return true;
		}		
		return false;
	}
	
	public static final boolean isRootDevice(ServiceReference sr){
		return (sr.getProperty(UPnPDevice.PARENT_UDN)==null)
				&& isUPnPDevice(sr);
	}
	
	public static final String getPropertyDefault(BundleContext bc, String propertyName, String defaultValue ){
		String value = bc.getProperty(propertyName);
		if(value == null)
			return defaultValue;
		return value;
	}

	public static String sanitizeFilename(String name){
		return name.replaceAll("[:;\\,/*?<>\"|]","_");
	}

	public static boolean makeParentPath(String filePath){
		int l=filePath.lastIndexOf(File.separator);
		filePath=filePath.substring(0,l);
		File p=new File(filePath);
		if(p.exists())
			return true;
		return p.mkdirs();
	}

	/**
	 * @param d
	 */
	public static boolean deleteRecursive(File d) {
		if(!d.delete()){
			if(d.isDirectory()){
				File[] subs = d.listFiles();
				for (int i = 0; i < subs.length; i++) {
					if(!deleteRecursive(subs[i]))
						return false;
				}
				return d.delete();
			}else{
				return false;
			}			
		}
		return true;
	}	
	
}
