/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.bridge.internal;

import javax.servlet.http.HttpServlet;
import java.util.Hashtable;
import org.apache.felix.http.base.internal.AbstractHttpActivator;
import org.apache.felix.http.base.internal.logger.SystemLogger;

public final class BridgeActivator
    extends AbstractHttpActivator
{
    @Override
    protected void doStart()
        throws Exception
    {
        super.doStart();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("http.felix.dispatcher", getDispatcherServlet().getClass().getName());
        getBundleContext().registerService(HttpServlet.class.getName(), getDispatcherServlet(), props);
        SystemLogger.info("Started bridged http service");
    }
}
