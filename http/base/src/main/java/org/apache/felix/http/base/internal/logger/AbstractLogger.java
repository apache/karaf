package org.apache.felix.http.base.internal.logger;

import org.osgi.service.log.LogService;
import org.osgi.framework.ServiceReference;

public abstract class AbstractLogger
    implements LogService
{
    public final void log(int level, String message)
    {
        log(null, level, message, null);
    }

    public final void log(int level, String message, Throwable cause)
    {
        log(null, level, message, cause);
    }

    public final void log(ServiceReference ref, int level, String message)
    {
        log(ref, level, message, null);
    }
}
