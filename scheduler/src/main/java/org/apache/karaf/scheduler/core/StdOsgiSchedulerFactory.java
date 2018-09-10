package org.apache.karaf.scheduler.core;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

public class StdOsgiSchedulerFactory extends StdSchedulerFactory {

    public StdOsgiSchedulerFactory() {
        throw new IllegalStateException("Not supported. Use: org.apache.karaf.scheduler.core.StdOsgiSchedulerFactory.StdOsgiSchedulerFactory(java.util.Properties)");
    }

    public StdOsgiSchedulerFactory(final Properties props) throws SchedulerException {
        super(props);
    }

    public StdOsgiSchedulerFactory(final String fileName) throws SchedulerException {
        throw new IllegalStateException("Not supported. Use: org.apache.karaf.scheduler.core.StdOsgiSchedulerFactory.StdOsgiSchedulerFactory(java.util.Properties)");
    }

    @Override
    protected Scheduler instantiate(final QuartzSchedulerResources rsrcs, final QuartzScheduler qs) {
        final Scheduler scheduler = new StdOsgiScheduler(qs);
        return scheduler;
    }

}
