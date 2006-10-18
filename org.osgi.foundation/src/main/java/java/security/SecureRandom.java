/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/SecureRandom.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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

package java.security;
public class SecureRandom extends java.util.Random {
	public SecureRandom() { }
	public SecureRandom(byte[] var0) { }
	protected SecureRandom(java.security.SecureRandomSpi var0, java.security.Provider var1) { }
	public byte[] generateSeed(int var0) { return null; }
	public static java.security.SecureRandom getInstance(java.lang.String var0) throws java.security.NoSuchAlgorithmException { return null; }
	public static java.security.SecureRandom getInstance(java.lang.String var0, java.lang.String var1) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException { return null; }
	public final java.security.Provider getProvider() { return null; }
	public static byte[] getSeed(int var0) { return null; }
	protected final int next(int var0) { return 0; }
	public void nextBytes(byte[] var0) { }
	public void setSeed(byte[] var0) { }
	public void setSeed(long var0) { }
}

