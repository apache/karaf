/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.service.deploymentadmin;

import java.io.InputStream;

/**
 * Checked exception received when something fails during any deployment
 * processes. A <code>DeploymentException</code> always contains an error code 
 * (one of the constants specified in this class), and may optionally contain 
 * the textual description of the error condition and a nested cause exception.
 */
public class DeploymentException extends Exception {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 916011169146851101L;

	/**
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}, 
	 * {@link DeploymentPackage#uninstall()} and {@link DeploymentPackage#uninstallForced()} 
	 * methods can throw {@link DeploymentException} with this error code if the 
	 * {@link DeploymentAdmin#cancel()} method is called from another thread.
	 */
	public static final int	CODE_CANCELLED                  = 401;

	/**
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)} 
	 * methods can throw {@link DeploymentException} with this error code if 
	 * the got InputStream is not a jar. 
	 */
	public static final int	CODE_NOT_A_JAR                  = 404;

	/**
	 * Order of files in the deployment package is bad. The right order is the 
	 * following:<p>
	 * 
	 * <ol>
	 *    <li>META-INF/MANIFEST.MF</li>
	 *    <li>META-INF/*.SF, META-INF/*.DSA, META-INF/*.RS</li>
	 *    <li>Localization files</li>
	 *    <li>Bundles</li>
	 *    <li>Resources</li>
	 * </ol>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_ORDER_ERROR				= 450;

	/**
	 * Missing mandatory manifest header.<p>
	 *  
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)} can throw 
	 * exception with this error code.  
	 */
	public static final int	CODE_MISSING_HEADER				= 451;

	/**
	 * Syntax error in any manifest header.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_BAD_HEADER					= 452;

	/**
	 * Fix pack version range doesn't fit to the version of the target
	 * deployment package or the target deployment package of the fix pack
	 * doesn't exist.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_MISSING_FIXPACK_TARGET		= 453;

	/**
	 * A bundle in the deployment package is marked as DeploymentPackage-Missing
	 * but there is no such bundle in the target deployment package.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_MISSING_BUNDLE				= 454;

	/**
	 * A resource in the source deployment package is marked as
	 * DeploymentPackage-Missing but there is no such resource in the target
	 * deployment package.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_MISSING_RESOURCE			= 455;

	/**
	 * Bad deployment package signing.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_SIGNING_ERROR				= 456;

	/**
	 * Bundle symbolic name is not the same as defined by the deployment package
	 * manifest.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_BUNDLE_NAME_ERROR			= 457;

	/**
	 * Matched resource processor service is a customizer from another
	 * deployment package.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_FOREIGN_CUSTOMIZER			= 458;

	/**
	 * Bundle with the same symbolic name alerady exists.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_BUNDLE_SHARING_VIOLATION	= 460;

	/**
	 * An artifact of any resource already exists.<p>
	 * 
	 * This exception is thrown when the called resource processor throws a 
	 * <code>ResourceProcessorException</code> with the 
	 * {@link org.osgi.service.deploymentadmin.spi.ResourceProcessorException#CODE_RESOURCE_SHARING_VIOLATION} 
	 * error code.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_RESOURCE_SHARING_VIOLATION	= 461;

	/**
	 * Exception with this error code is thrown when one of the Resource Processors 
	 * involved in the deployment session threw a <code>ResourceProcessorException</code> with the 
	 * {@link org.osgi.service.deploymentadmin.spi.ResourceProcessorException#CODE_PREPARE} error 
	 * code.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)} and 
	 * {@link DeploymentPackage#uninstall()} methods throw exception with this error code.  
	 */
	public static final int	CODE_COMMIT_ERROR				= 462;

	/**
	 * Other error condition.<p>
	 * 
	 * All Deployment Admin methods which throw <code>DeploymentException</code> 
	 * can throw an exception with this error code if the error condition cannot be 
	 * categorized. 
	 */
	public static final int	CODE_OTHER_ERROR				= 463;

	/**
	 * The Resource Processor service with the given PID (see
	 * <code>Resource-Processor</code> manifest header) is not found.<p>
	 *  
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)},
	 * {@link DeploymentPackage#uninstall()} and 
	 * {@link DeploymentPackage#uninstallForced()}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_PROCESSOR_NOT_FOUND		= 464;

	/**
	 * When a client requests a new session with an install or uninstall
	 * operation, it must block that call until the earlier session is
	 * completed. The Deployment Admin service must throw a Deployment Exception
	 * with this error code when the session can not be created after an appropriate
	 * time out period.<p>
	 * 
	 * {@link DeploymentAdmin#installDeploymentPackage(InputStream)},
	 * {@link DeploymentPackage#uninstall()} and 
	 * {@link DeploymentPackage#uninstallForced()}
	 * throws exception with this error code.  
	 */
	public static final int	CODE_TIMEOUT					= 465;

	private final int				code;

	/**
	 * Create an instance of the exception.
	 * 
	 * @param code The error code of the failure. Code should be one of the
	 *        predefined integer values (<code>CODE_X</code>).
	 * @param message Message associated with the exception
	 * @param cause the originating exception
	 */
	public DeploymentException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Create an instance of the exception. Cause exception is implicitly set to
	 * null.
	 * 
	 * @param code The error code of the failure. Code should be one of the
	 *        predefined integer values (<code>CODE_X</code>).
	 * @param message Message associated with the exception
	 */
	public DeploymentException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Create an instance of the exception. Cause exception and message are
	 * implicitly set to null.
	 * 
	 * @param code The error code of the failure. Code should be one of the
	 *        predefined integer values (<code>CODE_X</code>).
	 */
	public DeploymentException(int code) {
		super();
		this.code = code;
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
	 * @since 1.1
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}

	/**
	 * @return Returns the code.
	 */
	public int getCode() {
		return code;
	}
}
