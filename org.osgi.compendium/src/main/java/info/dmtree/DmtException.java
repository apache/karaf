/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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

import java.io.PrintStream;
import java.util.Vector;

/**
 * Checked exception received when a DMT operation fails. Beside the exception
 * message, a <code>DmtException</code> always contains an error code (one of
 * the constants specified in this class), and may optionally contain the URI of
 * the related node, and information about the cause of the exception.
 * <p>
 * Some of the error codes defined in this class have a corresponding error code
 * defined in OMA DM, in these cases the name and numerical value from OMA DM is
 * used. Error codes without counterparts in OMA DM were given numbers from a
 * different range, starting from 1.
 * <p>
 * The cause of the exception (if specified) can either be a single
 * <code>Throwable</code> instance, or a list of such instances if several
 * problems occurred during the execution of a method. An example for the latter
 * is the <code>close</code> method of <code>DmtSession</code> that tries to
 * close multiple plugins, and has to report the exceptions of all failures.
 * <p>
 * Each constructor has two variants, one accepts a <code>String</code> node
 * URI, the other accepts a <code>String[]</code> node path. The former is used
 * by the DmtAdmin implementation, the latter by the plugins, who receive the
 * node URI as an array of segment names. The constructors are otherwise
 * identical.
 * <p>
 * Getter methods are provided to retrieve the values of the additional
 * parameters, and the <code>printStackTrace(PrintWriter)</code> method is
 * extended to print the stack trace of all causing throwables as well.
 * 
 * @version $Revision: 5837 $
 */
public class DmtException extends Exception {
    private static final long serialVersionUID = -63006267148118655L;

    // ----- Public constants -----//

    /**
     * The originator's authentication credentials specify a principal with
     * insufficient rights to complete the command.
     * <p>
     * This status code is used as response to device originated sessions if the
     * remote management server cannot authorize the device to perform the
     * requested operation.
     * <p>
     * This error code corresponds to the OMA DM response status code 401
     * &quot;Unauthorized&quot;.
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * The requested target node was not found. No indication is given as to
     * whether this is a temporary or permanent condition, unless otherwise
     * noted.
     * <p>
     * This is only used when the requested node name is valid, otherwise the
     * more specific error codes {@link #URI_TOO_LONG} or {@link #INVALID_URI}
     * are used. This error code corresponds to the OMA DM response status code
     * 404 &quot;Not Found&quot;.
     */
    public static final int NODE_NOT_FOUND = 404;

    /**
     * The requested command is not allowed on the target node. This includes
     * the following situations:
     * <ul>
     * <li>an interior node operation is requested for a leaf node, or vice
     * versa (e.g. trying to retrieve the children of a leaf node)
     * <li>an attempt is made to create a node where the parent is a leaf node
     * <li>an attempt is made to rename or delete the root node of the tree
     * <li>an attempt is made to rename or delete the root node of the session
     * <li>a write operation (other than setting the ACL) is performed in a
     * non-atomic write session on a node provided by a plugin that is read-only
     * or does not support non-atomic writing
     * <li>a node is copied to its descendant
     * <li>the ACL of the root node is changed not to include Add rights for
     * all principals
     * </ul>
     * <p>
     * This error code corresponds to the OMA DM response status code 405
     * &quot;Command not allowed&quot;.
     */
    public static final int COMMAND_NOT_ALLOWED = 405;

    /**
     * The requested command failed because an optional feature required by the
     * command is not supported. For example, opening an atomic session might
     * return this error code if the DmtAdmin implementation does not support
     * transactions. Similarly, accessing the optional node properties (Title,
     * Timestamp, Version, Size) might not succeed if either the DmtAdmin
     * implementation or the underlying plugin does not support the property.
     * <p>
     * When getting or setting values for interior nodes (an optional
     * optimization feature), a plugin can use this error code to indicate that
     * the given interior node does not support values.
     * <p>
     * This error code corresponds to the OMA DM response status code 406
     * &quot;Optional feature not supported&quot;.
     */
    public static final int FEATURE_NOT_SUPPORTED = 406;
    
