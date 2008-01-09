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

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.util.Converter;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPStateVariableImpl implements UPnPStateVariable {

	private StateVariable variable;
    
    private Number max = null;
    private Number min = null;
    private Number step = null;

    private String[] values = null;    
    
    private Boolean hasMaxMinStep = null;
    private Boolean hasRangeValues = null;    

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
	} 

    /**
     * @see org.osgi.service.upnp.UPnPStateVariable#getName()
	 */
	public String getName() {
		return variable.getName();
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getJavaDataType()
	 */
	public Class getJavaDataType() {
		return (Class) upnp2javaTable.get(variable.getDataType());
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getUPnPDataType()
	 */
	public String getUPnPDataType() {
		return variable.getDataType();
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getDefaultValue()
	 */
	public Object getDefaultValue() {
		//TODO must be implemented from scretch, it's just raccommend
		return null;
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getAllowedValues()
	 */
	public String[] getAllowedValues() {
        if(hasRangeValues == null)
            initValueConstraint();
        
        return values;
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMinimum()
	 */
	public Number getMinimum() {
        if(hasMaxMinStep == null)
            initValueConstraint();
        
        return min;
	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMaximum()
	 */
	public Number getMaximum() {
        if(hasMaxMinStep == null)
            initValueConstraint();
        
        return max;
	}

    /**
     * <b>NOTE:</b>  This type of control caches the value recieved by the Device so if XML changes it doesn't affect the OSGi service
     * 
     * @since 0.3
     */
    private void initValueConstraint(){
        if(hasRangeValues != null || hasMaxMinStep != null)
            return;

        hasRangeValues = Boolean.FALSE;
        hasMaxMinStep = Boolean.FALSE;
        
        final AllowedValueRange allowedValueRange = variable.getAllowedValueRange();
        final AllowedValueList allowedValueList = variable.getAllowedValueList();
        
        if(allowedValueRange != null && allowedValueList != null){
            Activator.logger.WARNING("Imported device with StateVariable "
                                     +variable.getName()+" contains either AllowedValueRange and AllowedValueList UPnP doesn't allow it because it. Neither of the restriction will be applied");
            
        }else if( allowedValueRange != null ){
            
            initMaxMinStep(allowedValueRange);
            
        }else if( allowedValueList != null ){
            
            initAllowedValues(allowedValueList);
            
        }
    }
    
    /**
     * @param allowedValueList
     * @since 0.3
     */
    private void initAllowedValues(AllowedValueList allowedValueList){
        //PRE:invoked only by initValueConstraint() thus allowedValueList must not null
        if (String.class != getJavaDataType()) {
            Activator.logger.WARNING("Imported device with StateVariable "
                                     +variable.getName()+" contains AllowedValueList but its UPnP type doesn't allow it because it is +"+getUPnPDataType());            
            return;
        }

        if(allowedValueList.size() == 0){
            return ;
        } 

        values = new String[allowedValueList.size()];
        for (int i = 0; i < allowedValueList.size(); i++) {
            values[i] = allowedValueList.getAllowedValue(i).getValue();
        }
    }

    /**
     * @param allowedValueRange
     * @since 0.3
     */
    private void initMaxMinStep(AllowedValueRange allowedValueRange){
        //PRE:invoked only by initValueConstraint() thus allowedValueRange must not  be null
        if(allowedValueRange==null){
            return;
        }

        if(!Number.class.isAssignableFrom(getJavaDataType())){
            Activator.logger.WARNING("Imported device with StateVariable "
                                     +variable.getName()+" contains AllowedValueRange but its UPnP type doesn't allow it because it is +"+getUPnPDataType());            
            return;
        }
        
        final String maxStr = allowedValueRange.getMaximum();
        final String minStr = allowedValueRange.getMinimum();
        final String stepStr = allowedValueRange.getStep();
        
        try{
            final String type = getUPnPDataType();
            max = (Number)Converter.parseString(maxStr,type);
            min = (Number)Converter.parseString(minStr,type);
            step = (Number)Converter.parseString(stepStr,type);
        }catch(Exception ex){
            Activator.logger.WARNING("Imported device with StateVariable "
                +variable.getName()+" contains an invalid definition for AllowedValueRange");
        }
        hasMaxMinStep = Boolean.TRUE;
    }
    
	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#getStep()
	 */
	public Number getStep() {
        if(hasMaxMinStep == null)
            initValueConstraint();
        
        return step;        

	}

	/**
	 * @see org.osgi.service.upnp.UPnPStateVariable#sendsEvents()
	 */
	public boolean sendsEvents() {
		return variable.isSendEvents();
	}


}
