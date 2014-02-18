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
package org.apache.karaf.scr.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.inject.Init;
import org.apache.karaf.shell.inject.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ScrCommandSupport extends AbstractCommand implements CompletableFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrCommandSupport.class);

    private ComponentContext componentContext;

    public ScrCommandSupport() {
    }

    public void activate(ComponentContext componentContext) {
        LOGGER.info("Activating SCR command for " + componentContext.getProperties().get("component.name"));
        this.componentContext = componentContext;
    }

    public void deactivate(ComponentContext componentContext) {
    }

    public Class<? extends Action> getActionClass() {
        try {
            String className = (String) componentContext.getProperties().get("component.name");
            return (Class<? extends Action>) componentContext.getBundleContext().getBundle().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Action createNewAction() {
        Class actionClass = getActionClass();
        try {
            Action action = (Action) actionClass.newInstance();
            // Inject services
            for (Class<?> cl = actionClass; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        Object value;
                        if (field.getType() == BundleContext.class) {
                            value = componentContext.getBundleContext();
                        } else {
                            value = componentContext.locateService(field.getName());
                        }
                        if (value == null) {
                            throw new RuntimeException("No OSGi service matching " + field.getType().getName());
                        }
                        field.setAccessible(true);
                        field.set(action, value);
                    }
                }
            }
            if (action instanceof BundleContextAware) {
                ((BundleContextAware) action).setBundleContext(componentContext.getBundleContext());
            }
            for (Method method : actionClass.getDeclaredMethods()) {
                Init ann = method.getAnnotation(Init.class);
                if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                    method.setAccessible(true);
                    method.invoke(action);
                }
            }
            return action;
        } catch (Exception e) {
            throw new RuntimeException("Unable to creation command action " + actionClass.getName(), e);
        }
    }

    @Override
    public List<Completer> getCompleters() {
        return null;
    }

    @Override
    public Map<String, Completer> getOptionalCompleters() {
        return null;
    }
}
