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
package org.apache.karaf.webconsole.gogo;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;

@Services(
        requires = @RequireService(SessionFactory.class)
)
public class Activator extends BaseActivator {

    private GogoPlugin gogoPlugin;

    @Override
    protected void doStart() throws Exception {
        gogoPlugin = new GogoPlugin();
        gogoPlugin.setBundleContext(bundleContext);
        gogoPlugin.setSessionFactory(getTrackedService(SessionFactory.class));
        gogoPlugin.start();

        Dictionary<String, String> props = new Hashtable<>();
        props.put("felix.webconsole.label", "gogo");
        register(Servlet.class, gogoPlugin, props);
    }

    @Override
    protected void doStop() {
        super.doStop();
        if (gogoPlugin != null) {
            gogoPlugin.stop();
            gogoPlugin = null;
        }
    }

}
