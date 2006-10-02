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

package org.apache.felix.upnp.basedriver.importer.util;

import java.util.*;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/
public class DictionaryProp extends Dictionary {
	private Hashtable hash=null;
	/* (non-Javadoc)
	 * @see java.util.Dictionary#size()
	 */

	public DictionaryProp(){
		hash=new Hashtable();
	}
	
	public int size() {
		// TODO Auto-generated method stub
		return hash.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#isEmpty()
	 */
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return hash.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#elements()
	 */
	public Enumeration elements() {
		// TODO Auto-generated method stub
		return hash.elements();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#keys()
	 */
	public Enumeration keys() {
		// TODO Auto-generated method stub
		return hash.keys();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#get(java.lang.Object)
	 */
	public Object get(Object key) {
		// TODO Auto-generated method stub
		String s=((String)key).toLowerCase();
		return hash.get(s);
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#remove(java.lang.Object)
	 */
	public Object remove(Object arg0) {
		// TODO Auto-generated method stub
		String s=((String)arg0).toLowerCase();
		return hash.remove(s);
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		String s=((String)arg0).toLowerCase();
		return hash.put(s,arg1);
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean containsKey(Object key) {
		//String s=((String)key).toLowerCase();
		return hash.containsKey(key);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("");
		Enumeration e = keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			sb.append(key).append(" = ");
			if(get(key) instanceof String[]){
				String[] aux = (String[]) get(key);
				sb.append("[");
				for (int i = 0; i < aux.length-1; i++) {
					sb.append(aux[i]).append(",");
				}
				if(aux.length>0)
					sb.append(aux[aux.length-1]);
				sb.append("]");
			}else{
				sb.append(get(key).toString());
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
