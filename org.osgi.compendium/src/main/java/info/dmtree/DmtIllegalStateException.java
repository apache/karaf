/*
 * $Header: /cvshome/build/info.dmtree/src/info/dmtree/DmtIllegalStateException.java,v 1.4 2006/07/13 13:42:12 tszeredi Exp $
 *
 * Copyright (c) OSGi Alliance (2006). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.dmtree;

/**
 * Unchecked illegal state exception. This class is used in DMT because 
 * java.lang.IllegalStateException does not exist in CLDC.
 */
public class DmtIllegalStateException extends RuntimeException {
    private static final long serialVersionUID = 2015244852018469700L;

    /**
     * Nested exception.
     */
    private final Throwable cause;

    /**
     * Create an instance of the exception with no message.
     */
    public DmtIllegalStateException() {
        super();
        cause = null;
    }

    /**
     * Create an instance of the exception with the specified message.
     * 
     * @param message the reason for the exception
     */
    public DmtIllegalStateException(String message) {
        super(message);
        cause = null;
    }

    /**
     * Create an instance of the exception with the specified cause exception
     * and no message.
     * 
     * @param cause the cause of the exception
     */
    public DmtIllegalStateException(Throwable cause) {
        super();
        this.cause = cause;
    }

    /**
     * Create an instance of the exception with the specified message and cause
     * exception.
     * 
     * @param message the reason for the exception
     * @param cause the cause of the exception
     */
    public DmtIllegalStateException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /**
     * Returns the cause of this exception or <code>null</code> if no cause
     * was specified when this exception was created.
     * 
     * @return the cause of this exception or <code>null</code> if no cause
     *         was specified
     */
    public Throwable getCause() {
        return cause;
    }
}
