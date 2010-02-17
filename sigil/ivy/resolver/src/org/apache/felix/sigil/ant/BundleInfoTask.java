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

package org.apache.felix.sigil.ant;


import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


public class BundleInfoTask extends Task
{
    private File bundle;
    private String header;
    private String property;
    private String defaultValue;


    @Override
    public void execute() throws BuildException
    {
        if ( bundle == null )
            throw new BuildException( "missing attribute: bundle" );
        if ( header == null )
            throw new BuildException( "missing attribute: header" );

        JarFile jar = null;
        
        try
        {
            jar = new JarFile( bundle, false );
            Manifest mf = jar.getManifest();
            String value = mf.getMainAttributes().getValue( header );
            if ( property == null )
            {
                log( header + "=" + value );
            }
            else
            {
                if ( "Bundle-SymbolicName".equals( header ) && value != null )
                {
                    // remove singleton flag
                    int semi = value.indexOf( ';' );
                    if ( semi > 0 )
                        value = value.substring( 0, semi );
                }
                if ( value == null )
                {
                    value = defaultValue;
                }
                if ( value != null )
                {
                    getProject().setNewProperty( property, value );
                }
            }
        }
        catch ( IOException e )
        {
            throw new BuildException( "Failed to access bundle", e );
        }
        finally {
            if ( jar != null ) {
                try
                {
                    jar.close();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    public void setBundle( String bundle )
    {
        this.bundle = new File( bundle );
    }


    public void setHeader( String header )
    {
        this.header = header;
    }


    public void setProperty( String property )
    {
        this.property = property;
    }


    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }
}
