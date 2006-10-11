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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.NotificationFilter;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;


/**
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class DefaultRemoteNotificationServerHandler implements RemoteNotificationServerHandler
{
   private static int listenerID;

   private final NotificationListener listener;
   private final Map tuples = new HashMap();
   private final NotificationBuffer buffer;

   public DefaultRemoteNotificationServerHandler(Map environment)
   {
      listener = new ServerListener();
      buffer = new NotificationBuffer(environment);
   }

   public Integer generateListenerID(ObjectName name, NotificationFilter filter)
   {
      synchronized (DefaultRemoteNotificationServerHandler.class)
      {
         return new Integer(++listenerID);
      }
   }

   public NotificationListener getServerNotificationListener()
   {
      return listener;
   }

   public void addNotificationListener(Integer id, NotificationTuple tuple)
   {
      synchronized (tuples)
      {
         tuples.put(id, tuple);
      }
   }

   public void removeNotificationListener(Integer id)
   {
      synchronized (tuples)
      {
         tuples.remove(id);
      }
   }

   public NotificationTuple getNotificationListener(Integer id)
   {
      synchronized (tuples)
      {
         return (NotificationTuple)tuples.get(id);
      }
   }

   public NotificationResult fetchNotifications(long sequenceNumber, int maxNotifications, long timeout)
   {
      return buffer.getNotifications(sequenceNumber, maxNotifications, timeout);
   }

   /**
    * Called when there are no notifications to send to the client.
    * It is guaranteed that no notification can be added before this method waits on the given lock.
    * It should wait on the given lock for the specified timeout, and return true
    * to send notifications (if no notifications arrived, an empty notification array
    * will be returned to the client), or false if no notifications should be sent to
    * the client.
    * @param lock The object on which {@link #wait} should be called
    * @param timeout The amount of time to wait (guaranteed to be strictly greater than 0)
    */
   protected boolean waitForNotifications(Object lock, long timeout)
   {
      synchronized (lock)
      {
         try
         {
            lock.wait(timeout);
         }
         catch (InterruptedException x)
         {
            Thread.currentThread().interrupt();
         }
      }
      return true;
   }

   /**
    * This method filters the given notification array and returns a possibly smaller array containing
    * only notifications that passed successfully the filtering.
    * Default behavior is no filtering, but subclasses may choose to change this bahavior.
    * For example, for RMI, one can assure that all notifications are truly serializable, and log those
    * that are not.
    */
   protected TargetedNotification[] filterNotifications(TargetedNotification[] notifications)
   {
      return notifications;
   }

   private void addNotification(Integer id, Notification notification)
   {
      buffer.add(new TargetedNotification(notification, id));
   }

   public class ServerListener implements NotificationListener
   {
      public void handleNotification(Notification notification, Object handback)
      {
         Integer id = (Integer)handback;
         addNotification(id, notification);
      }
   }

   public class NotificationBuffer
   {
      private final List buffer = new LinkedList();
      private int maxCapacity;
      private int purgeDistance;
      private long firstSequence;
      private long lastSequence;
      private long lowestExpectedSequence;

      public NotificationBuffer(Map environment)
      {
         if (environment != null)
         {
            try
            {
               maxCapacity = ((Integer)environment.get(MX4JRemoteConstants.NOTIFICATION_BUFFER_CAPACITY)).intValue();
            }
            catch (Exception ignored)
            {
            }

            try
            {
               purgeDistance = ((Integer)environment.get(MX4JRemoteConstants.NOTIFICATION_PURGE_DISTANCE)).intValue();
            }
            catch (Exception ignored)
            {
            }
         }
         if (maxCapacity <= 0) maxCapacity = 1024;
         if (purgeDistance <= 0) purgeDistance = 128;
      }

      public int getSize()
      {
         synchronized (buffer)
         {
            return buffer.size();
         }
      }

      public void add(TargetedNotification notification)
      {
         synchronized (buffer)
         {
            if (buffer.size() == maxCapacity)
            {
               RmiConnectorActivator.log(LogService.LOG_DEBUG, "Notification buffer full: " + this, null);
               removeRange(0, 1);
            }
            buffer.add(notification);
            ++lastSequence;
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Notification added to buffer: " + this, null);
            buffer.notifyAll();
         }
      }

      private void removeRange(int start, int end)
      {
         synchronized (buffer)
         {
            buffer.subList(start, end).clear();
            firstSequence += end - start;
         }
      }

      private long getFirstSequenceNumber()
      {
         synchronized (buffer)
         {
            return firstSequence;
         }
      }

      private long getLastSequenceNumber()
      {
         synchronized (buffer)
         {
            return lastSequence;
         }
      }

      public NotificationResult getNotifications(long sequenceNumber, int maxNotifications, long timeout)
      {
         synchronized (buffer)
         {
            NotificationResult result = null;
            int size = 0;
            if (sequenceNumber < 0)
            {
               // We loose the notifications between addNotificationListener() and fetchNotifications(), but c'est la vie.
               long sequence = getLastSequenceNumber();
               size = new Long(sequence + 1).intValue();
               result = new NotificationResult(getFirstSequenceNumber(), sequence, new TargetedNotification[0]);
                RmiConnectorActivator.log(LogService.LOG_DEBUG,"First fetchNotification call: " + this + ", returning " + result, null);
            }
            else
            {
               int start = new Long(sequenceNumber - getFirstSequenceNumber()).intValue();

               List sublist = null;
               boolean send = false;
               while (size == 0)
               {
                  int end = buffer.size();
                  if (end - start > maxNotifications) end = start + maxNotifications;

                  sublist = buffer.subList(start, end);
                  size = sublist.size();

                  if (send) break;

                  if (size == 0)
                  {
                     if (timeout <= 0) break;
                      RmiConnectorActivator.log(LogService.LOG_DEBUG,"No notifications to send, waiting " + timeout + " ms", null);

                     // We wait for notifications to arrive. Since we release the lock on the buffer
                     // other threads can modify it. To avoid ConcurrentModificationException we compute
                     // again the sublist
                     send = waitForNotifications(buffer, timeout);
                  }
               }

               TargetedNotification[] notifications = (TargetedNotification[])sublist.toArray(new TargetedNotification[size]);
               notifications = filterNotifications(notifications);
               result = new NotificationResult(getFirstSequenceNumber(), sequenceNumber + size, notifications);
                RmiConnectorActivator.log(LogService.LOG_DEBUG,"Non-first fetchNotification call: " + this + ", returning " + result, null);

               purgeNotifications(sequenceNumber, size);
               RmiConnectorActivator.log(LogService.LOG_DEBUG,"Purged Notifications: " + this, null);
            }
            return result;
         }
      }

      private void purgeNotifications(long sequenceNumber, int size)
      {
         // Record the lowest expected sequence number sent by the client.
         // New clients will always have an initial big sequence number
         // (they're initialized with getLastSequenceNumber()), while old
         // clients can have lesser sequence numbers.
         // Here we record the lesser of these sequence numbers, that is the
         // sequence number of the oldest notification any client may ever ask.
         // This way we can purge old notifications that have already been
         // delivered to clients.

         // The worst case is when a client has a long interval between fetchNotifications()
         // calls, and another client has a short interval. The lowestExpectedSequence will
         // grow with the second client, until a purge happens, so the first client can
         // loose notifications. By tuning appropriately the purgeDistance and the interval
         // between fetchNotifications() calls, it should never happen.

         synchronized (buffer)
         {
            if (sequenceNumber <= lowestExpectedSequence)
            {
               long lowest = Math.min(lowestExpectedSequence, sequenceNumber);

               if (lowest - getFirstSequenceNumber() > purgeDistance)
               {
                  // Purge only half of the old notifications, for safety
                  int purgeSize = purgeDistance >> 1;
                  removeRange(0, purgeSize);
               }

               lowestExpectedSequence = sequenceNumber + size;
            }
         }
      }

      public String toString()
      {
         StringBuffer buffer = new StringBuffer("NotificationBuffer@");
         buffer.append(Integer.toHexString(hashCode())).append("[");
         buffer.append("first=").append(getFirstSequenceNumber()).append(", ");
         buffer.append("last=").append(getLastSequenceNumber()).append(", ");
         buffer.append("size=").append(getSize()).append(", ");
         buffer.append("lowest expected=").append(lowestExpectedSequence).append(", ");
         buffer.append("maxCapacity=").append(maxCapacity).append(", ");
         buffer.append("purgeDistance=").append(purgeDistance).append("]");
         return buffer.toString();
      }
   }
}
