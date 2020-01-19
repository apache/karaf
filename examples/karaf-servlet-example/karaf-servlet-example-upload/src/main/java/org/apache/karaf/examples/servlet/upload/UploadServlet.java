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

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class UploadServlet extends HttpServlet {

    /** The size threshold after which the file will be written to disk. */
    private static final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 2;

    /** The maximum size allowed for uploaded files (-1L means unlimited). */
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;

    /** The maximum size allowed for "multipart/form-data" requests (-1L means unlimited). */
    private static final long MAX_REQUEST_SIZE = 1024 * 1024 * 1024;

    private final File tempDir;

    public UploadServlet(final Path tempDir) {
        this.tempDir = tempDir.toFile();
    }

    @Override
    public void init() throws ServletException {
        final MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempDir.getAbsolutePath(), MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD);
        for (final Map.Entry<String, ? extends ServletRegistration> entry : getServletContext().getServletRegistrations()
                .entrySet()) {
            final ServletRegistration reg = entry.getValue();
            if (reg == null) {
                continue;
            }
            if (reg instanceof ServletRegistration.Dynamic) {
                final ServletRegistration.Dynamic regDyn = (ServletRegistration.Dynamic) reg;
                regDyn.setMultipartConfig(multipartConfigElement);
            }
        }
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final Collection<Part> parts = request.getParts();
    }

}
