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
package org.apache.felix.karaf.gshell.core;

import org.apache.geronimo.gshell.application.Application;
import org.apache.geronimo.gshell.application.ApplicationConfiguration;
import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.event.EventPublisher;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.wisdom.application.ShellCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class ApplicationManagerImpl implements ApplicationManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private EventPublisher eventPublisher;

    private Application application;

    private BlueprintContainer blueprintContainer;

    public ApplicationManagerImpl(EventPublisher eventPublisher, Application application, BlueprintContainer blueprintContainer) {
        this.eventPublisher = eventPublisher;
        this.application = application;
        this.blueprintContainer = blueprintContainer;
    }

    public void init() throws Exception {
        if (!SystemOutputHijacker.isInstalled()) {
            SystemOutputHijacker.install();
        }
        //SystemOutputHijacker.register(application.getIo().outputStream, application.getIo().errorStream);
    }

    public void destroy() {
        SystemOutputHijacker.uninstall();
    }

    public void configure(ApplicationConfiguration applicationConfiguration) throws Exception {
        throw new UnsupportedOperationException();
    }

    public Application getApplication() {
        return application;
    }

    public Shell create() throws Exception {
        final Shell shell = (Shell) blueprintContainer.getComponentInstance("shell");

        log.debug("Created shell instance: {}", shell);

        eventPublisher.publish(new ShellCreatedEvent(shell));

        return shell;
    }

}