    /**
     * The requested command failed because the target URI or one of its
     * segments is too long for what the recipient is able or willing to
     * process, or the target URI contains too many segments. The length and
     * segment number limits are implementation dependent, their minimum values
     * can be found in the Non Functional Requirements section of the OSGi
     * specification.
     * <p>
     * The {@link Uri#mangle(String)} method provides support for ensuring that
     * a URI segment conforms to the length limits set by the implementation.
     * <p>
     * This error code corresponds to the OMA DM response status code 414
     * &quot;URI too long&quot;.
     * 
     * @see "OSGi Service Platform, Mobile Specification Release 4"
     */
    public static final int URI_TOO_LONG = 414;

    /**
     * The requested node creation operation failed because the target already
     * exists. This can occur if the node is created directly (with one of the
     * <code>create...</code> methods), or indirectly (during a
     * <code>copy</code> operation).
     * <p>
     * This error code corresponds to the OMA DM response status code 418
     * &quot;Already exists&quot;.
     */
    public static final int NODE_ALREADY_EXISTS = 418;

    /**
     * The requested command failed because the principal associated with the
     * session does not have adequate access control permissions (ACL) on the
     * target. This can only appear in case of remote sessions, i.e. if the
     * session is associated with an authenticated principal.
     * <p>
     * This error code corresponds to the OMA DM response status code 425
     * &quot;Permission denied&quot;.
     */
    public static final int PERMISSION_DENIED = 425;

    /**
     * The recipient encountered an error which prevented it from fulfilling the
     * request.
     * <p>
     * This error code is only used in situations not covered by any of the
     * other error codes that a method may use. Some methods specify more
     * specific error situations for this code, but it can generally be used for
     * any unexpected condition that causes the command to fail.
     * <p>
     * This error code corresponds to the OMA DM response status code 500
     * &quot;Command Failed&quot;.
     */
    public static final int COMMAND_FAILED = 500;

    /**
     * An error related to the recipient data store occurred while processing
     * the request. This error code may be thrown by any of the methods
     * accessing the tree, but whether it is really used depends on the
     * implementation, and the data store it uses.
     * <p>
     * This error code corresponds to the OMA DM response status code 510
     * &quot;Data store failure&quot;.
     */
    public static final int DATA_STORE_FAILURE = 510;

    /**
     * The rollback command was not completed successfully. The tree might be in
     * an inconsistent state after this error.
     * <p>
     * This error code corresponds to the OMA DM response status code 516
     * &quot;Atomic roll back failed&quot;.
     */
    public static final int ROLLBACK_FAILED = 516;
    

    /**
     * A device initiated remote operation failed. This is used when the
     * protocol adapter fails to send an alert for any reason.
     * <p>
     * Alert routing errors (that occur while looking for the proper protocol
     * adapter to use) are indicated by {@link #ALERT_NOT_ROUTED}, this code is
     * only for errors encountered while sending the routed alert. This error
     * code does not correspond to any OMA DM response status code. It should be
     * translated to the code 500 &quot;Command Failed&quot; when transferring
     * over OMA DM.
     */
    public static final int REMOTE_ERROR = 1;

    /**
     * Operation failed because of meta data restrictions. This covers any
     * attempted deviation from the parameters defined by the
     * <code>MetaNode</code> objects of the affected nodes, for example in the
     * following situations:
     * <ul>
     * <li>creating, deleting or renaming a permanent node, or modifying its
     * type or value
     * <li>creating an interior node where the meta-node defines it as a leaf,
     * or vice versa
     * <li>any operation on a node which does not have the required access type
     * (e.g. executing a node that lacks the <code>MetaNode.CMD_EXECUTE</code>
     * access type)
     * <li>any node creation or deletion that would violate the cardinality
     * constraints
     * <li>any leaf node value setting that would violate the allowed formats,
     * values, mime types, etc.
     * <li>any node creation that would violate the allowed node names
     * </ul>
     * <p>
     * This error code can also be used to indicate any other meta data
     * violation, even if it cannot be described by the <code>MetaNode</code>
     * class. For example, detecting a multi-node constraint violation while
     * committing an atomic session should result in this error.
     * <p>
     * This error code does not correspond to any OMA DM response status code.
     * It should be translated to the code 405 &quot;Command not allowed&quot;
     * when transferring over OMA DM.
     */
    public static final int METADATA_MISMATCH = 2;

