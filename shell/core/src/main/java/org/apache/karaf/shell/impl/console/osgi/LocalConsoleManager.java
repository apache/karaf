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
package org.apache.karaf.shell.impl.console.osgi;

import java.nio.charset.Charset;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.console.JLineTerminal;
import org.apache.karaf.shell.impl.console.TerminalFactory;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class LocalConsoleManager {

    private SessionFactory sessionFactory;
    private BundleContext bundleContext;
    private TerminalFactory terminalFactory;
    private Session session;
    private ServiceRegistration<?> registration;
    private boolean closing;

    public LocalConsoleManager(BundleContext bundleContext,
                               TerminalFactory terminalFactory,
                               SessionFactory sessionFactory) throws Exception {
        this.bundleContext = bundleContext;
        this.terminalFactory = terminalFactory;
        this.sessionFactory = sessionFactory;
    }

    public void start() throws Exception {
        final jline.Terminal terminal = terminalFactory.getTerminal();
        final Runnable callback = new Runnable() {
            public void run() {
                try {
                    if (!closing) {
                        bundleContext.getBundle(0).stop();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        };

        
        final Subject subject = createLocalKarafSubject();    
        this.session = JaasHelper.<Session>doAs(subject, new PrivilegedAction<Session>() {
            public Session run() {
                String encoding = getEncoding();
                session = sessionFactory.create(
                                      StreamWrapUtil.reWrapIn(terminal, System.in),
                                      StreamWrapUtil.reWrap(System.out), 
                                      StreamWrapUtil.reWrap(System.err),
                                      new JLineTerminal(terminal),
                                      encoding, 
                                      callback);
                String name = "Karaf local console user " + ShellUtil.getCurrentUserName();
                boolean delayconsole = Boolean.parseBoolean(System.getProperty("karaf.delay.console"));
                if (delayconsole) {
                    DelayedStarted watcher = new DelayedStarted(session, name, bundleContext, System.in);
                    new Thread(watcher).start();
                } else {
                    new Thread(session, name).start();
                }
                return session;
            }
        });
        // TODO: register the local session so that ssh can add the agent
//        registration = bundleContext.register(CommandSession.class, console.getSession(), null);

    }

    private String getEncoding() {
        String ctype = System.getenv("LC_CTYPE");
        String encoding = ctype;
        if (encoding != null && encoding.indexOf('.') > 0) {
            encoding = encoding.substring(encoding.indexOf('.') + 1);
        } else {
            encoding = System.getProperty("input.encoding", Charset.defaultCharset().name());
        }
        return encoding;
    }

    private Subject createLocalKarafSubject() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("karaf"));

        String roles = System.getProperty("karaf.local.roles");
        if (roles != null) {
            for (String role : roles.split("[,]")) {
                subject.getPrincipals().add(new RolePrincipal(role.trim()));
            }
        }
        return subject;
    }

    public void stop() throws Exception {
        closing = true;
        if (registration != null) {
            registration.unregister();
        }
        // The bundle is stopped
        // so close the console and remove the callback so that the
        // osgi framework isn't stopped
        if (session != null) {
            session.close();
        }
    }

}
