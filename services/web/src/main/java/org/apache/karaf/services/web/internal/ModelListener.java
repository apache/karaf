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
package org.apache.karaf.services.web.internal;

import org.apache.karaf.services.web.internal.wab.WebApplication;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardService;

/**
 * Notified when a whiteboard service or a web application bundle comes, changes or goes.
 */
public interface ModelListener {

    /**
     * A service or a bundle came or changed: the contexts are deployed again shortly.
     */
    void changed();

    /**
     * A whiteboard service goes: it is undeployed before the call returns, while it is still registered.
     */
    void removed(WhiteboardService service);

    /**
     * A web application bundle stops: it is undeployed before the call returns, while it is still active.
     */
    void removed(WebApplication application);

}
