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

package org.apache.felix.upnp.basedriver.export;


import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.AllowedValueList;
import org.cybergarage.upnp.AllowedValueRange;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.RootDescription;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.xml.DeviceData;
import org.cybergarage.xml.Node;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.util.Converter;
/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class BuildDevice {
	
	private static Node buildRootNode(){
		Node root = new Node(RootDescription.ROOT_ELEMENT);
		root.setAttribute("xmlns",RootDescription.ROOT_ELEMENT_NAMESPACE);
		Node spec = new Node(RootDescription.SPECVERSION_ELEMENT);
		Node maj =new Node(RootDescription.MAJOR_ELEMENT);
		maj.setValue("1");
		Node min =new Node(RootDescription.MINOR_ELEMENT);
		min.setValue("0");
		spec.addNode(maj);
		spec.addNode(min);
		root.addNode(spec);
		return root;
	}
	
	private static Device buildRootDeviceNode(Node root,ServiceReference sr){		
		Node dev = new Node(Device.ELEM_NAME);
		root.addNode(dev);
		DeviceData dd = new DeviceData();
		dd.setDescriptionURI("/gen-desc.xml");
		dev.setUserData(dd);
		Device devUPnP = new Device(root,dev);


		Object aux = sr.getProperty(UPnPDevice.TYPE);
		if(aux==null){
			devUPnP.setDeviceType(null);		
		}else if(aux instanceof String){
			devUPnP.setDeviceType((String) aux);
		}else if(aux instanceof String[]){
			//The property key UPnP.device.type should be a String
			String[] v = (String[]) aux;
			int maxindex=0;
			int max=Integer.parseInt(v[0].substring(v[0].lastIndexOf(":")+1));
			int tmp;
			for (int i = 1; i < v.length; i++) {
				tmp=Integer.parseInt(v[i].substring(v[i].lastIndexOf(":")+1));				
				if(max<tmp){
					max=tmp;
					maxindex=i;
				}				
			}
			devUPnP.setDeviceType(v[maxindex]);
		}				
		
		devUPnP.setFriendlyName((String) sr.getProperty(UPnPDevice.FRIENDLY_NAME));
		devUPnP.setManufacture((String) sr.getProperty(UPnPDevice.MANUFACTURER));
		devUPnP.setManufactureURL((String) sr.getProperty(UPnPDevice.MANUFACTURER_URL));
		devUPnP.setModelDescription((String) sr.getProperty(UPnPDevice.MODEL_DESCRIPTION));
		devUPnP.setModelName((String) sr.getProperty(UPnPDevice.MODEL_NAME));
		devUPnP.setModelNumber((String) sr.getProperty(UPnPDevice.MODEL_NUMBER));
		devUPnP.setModelURL((String) sr.getProperty(UPnPDevice.MODEL_URL));
		devUPnP.setSerialNumber((String) sr.getProperty(UPnPDevice.SERIAL_NUMBER));
		devUPnP.setUDN((String) sr.getProperty(UPnPDevice.UDN));
		devUPnP.setUPC((String) sr.getProperty(UPnPDevice.UPC));

		devUPnP.setLocation("/gen-desc.xml");		

		addServices("",devUPnP,sr);
		addDevices("",devUPnP,sr);
		devUPnP.setPresentationURL((String) sr.getProperty(UPnPDevice.PRESENTATION_URL));
		
		return devUPnP;
	}
	
	private static void addDevices(String id,Device devUPnP, ServiceReference sr) {

		String[] udns=(String[]) sr.getProperty(UPnPDevice.CHILDREN_UDN);
		if(udns==null) {
			return;
		}
		for (int i = 0; i < udns.length; i++) {
			try {
				ServiceReference[] aux = Activator.bc.getServiceReferences(
						UPnPDevice.class.getName(),"("+UPnPDevice.UDN+"="+udns[i]+")"
					);
				if(aux==null || aux.length == 0)
					continue;
				//id=+"/device/"+i;						// twa: wrong in recursion
				//buildDevice(id,devUPnP,aux[0]);		// twa: wrong in recursion
				String localId = new StringBuffer(id).append("/device/").append(i).toString();		
				buildDevice(localId,devUPnP,aux[0]); 	// twa: better
			} catch (InvalidSyntaxException ignored) {}						
		}		
	}

	private static void buildDevice(String id,Device parent, ServiceReference sr) {
		Node dev = new Node(Device.ELEM_NAME);
		DeviceData dd = new DeviceData();
		dd.setDescriptionURI(id+"/gen-desc.xml");
		dev.setUserData(dd);
		
		Device devUPnP = new Device(dev);
		
		devUPnP.setFriendlyName((String) sr.getProperty(UPnPDevice.FRIENDLY_NAME));
		devUPnP.setManufacture((String) sr.getProperty(UPnPDevice.MANUFACTURER));
		devUPnP.setManufactureURL((String) sr.getProperty(UPnPDevice.MANUFACTURER_URL));
		devUPnP.setModelDescription((String) sr.getProperty(UPnPDevice.MODEL_DESCRIPTION));
		devUPnP.setModelName((String) sr.getProperty(UPnPDevice.MODEL_NAME));
		devUPnP.setModelNumber((String) sr.getProperty(UPnPDevice.MODEL_NUMBER));
		devUPnP.setModelURL((String) sr.getProperty(UPnPDevice.MODEL_URL));
		devUPnP.setSerialNumber((String) sr.getProperty(UPnPDevice.SERIAL_NUMBER));
		devUPnP.setUDN((String) sr.getProperty(UPnPDevice.UDN));
		devUPnP.setUPC((String) sr.getProperty(UPnPDevice.UPC));
		devUPnP.setLocation(id+"/gen-desc.xml");		

		addServices(id,devUPnP,sr);
		addDevices(id,devUPnP,sr);

		parent.addDevice(devUPnP); //		twa: essential!!!!!!!
		devUPnP.setPresentationURL((String) sr.getProperty(UPnPDevice.PRESENTATION_URL));
		
	}
	
	/**
	* Method used to create a new Service in CyberLink world without creating the XML
	*
	* @param id ServiceId
	* @param devUPnP the CyberLink device that where the new Service will be created
	* @param sr ServiceReference to OSGi Device that used as source of the information
	*              for the creation of the device
	*/
	private static void addServices(String id,Device devUPnP, ServiceReference sr) {
		UPnPDevice devOSGi = (UPnPDevice) Activator.bc.getService(sr);

		if( devOSGi == null) {	//added by twa to prevent a null pointer exception
			Activator.logger.WARNING("UPnP Device that cotains serviceId="
					+id+" is deregistered from the framework while is exported");
			return;
		}

		UPnPService[] services =  devOSGi.getServices();
		if(services==null || services.length==0)
			return;
		
		
		
		for (int i = 0; i < services.length; i++) {
			Service ser = new Service();
			devUPnP.addService(ser);
			ser.setServiceType(services[i].getType() );
			ser.setServiceID(services[i].getId());
			ser.setSCPDURL(id+"/service/"+i+"/gen-desc.xml");
			ser.setDescriptionURL(id+"/service/"+i+"/gen-desc.xml");
			ser.setControlURL(id+"/service/"+i+"/ctrl");
			ser.setEventSubURL(id+"/service/"+i+"/event");

			UPnPAction[] actions = services[i].getActions();
			for (int j = 0; j < actions.length; j++) {
                boolean valid=true;
				Action act = new Action(ser.getServiceNode());
				act.setName(actions[j].getName());
				ArgumentList al = new ArgumentList();
				
				String[] names=actions[j].getInputArgumentNames();				
				if(names!=null){
					for (int k = 0; k < names.length; k++) {
                        UPnPStateVariable variable = actions[j].getStateVariable(names[k]);
                        if(variable==null){
                            /*
                             * //TODO Create a stict and relaxed behavior of the base driver which 
                             * export as much it can or export only 100% complaint UPnPDevice service 
                             */
                            Activator.logger.WARNING(
                                "UPnP Device that cotains serviceId="+id+" contains the action "
                                +actions[j].getName()+" with the Input argument "+names[k]
                                +" not related to any UPnPStateVariable. Thus this action won't be exported");
                            valid=false;
                            break;
                        }
                        Argument a = new Argument();
						a.setDirection(Argument.IN);
						a.setName(names[k]);
						a.setRelatedStateVariableName(variable.getName());						
						al.add(a);						
					}
				}
				names=actions[j].getOutputArgumentNames();
				if(names!=null && valid){
					for (int k = 0; k < names.length; k++) {
                        UPnPStateVariable variable = actions[j].getStateVariable(names[k]);
                        if(variable==null){
                            /*
                             * //TODO Create a stict and relaxed behavior of the base driver which 
                             * export as much it can or export only 100% complaint UPnPDevice service 
                             */
                            Activator.logger.WARNING(
                                "UPnP Device that cotains serviceId="+id+" contains the action "
                                +actions[j].getName()+" with the Output argument "+names[k]
                                +" not related to any UPnPStateVariable. Thus this action won't be exported");                            
                        }
						Argument a = new Argument();
						a.setDirection(Argument.OUT);
						a.setName(names[k]);
						a.setRelatedStateVariableName(variable.getName());						
						al.add(a);						
					}
				}
                if(valid) {
    				act.setArgumentList(al);
    				ser.addAction(act);
                }
			}			
			
			UPnPStateVariable[] vars = services[i].getStateVariables();
			for (int j = 0; j < vars.length; j++) {
				StateVariable var = new StateVariable();
				var.setDataType(vars[j].getUPnPDataType());
				var.setName(vars[j].getName());
				var.setSendEvents(vars[j].sendsEvents());
				String[] values = vars[j].getAllowedValues();
				if(values!=null){
					AllowedValueList avl = new AllowedValueList(values);
					var.setAllowedValueList(avl);
				}else if(vars[j].getMaximum()!= null){
					AllowedValueRange avr = new AllowedValueRange(
							vars[j].getMaximum(),
							vars[j].getMinimum(),
							vars[j].getStep()
						);
					var.setAllowedValueRange(avr);
				}
				if(vars[j].getDefaultValue()!=null)
					try {
						var.setDefaultValue(Converter.toString(
								vars[j].getDefaultValue(),vars[j].getUPnPDataType()
							));
					} catch (Exception ignored) {
					}
				ser.addStateVariable(var);				
			}
						
			Activator.bc.ungetService(sr);
		}
		
		
	}

	public static Device createCyberLinkDevice(ServiceReference sr){
		Node root = buildRootNode();
		Device devUPnP = buildRootDeviceNode(root,sr);
		return devUPnP;
	}
}
