/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/zip/Deflater.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public class Deflater {
	public int deflate(byte[] var0) { return 0; }
	public int deflate(byte[] var0, int var1, int var2) { return 0; }
	public void end() { }
	protected void finalize() { }
	public void finish() { }
	public boolean finished() { return false; }
	public int getAdler() { return 0; }
	public int getTotalIn() { return 0; }
	public int getTotalOut() { return 0; }
	public boolean needsInput() { return false; }
	public void reset() { }
	public void setDictionary(byte[] var0) { }
	public void setDictionary(byte[] var0, int var1, int var2) { }
	public void setInput(byte[] var0) { }
	public void setInput(byte[] var0, int var1, int var2) { }
	public void setLevel(int var0) { }
	public void setStrategy(int var0) { }
	public Deflater() { }
	public Deflater(int var0, boolean var1) { }
	public Deflater(int var0) { }
	public final static int BEST_COMPRESSION = 9;
	public final static int BEST_SPEED = 1;
	public final static int DEFAULT_COMPRESSION = -1;
	public final static int DEFAULT_STRATEGY = 0;
	public final static int DEFLATED = 8;
	public final static int FILTERED = 1;
	public final static int HUFFMAN_ONLY = 2;
	public final static int NO_COMPRESSION = 0;
}

