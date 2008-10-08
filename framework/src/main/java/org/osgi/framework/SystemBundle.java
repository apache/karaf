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
package org.osgi.framework;

public interface SystemBundle extends Bundle
{
    public static final String FRAMEWORK_STORAGE_PROP = "org.osgi.framework.storage";

    /**
     * Initializes the System Bundle. After calling this method, the System
     * Bundle must be in the STARTING state and must have a valid Bundle Context.
     * The System Bundle will not actually be started until start() is called.
     * If the System Bundle is not initialized called prior to calling start(),
     * then the start method must initialize the System Bundle prior to starting. 
    **/
    public void init() throws BundleException;

    /**
     * Wait until this System Bundle has completely stopped. The stop and update
     * methods on a System Bundle perform an asynchronous stop of the System
     * Bundle. This method can be used to wait until the asynchronous stop of
     * this System Bundle has completed. This method will only wait if called
     * when this System Bundle is in the STARTING, ACTIVE, or STOPPING states.
     * Otherwise it will return immediately.
    **/
    public void waitForStop(long timeout) throws InterruptedException;
}