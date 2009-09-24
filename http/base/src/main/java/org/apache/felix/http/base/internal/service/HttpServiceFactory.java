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
package org.apache.felix.http.base.internal.service;

import org.osgi.framework.ServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import javax.servlet.ServletContext;

public final class HttpServiceFactory
    implements ServiceFactory
{
    private final ServletContext context;
    private final HandlerRegistry handlerRegistry;

    public HttpServiceFactory(ServletContext context, HandlerRegistry handlerRegistry)
    {
        this.context = context;
        this.handlerRegistry = handlerRegistry;
    }

    public Object getService(Bundle bundle, ServiceRegistration reg)
    {
        return new HttpServiceImpl(bundle, this.context, this.handlerRegistry);
    }

    public void ungetService(Bundle bundle, ServiceRegistration reg, Object service)
    {
        ((HttpServiceImpl)service).unregisterAll();
    }
}
