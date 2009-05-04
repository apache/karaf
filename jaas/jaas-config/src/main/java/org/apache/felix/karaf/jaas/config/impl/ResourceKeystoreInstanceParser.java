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
package org.apache.felix.karaf.jaas.config.impl;

import org.w3c.dom.Element;

import org.apache.felix.karaf.jaas.config.KeystoreInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;

/**
 * Spring parser for a keystore instance
 */
public class ResourceKeystoreInstanceParser extends AbstractSingleBeanDefinitionParser {

    public static final String PUBLISH_ATTRIBUTE = "publish";

    protected Class getBeanClass(Element element) {
        return ResourceKeystoreInstance.class;
    }

    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        // Parse name
        String name = element.getAttribute("name");
        if (name == null || name.length() == 0) {
            name = element.getAttribute("id");
        }
        if (name != null && name.length() > 0) {
            builder.addPropertyValue("name", name);
        }
        // Parse rank
        String rank = element.getAttribute("rank");
        if (rank != null && rank.length() > 0) {
            builder.addPropertyValue("rank", rank);
        }
        // Parse path
        String path = element.getAttribute("path");
        if (path != null && path.length() > 0) {
            builder.addPropertyValue("path", path);
        }
        // Parse keystorePassword
        String keystorePassword = element.getAttribute("keystorePassword");
        if (keystorePassword != null && keystorePassword.length() > 0) {
            builder.addPropertyValue("keystorePassword", keystorePassword);
        }
        // Parse keyPasswords
        String keyPasswords = element.getAttribute("keyPasswords");
        if (keyPasswords != null && keyPasswords.length() > 0) {
            builder.addPropertyValue("keyPasswords", keyPasswords);
        }
        // Parse publish
        String publish = element.getAttribute("publish");
        if (Boolean.valueOf(publish)) {
            // Publish Config
            BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
            bd.addPropertyReference("bundleContext", "bundleContext");
            bd.addPropertyValue("target", builder.getBeanDefinition());
            bd.addPropertyValue("interfaces", new Class[] { KeystoreInstance.class });
            BeanDefinition def = bd.getBeanDefinition();
            String id = parserContext.getReaderContext().generateBeanName(def);
            BeanDefinitionHolder holder = new BeanDefinitionHolder(def, id);
            registerBeanDefinition(holder, parserContext.getRegistry());
            if (shouldFireEvents()) {
                BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
                postProcessComponentDefinition(componentDefinition);
                parserContext.registerComponent(componentDefinition);
            }
        }
    }

}
