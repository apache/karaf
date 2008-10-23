package org.apache.servicemix.kernel.gshell.core;

import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

public class BeanContainerAwareProcessor implements InitializingBean, BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private BeanContainer container;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        this.container = new BeanContainerWrapper(applicationContext);
    }

    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        assert bean != null;

        if (bean instanceof BeanContainerAware) {
            ((BeanContainerAware)bean).setBeanContainer(container);
        }

        return bean;
    }

    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }
}