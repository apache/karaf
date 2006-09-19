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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

public class RemoteNotificationClientHandler {
   private static int fetcherID;
   private static int delivererID;

   public interface NotificationHandler
   {
      public NotificationResult fetchNotifications(long sequenceNumber, int maxNumber, long timeout) throws IOException;
      public void sendNotificationsLost(long howMany);
   }

   private final Map tuples = new HashMap();
   private NotificationFetcherThread fetcherThread;
   private NotificationDelivererThread delivererThread;

   public RemoteNotificationClientHandler(NotificationHandler fetcher, Map environment)
   {
      this.fetcherThread = new NotificationFetcherThread(fetcher, environment);
      this.delivererThread = new NotificationDelivererThread();
   }

   private boolean isActive()
   {
      return fetcherThread.isActive();
   }

   private void start()
   {
      delivererThread.start();
      fetcherThread.start();
   }

   private void stop()
   {
      fetcherThread.stop();
      delivererThread.stop();
   }

   private synchronized static int getFetcherID()
   {
      return ++fetcherID;
   }

   private synchronized static int getDelivererID()
   {
      return ++delivererID;
   }

   public boolean contains(NotificationTuple tuple)
   {
      synchronized (tuples)
      {
         return tuples.containsValue(tuple);
      }
   }

   public void addNotificationListener(Integer id, NotificationTuple tuple)
   {
      if (!isActive()) start();

      synchronized (tuples)
      {
         tuples.put(id, tuple);
      }

      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Adding remote NotificationListener " + tuple,null);
   }

   public Integer[] getNotificationListeners(NotificationTuple tuple)
   {
      synchronized (tuples)
      {
         ArrayList ids = new ArrayList();
         for (Iterator i = tuples.entrySet().iterator(); i.hasNext();)
         {
            Map.Entry entry = (Map.Entry)i.next();
            if (entry.getValue().equals(tuple)) ids.add(entry.getKey());
         }
         if (ids.size() > 0) return (Integer[])ids.toArray(new Integer[ids.size()]);
      }
      return null;
   }

   public Integer getNotificationListener(NotificationTuple tuple)
   {
      synchronized (tuples)
      {
         for (Iterator i = tuples.entrySet().iterator(); i.hasNext();)
         {
            Map.Entry entry = (Map.Entry)i.next();
            if (entry.getValue().equals(tuple)) return (Integer)entry.getKey();
         }
      }
      return null;
   }

   public void removeNotificationListeners(Integer[] ids)
   {
      NotificationTuple tuple = null;
      boolean stop = false;
      synchronized (tuples)
      {
         for (int i = 0; i < ids.length; ++i)
         {
            Integer id = ids[i];
            tuple = (NotificationTuple)tuples.remove(id);
         }
         stop = tuples.size() == 0;
      }
      if (stop) stop();

      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Removing remote NotificationListener " + tuple,null);
   }

   private void deliverNotifications(TargetedNotification[] notifications)
   {
      delivererThread.addNotifications(notifications);
   }

   private void sendNotification(TargetedNotification notification)
   {
      NotificationTuple tuple = null;
      synchronized (tuples)
      {
         tuple = (NotificationTuple)tuples.get(notification.getListenerID());
      }

      // It may be possible that a notification arrived after the client already removed the listener
      if (tuple == null) return;

      Notification notif = notification.getNotification();


      if (tuple.getInvokeFilter())
      {
         // Invoke the filter on client side
         NotificationFilter filter = tuple.getNotificationFilter();
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Filtering notification " + notif + ", filter = " + filter,null);
         if (filter != null)
         {
            try
            {
               boolean deliver = filter.isNotificationEnabled(notif);
               if (!deliver) return;
            }
            catch (RuntimeException x)
            {
               RmiConnectorActivator.log(LogService.LOG_WARNING,"RuntimeException caught from isNotificationEnabled, filter = " + filter, x);
               // And go on quietly
            }
         }
      }

      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Sending Notification " + notif + ", listener info is " + tuple,null);

      NotificationListener listener = tuple.getNotificationListener();

      try
      {
         listener.handleNotification(notif, tuple.getHandback());
      }
      catch (RuntimeException x)
      {
         RmiConnectorActivator.log(LogService.LOG_WARNING,"RuntimeException caught from handleNotification, listener = " + listener, x);
         // And return quietly
      }
   }

   private class NotificationFetcherThread implements Runnable {
      private final NotificationHandler handler;
      private long sequenceNumber;
      private volatile boolean active;
      private Thread thread;
      private long timeout;
      private int maxNumber;
      private long sleep;

