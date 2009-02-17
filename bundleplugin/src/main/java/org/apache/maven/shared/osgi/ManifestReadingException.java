/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.osgi;

/**
 * Exception while reading the manifest. Encapsulates an IOException to make it runtime
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id: ManifestReadingException.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public class ManifestReadingException
    extends RuntimeException
{

    public ManifestReadingException()
    {
        super();
    }

    public ManifestReadingException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ManifestReadingException( String message )
    {
        super( message );
    }

    public ManifestReadingException( Throwable cause )
    {
        super( cause );
    }
}
