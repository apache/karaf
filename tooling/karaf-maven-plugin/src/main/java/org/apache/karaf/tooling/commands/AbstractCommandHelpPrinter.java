/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;

public abstract class AbstractCommandHelpPrinter implements CommandHelpPrinter {

    protected Argument replaceDefaultArgument(Field field, Argument argument) {
        if (Argument.DEFAULT.equals(argument.name())) {
            final Argument delegate = argument;
            final String name = field.getName();
            argument = new Argument() {
                public String name() {
                    return name;
                }

                public String description() {
                    return delegate.description();
                }

                public boolean required() {
                    return delegate.required();
                }

                public int index() {
                    return delegate.index();
                }

                public boolean multiValued() {
                    return delegate.multiValued();
                }

                public String valueToShowInHelp() {
                    return delegate.valueToShowInHelp();
                }

                public Class<? extends Annotation> annotationType() {
                    return delegate.annotationType();
                }

                public boolean censor() {
                    return delegate.censor();
                }

                public char mask() {
                    return delegate.mask();
                }
            };
        }
        return argument;
    }

    protected Object getDefaultValue(Action action, Field field) {
        try {
            field.setAccessible(true);
            return field.get(action);
        } catch (Exception e) {
            return null;
        }
    }

    protected String getDefaultValueString(Object o) {
        if (o != null
                && (!(o instanceof Boolean) || ((Boolean) o))
                && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
            return o.toString();
        } else {
            return null;
        }
    }

}
