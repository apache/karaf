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
package org.apache.karaf.features.internal.region;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.utils.repository.BaseRepository;
import org.apache.karaf.features.internal.repository.JsonRepository;
import org.apache.karaf.features.internal.repository.XmlRepository;

public class RepositoryManager {

    private final ConcurrentMap<String, BaseRepository> repositories = new ConcurrentHashMap<>();

    public BaseRepository getRepository(String base, String uri) {
        BaseRepository repo;
        if (uri.startsWith("xml:")) {
            String u = URI.create(base).resolve(uri.substring("xml:".length())).toString();
            uri = "xml:" + u;
            repo = new XmlRepository(u, 0, false);
        }
        else if (uri.startsWith("json:")) {
            String u = URI.create(base).resolve(uri.substring("json:".length())).toString();
            uri = "json:" + u;
            repo = new JsonRepository(u, 0, false);
        }
        else {
            String u = URI.create(base).resolve(uri).toString();
            uri = "xml:" + u;
            repo = new XmlRepository(u, 0, false);
        }
        repositories.putIfAbsent(uri, repo);
        return repositories.get(uri);
    }

}
