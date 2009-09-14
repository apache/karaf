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
package org.apache.felix.http.samples.whiteboard;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import javax.servlet.Servlet;
import javax.servlet.Filter;
import java.util.Hashtable;

public final class Activator
    implements BundleActivator
{
    private ServiceRegistration reg1;
    private ServiceRegistration reg2;
    private ServiceRegistration reg3;
    private ServiceRegistration reg4;
    
    public void start(BundleContext context)
        throws Exception
    {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("alias", "/");
        this.reg1 = context.registerService(Servlet.class.getName(), new TestServlet("servlet1"), props);

        props = new Hashtable<String, String>();
        props.put("alias", "/other");
        this.reg2 = context.registerService(Servlet.class.getName(), new TestServlet("servlet2"), props);

        props = new Hashtable<String, String>();
        props.put("pattern", ".*");
        this.reg3 = context.registerService(Filter.class.getName(), new TestFilter("filter1"), props);

        props = new Hashtable<String, String>();
        props.put("pattern", "/other/.*");
        props.put("service.ranking", "100");
        this.reg4 = context.registerService(Filter.class.getName(), new TestFilter("filter2"), props);
    }

    public void stop(BundleContext context)
        throws Exception
    {
        this.reg1.unregister();
        this.reg2.unregister();
        this.reg3.unregister();
        this.reg4.unregister();
    }
}
