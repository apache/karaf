/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cache.core.commands;

import org.apache.karaf.cache.api.CacheService;
import org.apache.karaf.cache.core.commands.completers.CacheNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "cache", name = "get", description = "Get a single value from a given cache.")
public class Get extends CacheCommandSupport {

    @Argument(index = 0, required = true, description = "Name of the cache to access.")
    @Completion(CacheNameCompleter.class)
    String cacheName;

    @Argument(index = 1, required = true, description = "Key storing the value that we wish to check.")
    Object key;

    @Override
    public Object doExecute(CacheService cacheService) throws Exception {
        return "" + cacheService.get(cacheName, castKey(cacheName, key));
    }
}
