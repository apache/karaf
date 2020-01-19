/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.servlet.upload;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import java.nio.file.Path;
import java.nio.file.Paths;

@org.osgi.service.component.annotations.Component
public class Component {

    @Reference
    protected HttpService httpService;

    @Activate
    public void activate() throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final Path uploadPath = Paths.get(tmpDir, "karaf", "upload");
        uploadPath.toFile().mkdirs();
        httpService.registerServlet("/upload-example", new UploadServlet(uploadPath), null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/upload-example");
    }

}
