/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.daemon;


import java.io.File;
import org.apache.directory.daemon.InstallationLayout;


/**
 * A felix specific installation layout.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FelixLayout extends InstallationLayout
{
    public FelixLayout( InstallationLayout layout )
    {
        super( layout.getBaseDirectory() );
    }


    public FelixLayout( File baseDirectory )
    {
        super( baseDirectory );
    }


    public FelixLayout( String baseDirectoryPath )
    {
        super( baseDirectoryPath );
    }


    public File getBundleDirectory()
    {
        return new File( super.baseDirectory, "bundle" );
    }
    
    
    public File getConfigurationFile()
    {
        return super.getConfigurationFile( "config.properties" );
    }
    

    public File getSystemPropertiesFile()
    {
        return new File( super.baseDirectory, "system.properties" );
    }
}
