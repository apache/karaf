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

package org.apache.felix.ipojo.handlers.jmx;

/** 
 * this calss build the notification descritpion structure.
 *  
 *  @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PropertyField {

    /** 
     * m_name : name of the notification.
     */
    private String m_name;
    /** 
     * m_name : name of the notification.
     */
    private String m_field;
    /** 
     * m_name : name of the notification.
     */
    private String m_rights;
    /** 
     * m_name : name of the notification.
     */
    private String m_type;
    /** 
     * m_name : name of the notification.
     */
    private Object m_value;
    /** 
     * m_name : name of the notification.
     */
    private boolean m_notification = false;
    
    /** 
     * PropertyField : constructor.
     * @param name : name of the properety 
     * @param field : field which send a notification when it is modified
     * @param rights : the rights of the attribute (ie: 'r' or 'w')
     * @param type : the type of the attribute
     */
    public PropertyField(String name, String field, String rights, String type) {
        this.setName(name);
        this.setField(field);
        this.m_type = type;
        if (isRightsValid(rights)) {
            this.setRights(rights);
        } else {
            this.setField("r"); //default rights is read only
        }
    }
    
    public String getField() {
        return m_field;
    }
    public void setField(String field) {
        this.m_field = field;
    }
    public String getName() {
        return m_name;
    }
    public void setName(String name) {
        this.m_name = name;
    }
    public String getRights() {
        return m_rights;
    }
    public void setRights(String rights) {
        this.m_rights = rights;
    }
    public Object getValue() {
        return m_value;
    }
    public void setValue(Object value) {
        this.m_value = value;
    }

    public String getType() {
        return this.m_type;
    }

    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isReadable() {
        return this.getRights().equals("r") || this.getRights().equals("w");
    }

    public boolean isWritable() {
        return  this.getRights().equals("w");
    }
    
    public boolean isNotifiable() {
        return this.m_notification;
    }
    
    public void setNotifiable(boolean value) {
        this.m_notification = value;
    }
    
    /** 
     * isRightsValid : return is the rights is valid or not (ie = 'r' || 'w').
     * @param rights :  string represents the rights
     * @return boolean : return true if rights = 'r' or 'w'
     */
    public static boolean isRightsValid(String rights) {
        return rights != null && (rights.equals("r") || rights.equals("w"));
    }
    
}
