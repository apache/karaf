/*
 * $Header: /cvshome/build/ee.foundation/src/java/io/ObjectStreamConstants.java,v 1.6 2006/03/14 01:20:23 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
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

package java.io;
public abstract interface ObjectStreamConstants {
	public final static short STREAM_MAGIC = -21267;
	public final static short STREAM_VERSION = 5;
	public final static byte TC_BASE = 112;
	public final static byte TC_NULL = 112;
	public final static byte TC_REFERENCE = 113;
	public final static byte TC_CLASSDESC = 114;
	public final static byte TC_OBJECT = 115;
	public final static byte TC_STRING = 116;
	public final static byte TC_ARRAY = 117;
	public final static byte TC_CLASS = 118;
	public final static byte TC_BLOCKDATA = 119;
	public final static byte TC_ENDBLOCKDATA = 120;
	public final static byte TC_RESET = 121;
	public final static byte TC_BLOCKDATALONG = 122;
	public final static byte TC_EXCEPTION = 123;
	public final static byte TC_LONGSTRING = 124;
	public final static byte TC_PROXYCLASSDESC = 125;
	public final static byte TC_MAX = 125;
	public final static int baseWireHandle = 8257536;
	public final static int PROTOCOL_VERSION_1 = 1;
	public final static int PROTOCOL_VERSION_2 = 2;
	public final static java.io.SerializablePermission SUBCLASS_IMPLEMENTATION_PERMISSION = null;
	public final static java.io.SerializablePermission SUBSTITUTION_PERMISSION = null;
	public final static byte SC_WRITE_METHOD = 1;
	public final static byte SC_SERIALIZABLE = 2;
	public final static byte SC_EXTERNALIZABLE = 4;
	public final static byte SC_BLOCK_DATA = 8;
}

