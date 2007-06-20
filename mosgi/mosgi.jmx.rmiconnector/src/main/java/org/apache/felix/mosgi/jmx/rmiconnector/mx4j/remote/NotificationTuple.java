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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote;

import javax.management.NotificationFilter;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.NotificationListener;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class NotificationTuple
{
   private static final NotificationFilter NO_FILTER = new NotificationFilter()
   {
      public boolean isNotificationEnabled(Notification notification)
      {
         return true;
      }

      public String toString()
      {
         return "no filter";
      }
   };
   private static final Object NO_HANDBACK = new Object()
   {
      public String toString()
      {
         return "no handback";
      }
   };

   private final ObjectName observed;
   private final NotificationListener listener;
   private final NotificationFilter filter;
   private final Object handback;
   private boolean invokeFilter;

   public NotificationTuple(ObjectName observed, NotificationListener listener)
   {
      this(observed, listener, NO_FILTER, NO_HANDBACK);
   }

   public NotificationTuple(ObjectName observed, NotificationListener listener, NotificationFilter filter, Object handback)
   {
      this.observed = observed;
      this.listener = listener;
      this.filter = filter;
      this.handback = handback;
      this.invokeFilter = false;
   }

   public NotificationListener getNotificationListener()
   {
      return listener;
   }

   public Object getHandback()
   {
      if (handback == NO_HANDBACK) return null;
      return handback;
   }

   public NotificationFilter getNotificationFilter()
   {
      if (filter == NO_FILTER) return null;
      return filter;
   }

   public void setInvokeFilter(boolean invoke)
   {
      this.invokeFilter = invoke;
   }

   public boolean getInvokeFilter()
   {
      if (!invokeFilter) return false;
      NotificationFilter filter = getNotificationFilter();
      if (filter == null) return false;
      return true;
   }

   public boolean equals(Object obj)
   {
      if (this == obj) return true;
      if (!(obj instanceof NotificationTuple)) return false;

      final NotificationTuple other = (NotificationTuple)obj;

      if (!observed.equals(other.observed)) return false;
      if (!listener.equals(other.listener)) return false;

      // Special treatment for special filter
      if (filter == NO_FILTER) return true;
      if (other.filter == NO_FILTER) return true;

      if (filter != null ? !filter.equals(other.filter) : other.filter != null) return false;
      if (handback != null ? !handback.equals(other.handback) : other.handback != null) return false;

      return true;
   }

   public int hashCode()
   {
      int result;
      result = observed.hashCode();
      result = 29 * result + listener.hashCode();
      result = 29 * result + (filter != null ? filter.hashCode() : 0);
      result = 29 * result + (handback != null ? handback.hashCode() : 0);
      return result;
   }

   public String toString()
   {
      StringBuffer buffer = new StringBuffer("NotificationTuple [");
      buffer.append(observed).append(", ");
      buffer.append(listener).append(", ");
      buffer.append(filter).append(", ");
      buffer.append(handback).append("]");
      return buffer.toString();
   }
}
