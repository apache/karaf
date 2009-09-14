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
package org.apache.felix.http.base.internal;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;

public final class DispatcherServlet
    extends HttpServlet
{
    private final HttpServiceController controller;

    public DispatcherServlet(HttpServiceController controller)
    {
        this.controller = controller;
    }

    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        this.controller.register(getServletContext());
    }

    @Override
    public void destroy()
    {
        this.controller.unregister();
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        this.controller.getDispatcher().dispatch(req, res);
    }
}
