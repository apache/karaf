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

import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.application.ApplicationConfiguration;
import org.apache.geronimo.gshell.application.Application;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.wisdom.application.ShellCreatedEvent;
import org.apache.geronimo.gshell.event.EventPublisher;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationManagerImpl implements ApplicationManager, ApplicationContextAware, InitializingBean, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private EventPublisher eventPublisher;

    private Application application;

    private ApplicationContext applicationContext;

    public ApplicationManagerImpl(EventPublisher eventPublisher, Application application) {
        this.eventPublisher = eventPublisher;
        this.application = application;
    }

    public void afterPropertiesSet() throws Exception {
        if (!SystemOutputHijacker.isInstalled()) {
            SystemOutputHijacker.install();
        }
        //SystemOutputHijacker.register(application.getIo().outputStream, application.getIo().errorStream);
    }

    public void destroy() {
        SystemOutputHijacker.uninstall();
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

        eventPublisher.publish(new ShellCreatedEvent(shell));

        return shell;
    }

}
