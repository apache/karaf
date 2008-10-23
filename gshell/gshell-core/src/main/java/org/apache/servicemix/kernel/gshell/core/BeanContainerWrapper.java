package org.apache.servicemix.kernel.gshell.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Collection;
import java.net.URL;

import org.apache.geronimo.gshell.spring.BeanContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class BeanContainerWrapper implements BeanContainer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationContext context;

    public BeanContainerWrapper(ApplicationContext context) {
        this.context = context;
    }

    public BeanContainer getParent() {
        return null;
    }

    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    public void loadBeans(String[] strings) throws Exception {
        throw new UnsupportedOperationException();
    }

    public <T> T getBean(Class<T> type) {
        assert type != null;

        log.trace("Getting bean of type: {}", type);

        String[] names = context.getBeanNamesForType(type);

        if (names.length == 0) {
            throw new NoSuchBeanDefinitionException(type, "No bean defined for type: " + type);
        }
        if (names.length > 1) {
            throw new NoSuchBeanDefinitionException(type, "No unique bean defined for type: " + type + ", found matches: " + Arrays.asList(names));
        }

        return getBean(names[0], type);
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        assert name != null;
        assert requiredType != null;

        log.trace("Getting bean named '{}' of type: {}", name, requiredType);

        return (T) context.getBean(name, requiredType);
    }

    public <T> Map<String, T> getBeans(Class<T> type) {
        assert type != null;

        log.trace("Getting beans of type: {}", type);

        return (Map<String,T>) context.getBeansOfType(type);
    }

    public String[] getBeanNames() {
        log.trace("Getting bean names");

        return context.getBeanDefinitionNames();
    }

    public String[] getBeanNames(Class type) {
        assert type != null;

        log.trace("Getting bean names of type: {}", type);

        return context.getBeanNamesForType(type);
    }

    public BeanContainer createChild(String s, Collection<URL> urls) {
        throw new UnsupportedOperationException();
    }

    public BeanContainer createChild(String s) {
        throw new UnsupportedOperationException();
    }
}
