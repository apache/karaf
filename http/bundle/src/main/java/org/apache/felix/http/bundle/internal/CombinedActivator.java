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
package org.apache.felix.http.bundle.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.felix.http.bridge.internal.BridgeActivator;
import org.apache.felix.http.whiteboard.internal.WhiteboardActivator;
import org.apache.felix.http.jetty.internal.JettyActivator;

public final class CombinedActivator
    implements BundleActivator
{
    private final static String JETTY_ENABLED_PROP = "org.apache.felix.http.jettyEnabled";
    private final static String WHITEBOARD_ENABLED_PROP = "org.apache.felix.http.whiteboardEnabled";

    private BundleActivator jettyActivator;
    private BundleActivator bridgeActivator;
    private BundleActivator whiteboardActivator;

    public void start(BundleContext context)
        throws Exception
    {
        if ("true".equals(context.getProperty(JETTY_ENABLED_PROP))) {
            this.jettyActivator = new JettyActivator();
        } else {
            this.bridgeActivator = new BridgeActivator();
        }

        if ("true".equals(context.getProperty(WHITEBOARD_ENABLED_PROP))) {
            this.whiteboardActivator = new WhiteboardActivator();
        }

        if (this.jettyActivator != null) {
            this.jettyActivator.start(context);
        }

        if (this.bridgeActivator != null) {
            this.bridgeActivator.start(context);
        }

        if (this.whiteboardActivator != null) {
            this.whiteboardActivator.start(context);
        }
    }

    public void stop(BundleContext context)
        throws Exception
    {
        if (this.whiteboardActivator != null) {
            this.whiteboardActivator.stop(context);
        }

        if (this.jettyActivator != null) {
            this.jettyActivator.stop(context);
        }

        if (this.bridgeActivator != null) {
            this.bridgeActivator.stop(context);
        }
    }
}
