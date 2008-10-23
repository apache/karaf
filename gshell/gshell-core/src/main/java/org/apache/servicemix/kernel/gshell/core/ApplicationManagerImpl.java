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
package org.apache.servicemix.kernel.gshell.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.application.ApplicationConfiguration;
import org.apache.geronimo.gshell.application.Application;
import org.apache.geronimo.gshell.application.ApplicationSecurityManager;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.wisdom.application.ShellCreatedEvent;
import org.apache.geronimo.gshell.event.EventPublisher;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationManagerImpl implements ApplicationManager, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private EventPublisher eventPublisher;

    private Application application;

    private ApplicationContext applicationContext;

    public ApplicationManagerImpl(EventPublisher eventPublisher, Application application) {
        this.eventPublisher = eventPublisher;
        this.application = application;
    }

    @PostConstruct
    public void init() throws Exception {
        if (!SystemOutputHijacker.isInstalled()) {
            SystemOutputHijacker.install();
        }
        SystemOutputHijacker.register(application.getIo().outputStream, application.getIo().errorStream);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void configure(ApplicationConfiguration applicationConfiguration) throws Exception {
        throw new UnsupportedOperationException();
    }

    public Application getApplication() {
        return application;
    }

    public Shell create() throws Exception {
        final Shell shell = (Shell) applicationContext.getBean("shell");

        log.debug("Created shell instance: {}", shell);

        InvocationHandler handler = new InvocationHandler()
        {
            //
            // FIXME: Need to resolve how to handle the security manager for the application,
            //        the SM is not thread-specific, but VM specific... so not sure this is
            //        the right approache at all :-(
            //

            private final ApplicationSecurityManager sm = new ApplicationSecurityManager();

            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                assert proxy != null;
                assert method != null;
                // args may be null

                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                }

                //
                // TODO: This would be a good place to inject the shell or the shell context into a thread holder
                //

                final SecurityManager prevSM = System.getSecurityManager();
                System.setSecurityManager(sm);
                try {
                    return method.invoke(shell, args);
                }
                catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
                finally {
                    System.setSecurityManager(prevSM);
                }
            }
        };

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Shell proxy = (Shell) Proxy.newProxyInstance(cl, new Class[] { Shell.class }, handler);

        log.debug("Create shell proxy: {}", proxy);

        eventPublisher.publish(new ShellCreatedEvent(proxy));

        return proxy;
    }

}