      public NotificationFetcherThread(NotificationHandler fetcher, Map environment)
      {
         this.handler = fetcher;

         // Default server timeout is one minute
         timeout = 60 * 1000;
         // At most 25 notifications at time
         maxNumber = 25;
         // By default we don't sleep and we call the server again.
         sleep = 0;
         if (environment != null)
         {
            try
            {
               timeout = ((Long)environment.get(MX4JRemoteConstants.FETCH_NOTIFICATIONS_TIMEOUT)).longValue();
            }
            catch (Exception ignored)
            {
            }
            try
            {
               maxNumber = ((Integer)environment.get(MX4JRemoteConstants.FETCH_NOTIFICATIONS_MAX_NUMBER)).intValue();
            }
            catch (Exception ignored)
            {
            }
            try
            {
               sleep = ((Integer)environment.get(MX4JRemoteConstants.FETCH_NOTIFICATIONS_SLEEP)).intValue();
            }
            catch (Exception ignored)
            {
            }
         }
      }

      private synchronized long getSequenceNumber()
      {
         return sequenceNumber;
      }

      private synchronized void setSequenceNumber(long sequenceNumber)
      {
         this.sequenceNumber = sequenceNumber;
      }

      public boolean isActive()
      {
         return active;
      }

      public synchronized void start()
      {
         active = true;
         // Initialized to a negative value for the first fetchNotification call
         sequenceNumber = -1;
         thread = new Thread(this, "Notification Fetcher #" + getFetcherID());
         thread.setDaemon(true);
         thread.start();
      }

      public synchronized void stop()
      {
         active = false;
         thread.interrupt();
      }

      public void run()
      {

         while (isActive() && !thread.isInterrupted())
         {
            try
            {
               long sequence = getSequenceNumber();
               NotificationResult result = handler.fetchNotifications(sequence, maxNumber, timeout);
               RmiConnectorActivator.log(LogService.LOG_WARNING,"Fetched Notifications: " + result, null);

               long sleepTime = sleep;
               if (result != null)
               {
                  long nextSequence = result.getNextSequenceNumber();
                  TargetedNotification[] targeted = result.getTargetedNotifications();
                  int targetedLength = targeted == null ? 0 : targeted.length;
                  boolean notifsFilteredByServer = nextSequence - sequence != targetedLength;
                  boolean notifsLostByServer = sequence >= 0 && result.getEarliestSequenceNumber() > sequence;
                  if (notifsFilteredByServer)
                  {
                     // We lost some notification
                     handler.sendNotificationsLost(nextSequence - sequence - targetedLength);
                  }
                  if (notifsLostByServer)
                  {
                     // We lost some notification
                     handler.sendNotificationsLost(result.getEarliestSequenceNumber() - sequence);
                  }

                  setSequenceNumber(nextSequence);
                  deliverNotifications(targeted);

                  // If we got a maxNumber of notifications, probably the server has more to send, don't sleep
                  if (targeted != null && targeted.length == maxNumber) sleepTime = 0;
               }

               if (sleepTime > 0) Thread.sleep(sleepTime);
            }
            catch (IOException x)
            {
               RmiConnectorActivator.log(LogService.LOG_INFO,"Caught IOException from fetchNotifications", x);
               // And try again
            }
            catch (InterruptedException x)
            {
               active = false;
               Thread.currentThread().interrupt();
               break;
            }
         }
      }
   }

   private class NotificationDelivererThread implements Runnable
   {
      private final List notificationQueue = new LinkedList();
      private volatile boolean active;
      private Thread thread;

      public void addNotifications(TargetedNotification[] notifications)
      {
         if (notifications == null || notifications.length == 0) return;

         List notifs = Arrays.asList(notifications);

         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Enqueuing notifications for delivery: " + notifs,null);

         synchronized (notificationQueue)
         {
            notificationQueue.addAll(notifs);
            notificationQueue.notifyAll();
         }
      }

      public boolean isActive()
      {
         return active;
      }

      public synchronized void start()
      {
         active = true;
         notificationQueue.clear();
         thread = new Thread(this, "Notification Deliverer #" + getDelivererID());
         thread.setDaemon(true);
         thread.start();
      }

      public synchronized void stop()
      {
         active = false;
         thread.interrupt();
      }

      public void run()
      {
         while (isActive() && !thread.isInterrupted())
         {
            try
            {
               TargetedNotification notification = null;
               synchronized (notificationQueue)
               {
                  while (notificationQueue.isEmpty()) notificationQueue.wait();
                  notification = (TargetedNotification)notificationQueue.remove(0);
               }
               sendNotification(notification);
            }
            catch (InterruptedException x)
            {
               active = false;
               Thread.currentThread().interrupt();
               break;
            }
         }
      }
   }
}
