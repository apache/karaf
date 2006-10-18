/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/zip/ZipConstants.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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

package java.util.zip;
abstract interface ZipConstants {
	public final static long LOCSIG = 67324752l;
	public final static long EXTSIG = 134695760l;
	public final static long CENSIG = 33639248l;
	public final static long ENDSIG = 101010256l;
	public final static int LOCHDR = 30;
	public final static int EXTHDR = 16;
	public final static int CENHDR = 46;
	public final static int ENDHDR = 22;
	public final static int LOCVER = 4;
	public final static int LOCFLG = 6;
	public final static int LOCHOW = 8;
	public final static int LOCTIM = 10;
	public final static int LOCCRC = 14;
	public final static int LOCSIZ = 18;
	public final static int LOCLEN = 22;
	public final static int LOCNAM = 26;
	public final static int LOCEXT = 28;
	public final static int EXTCRC = 4;
	public final static int EXTSIZ = 8;
	public final static int EXTLEN = 12;
	public final static int CENVEM = 4;
	public final static int CENVER = 6;
	public final static int CENFLG = 8;
	public final static int CENHOW = 10;
	public final static int CENTIM = 12;
	public final static int CENCRC = 16;
	public final static int CENSIZ = 20;
	public final static int CENLEN = 24;
	public final static int CENNAM = 28;
	public final static int CENEXT = 30;
	public final static int CENCOM = 32;
	public final static int CENDSK = 34;
	public final static int CENATT = 36;
	public final static int CENATX = 38;
	public final static int CENOFF = 42;
	public final static int ENDSUB = 8;
	public final static int ENDTOT = 10;
	public final static int ENDSIZ = 12;
	public final static int ENDOFF = 16;
	public final static int ENDCOM = 20;
}

