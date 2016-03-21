/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlTransient;


/**
 * {@code Exception} is the superclass of all classes that represent recoverable
 * exceptions. When exceptions are thrown, they may be caught by application
 * code.
 *
 * @see Throwable
 * @see Error
 * @see RuntimeException
 */
public class Exception extends Throwable {
    private static final long serialVersionUID = -3387516993124229948L;

    private transient Class[] classContext = SecurityManagerEx.getInstance().getThrowableContext(this);

    /**
     * Constructs a new {@code Exception} that includes the current stack trace.
     */
    public Exception() {
        super();
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace and the
     * specified detail message.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    public Exception(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace, the
     * specified detail message and the specified cause.
     *
     * @param detailMessage
     *            the detail message for this exception.
     * @param throwable
     *            the cause of this exception.
     */
    public Exception(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace and the
     * specified cause.
     *
     * @param throwable
     *            the cause of this exception.
     */
    public Exception(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructs a new exception with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack
     * trace enabled or disabled.
     *
     * @param  message the detail message.
     * @param cause the cause.  (A {@code null} value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression whether or not suppression is enabled
     *                          or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     */
    protected Exception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        try {
            Field field = null;
            if (writableStackTrace) {
                fillInStackTrace();
            } else {
                field = Throwable.class.getDeclaredField("stackTrace");
                field.setAccessible(true);
                field.set(this, null);
            }
            field = Throwable.class.getDeclaredField("detailMessage");
            field.setAccessible(true);
            field.set(this, message);
            field = Throwable.class.getDeclaredField("cause");
            field.setAccessible(true);
            field.set(this, cause);
            if (!enableSuppression) {
                field = Throwable.class.getDeclaredField("suppressedExceptions");
                field.setAccessible(true);
                field.set(this, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
 
    @XmlTransient
    @Deprecated
    public Class[] getClassContext() {
        return classContext;
    }
    
    protected Class[] classContext() {
      return classContext;
    }

    private static class SecurityManagerEx extends SecurityManager
    {

        private static SecurityManagerEx sm;

        public static SecurityManagerEx getInstance() {
            // No synchronized block because we don't really care
            // if multiple instances are created at some point
            if (sm == null) {
                sm = new SecurityManagerEx();
            }
            return sm;
        }

        public Class[] getThrowableContext(Throwable t) {
            try {
                Class[] context = getClassContext();
                int nb = 0;
                for (;;) {
                    if (context[context.length - 1 - nb] == t.getClass()) {
                        break;
                    }
                    nb++;
                }
                Class[] nc = new Class[nb];
                System.arraycopy(context, context.length - nb, nc, 0, nb);
                return nc;
            } catch (Exception e) {
                return null;
            }
        }
    }

}
