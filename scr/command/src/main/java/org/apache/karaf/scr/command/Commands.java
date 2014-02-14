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
package org.apache.karaf.scr.command;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.scr.command.action.ActivateAction;
import org.apache.karaf.scr.command.action.DeactivateAction;
import org.apache.karaf.scr.command.action.DetailsAction;
import org.apache.karaf.scr.command.action.ListAction;
import org.apache.karaf.scr.command.completer.ActivateCompleter;
import org.apache.karaf.scr.command.completer.DeactivateCompleter;
import org.apache.karaf.scr.command.completer.DetailsCompleter;
import org.apache.karaf.scr.command.completer.ScrCompleterSupport;
import org.apache.karaf.shell.commands.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Component
public class Commands extends org.apache.karaf.shell.commands.Commands {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    protected ScrService scrService;

    @Activate
    public void activate() {
        logger.info("Activating SCR commands");
        completer(ActivateCompleter.class);
        completer(DeactivateCompleter.class);
        completer(DetailsCompleter.class);
        command(ActivateAction.class);
        command(DeactivateAction.class);
        command(DetailsAction.class);
        command(ListAction.class);
        register();
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivating SCR commands");
        unregister();
    }

    @Reference
    public void setScrService(ScrService scrService) {
        this.scrService = scrService;
    }

    public CompleterBuilder completer(Class<? extends ScrCompleterSupport> completerClass) {
        try {
            ScrCompleterSupport completer = completerClass.newInstance();
            completer.setScrService(scrService);
            return completer(completer);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create completer", e);
        }
    }

    public CommandBuilder command(Class<? extends Action> actionClass) {
        return super.command(actionClass)
                .properties(scrService)
                .serviceProp(ScrCommandConstants.HIDDEN_COMPONENT_KEY, "true");
    }

}
