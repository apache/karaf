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

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import java.util.HashMap;

public final class HttpContextManager
{
    private final HashMap<String, HttpContext> idMap;
    private final HashMap<HttpContext, String> contextMap;

    public HttpContextManager()
    {
        this.idMap = new HashMap<String, HttpContext>();
        this.contextMap = new HashMap<HttpContext, String>();
    }

    private String createId(Bundle bundle, String contextId)
    {
        return bundle.getBundleId() + "-" + contextId;
    }

    public synchronized HttpContext getHttpContext(Bundle bundle, String contextId)
    {
        String id = createId(bundle, contextId);
        HttpContext context = this.idMap.get(id);

        if (context == null) {
            context = new DefaultHttpContext(bundle);
            this.idMap.put(id, context);
            this.contextMap.put(context, id);
            SystemLogger.debug("Added context with id [" + contextId + "]");
        } else {
            SystemLogger.debug("Reusing context with id [" + contextId + "]");            
        }

        return context;
    }

    public synchronized void removeHttpContext(HttpContext context)
    {
        String id = this.contextMap.remove(context);
        if (id != null) {
            this.idMap.remove(id);
        }
    }

    public synchronized void addHttpContext(Bundle bundle, String contextId, HttpContext context)
    {
        String id = createId(bundle, contextId);
        this.idMap.put(id, context);
        this.contextMap.put(context, id);
    }
}
