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

import javax.management.MBeanNotificationInfo;

/**
 * This class builds the notification description structure.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class NotificationField {
    /**
     * The name of the notification.
     */
    private String m_name;
    /**
     * The description of the notification.
     */
    private String m_description;
    /**
     * The field of the notification.
     */
    private String m_field;

    /**
     * Constructor.
     * 
     * @param name the name of the notification
     * @param field the field which send a notification when it is modified
     * @param description the description which appears in JMX console
     */

    public NotificationField(String name, String field, String description) {
        this.m_name = name;
        this.m_field = field;
        this.m_description = description;
    }

    /**
     * Returns the MBeanNotificationInfo from this class.
     * 
     * @return the type of the field or {@code null} if it wasn't found
     */
    public MBeanNotificationInfo getNotificationInfo() {
        String[] notificationTypes = new String[1];
        notificationTypes[0] = m_field;
        MBeanNotificationInfo mbni = new MBeanNotificationInfo(
            notificationTypes, m_name, m_description);
        return mbni;
    }
}
