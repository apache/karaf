/*
 * $Header: /cvshome/build/org.osgi.service.permissionadmin/src/org/osgi/service/permissionadmin/PermissionInfo.java,v 1.8 2005/06/21 15:41:57 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.permissionadmin;

/**
 * Permission representation used by the Permission Admin service.
 * 
 * <p>
 * This class encapsulates three pieces of information: a Permission <i>type
 * </i> (class name), which must be a subclass of
 * <code>java.security.Permission</code>, and the <i>name </i> and <i>actions
 * </i> arguments passed to its constructor.
 * 
 * <p>
 * In order for a permission represented by a <code>PermissionInfo</code> to be
 * instantiated and considered during a permission check, its Permission class
 * must be available from the system classpath or an exported package. This
 * means that the instantiation of a permission represented by a
 * <code>PermissionInfo</code> may be delayed until the package containing its
 * Permission class has been exported by a bundle.
 * 
 * @version $Revision: 1.8 $
 */
public class PermissionInfo {
	private String	type;
	private String	name;
	private String	actions;

	/**
	 * Constructs a <code>PermissionInfo</code> from the given type, name, and
	 * actions.
	 * 
	 * @param type The fully qualified class name of the permission represented
	 *        by this <code>PermissionInfo</code>. The class must be a subclass
	 *        of <code>java.security.Permission</code> and must define a
	 *        2-argument constructor that takes a <i>name </i> string and an
	 *        <i>actions </i> string.
	 * 
	 * @param name The permission name that will be passed as the first argument
	 *        to the constructor of the <code>Permission</code> class identified
	 *        by <code>type</code>.
	 * 
	 * @param actions The permission actions that will be passed as the second
	 *        argument to the constructor of the <code>Permission</code> class
	 *        identified by <code>type</code>.
	 * 
	 * @exception java.lang.NullPointerException if <code>type</code> is
	 *            <code>null</code>.
	 * @exception java.lang.IllegalArgumentException if <code>action</code> is not
	 *            <code>null</code> and <code>name</code> is <code>null</code>.
	 */
	public PermissionInfo(String type, String name, String actions) {
		this.type = type;
		this.name = name;
		this.actions = actions;
		if (type == null) {
			throw new NullPointerException("type is null");
		}
		if ((name == null) && (actions != null)) {
			throw new IllegalArgumentException("name missing");
		}
	}

