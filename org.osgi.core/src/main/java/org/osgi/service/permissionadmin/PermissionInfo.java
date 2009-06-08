/*
 * Copyright (c) OSGi Alliance (2001, 2009). All Rights Reserved.
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

package org.osgi.service.permissionadmin;

/**
 * Permission representation used by the Permission Admin service.
 * 
 * <p>
 * This class encapsulates three pieces of information: a Permission <i>type
 * </i> (class name), which must be a subclass of
 * <code>java.security.Permission</code>, and the <i>name</i> and <i>actions</i>
 * arguments passed to its constructor.
 * 
 * <p>
 * In order for a permission represented by a <code>PermissionInfo</code> to be
 * instantiated and considered during a permission check, its Permission class
 * must be available from the system classpath or an exported package. This
 * means that the instantiation of a permission represented by a
 * <code>PermissionInfo</code> may be delayed until the package containing its
 * Permission class has been exported by a bundle.
 * 
 * @Immutable
 * @version $Revision: 6492 $
 */
public class PermissionInfo {
	private final String	type;
	private final String	name;
	private final String	actions;

	/**
	 * Constructs a <code>PermissionInfo</code> from the specified type, name,
	 * and actions.
	 * 
	 * @param type The fully qualified class name of the permission represented
	 *        by this <code>PermissionInfo</code>. The class must be a subclass
	 *        of <code>java.security.Permission</code> and must define a
	 *        2-argument constructor that takes a <i>name</i> string and an
	 *        <i>actions</i> string.
	 * 
	 * @param name The permission name that will be passed as the first argument
	 *        to the constructor of the <code>Permission</code> class identified
	 *        by <code>type</code>.
	 * 
	 * @param actions The permission actions that will be passed as the second
	 *        argument to the constructor of the <code>Permission</code> class
	 *        identified by <code>type</code>.
	 * 
	 * @throws NullPointerException If <code>type</code> is <code>null</code>.
	 * @throws IllegalArgumentException If <code>action</code> is not
	 *         <code>null</code> and <code>name</code> is <code>null</code>.
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
	 * Constructs a <code>PermissionInfo</code> object from the specified
	 * encoded <code>PermissionInfo</code> string. White space in the encoded
	 * <code>PermissionInfo</code> string is ignored.
	 * 
	 * 
	 * @param encodedPermission The encoded <code>PermissionInfo</code>.
	 * @see #getEncoded
	 * @throws IllegalArgumentException If the specified
	 *         <code>encodedPermission</code> is not properly formatted.
	 */
	public PermissionInfo(String encodedPermission) {
		if (encodedPermission == null) {
			throw new NullPointerException("missing encoded permission");
		}
		if (encodedPermission.length() == 0) {
			throw new IllegalArgumentException("empty encoded permission");
		}
		String parsedType = null;
		String parsedName = null;
		String parsedActions = null;
		try {
			char[] encoded = encodedPermission.toCharArray();
			int length = encoded.length;
			int pos = 0;

			/* skip whitespace */
			while (Character.isWhitespace(encoded[pos])) {
				pos++;
			}

			/* the first character must be '(' */
			if (encoded[pos] != '(') {
				throw new IllegalArgumentException("expecting open parenthesis");
			}
			pos++;

			/* skip whitespace */
			while (Character.isWhitespace(encoded[pos])) {
				pos++;
			}

			/* type is not quoted or encoded */
			int begin = pos;
			while (!Character.isWhitespace(encoded[pos])
					&& (encoded[pos] != ')')) {
				pos++;
			}
			if (pos == begin || encoded[begin] == '"') {
				throw new IllegalArgumentException("expecting type");
			}
			parsedType = new String(encoded, begin, pos - begin);

			/* skip whitespace */
			while (Character.isWhitespace(encoded[pos])) {
				pos++;
			}

			/* type may be followed by name which is quoted and encoded */
			if (encoded[pos] == '"') {
				pos++;
				begin = pos;
				while (encoded[pos] != '"') {
					if (encoded[pos] == '\\') {
						pos++;
					}
					pos++;
				}
				parsedName = unescapeString(encoded, begin, pos);
				pos++;

				if (Character.isWhitespace(encoded[pos])) {
					/* skip whitespace */
					while (Character.isWhitespace(encoded[pos])) {
						pos++;
					}

					/*
					 * name may be followed by actions which is quoted and
					 * encoded
					 */
					if (encoded[pos] == '"') {
						pos++;
						begin = pos;
						while (encoded[pos] != '"') {
							if (encoded[pos] == '\\') {
								pos++;
							}
							pos++;
						}
						parsedActions = unescapeString(encoded, begin, pos);
						pos++;

						/* skip whitespace */
						while (Character.isWhitespace(encoded[pos])) {
							pos++;
						}
					}
				}
			}

			/* the final character must be ')' */
			char c = encoded[pos];
			pos++;
			while ((pos < length) && Character.isWhitespace(encoded[pos])) {
				pos++;
			}
			if ((c != ')') || (pos != length)) {
				throw new IllegalArgumentException(
						"expecting close parenthesis");
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("parsing terminated abruptly");
		}

		type = parsedType;
		name = parsedName;
		actions = parsedActions;
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
	 * where <i>name</i> and <i>actions</i> are strings that must be encoded for
	 * proper parsing. Specifically, the <code>&quot;</code>,<code>\</code>,
	 * carriage return, and line feed characters must be escaped using
	 * <code>\&quot;</code>, <code>\\</code>,<code>\r</code>, and
	 * <code>\n</code>, respectively.
	 * 
	 * <p>
	 * The encoded string contains no leading or trailing whitespace characters.
	 * A single space character is used between <i>type</i> and
	 * &quot;<i>name</i>&quot; and between &quot;<i>name</i>&quot; and
	 * &quot;<i>actions</i>&quot;.
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
			escapeString(name, output);
			if (actions != null) {
				output.append("\" \"");
				escapeString(actions, output);
			}
			output.append('\"');
		}
		output.append(')');
		return output.toString();
	}

