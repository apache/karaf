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

package org.apache.felix.sigil.core.repository;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.ModelElementFactoryException;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


public class DirectoryHelper
{
    public static void scanBundles( AbstractBundleRepository repository, List<ISigilBundle> bundles, IPath path,
        IPath source, boolean recursive )
    {
        File dir = path.toFile();

        if ( dir.exists() )
        {
            for ( File f : dir.listFiles() )
            {
                if ( f.isDirectory() )
                {
                    if ( recursive )
                    {
                        scanBundles( repository, bundles, new Path( f.getAbsolutePath() ), source, recursive );
                    }
                }
                else if ( f.isFile() && f.getName().endsWith( ".jar" ) )
                {
                    JarFile jar = null;
                    try
                    {
                        jar = new JarFile( f, false );
                        ISigilBundle bundle = buildBundle( repository, jar.getManifest(), f );
                        if ( bundle != null )
                        {
                            bundle.setSourcePathLocation( source );
                            bundle.setSourceRootPath( new Path( "src" ) );
                            bundles.add( bundle );
                        }
                    }
                    catch ( IOException e )
                    {
                        BldCore.error( "Failed to read jar file " + f, e );
                    }
                    catch ( ModelElementFactoryException e )
                    {
                        BldCore.error( "Failed to build bundle " + f, e );
                    }
                    catch ( RuntimeException e )
                    {
                        BldCore.error( "Failed to build bundle " + f, e );
                    }
                    finally
                    {
                        if ( jar != null )
                        {
                            try
                            {
                                jar.close();
                            }
                            catch ( IOException e )
                            {
                                BldCore.error( "Failed to close jar file", e );
                            }
                        }
                    }
                }
            }
        }
    }


    private static ISigilBundle buildBundle( AbstractBundleRepository repository, Manifest manifest, File f )
    {
        IBundleModelElement info = repository.buildBundleModelElement( manifest );

        ISigilBundle bundle = null;

        if ( info != null )
        {
            bundle = ModelElementFactory.getInstance().newModelElement( ISigilBundle.class );
            bundle.addChild( info );
            bundle.setLocation( new Path( f.getAbsolutePath() ) );
        }

        return bundle;
    }

}
