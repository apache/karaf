/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.features.internal.service;

import java.net.URI;

import org.apache.karaf.features.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class RepositoryCacheTest {

    private String pkgs;

    @Before
    public void init() {
        String _pkgs = pkgs = System.getProperty("java.protocol.handler.pkgs");
        if (_pkgs == null || "".equals(_pkgs.trim())) {
            _pkgs = "";
        } else {
            _pkgs += "|";
        }
        _pkgs += this.getClass().getPackage().getName();
        System.setProperty("java.protocol.handler.pkgs", _pkgs);
    }

    @After
    public void cleanup() {
        if (pkgs != null) {
            System.setProperty("java.protocol.handler.pkgs", pkgs);
        }
    }

    @Test
    @Ignore("Ignoring to check if it's real problem")
    public void refCountForIncludedRepositories() throws Exception {
        RepositoryCacheImpl cache = new RepositoryCacheImpl(null);
        Repository repo1 = cache.create(getClass().getResource("/org/apache/karaf/features/repo1.xml").toURI(), false);
        Repository repo2 = cache.create(getClass().getResource("/org/apache/karaf/features/repo2.xml").toURI(), false);
        cache.addRepository(repo1);
        cache.addRepository(repo2);
        cache.addRepository(new RepositoryImpl(URI.create("urn:r1"), false));
        assertNotNull(cache.getRepository("urn:r1"));

        cache.removeRepository(repo2.getURI());
        assertNotNull("Repository referenced from two different repositories should not be cascade-removed",
                cache.getRepository("urn:r1"));
    }

}
