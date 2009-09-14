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
package org.apache.felix.http.samples.filter;

import javax.servlet.*;
import java.io.IOException;

public class TestFilter
    implements Filter
{
    private final String name;

    public TestFilter(String name)
    {
        this.name = name;
    }
    
    public void init(FilterConfig config)
        throws ServletException
    {
        doLog("Init with config [" + config + "]");
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException
    {
        doLog("Filter request [" + req + "]");
        chain.doFilter(req, res);
    }

    public void destroy()
    {
        doLog("Destroyed filter");
    }

    private void doLog(String message)
    {
        System.out.println("## [" + this.name + "] " + message);
    }
}
