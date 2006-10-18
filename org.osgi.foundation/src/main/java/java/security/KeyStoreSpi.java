/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/KeyStoreSpi.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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
public abstract class KeyStoreSpi {
	public KeyStoreSpi() { }
	public abstract java.util.Enumeration engineAliases();
	public abstract boolean engineContainsAlias(java.lang.String var0);
	public abstract void engineDeleteEntry(java.lang.String var0) throws java.security.KeyStoreException;
	public abstract java.security.cert.Certificate engineGetCertificate(java.lang.String var0);
	public abstract java.lang.String engineGetCertificateAlias(java.security.cert.Certificate var0);
	public abstract java.security.cert.Certificate[] engineGetCertificateChain(java.lang.String var0);
	public abstract java.util.Date engineGetCreationDate(java.lang.String var0);
	public abstract java.security.Key engineGetKey(java.lang.String var0, char[] var1) throws java.security.NoSuchAlgorithmException, java.security.UnrecoverableKeyException;
	public abstract boolean engineIsCertificateEntry(java.lang.String var0);
	public abstract boolean engineIsKeyEntry(java.lang.String var0);
	public abstract void engineLoad(java.io.InputStream var0, char[] var1) throws java.io.IOException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException;
	public abstract void engineSetCertificateEntry(java.lang.String var0, java.security.cert.Certificate var1) throws java.security.KeyStoreException;
	public abstract void engineSetKeyEntry(java.lang.String var0, byte[] var1, java.security.cert.Certificate[] var2) throws java.security.KeyStoreException;
	public abstract void engineSetKeyEntry(java.lang.String var0, java.security.Key var1, char[] var2, java.security.cert.Certificate[] var3) throws java.security.KeyStoreException;
	public abstract int engineSize();
	public abstract void engineStore(java.io.OutputStream var0, char[] var1) throws java.io.IOException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException;
}

