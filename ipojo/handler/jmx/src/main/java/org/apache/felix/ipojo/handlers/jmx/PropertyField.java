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
 * This class build the notification description structure.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PropertyField {

    /**
     * The name of the notification.
     */
    private String m_name;
    /**
     *  The name of the notification.
     */
    private String m_field;
    /**
     * The name of the notification.
     */
    private String m_rights;
    /**
     * The name of the notification.
     */
    private String m_type;
    /**
     * The name of the notification.
     */
    private Object m_value;
    /**
     * The name of the notification.
     */
    private boolean m_notification = false;

    /**
     * Constructor.
     * 
     * @param name the name of the properety
     * @param field the field which send a notification when it is modified
     * @param rights the rights of the attribute (ie: 'r' or 'w')
     * @param type the type of the attribute
     */
    public PropertyField(String name, String field, String rights, String type) {
        this.setName(name);
        this.setField(field);
        this.m_type = type;
        if (isRightsValid(rights)) {
            this.setRights(rights);
        } else {
            this.setField("r"); // default rights is read only
        }
    }

    /**
     * Returns the field.
     * @return the field
     */
    public String getField() {
        return m_field;
    }

    /**
     * Modifies the field.
     * @param field the new field
     */
    public void setField(String field) {
        this.m_field = field;
    }

    /**
     * Returns the name.
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Modifies the name.
     * @param name the new name
     */
    public void setName(String name) {
        this.m_name = name;
    }

    /**
     * Returns the rights.
     * @return the rights
     */
    public String getRights() {
        return m_rights;
    }

    /**
     * Modifies the rights.
     * @param rights the new rights
     */
    public void setRights(String rights) {
        this.m_rights = rights;
    }

    /**
     * Returns the value.
     * @return the value
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Modifies the value.
     * @param value the new value
     */
    public void setValue(Object value) {
        this.m_value = value;
    }

    /**
     * Returns the type.
     * @return the type
     */
    public String getType() {
        return this.m_type;
    }

    /**
     * Returns the description.
     * @return the description
     */
    public String getDescription() {
        //TODO Implement this method.
        return null;
    }

    /**
     * Returns true if this property field is readable, false otherwise.
     * @return {@code true} if this property field is readable, {@code false} otherwise.
     */
    public boolean isReadable() {
        return this.getRights().equals("r") || this.getRights().equals("w");
    }

    /**
     * Returns true if this property field is writable, false otherwise.
     * @return {@code true} if this property field is writable, {@code false} otherwise.
     */
    public boolean isWritable() {
        return this.getRights().equals("w");
    }

    /**
     * Returns true if this property field is notifiable, false otherwise.
     * @return {@code true} if this property field is notifiable, {@code false} otherwise.
     */
    public boolean isNotifiable() {
        return this.m_notification;
    }

    /**
     * Modify the notifiability of this property field.
     * @param value the new notifiability of this property field.
     */
    public void setNotifiable(boolean value) {
        this.m_notification = value;
    }

    /**
     * Is the rights is valid or not ? (ie = 'r' || 'w').
     * 
     * @param rights string representing the rights
     * @return boolean : return {@code true} if rights = 'r' or 'w'
     */
    public static boolean isRightsValid(String rights) {
        return rights != null && (rights.equals("r") || rights.equals("w"));
    }

}