    /**
     * The requested command failed because the target URI or node name is
     * <code>null</code> or syntactically invalid. This covers the following
     * cases:
     * <ul>
     * <li>the URI or node name ends with the '\' or '/' character
     * <li>the URI is an empty string (only invalid if the method does not 
     * accept relative URIs)
     * <li>the URI contains the segment &quot;<code>.</code>&quot; at a position
     * other than the beginning of the URI
     * <li>the node name is &quot;<code>..</code>&quot; or the URI contains such
     * a segment
     * <li>the node name is an empty string or the URI contains an empty segment
     * <li>the node name contains an unescaped '/' character
     * </ul>
     * <p>
     * See the {@link Uri#mangle(String)} method for support on escaping invalid
     * characters in a URI.
     * <p>
     * This code is only used if the URI or node name does not match any of the
     * criteria for {@link #URI_TOO_LONG}. This error code does not correspond
     * to any OMA DM response status code. It should be translated to the code
     * 404 &quot;Not Found&quot; when transferring over OMA DM.
     */
    public static final int INVALID_URI = 3;

    /**
     * An error occurred related to concurrent access of nodes. This can happen
     * for example if a configuration node was deleted directly through the
     * Configuration Admin service, while the node was manipulated via the tree.
     * <p>
     * This error code does not correspond to any OMA DM response status code.
     * It should be translated to the code 500 &quot;Command Failed&quot; when
     * transferring over OMA DM.
     */
    public static final int CONCURRENT_ACCESS = 4;

    /**
     * An alert can not be sent from the device to the given principal. This can
     * happen if there is no Remote Alert Sender willing to forward the alert to
     * the given principal, or if no principal was given and the DmtAdmin did
     * not find an appropriate default destination.
     * <p>
     * This error code does not correspond to any OMA DM response status code.
     * It should be translated to the code 500 &quot;Command Failed&quot; when
     * transferring over OMA DM.
     */
    public static final int ALERT_NOT_ROUTED = 5;

    /**
     * A transaction-related error occurred in an atomic session. This error is
     * caused by one of the following situations:
     * <ul>
     * <li>an updating method within an atomic session can not be executed
     * because the underlying plugin is read-only or does not support atomic
     * writing</li>
     * <li>a commit operation at the end of an atomic session failed because
     * one of the underlying plugins failed to close</li>
     * </ul>
     * The latter case may leave the tree in an inconsistent state due to the
     * lack of a two-phase commit system, see {@link DmtSession#commit} for
     * details.
     * <p>
     * This error code does not correspond to any OMA DM response status code.
     * It should be translated to the code 500 &quot;Command Failed&quot; when
     * transferring over OMA DM.
     */
    public static final int TRANSACTION_ERROR = 6;

    /**
     * Creation of a session timed out because of another ongoing session. The
     * length of time while the DmtAdmin waits for the blocking session(s) to
     * finish is implementation dependant.
     * <p>
     * This error code does not correspond to any OMA DM response status code.
     * OMA has several status codes related to timeout, but these are meant to
     * be used when a request times out, not if a session can not be
     * established. This error code should be translated to the code 500
     * &quot;Command Failed&quot; when transferring over OMA DM.
     */
    public static final int SESSION_CREATION_TIMEOUT = 7;

    // ----- Content fields -----//

    /**
     * The URI of the node on which the failed DMT operation was issued, or
     * <code>null</code> if the operation was not associated with a node.
     */
    private final String uri;

    /**
     * The error code of the failure, one of the constants defined in this
     * class.
     */
    private final int code;

	/**
	 * The message associated with the exception, or <code>null</code> if there
	 * is no error message.
	 */
	private final String		message;

	/**
	 * The list of originating exceptions, or empty list or <code>null</code> if
	 * there are no originating exceptions.
	 */
    private final Throwable[] causes;

    /**
     * Determines whether the exception is fatal or not. This is basically a
     * two-state severity indicator, with the 'fatal' severity being the more
     * serious one.
     */
    private final boolean fatal;

    // ----- Constructors -----//

