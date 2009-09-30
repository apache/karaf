/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.log;

/**
 * Implementation dependent exception class used to avoid references to any
 * bundle defined exception class, which might prevent an uninstalled bundle
 * from being garbage collected.  This exception maintains the class of the
 * original exception (as part of the message), the message (appended to the
 * class name) and the stack trace of both the exception thrown and any
 * embedded exceptions.
 */
final class LogException extends Exception
{
    /** The class name of the original exception. */
    private final String m_className;
    /** The message from the original exception. */
    private final String m_message;
    /** The localized message from the original exception. */
    private final String m_localizedMessage;

    /**
     * Create a new instance.
     * @param exception the original exception.
     */
    private LogException(final Throwable exception)
    {
        m_className = exception.getClass().getName();
        m_message = exception.getMessage();
        m_localizedMessage = exception.getLocalizedMessage();
        setStackTrace(exception.getStackTrace());

        Throwable cause = exception.getCause();
        if (cause != null)
        {
            cause = getException(cause);
            initCause(cause);
        }
    }

    /**
     * Returns the message associated with the exception.  The message
     * will be the class name of the original exception followed by the
     * message of the original exception.
     * @return the message associated with the exception
     */
    public String getMessage()
    {
        return m_className + ": " + m_message;
    }

    /**
     * Returns the localized message associated with the exception.  The
     * localized message will be the class name of the original exception
     * followed by the localized message of the original exception.
     * @return the localized message associated with the exception
     */
    public String getLocalizedMessage()
    {
        return m_className + ": " + m_localizedMessage;
    }

    /** The prefix that identifies classes from the "java" namespace. */
    private static final String JAVA_PACKAGE_PREFIX = "java.";

    /**
     * Returns the exception to store in the {@link LogEntry}.
     * @param exception the exception that was originally thrown.
     * @return the exception to store in the {@link LogEntry}
     */
    static Throwable getException(final Throwable exception)
    {
        Throwable result = null;

        if (exception != null)
        {
            String className = exception.getClass().getName();
            if (exception.getCause() == null && className.startsWith(JAVA_PACKAGE_PREFIX))
            {
                result = exception;
            }
            else
            {
                result = new LogException(exception);
            }
        }

        return result;
    }
}