	/**
	 * Returns the string representation of this <code>PermissionInfo</code>.
	 * The string is created by calling the <code>getEncoded</code> method on
	 * this <code>PermissionInfo</code>.
	 * 
	 * @return The string representation of this <code>PermissionInfo</code>.
	 */
	public String toString() {
		return getEncoded();
	}

	/**
	 * Returns the fully qualified class name of the permission represented by
	 * this <code>PermissionInfo</code>.
	 * 
	 * @return The fully qualified class name of the permission represented by
	 *         this <code>PermissionInfo</code>.
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Returns the name of the permission represented by this
	 * <code>PermissionInfo</code>.
	 * 
	 * @return The name of the permission represented by this
	 *         <code>PermissionInfo</code>, or <code>null</code> if the
	 *         permission does not have a name.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Returns the actions of the permission represented by this
	 * <code>PermissionInfo</code>.
	 * 
	 * @return The actions of the permission represented by this
	 *         <code>PermissionInfo</code>, or <code>null</code> if the
	 *         permission does not have any actions associated with it.
	 */
	public final String getActions() {
		return actions;
	}

	/**
	 * Determines the equality of two <code>PermissionInfo</code> objects.
	 * 
	 * This method checks that specified object has the same type, name and
	 * actions as this <code>PermissionInfo</code> object.
	 * 
	 * @param obj The object to test for equality with this
	 *        <code>PermissionInfo</code> object.
	 * @return <code>true</code> if <code>obj</code> is a
	 *         <code>PermissionInfo</code>, and has the same type, name and
	 *         actions as this <code>PermissionInfo</code> object;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PermissionInfo)) {
			return false;
		}
		PermissionInfo other = (PermissionInfo) obj;
		if (!type.equals(other.type) || ((name == null) ^ (other.name == null))
				|| ((actions == null) ^ (other.actions == null))) {
			return false;
		}
		if (name != null) {
			if (actions != null) {
				return name.equals(other.name) && actions.equals(other.actions);
			}
			else {
				return name.equals(other.name);
			}
		}
		else {
			return true;
		}
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		int h = 31 * 17 + type.hashCode();
		if (name != null) {
			h = 31 * h + name.hashCode();
			if (actions != null) {
				h = 31 * h + actions.hashCode();
			}
		}
		return h;
	}

	/**
	 * This escapes the quotes, backslashes, \n, and \r in the string using a
	 * backslash and appends the newly escaped string to a StringBuffer.
	 */
	private static void escapeString(String str, StringBuffer output) {
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
	private static String unescapeString(char[] str, int begin, int end) {
		StringBuffer output = new StringBuffer(end - begin);
		for (int i = begin; i < end; i++) {
			char c = str[i];
			if (c == '\\') {
				i++;
				if (i < end) {
					c = str[i];
					switch (c) {
						case '"' :
						case '\\' :
							break;
						case 'r' :
							c = '\r';
							break;
						case 'n' :
							c = '\n';
							break;
						default :
							c = '\\';
							i--;
							break;
					}
				}
			}
			output.append(c);
		}

		return output.toString();
	}
}
