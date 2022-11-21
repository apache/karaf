/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cache.core.commands;

import org.apache.karaf.cache.api.CacheService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

@Service
@Command(scope = "cache",
        name = "create",
        description = "Create a new cache from XML config.")
public class Create extends CacheCommandSupport {
    @Argument(index = 0,
            required = true,
            description = "Path to ehcache xml configuration file in Karaf's etc directory")
    String configPath;

    @Override
    protected Object doExecute(CacheService cacheService) {
        try {
            URL configUrl = Paths.get(System.getProperty("karaf.etc"), configPath).toUri().toURL();
            cacheService.createCache(configUrl, this.getClass().getClassLoader());
        } catch (MalformedURLException e) {
            System.err.println(e);
        }

        return null;
    }
}
