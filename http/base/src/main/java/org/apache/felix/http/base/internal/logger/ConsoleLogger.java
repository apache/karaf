package org.apache.felix.http.base.internal.logger;

import org.osgi.framework.ServiceReference;
import java.io.PrintStream;

public final class ConsoleLogger
    extends AbstractLogger
{
    private final PrintStream out;

    public ConsoleLogger()
    {
        this(System.out);
    }

    public ConsoleLogger(PrintStream out)
    {
        this.out = out;
    }

    public void log(ServiceReference ref, int level, String message, Throwable cause)
    {
        StringBuffer str = new StringBuffer();
        switch (level) {
            case LOG_DEBUG:
                str.append("[DEBUG] ");
                break;
            case LOG_INFO:
                str.append("[INFO] ");
                break;
            case LOG_WARNING:
                str.append("[WARNING] ");
                break;
            case LOG_ERROR:
                str.append("[ERROR] ");
                break;
        }

        if (ref != null) {
            str.append("(").append(ref.toString()).append(") ");
        }

        str.append(message);
        this.out.println(str.toString());
        if (cause != null) {
            cause.printStackTrace(this.out);
        }
    }
}
