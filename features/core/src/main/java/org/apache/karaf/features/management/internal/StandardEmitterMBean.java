/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.karaf.features.management.internal;

import javax.management.*;

public class StandardEmitterMBean extends StandardMBean implements NotificationEmitter {

    private final NotificationBroadcasterSupport emitter;

    @SuppressWarnings("rawtypes")
	public StandardEmitterMBean(Class mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
        this.emitter = new NotificationBroadcasterSupport() {
            @Override
            public MBeanNotificationInfo[] getNotificationInfo() {
                return StandardEmitterMBean.this.getNotificationInfo();
            }
        };
    }

    public void sendNotification(Notification notification) {
        emitter.sendNotification(notification);
    }


    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener, filter, handback);
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        emitter.addNotificationListener(listener, filter, handback);
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[0];
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mbeanInfo = super.getMBeanInfo();
        if (mbeanInfo != null) {
            MBeanNotificationInfo[] notificationInfo = getNotificationInfo();
            mbeanInfo = new MBeanInfo(mbeanInfo.getClassName(), mbeanInfo.getDescription(), mbeanInfo.getAttributes(),
                    mbeanInfo.getConstructors(), mbeanInfo.getOperations(), notificationInfo);
        }
        return mbeanInfo;
    }

}
