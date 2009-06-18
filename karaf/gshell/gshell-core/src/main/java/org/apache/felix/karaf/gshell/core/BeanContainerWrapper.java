/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.karaf.gshell.core;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.wisdom.command.GroupCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class BeanContainerWrapper implements BeanContainer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BlueprintContainer container;

    public BeanContainerWrapper(BlueprintContainer container) {
        this.container = container;
    }

    public BeanContainer getParent() {
        return null;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void loadBeans(String[] strings) throws Exception {
        throw new UnsupportedOperationException();
    }

    public <T> T getBean(Class<T> type) {
        assert type != null;

        if (GroupCommand.class.isAssignableFrom(type)) {
            return type.cast(new GroupCommand());
        }
        throw new UnsupportedOperationException();
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        assert name != null;
        assert requiredType != null;

        log.trace("Getting bean named '{}' of type: {}", name, requiredType);

        return requiredType.cast(container.getComponentInstance(name));
    }

    public <T> Map<String, T> getBeans(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    public String[] getBeanNames() {
        throw new UnsupportedOperationException();
    }

    public String[] getBeanNames(Class type) {
        throw new UnsupportedOperationException();
    }

    public BeanContainer createChild(Collection<URL> urls) {
        throw new UnsupportedOperationException();
    }

    public BeanContainer createChild() {
        throw new UnsupportedOperationException();
    }
}
