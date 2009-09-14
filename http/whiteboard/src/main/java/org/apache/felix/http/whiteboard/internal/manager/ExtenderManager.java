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
package org.apache.felix.http.whiteboard.internal.manager;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.framework.ServiceReference;
import javax.servlet.Servlet;
import javax.servlet.Filter;

public interface ExtenderManager
{
    public void add(HttpContext service, ServiceReference ref);

    public void remove(HttpContext service);

    public void add(Filter service, ServiceReference ref);

    public void remove(Filter service);

    public void add(Servlet service, ServiceReference ref);

    public void remove(Servlet service);

    public void setHttpService(HttpService service);

    public void unsetHttpService();

    public void unregisterAll();
}