	/**
	 * Constructs a <code>PermissionInfo</code> object from the given encoded
	 * <code>PermissionInfo</code> string.
	 * 
	 * @param encodedPermission The encoded <code>PermissionInfo</code>.
	 * @see #getEncoded
	 * @exception java.lang.IllegalArgumentException if
	 *            <code>encodedPermission</code> is not properly formatted.
	 */
	public PermissionInfo(String encodedPermission) {
		if (encodedPermission == null) {
			throw new NullPointerException("missing encoded permission");
		}
		if (encodedPermission.length() == 0) {
			throw new IllegalArgumentException("empty encoded permission");
		}
		try {
			char[] encoded = encodedPermission.toCharArray();
			/* the first character must be '(' */
			if (encoded[0] != '(') {
				throw new IllegalArgumentException(
						"first character not open parenthesis");
			}
			/* type is not quoted or encoded */
			int end = 1;
			int begin = end;
			while ((encoded[end] != ' ') && (encoded[end] != ')')) {
				end++;
			}
			if (end == begin) {
				throw new IllegalArgumentException("expecting type");
			}
			this.type = new String(encoded, begin, end - begin);
			/* type may be followed by name which is quoted and encoded */
			// TODO Need to support multiple spaces
			if (encoded[end] == ' ') {
				end++;
				if (encoded[end] != '"') {
					throw new IllegalArgumentException("expecting quoted name");
				}
				end++;
				begin = end;
				while (encoded[end] != '"') {
					if (encoded[end] == '\\') {
						end++;
					}
					end++;
				}
				this.name = decodeString(encoded, begin, end);
				end++;
				/* name may be followed by actions which is quoted and encoded */
				// TODO Need to support multiple spaces
				if (encoded[end] == ' ') {
					end++;
					if (encoded[end] != '"') {
						throw new IllegalArgumentException(
								"expecting quoted actions");
					}
					end++;
					begin = end;
					while (encoded[end] != '"') {
						if (encoded[end] == '\\') {
							end++;
						}
						end++;
					}
					this.actions = decodeString(encoded, begin, end);
					end++;
				}
			}
			/* the final character must be ')' */
			if ((encoded[end] != ')') || (end + 1 != encoded.length)) {
				throw new IllegalArgumentException("last character not "
						+ "close parenthesis");
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("parsing terminated abruptly");
		}
	}

	/**
	 * Returns the string encoding of this <code>PermissionInfo</code> in a form
	 * suitable for restoring this <code>PermissionInfo</code>.
	 * 
	 * <p>
	 * The encoded format is:
	 * 
	 * <pre>
	 * (type)
	 * </pre>
	 * 
	 * or
	 * 
	 * <pre>
	 * (type &quot;name&quot;)
	 * </pre>
	 * 
	 * or
	 * 
	 * <pre>
	 * (type &quot;name&quot; &quot;actions&quot;)
	 * </pre>
	 * 
	 * where <i>name</i> and <i>actions</i> are strings that are encoded for
	 * proper parsing. Specifically, the <code>"</code>,<code>\</code>, carriage
	 * return, and linefeed characters are escaped using <code>\"</code>,
	 * <code>\\</code>,<code>\r</code>, and <code>\n</code>, respectively.
	 * 
	 * <p>
	 * The encoded string must contain no leading or trailing whitespace
	 * characters. A single space character must be used between <i>type</i> and 
	 * &quot;<i>name</i>&quot; and between &quot;<i>name</i>&quot; and &quot;<i>actions</i>&quot;.
	 * 
	 * @return The string encoding of this <code>PermissionInfo</code>.
	 */
	public final String getEncoded() {
		StringBuffer output = new StringBuffer(
				8
						+ type.length()
						+ ((((name == null) ? 0 : name.length()) + ((actions == null) ? 0
								: actions.length())) << 1));
		output.append('(');
		output.append(type);
		if (name != null) {
			output.append(" \"");
			encodeString(name, output);
			if (actions != null) {
				output.append("\" \"");
				encodeString(actions, output);
			}
			output.append('\"');
		}
		output.append(')');
		return (output.toString());
	}

	/**
	 * Returns the string representation of this <code>PermissionInfo</code>. The
	 * string is created by calling the <code>getEncoded</code> method on this
	 * <code>PermissionInfo</code>.
	 * 
	 * @return The string representation of this <code>PermissionInfo</code>.
	 */
	public String toString() {
		return (getEncoded());
	}

	/**
	 * Returns the fully qualified class name of the permission represented by
	 * this <code>PermissionInfo</code>.
	 * 
	 * @return The fully qualified class name of the permission represented by
	 *         this <code>PermissionInfo</code>.
	 */
	public final String getType() {
		return (type);
	}

	/**
	 * Returns the name of the permission represented by this
	 * <code>PermissionInfo</code>.
	 * 
	 * @return The name of the permission represented by this
	 *         <code>PermissionInfo</code>, or <code>null</code> if the permission
	 *         does not have a name.
	 */
	public final String getName() {
		return (name);
	}

	/**
	 * Returns the actions of the permission represented by this
	 * <code>PermissionInfo</code>.
	 * 
	 * @return The actions of the permission represented by this
	 *         <code>PermissionInfo</code>, or <code>null</code> if the permission
	 *         does not have any actions associated with it.
	 */
	public final String getActions() {
		return (actions);
	}

	/**
	 * Determines the equality of two <code>PermissionInfo</code> objects.
	 * 
	 * This method checks that specified object has the same type, name and
	 * actions as this <code>PermissionInfo</code> object.
	 * 
	 * @param obj The object to test for equality with this
	 *        <code>PermissionInfo</code> object.
	 * @return <code>true</code> if <code>obj</code> is a <code>PermissionInfo</code>,
	 *         and has the same type, name and actions as this
	 *         <code>PermissionInfo</code> object; <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return (true);
		}
		if (!(obj instanceof PermissionInfo)) {
			return (false);
		}
		PermissionInfo other = (PermissionInfo) obj;
		if (!type.equals(other.type) || ((name == null) ^ (other.name == null))
				|| ((actions == null) ^ (other.actions == null))) {
			return (false);
		}
		if (name != null) {
			if (actions != null) {
				return (name.equals(other.name) && actions
						.equals(other.actions));
			}
			else {
				return (name.equals(other.name));
			}
		}
		else {
			return (true);
		}
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		int hash = type.hashCode();
		if (name != null) {
			hash ^= name.hashCode();
			if (actions != null) {
				hash ^= actions.hashCode();
			}
		}
		return (hash);
	}

	/**
	 * This escapes the quotes, backslashes, \n, and \r in the string using a
	 * backslash and appends the newly escaped string to a StringBuffer.
	 */
	private static void encodeString(String str, StringBuffer output) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '"' :
				case '\\' :
					output.append('\\');
					output.append(c);
					break;
				case '\r' :
					output.append("\\r");
					break;
				case '\n' :
					output.append("\\n");
					break;
				default :
					output.append(c);
					break;
			}
		}
	}

	/**
	 * Takes an encoded character array and decodes it into a new String.
	 */
	private static String decodeString(char[] str, int begin, int end) {
		StringBuffer output = new StringBuffer(end - begin);
		for (int i = begin; i < end; i++) {
			char c = str[i];
			if (c == '\\') {
				i++;
				if (i < end) {
					c = str[i];
					if (c == 'n') {
						c = '\n';
					}
					else
						if (c == 'r') {
							c = '\r';
						}
				}
			}
			output.append(c);
		}
		return (output.toString());
	}
}