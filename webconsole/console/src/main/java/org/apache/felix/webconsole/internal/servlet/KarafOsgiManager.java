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
package org.apache.felix.webconsole.internal.servlet;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.framework.BundleContext;

public class KarafOsgiManager extends OsgiManager {
	private static final long serialVersionUID = 1090035807469459598L;

    private static final Class[] SECURITY_BUGFIX = {
            JaasHelper.class,
            JaasHelper.OsgiSubjectDomainCombiner.class,
            JaasHelper.DelegatingProtectionDomain.class,
    };

    public static final String SUBJECT_RUN_AS = "karaf.subject.runas";

    public KarafOsgiManager(BundleContext bundleContext) {
        super(bundleContext);
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
        Object obj = req.getAttribute(SUBJECT_RUN_AS);
        if (obj instanceof Subject) {
            try {
                JaasHelper.doAs((Subject) obj, (PrivilegedExceptionAction<Object>) () -> {
                    doService((HttpServletRequest) req, (HttpServletResponse) res);
                    return null;
                });
            } catch (PrivilegedActionException e) {
                Exception cause = e.getException();
                if (cause instanceof ServletException) {
                    throw (ServletException) cause;
                }
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                throw new ServletException(cause);
            }
        } else {
            super.service(req, res);
        }
    }

    protected void doService(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        super.service(req, res);
    }
}
