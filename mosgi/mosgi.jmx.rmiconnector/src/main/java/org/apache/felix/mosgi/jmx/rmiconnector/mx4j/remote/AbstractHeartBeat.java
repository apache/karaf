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
import java.util.Map;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public abstract class AbstractHeartBeat implements HeartBeat, Runnable
{
   private final ConnectionNotificationEmitter emitter;
   private long pulsePeriod;
   private int maxRetries;
   private Thread thread;
   private volatile boolean stopped;

   protected AbstractHeartBeat(ConnectionNotificationEmitter emitter, Map environment)
   {
      this.emitter = emitter;
      if (environment != null)
      {
         try
         {
            pulsePeriod = ((Long)environment.get(MX4JRemoteConstants.CONNECTION_HEARTBEAT_PERIOD)).longValue();
         }
         catch (Exception ignored)
         {
         }
         try
         {
            maxRetries = ((Integer)environment.get(MX4JRemoteConstants.CONNECTION_HEARTBEAT_RETRIES)).intValue();
         }
         catch (Exception ignored)
         {
         }
      }
      if (pulsePeriod <= 0) pulsePeriod = 5000;
      if (maxRetries <= 0) maxRetries = 3;
   }

   protected abstract void pulse() throws IOException;

   public void start() throws IOException
   {
      thread = new Thread(this, "Connection HeartBeat");
      thread.setDaemon(true);
      thread.start();
   }

   public void stop() throws IOException
   {
      if (stopped) return;
      stopped = true;
      thread.interrupt();
   }

   public void run()
   {
      int retries = 0;
      while (!stopped && !thread.isInterrupted())
      {
         try
         {
            Thread.sleep(pulsePeriod);

            try
            {
               pulse();
               retries = 0;
            }
            catch (IOException x)
            {
               if (retries++ == maxRetries)
               {
                  // The connection has died
                  sendConnectionNotificationFailed();
                  // And go on
               }
            }
         }
         catch (InterruptedException x)
         {
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   protected void sendConnectionNotificationFailed()
   {
      emitter.sendConnectionNotificationFailed();
   }
}
