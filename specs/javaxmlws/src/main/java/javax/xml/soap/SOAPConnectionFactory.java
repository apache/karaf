/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.xml.soap;

public abstract class SOAPConnectionFactory {

    private static final String DEFAULT_SOAP_CONNECTION_FACTORY = "com.sun.xml.internal.messaging.saaj.client.p2p.HttpSOAPConnectionFactory";

    public static SOAPConnectionFactory newInstance() throws SOAPException, UnsupportedOperationException {
        try {
            return $FactoryFinder.find(SOAPConnectionFactory.class, DEFAULT_SOAP_CONNECTION_FACTORY, true);
        } catch (Exception ex) {
            throw new SOAPException("Unable to create SOAP connection factory: " + ex.getMessage(), ex);
        }
    }

    public abstract SOAPConnection createConnection() throws SOAPException;

}
