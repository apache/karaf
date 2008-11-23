package org.apache.felix.ipojo.junit4osgi.impl;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class LogServiceImpl implements LogService {
    
    private String computeLogMessage(int level, String msg, Throwable exception) {
        String message = null;
        switch (level) {
            case LogService.LOG_DEBUG:
                message = "[DEBUG] " + msg;
                break;
            case LogService.LOG_ERROR:
                message = "[ERROR] " + msg;
                break;
            case LogService.LOG_INFO:
                message = "[INFO] " + msg;
                break;
            case LogService.LOG_WARNING:
                message = "[WARNING] " + msg;
                break;
        }
        
        if (exception != null) {
            message = message + " : " + exception.getMessage();
        }
        
        return message;
    }

    public void log(int arg0, String arg1) {
        System.err.println(computeLogMessage(arg0, arg1, null));
    }

    public void log(int arg0, String arg1, Throwable arg2) {
        System.err.println(computeLogMessage(arg0, arg1, arg2));
    }

    public void log(ServiceReference arg0, int arg1, String arg2) {
        System.err.println(computeLogMessage(arg1, arg2, null));
    }

    public void log(ServiceReference arg0, int arg1, String arg2, Throwable arg3) {
        System.err.println(computeLogMessage(arg1, arg2, arg3));
    }

}
