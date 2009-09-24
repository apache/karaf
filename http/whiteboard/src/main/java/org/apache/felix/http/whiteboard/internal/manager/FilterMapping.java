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

import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import javax.servlet.Filter;

public final class FilterMapping
    extends AbstractMapping
{
    private final Filter filter;
    private final int ranking;
    private final String pattern;

    public FilterMapping(HttpContext context, Filter filter, String pattern, int ranking)
    {
        super(context);
        this.filter = filter;
        this.pattern = pattern;
        this.ranking = ranking;
    }

    public void register(HttpService httpService)
    {    
        if (httpService instanceof ExtHttpService) {
            register((ExtHttpService)httpService);
        }
    }

    private void register(ExtHttpService httpService)
    {
        try {
            httpService.registerFilter(this.filter, this.pattern, getInitParams(), ranking, getContext());
        } catch (Exception e) {
            SystemLogger.error("Failed to register filter", e);
        }
    }

    public void unregister(HttpService httpService)
    {
        if (httpService instanceof ExtHttpService) {
            unregister((ExtHttpService)httpService);
        }
    }

    private void unregister(ExtHttpService httpService)
    {
        httpService.unregisterFilter(this.filter);
    }
}
