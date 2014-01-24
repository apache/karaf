/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.console.commands;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.commands.basic.ActionPreparator;
import org.apache.karaf.shell.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.BlueprintContainerAware;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

public class
        BlueprintCommand extends AbstractCommand implements CompletableFunction
{

    protected BlueprintContainer blueprintContainer;
    protected Converter blueprintConverter;
    protected String actionId;
    protected String shell;
    protected List<Completer> completers;
    protected Map<String,Completer> optionalCompleters;

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public void setBlueprintConverter(Converter blueprintConverter) {
        this.blueprintConverter = blueprintConverter;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    public void setShell(String shell) {
        this.shell = shell;
    }

    public List<Completer> getCompleters() {
        return completers;
    }

    public void setCompleters(List<Completer> completers) {
        this.completers = completers;
    }

    public Map<String, Completer> getOptionalCompleters() {
        return optionalCompleters;
    }

    public void setOptionalCompleters(Map<String, Completer> optionalCompleters) {
        this.optionalCompleters = optionalCompleters;
    }

    @Override
    protected ActionPreparator getPreparator() throws Exception {
        return new BlueprintActionPreparator();
    }

    protected class BlueprintActionPreparator extends DefaultActionPreparator {

        @Override
        protected Object convert(Action action, CommandSession commandSession, Object o, Type type) throws Exception {
            GenericType t = new GenericType(type);
            if (t.getRawClass() == String.class) {
                return o != null ? o.toString() : null;
            }
            return blueprintConverter.convert(o, t);
        }

    }

    public Action createNewAction() {
        Action action = (Action) blueprintContainer.getComponentInstance(actionId);
        if (action instanceof BlueprintContainerAware) {
            ((BlueprintContainerAware) action).setBlueprintContainer(blueprintContainer);
        }
        if (action instanceof BundleContextAware) {
            BundleContext context = (BundleContext) blueprintContainer.getComponentInstance("blueprintBundleContext");
            ((BundleContextAware) action).setBundleContext(context);
        }
        return action;
    }

}
