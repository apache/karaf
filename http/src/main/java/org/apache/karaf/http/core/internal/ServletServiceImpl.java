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
package org.apache.karaf.http.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.karaf.http.core.ServletService;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.ServletInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;

public class ServletServiceImpl implements ServletService {
    private final WebContainer webContainer;

    public ServletServiceImpl(WebContainer webContainer) {
        this.webContainer = webContainer;
    }

    @Override
    public List<ServletInfo> getServlets() {
        if (webContainer == null) {
            return Collections.emptyList();
        }
        ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);
        if (view == null) {
            return Collections.emptyList();
        }

        Set<ServletInfo> servletInfos = view.listServlets();
        return new ArrayList<>(servletInfos);
    }

}
