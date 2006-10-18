/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/AlgorithmParameterGenerator.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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
public class AlgorithmParameterGenerator {
	protected AlgorithmParameterGenerator(java.security.AlgorithmParameterGeneratorSpi var0, java.security.Provider var1, java.lang.String var2) { }
	public final java.security.AlgorithmParameters generateParameters() { return null; }
	public final java.lang.String getAlgorithm() { return null; }
	public static java.security.AlgorithmParameterGenerator getInstance(java.lang.String var0) throws java.security.NoSuchAlgorithmException { return null; }
	public static java.security.AlgorithmParameterGenerator getInstance(java.lang.String var0, java.lang.String var1) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException { return null; }
	public final java.security.Provider getProvider() { return null; }
	public final void init(int var0) { }
	public final void init(int var0, java.security.SecureRandom var1) { }
	public final void init(java.security.spec.AlgorithmParameterSpec var0) throws java.security.InvalidAlgorithmParameterException { }
	public final void init(java.security.spec.AlgorithmParameterSpec var0, java.security.SecureRandom var1) throws java.security.InvalidAlgorithmParameterException { }
}

