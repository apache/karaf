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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;

/**
 * As defined by the OSGi Web Applications specification, the {@code OSGI-INF} and {@code OSGI-OPT} directories of a
 * web application bundle are not served, as Tomcat does not serve {@code WEB-INF} and {@code META-INF}.
 */
class OsgiDirectoriesValve extends ValveBase {

    private static final String[] DIRECTORIES = {"/OSGI-INF", "/OSGI-OPT"};

    OsgiDirectoriesValve() {
        super(true);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (isProtected(request.getRequestPathMB())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        getNext().invoke(request, response);
    }

    static boolean isProtected(MessageBytes path) {
        for (String directory : DIRECTORIES) {
            if (path.equalsIgnoreCase(directory) || path.startsWithIgnoreCase(directory + "/", 0)) {
                return true;
            }
        }
        return false;
    }

}
