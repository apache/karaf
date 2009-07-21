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

package org.apache.felix.sigil.utils;


import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


public class PathHelper
{

    public static void scanFiles( List<IPath> paths, IPath path, String pattern, boolean recurse )
    {
        Pattern p = GlobCompiler.compile( pattern );

        for ( File f : path.toFile().listFiles() )
        {
            if ( f.isDirectory() && recurse )
            {
                scanFiles( paths, new Path( f.getAbsolutePath() ), pattern, recurse );
            }
            else if ( f.isFile() && p.matcher( f.getName() ).matches() )
            {
                paths.add( new Path( f.getAbsolutePath() ) );
            }
        }
    }
}
