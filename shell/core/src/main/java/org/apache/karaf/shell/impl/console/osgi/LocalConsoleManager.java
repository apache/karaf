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

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.console.JLineTerminal;
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.util.jaas.JaasHelper;
import org.jline.terminal.TerminalBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class LocalConsoleManager {

    private SessionFactory sessionFactory;
    private BundleContext bundleContext;
    private Session session;
    private ServiceRegistration<?> registration;
    private boolean closing;

    public LocalConsoleManager(BundleContext bundleContext,
                               SessionFactory sessionFactory) throws Exception {
        this.bundleContext = bundleContext;
        this.sessionFactory = sessionFactory;
    }

    public void start() throws Exception {
        final org.jline.terminal.Terminal terminal = TerminalBuilder.terminal();
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
        this.session = JaasHelper.doAs(subject, new PrivilegedAction<Session>() {
            public Session run() {
                String encoding = getEncoding();
                session = sessionFactory.create(
                                      terminal.input(),
                                      new PrintStream(terminal.output()),
                                      new PrintStream(terminal.output()),
                                      new JLineTerminal(terminal),
                                      encoding, 
                                      callback);
                registration = bundleContext.registerService(Session.class, session, null);
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

    /**
     * Get the default encoding.  Will first look at the LC_CTYPE environment variable, then the input.encoding
     * system property, then the default charset according to the JVM.
     *
     * @return The default encoding to use when none is specified.
     */
    public static String getEncoding() {
        // LC_CTYPE is usually in the form en_US.UTF-8
        String envEncoding = extractEncodingFromCtype(System.getenv("LC_CTYPE"));
        if (envEncoding != null) {
            return envEncoding;
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }

    /**
     * Parses the LC_CTYPE value to extract the encoding according to the POSIX standard, which says that the LC_CTYPE
     * environment variable may be of the format <code>[language[_territory][.codeset][@modifier]]</code>
     *
     * @param ctype The ctype to parse, may be null
     * @return The encoding, if one was present, otherwise null
     */
    static String extractEncodingFromCtype(String ctype) {
        if (ctype != null && ctype.indexOf('.') > 0) {
            String encodingAndModifier = ctype.substring(ctype.indexOf('.') + 1);
            if (encodingAndModifier.indexOf('@') > 0) {
                return encodingAndModifier.substring(0, encodingAndModifier.indexOf('@'));
            } else {
                return encodingAndModifier;
            }
        }
        return null;
    }

    private Subject createLocalKarafSubject() {

        String userName = System.getProperty("karaf.local.user");
        if (userName == null) {
            userName = "karaf";
        }

        final Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal(userName));

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
