package org.apache.felix.http.base.internal.logger;

import org.osgi.service.log.LogService;

public final class SystemLogger
{
    private final static LogService NOP = new NopLogger();
    private static LogService LOGGER;

    public static void setLogService(LogService service)
    {
        LOGGER = service;
    }

    private static LogService getLogger()
    {
        return LOGGER != null ? LOGGER : NOP;
    }
    
    public static void debug(String message)
    {
        getLogger().log(LogService.LOG_DEBUG, message);
    }

    public static void info(String message)
    {
        getLogger().log(LogService.LOG_INFO, message);
    }

    public static void warning(String message, Throwable cause)
    {
        getLogger().log(LogService.LOG_WARNING, message, cause);
    }

    public static void error(String message, Throwable cause)
    {
        getLogger().log(LogService.LOG_ERROR, message, cause);
    }
}