    /**
     * Create an instance of the exception. The <code>uri</code> and
     * <code>message</code> parameters are optional. No originating exception
     * is specified.
     * 
     * @param uri the node on which the failed DMT operation was issued, or
     *        <code>null</code> if the operation is not associated with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     */
    public DmtException(String uri, int code, String message) {
        this(uri, code, message, new Throwable[0], false);
    }

    /**
     * Create an instance of the exception, specifying the cause exception. The
     * <code>uri</code>, <code>message</code> and <code>cause</code>
     * parameters are optional.
     * 
     * @param uri the node on which the failed DMT operation was issued, or
     *        <code>null</code> if the operation is not associated with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     * @param cause the originating exception, or <code>null</code> if there
     *        is no originating exception
     */
    public DmtException(String uri, int code, String message, Throwable cause) {
        this(uri, code, message, (cause == null) ? new Throwable[0]
                : new Throwable[] { cause }, false);
    }

    /**
     * Create an instance of the exception, specifying the list of cause
     * exceptions and whether the exception is a fatal one. This constructor is
     * meant to be used by plugins wishing to indicate that a serious error
     * occurred which should invalidate the ongoing atomic session. The
     * <code>uri</code>, <code>message</code> and <code>causes</code>
     * parameters are optional.
     * <p>
     * If a fatal exception is thrown, no further business methods will be
     * called on the originator plugin. In case of atomic sessions, all other
     * open plugins will be rolled back automatically, except if the fatal
     * exception was thrown during commit.
     * 
     * @param uri the node on which the failed DMT operation was issued, or
     *        <code>null</code> if the operation is not associated with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     * @param causes the list of originating exceptions, or empty list or
     *        <code>null</code> if there are no originating exceptions
     * @param fatal whether the exception is fatal
     */
    public DmtException(String uri, int code, String message, Vector causes,
            boolean fatal) {
        this(uri, code, message, (causes == null) ? new Throwable[0]
                : (Throwable[]) causes.toArray(new Throwable[causes.size()]),
                fatal);
    }
    
    private DmtException(String uri, int code, String message, 
            Throwable[] causes, boolean fatal) {
    	super((Throwable) null);
        this.uri = uri;
        this.code = code;
        this.message = message;
        this.causes = causes;
        this.fatal = fatal;
    }

    /**
     * Create an instance of the exception, specifying the target node as an
     * array of path segments. This method behaves in exactly the same way as if
     * the path was given as a URI string.
     * 
     * @param path the path of the node on which the failed DMT operation was
     *        issued, or <code>null</code> if the operation is not associated
     *        with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     * @see #DmtException(String, int, String)
     */
    public DmtException(String[] path, int code, String message) {
        this(pathToUri(path), code, message);
    }

    /**
     * Create an instance of the exception, specifying the target node as an
     * array of path segments, and specifying the cause exception. This method
     * behaves in exactly the same way as if the path was given as a URI string.
     * 
     * @param path the path of the node on which the failed DMT operation was
     *        issued, or <code>null</code> if the operation is not associated
     *        with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     * @param cause the originating exception, or <code>null</code> if there
     *        is no originating exception
     * @see #DmtException(String, int, String, Throwable)
     */
    public DmtException(String[] path, int code, String message, Throwable cause) {
        this(pathToUri(path), code, message, cause);
    }

    /**
     * Create an instance of the exception, specifying the target node as an
     * array of path segments, the list of cause exceptions, and whether the
     * exception is a fatal one. This method behaves in exactly the same way as
     * if the path was given as a URI string.
     * 
     * @param path the path of the node on which the failed DMT operation was
     *        issued, or <code>null</code> if the operation is not associated
     *        with a node
     * @param code the error code of the failure
     * @param message the message associated with the exception, or
     *        <code>null</code> if there is no error message
     * @param causes the list of originating exceptions, or empty list or
     *        <code>null</code> if there are no originating exceptions
     * @param fatal whether the exception is fatal
     * @see #DmtException(String, int, String, Vector, boolean)
     */
    public DmtException(String[] path, int code, String message, Vector causes,
            boolean fatal) {
        this(pathToUri(path), code, message, causes, fatal);
    }

    // ----- Public methods -----//

    /**
     * Get the node on which the failed DMT operation was issued. Some
     * operations like <code>DmtSession.close()</code> don't require an URI,
     * in this case this method returns <code>null</code>.
     * 
     * @return the URI of the node, or <code>null</code>
     */
    public String getURI() {
        return uri;
    }

