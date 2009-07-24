/*
 * Copyright (c) OSGi Alliance (2006, 2008). All Rights Reserved.
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
 * 
 * @version $Revision: 6083 $
 */
public class DmtIllegalStateException extends RuntimeException {
    private static final long serialVersionUID = 2015244852018469700L;

    /**
     * Create an instance of the exception with no message.
     */
    public DmtIllegalStateException() {
        super();
    }

    /**
     * Create an instance of the exception with the specified message.
     * 
     * @param message the reason for the exception
     */
    public DmtIllegalStateException(String message) {
        super(message);
    }

    /**
     * Create an instance of the exception with the specified cause exception
     * and no message.
     * 
     * @param cause the cause of the exception
     */
    public DmtIllegalStateException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an instance of the exception with the specified message and cause
     * exception.
     * 
     * @param message the reason for the exception
     * @param cause the cause of the exception
     */
    public DmtIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause was
	 * set.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was
	 *         set.
	 */
    public Throwable getCause() {
        return super.getCause();
    }

	/**
	 * Initializes the cause of this exception to the specified value.
	 * 
	 * @param cause The cause of this exception.
	 * @return This exception.
	 * @throws IllegalArgumentException If the specified cause is this
	 *         exception.
	 * @throws IllegalStateException If the cause of this exception has already
	 *         been set.
	 * @since 1.0.1
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
    }
}
