/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/SignedObject.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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
public final class SignedObject implements java.io.Serializable {
	public SignedObject(java.io.Serializable var0, java.security.PrivateKey var1, java.security.Signature var2) throws java.io.IOException, java.security.InvalidKeyException, java.security.SignatureException { }
	public java.lang.String getAlgorithm() { return null; }
	public byte[] getSignature() { return null; }
	public boolean verify(java.security.PublicKey var0, java.security.Signature var1) throws java.security.InvalidKeyException, java.security.SignatureException { return false; }
	public java.lang.Object getObject() throws java.io.IOException, java.lang.ClassNotFoundException { return null; }
}

