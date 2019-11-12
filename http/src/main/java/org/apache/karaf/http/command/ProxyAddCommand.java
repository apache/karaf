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
package org.apache.karaf.http.command;

import org.apache.karaf.http.core.ProxyService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "http", name = "proxy-add", description = "Add a new HTTP proxy")
@Service
public class ProxyAddCommand implements Action {

    @Reference
    private ProxyService proxyService;

    @Argument(index = 0, name = "url", description = "HTTP proxy URL", required = true, multiValued = false)
    String url;

    @Argument(index = 1, name = "proxyTo", description = "HTTP location to proxy on the prefix", required = true, multiValued = false)
    String proxyTo;

    @Option(name = "-b", aliases = { "--lb" }, description = "Define the filter to the balancing service to use", required = false, multiValued = false)
    String balancingPolicy;

    @Override
    public Object execute() throws Exception {
        proxyService.addProxy(url, proxyTo, balancingPolicy);
        return null;
    }

}
