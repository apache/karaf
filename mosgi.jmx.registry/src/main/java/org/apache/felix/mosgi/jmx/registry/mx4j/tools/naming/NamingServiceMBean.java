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

package org.apache.felix.mosgi.jmx.registry.mx4j.tools.naming;

/**
 * Management interface for the NamingService MBean.
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.3 $
 */
public interface NamingServiceMBean
{
   /**
    * Sets the port on which rmiregistry listens for incoming connections.
    * @see #getPort
    */
   public void setPort(int port);

   /**
    * Returns the port on which rmiregistry listens for incoming connections
    * @see #setPort
    */
   public int getPort();

   /**
    * Returns whether this MBean has been started and not yet stopped.
    * @see #start
    */
   public boolean isRunning();

}
