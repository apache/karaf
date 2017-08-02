package org.apache.karaf.jms.internal.osgi;

import org.apache.karaf.jms.JmsService;
import org.apache.karaf.jms.internal.JmsMBeanImpl;
import org.apache.karaf.jms.internal.JmsServiceImpl;
import org.apache.karaf.shell.api.console.CommandLoggingFilter;
import org.apache.karaf.shell.support.RegexCommandLoggingFilter;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;

@Services(
        provides = @ProvideService(JmsService.class),
        requires = @RequireService(ConfigurationAdmin.class)
)
public class Activator extends BaseActivator {
    @Override
    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);

        JmsServiceImpl service = new JmsServiceImpl();
        service.setBundleContext(bundleContext);
        service.setConfigAdmin(configurationAdmin);
        register(JmsService.class, service);

        JmsMBeanImpl mbean = new JmsMBeanImpl();
        mbean.setJmsService(service);
        registerMBean(mbean, "type=jms");

        RegexCommandLoggingFilter filter = new RegexCommandLoggingFilter();
        filter.addRegEx("create +.*?--password ([^ ]+)", 2);
        filter.addRegEx("create +.*?-p ([^ ]+)", 2);
        register(CommandLoggingFilter.class, filter);

    }
}
