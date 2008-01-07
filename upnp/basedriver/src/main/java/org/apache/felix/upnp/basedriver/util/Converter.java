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

package org.apache.felix.upnp.basedriver.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.xerces.impl.dv.util.Base64;
import org.apache.xerces.impl.dv.util.HexBin;

import org.osgi.service.upnp.UPnPStateVariable;

/** 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class Converter {
	
	/**
	 * 
	 * @param value Object that contain the value
	 * @param upnpType String conating the UPnP Type of the Object
	 * @return a String that contain the UPnP rappresentation of the value contained in Object
	 * 		of type specified by typeUPnP
	 */
	public static String toString(Object value,String upnpType) throws Exception{
		if((value==null)||(upnpType==null))
			throw new NullPointerException("Must be specified a valid value and upnpType");
		
		if(value instanceof Number){
			if(value instanceof Integer){
				return value.toString();
			}else if(value instanceof Float){
				return value.toString();
			}else if(value instanceof Long){
				if(upnpType.equals(UPnPStateVariable.TYPE_TIME)){
					long l = ((Long)value).longValue();
					if(l<0) throw new IllegalArgumentException(l+ "Must be greater than 0");
					Calendar c = Calendar.getInstance();
					c.set(Calendar.HOUR_OF_DAY,(int) (l/3600000));					
					int x=(int) (l % 3600000);
					c.set(Calendar.MINUTE,(int) (x / 60000));
					c.set(Calendar.SECOND,(x % 60000)/1000);
					SimpleDateFormat sdt = new SimpleDateFormat("HH:mm:ss");
					return sdt.format(c.getTime());
				}else if(upnpType.equals(UPnPStateVariable.TYPE_TIME_TZ)){
					long l = ((Long)value).longValue();
					if(l<0) throw new IllegalArgumentException(l+ "Must be greater than 0");
					Calendar c = Calendar.getInstance();
					c.set(Calendar.HOUR_OF_DAY,(int) (l/3600000));					
					int x=(int) (l % 3600000);
					c.set(Calendar.MINUTE,(int) (x / 60000));
					c.set(Calendar.SECOND,(x % 60000)/1000);
					SimpleDateFormat sdt = new SimpleDateFormat("HH:mm:ssZ");
					return sdt.format(c.getTime());
				}else{
					//Must be UPnPStateVariable.TYPE_UI4)
					return value.toString();
				}
			}else if(value instanceof Double){
				if(upnpType.equals(UPnPStateVariable.TYPE_FIXED_14_4)){
					return Long.toString(((Double)value).longValue())+"."+
						Integer.toString((int) (((((Double)value).doubleValue()*10000D) % 10000)));  
				}else{
					//Must be UPnPStateVariable.TYPE_R8 or UPnPStateVariable.TYPE_NUMBER
					return value.toString();
				}
			}
		}else if(value instanceof Date){
			if(upnpType.equals("dateTime")){
				SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				return sdt.format(value);				
			}else if(upnpType.equals("dateTime.tz")){
				SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				return sdt.format(value);								
			}else if(upnpType.equals("date")){
				SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd");
				return sdt.format(value);
			}
		}else if(value instanceof Boolean){
			//Must be UPnPStateVariable.TYPE_BOOLEAN
			if(((Boolean)value).booleanValue()){
				return "1";
			}else{
				return "0";
			}
		}else if(value instanceof Character){
			//Must be UPnPStateVariable.TYPE_CHAR
			return value.toString();
		}else if(value instanceof String){
			return value.toString();
			//Must be one of 
			//	UPnPStateVariable.TYPE_STRING or 
			//	UPnPStateVariable.TYPE_URI or 
			//	UPnPStateVariable.TYPE_UUID
		}else if(value instanceof byte[]){
			if(upnpType.equals("bin.hex")){
				return HexBin.encode((byte[]) value);				
			}else if(upnpType.equals("bin.base64")){
				return Base64.encode((byte[]) value);
			}
		}
		throw new IllegalArgumentException("Invalid Binding");
	}
	
	/**
	 * 
	 * @param value
	 * @param upnpType
	 * @return
	 */
	public static Object parseString(String value,String upnpType) throws Exception{
		if (value ==null && upnpType.equals("string"))
                value = "";
        if((value==null)||(upnpType==null))
				throw new NullPointerException("Must be specified a valid value and upnpType");
		
		if (upnpType.equals("ui1") || upnpType.equals("ui2")
				|| upnpType.equals("i1") || upnpType.equals("i2")
				|| upnpType.equals("i4") || upnpType.equals("int")) {
			
			return new Integer(value);
		} else if (upnpType.equals("ui4")){			
			return new Long(value);
		} else if(upnpType.equals("time")){
			String[] timeFormats=new String[]{"HH:mm:ss"};
			Date d=getDateValue(value,timeFormats,timeFormats);
			
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			return new Long(
					c.get(Calendar.HOUR_OF_DAY)*3600000
					+c.get(Calendar.MINUTE)*60000
					+c.get(Calendar.SECOND)*1000
			);
		} else if(upnpType.equals("time.tz")) {
			String[] timeFormats=new String[]{"HH:mm:ssZ","HH:mm:ss"};
			Date d=getDateValue(value,timeFormats,timeFormats);
			TimeZone tz = TimeZone.getDefault();			
			Calendar c = Calendar.getInstance(tz);
			c.setTime(d);
			
			if(timeFormats[0].equals("HH:mm:ssZ")&&(tz.inDaylightTime(d)))
				c.add(Calendar.MILLISECOND,3600000);
			return new Long(
					c.get(Calendar.HOUR_OF_DAY)*3600000
					+c.get(Calendar.MINUTE)*60000
					+c.get(Calendar.SECOND)*1000					
			);
		} else if (upnpType.equals("r4") || upnpType.equals("float")) {				
			return new Float(value);
		} else if (upnpType.equals("r8") || upnpType.equals("number")
			|| upnpType.equals("fixed.14.4")){			
			return new Double(value);
		} else if (upnpType.equals("char")) {			
			return new Character(value.charAt(0));
		} else if (upnpType.equals("string") || upnpType.equals("uri")
				|| upnpType.equals("uuid")) {			
			return value;
		} else if (upnpType.equals("date")) {
			String[] timeFormats=new String[]{"yyyy-MM-dd"};
			
			Date d=getDateValue(value,timeFormats,timeFormats);
			return d;			
		} else if (upnpType.equals("dateTime")) {
			
			String[] timeFormats=new String[]{
					"yyyy-MM-dd",
					"yyyy-MM-dd'T'HH:mm:ss"
			};
			
			Date d=getDateValue(value,timeFormats,timeFormats);
			return d;
		} else if (upnpType.equals("dateTime.tz")) {
			
			String[] timeFormats=new String[]{
					"yyyy-MM-dd",
					"yyyy-MM-dd'T'HH:mm:ss",
					"yyyy-MM-dd'T'HH:mm:ssZ"
			};
			
			Date d=getDateValue(value,timeFormats,timeFormats);
			return d;			
		} else if (upnpType.equals("boolean")) {
			if(value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")
			|| value.equalsIgnoreCase("1"))
				return Boolean.TRUE;
			else
				return Boolean.FALSE;					
		} else if (upnpType.equals("bin.base64")) {
			return Base64.decode(value);
		} else if (upnpType.equals("bin.hex")) {
			return HexBin.decode(value);
		}
		throw new IllegalArgumentException("Invalid Binding");		
	}
	
	private static String normalizeTimeZone(String value){
		if(value.endsWith("Z")){
			value=value.substring(0,value.length()-1)+"+0000";
		}else if((value.length()>7)
			&&(value.charAt(value.length()-3)==':')
			&&((value.charAt(value.length()-6)=='-')||(value.charAt(value.length()-6)=='+'))){
			
			value=value.substring(0,value.length()-3)+value.substring(value.length()-2);
		}		
		return value;
	}
	
	/**
	 * @param value
	 * @param timeFormats
	 * @param choosedIndex
	 * @return
	 * @throws ParseException
	 */
	private static Date getDateValue(String value, String[] timeFormats, String[] choosedIndex) throws ParseException {
		ParsePosition position = null;
		Date d;
		value=normalizeTimeZone(value);
		for (int i=0; i<timeFormats.length; i++) {
			position =  new ParsePosition(0);
			SimpleDateFormat  sdt = new SimpleDateFormat(timeFormats[i]);
			d=sdt.parse(value,position);
			if(d!=null){
				if(position.getIndex()>=value.length()){
					choosedIndex[0]=timeFormats[i];
					return d;			
				}
			}
		}
		throw new ParseException("Error parsing "+value,position.getIndex());
	}
}
