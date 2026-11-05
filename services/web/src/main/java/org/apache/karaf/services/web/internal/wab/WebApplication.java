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
package org.apache.karaf.services.web.internal.wab;

import org.apache.karaf.services.web.internal.Contexts;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * A web application bundle: a bundle with a {@code Web-ContextPath} header.
 */
public class WebApplication {

    public static final String WEB_CONTEXT_PATH = "Web-ContextPath";

    private final Bundle bundle;
    private final String contextPathHeader;
    private final String path;

    WebApplication(Bundle bundle, String contextPathHeader) {
        this.bundle = bundle;
        this.contextPathHeader = contextPathHeader;
        this.path = Contexts.normalizePath(contextPathHeader);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getContextPathHeader() {
        return contextPathHeader;
    }

    /**
     * @return the context path, or {@code null} if the {@code Web-ContextPath} header is not valid.
     */
    public String getPath() {
        return path;
    }

    /**
     * A web application bundle is deployed when it is active, or starting with a lazy activation policy.
     */
    boolean isReady() {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE) {
            return true;
        }
        String policy = bundle.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY);
        return state == Bundle.STARTING && policy != null && policy.trim().startsWith(Constants.ACTIVATION_LAZY);
    }

}
