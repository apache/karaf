package org.apache.felix.http.base.internal.logger;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public final class LogServiceLogger
    extends AbstractLogger
{
    private final ConsoleLogger consoleLogger;
    private final ServiceTracker tracker;

    public LogServiceLogger(BundleContext context)
    {
        this.consoleLogger = new ConsoleLogger();
        this.tracker = new ServiceTracker(context, LogService.class.getName(), null);
        this.tracker.open();
    }

    public void close()
    {
        this.tracker.close();
    }

    public void log(ServiceReference ref, int level, String message, Throwable cause)
    {
        LogService log = (LogService)this.tracker.getService();
        if (log != null) {
            log.log(ref, level, message, cause);
        } else {
            this.consoleLogger.log(ref, level, message, cause);
        }
    }
}
