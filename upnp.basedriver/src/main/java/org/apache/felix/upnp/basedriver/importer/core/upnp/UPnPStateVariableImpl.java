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

package org.apache.felix.upnp.basedriver.importer.core.upnp;


import java.util.Date;
import java.util.Hashtable;

import org.cybergarage.upnp.AllowedValueList;
import org.cybergarage.upnp.AllowedValueRange;
import org.cybergarage.upnp.StateVariable;

import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.extra.util.Converter;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/
public class UPnPStateVariableImpl implements UPnPStateVariable {

	private StateVariable variable;
	private static Hashtable upnp2javaTable = null;
	
	static{
		upnp2javaTable = new Hashtable(30);
		String[] upnpType = null;
		upnpType = new String[]{"ui1","ui2","i1","i2","i4","int"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Integer.class);
		}

		upnpType = new String[]{"ui4","time","time.tz"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Long.class);
		}		

		upnpType = new String[]{"r4","float"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Float.class);
		}		

		upnpType = new String[]{"r8","number","fixed.14.4"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Double.class);
		}		

		upnpType = new String[]{"char"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Character.class);
		}		

		upnpType = new String[]{"string","uri","uuid"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],String.class);
		}		

		upnpType = new String[]{"date","dateTime","dateTime.tz"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Date.class);
		}		

		upnpType = new String[]{"boolean"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],Boolean.class);
		}		

		upnpType = new String[]{"bin.base64","bin.hex"};
		for (int i = 0; i < upnpType.length; i++) {
			upnp2javaTable.put(upnpType[i],byte[].class);
		}		
		
		
	}
	
	/**
	 * @param variable
	 */
	public UPnPStateVariableImpl(StateVariable variable) {

		this.variable = variable;
	} /*
	   * (non-Javadoc)
	   * 
	   * @see org.osgi.service.upnp.UPnPStateVariable#getName()
	   */

	public String getName() {
		return variable.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getJavaDataType()
	 */
	public Class getJavaDataType() {
		return (Class) upnp2javaTable.get(variable.getDataType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getUPnPDataType()
	 */
	public String getUPnPDataType() {
		return variable.getDataType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getDefaultValue()
	 */
	public Object getDefaultValue() {
		//TODO must be implemented from scretch, it's just raccommend
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getAllowedValues()
	 */
	public String[] getAllowedValues() {
		if (variable.getDataType().equals("string")) {
			AllowedValueList allowedvalue = variable.getAllowedValueList();
            if (allowedvalue == null) return null;
			if(allowedvalue.size()==0){
				return null;
			} 
			String[] values = new String[allowedvalue.size()];
			for (int i = 0; i < allowedvalue.size(); i++) {
				values[i] = allowedvalue.getAllowedValue(i).getValue();
			}
			return values;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMinimum()
	 */
	public Number getMinimum() {
		//TODO the same thing for getMaximum
		AllowedValueRange allowedValueRange = variable.getAllowedValueRange();
		if(allowedValueRange==null){
			return null;
		}
		String min=allowedValueRange.getMinimum();
        //francesco 22/10/2005
        if (min.equals("")) return null;
		try {
			return (Number)Converter.parseString(min,getUPnPDataType());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMaximum()
	 */
	public Number getMaximum() {
		//TODO I think that this method will be invoked from people that know what is doing
		AllowedValueRange allowedValueRange = variable.getAllowedValueRange();
		if(allowedValueRange==null){
			return null;
		}
		String max = allowedValueRange.getMaximum();
        //francesco 22/10/2005
        if (max.equals("")) return null;
		try {
			return (Number)Converter.parseString(max,getUPnPDataType());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#getStep()
	 */
	public Number getStep() {
		//TODO same things of getMaxium
		AllowedValueRange allowedValueRange = variable.getAllowedValueRange();
		if(allowedValueRange==null){
			return null;
		}
		String step = allowedValueRange.getStep();
        //francesco 22/10/2005
        if (step.equals("")) return null;
		try {
			return (Number)Converter.parseString(step,getUPnPDataType());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.upnp.UPnPStateVariable#sendsEvents()
	 */
	public boolean sendsEvents() {
		return variable.isSendEvents();
	}


}
