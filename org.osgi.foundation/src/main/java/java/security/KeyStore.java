/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/KeyStore.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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
public class KeyStore {
	protected KeyStore(java.security.KeyStoreSpi var0, java.security.Provider var1, java.lang.String var2) { }
	public final java.util.Enumeration aliases() throws java.security.KeyStoreException { return null; }
	public final boolean containsAlias(java.lang.String var0) throws java.security.KeyStoreException { return false; }
	public final void deleteEntry(java.lang.String var0) throws java.security.KeyStoreException { }
	public final java.security.cert.Certificate getCertificate(java.lang.String var0) throws java.security.KeyStoreException { return null; }
	public final java.lang.String getCertificateAlias(java.security.cert.Certificate var0) throws java.security.KeyStoreException { return null; }
	public final java.security.cert.Certificate[] getCertificateChain(java.lang.String var0) throws java.security.KeyStoreException { return null; }
	public final java.util.Date getCreationDate(java.lang.String var0) throws java.security.KeyStoreException { return null; }
	public static java.security.KeyStore getInstance(java.lang.String var0) throws java.security.KeyStoreException { return null; }
	public static java.security.KeyStore getInstance(java.lang.String var0, java.lang.String var1) throws java.security.KeyStoreException, java.security.NoSuchProviderException { return null; }
	public final java.security.Key getKey(java.lang.String var0, char[] var1) throws java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.UnrecoverableKeyException { return null; }
	public final java.security.Provider getProvider() { return null; }
	public final java.lang.String getType() { return null; }
	public final boolean isCertificateEntry(java.lang.String var0) throws java.security.KeyStoreException { return false; }
	public final boolean isKeyEntry(java.lang.String var0) throws java.security.KeyStoreException { return false; }
	public final void load(java.io.InputStream var0, char[] var1) throws java.io.IOException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException { }
	public final void setCertificateEntry(java.lang.String var0, java.security.cert.Certificate var1) throws java.security.KeyStoreException { }
	public final void setKeyEntry(java.lang.String var0, byte[] var1, java.security.cert.Certificate[] var2) throws java.security.KeyStoreException { }
	public final void setKeyEntry(java.lang.String var0, java.security.Key var1, char[] var2, java.security.cert.Certificate[] var3) throws java.security.KeyStoreException { }
	public final int size() throws java.security.KeyStoreException { return 0; }
	public final void store(java.io.OutputStream var0, char[] var1) throws java.security.KeyStoreException, java.io.IOException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException { }
	public final static java.lang.String getDefaultType() { return null; }
}

