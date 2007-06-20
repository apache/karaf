/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.server.interceptor;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.felix.mosgi.jmx.agent.mx4j.server.MBeanMetaData;

/**
 * Interceptor that takes care of replacing the source of Notifications to the
 * ObjectName of the NotificationBroadcaster that emitted it.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class NotificationListenerMBeanServerInterceptor extends DefaultMBeanServerInterceptor
{
   private static class ListenerWrapper implements NotificationListener
   {
      private final NotificationListener listener;
      private final ObjectName objectName;

      private ListenerWrapper(NotificationListener listener, ObjectName name)
      {
         this.listener = listener;
         this.objectName = name;
      }

      public void handleNotification(Notification notification, Object handback)
      {
         // The JMX spec does not specify how to change the source to be the ObjectName
         // of the broadcaster. If we serialize the calls to the listeners, then it's
         // possible to change the source and restore it back to the old value before
         // calling the next listener; but if we want to support concurrent calls
         // to the listeners, this is not possible. Here I chose to support concurrent
         // calls so I change the value once and I never restore it.
         Object src = notification.getSource();
         if (!(src instanceof ObjectName))
         {
            // Change the source to be the ObjectName of the notification broadcaster
            // if we are not already an ObjectName (compliant with RI behaviour)
            notification.setSource(objectName);
         }

         // Notify the real listener
         NotificationListener listener = getTargetListener();
         listener.handleNotification(notification, handback);
      }

      private NotificationListener getTargetListener()
      {
         return listener;
      }

      public int hashCode()
      {
         return getTargetListener().hashCode();
      }

      public boolean equals(Object obj)
      {
         if (obj == null) return false;
         if (obj == this) return true;

         try
         {
            ListenerWrapper other = (ListenerWrapper)obj;
            return getTargetListener().equals(other.getTargetListener());
         }
         catch (ClassCastException ignored)
         {
         }
         return false;
      }

      public String toString()
      {
         return getTargetListener().toString();
      }
   }

   public String getType()
   {
      return "notificationlistener";
   }

   public void addNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback)
   {
      if (isEnabled())
      {
         ListenerWrapper wrapper = new ListenerWrapper(listener, metadata.name);
         super.addNotificationListener(metadata, wrapper, filter, handback);
      }
      else
      {
         super.addNotificationListener(metadata, listener, filter, handback);
      }
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener) throws ListenerNotFoundException
   {
      if (isEnabled())
      {
         ListenerWrapper wrapper = new ListenerWrapper(listener, metadata.name);
         super.removeNotificationListener(metadata, wrapper);
      }
      else
      {
         super.removeNotificationListener(metadata, listener);
      }
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException
   {
      if (isEnabled())
      {
         ListenerWrapper wrapper = new ListenerWrapper(listener, metadata.name);
         super.removeNotificationListener(metadata, wrapper, filter, handback);
      }
      else
      {
         super.removeNotificationListener(metadata, listener, filter, handback);
      }
   }
}