    /**
     * Get the error code associated with this exception. Most of the error
     * codes within this exception correspond to OMA DM error codes.
     * 
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the message associated with this exception. The returned string also
     * contains the associated URI (if any) and the exception code. The
     * resulting message has the following format (parts in square brackets are
     * only included if the field inside them is not <code>null</code>):
     * 
     * <pre>
     *  &lt;exception_code&gt;[: '&lt;uri&gt;'][: &lt;error_message&gt;]
     * </pre>
     * 
     * @return the error message in the format described above
     */
    public String getMessage() {
        StringBuffer sb = new StringBuffer(getCodeText(code));
        if (uri != null)
            sb.append(": '").append(uri).append('\'');
        if (message != null)
			sb.append(": ").append(message);

        return sb.toString();
    }

    /**
     * Get the cause of this exception. Returns non-<code>null</code>, if
     * this exception is caused by one or more other exceptions (like a
     * <code>NullPointerException</code> in a DmtPlugin). If there are more
     * than one cause exceptions, the first one is returned.
     * 
     * @return the cause of this exception, or <code>null</code> if no cause
     *         was given
     */
    public Throwable getCause() {
        return causes.length == 0 ? null : causes[0];
    }

    /**
     * Get all causes of this exception. Returns the causing exceptions in an
     * array. If no cause was specified, an empty array is returned.
     * 
     * @return the list of causes of this exception
     */
    public Throwable[] getCauses() {
        return (Throwable[]) causes.clone();
    }

    /**
     * Check whether this exception is marked as fatal in the session. Fatal
     * exceptions trigger an automatic rollback of atomic sessions.
     * 
     * @return whether the exception is marked as fatal
     */
    public boolean isFatal() {
        return fatal;
    }

    /**
     * Prints the exception and its backtrace to the specified print stream. Any
     * causes that were specified for this exception are also printed, together
     * with their backtraces.
     * 
     * @param s <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        for (int i = 0; i<causes.length; i++) {
            s.print("Caused by" + (i > 0 ? " (" + (i+1) + ")" : "") + ": ");
            causes[i].printStackTrace(s);
        }
    }

    // ----- Utility methods -----//

    /**
     * Converts the given path, given as an array of path segments, to a single
     * URI string.
     * 
     * @param path the path to convert
     * @return the URI string representing the same node as the given path
     */
    static String pathToUri(String[] path) {
        if (path == null)
            return null;

        return Uri.toUri(path);
    }

    /**
     * Returns the name of the given error code.
     * 
     * @param code the error code
     * @return a string containing the error code name
     */
    private static String getCodeText(int code) {
        // todo sync codes
        switch (code) {
        case NODE_NOT_FOUND:
            return "NODE_NOT_FOUND";
        case COMMAND_NOT_ALLOWED:
            return "COMMAND_NOT_ALLOWED";
        case FEATURE_NOT_SUPPORTED:
            return "FEATURE_NOT_SUPPORTED";
        case URI_TOO_LONG:
            return "URI_TOO_LONG";
        case NODE_ALREADY_EXISTS:
            return "NODE_ALREADY_EXISTS";
        case PERMISSION_DENIED:
            return "PERMISSION_DENIED";
        case COMMAND_FAILED:
            return "COMMAND_FAILED";
        case DATA_STORE_FAILURE:
            return "DATA_STORE_FAILURE";
        case ROLLBACK_FAILED:
            return "ROLLBACK_FAILED";

        case REMOTE_ERROR:
            return "REMOTE_ERROR";
        case METADATA_MISMATCH:
            return "METADATA_MISMATCH";
        case INVALID_URI:
            return "INVALID_URI";
        case CONCURRENT_ACCESS:
            return "CONCURRENT_ACCESS";
        case ALERT_NOT_ROUTED:
            return "ALERT_NOT_ROUTED";
        case TRANSACTION_ERROR:
            return "TRANSACTION_ERROR";
        case SESSION_CREATION_TIMEOUT:
            return "SESSION_CREATION_TIMEOUT";
        default:
            return "<unknown code>";
        }
    }
}
