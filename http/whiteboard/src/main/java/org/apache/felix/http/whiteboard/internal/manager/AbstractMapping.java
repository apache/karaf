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
import java.util.Hashtable;

public abstract class AbstractMapping
{
    private final HttpContext context;
    private final Hashtable<String, String> initParams;

    public AbstractMapping(HttpContext context)
    {
        this.context = context;
        this.initParams = new Hashtable<String, String>();
    }

    public final HttpContext getContext()
    {
        return this.context;
    }

    public final Hashtable<String, String> getInitParams()
    {
        return this.initParams;
    }

    public abstract void register(HttpService httpService);

    public abstract void unregister(HttpService httpService);
}
