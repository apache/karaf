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
package org.apache.servicemix.kernel.jaas.config;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.util.xml.DomUtils;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.apache.servicemix.kernel.jaas.boot.ProxyLoginModule;

public class NamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("config", new ConfigParser());
    }

    protected class ConfigParser extends AbstractSingleBeanDefinitionParser {
        protected Class getBeanClass(Element element) {
            return Config.class;
        }
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            String name = element.getAttribute("name");
            if (name == null || name.length() == 0) {
                name = element.getAttribute("id");
            }
            builder.addPropertyValue("name", name);
            String rank = element.getAttribute("rank");
            if (rank != null && rank.length() > 0) {
                builder.addPropertyValue("rank", Integer.parseInt(rank));
            }
            List childElements = DomUtils.getChildElementsByTagName(element, "module");
            if (childElements != null && childElements.size() > 0) {
                ManagedList children = new ManagedList(childElements.size());
                BeanDefinitionParser parser = new ModuleParser();
                for (int i = 0; i < childElements.size(); ++i) {
                    Element childElement = (Element) childElements.get(i);
                    BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(Module.class);
                    bd.addPropertyValue("className", childElement.getAttribute("className"));
                    if (childElement.getAttribute("flags") != null) {
                        bd.addPropertyValue("flags", childElement.getAttribute("flags"));
                    }
                    String options = DomUtils.getTextValue(childElement);
                    if (options != null && options.length() > 0) {
                        Properties props = new Properties();
                        try {
                            props.load(new ByteArrayInputStream(options.getBytes()));
                        } catch (IOException e) {
                            throw new IllegalStateException("Can not load options for JAAS module "
                                            + childElement.getAttribute("className") + " in config " + name);
                        }
                        bd.addPropertyValue("options", props);
                    }
                    children.add(bd.getBeanDefinition());
                }
                builder.addPropertyValue("modules", children);
            }
            // Publish to OSGi
            String publish = element.getAttribute("publish");
            if (Boolean.valueOf(publish)) {
                // Publish Config
                BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
                bd.addPropertyValue("target", builder.getBeanDefinition());
                bd.addPropertyValue("interfaces", new Class[] { JaasRealm.class });
                Map<String,String> props = new HashMap<String,String>();
                props.put(ProxyLoginModule.PROPERTY_MODULE, name);
                bd.addPropertyValue("serviceProperties", props);
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

    protected class ModuleParser extends AbstractSimpleBeanDefinitionParser {
        protected Class getBeanClass(Element element) {
            return Module.class;
        }
    }


}
