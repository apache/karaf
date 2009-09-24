package org.apache.felix.http.base.internal.logger;

import org.osgi.framework.ServiceReference;

public final class NopLogger
    extends AbstractLogger
{
    public void log(ServiceReference ref, int level, String message, Throwable cause)
    {
        // Do nothing
    }
}
