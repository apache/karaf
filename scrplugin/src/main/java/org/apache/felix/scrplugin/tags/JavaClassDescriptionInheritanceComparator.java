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
package org.apache.felix.scrplugin.tags;


import java.util.Comparator;

import org.apache.maven.plugin.MojoExecutionException;


/**
 * The <code>JavaClassDescriptionInheritanceComparator</code> orders
 * {@link JavaClassDescription} objects by their inheritance:
 * <ol>
 * <li>If the descriptors are the same, zero is returned
 * <li>If the first descriptor is an extension of the second, 1 is returned
 * <li>If the second descriptor is an extension of the first, -1 is returned
 * <li>Otherwise if the first descriptor is nested deeper in the hierarchy 1 is
 * returned, else if the second descriptor is nested deeper in the hierarchy -1
 * is returned.
 * <li>Finally, the natural order of the class names is returned
 * </ol>
 */
public class JavaClassDescriptionInheritanceComparator implements Comparator
{

    public int compare( Object o1, Object o2 )
    {
        JavaClassDescription cd1 = ( JavaClassDescription ) o1;
        JavaClassDescription cd2 = ( JavaClassDescription ) o2;

        // the descriptors are the same
        if ( equals( cd1, cd2 ) )
        {
            return 0;
        }

        try
        {

            int level1 = 0;
            int level2 = 0;

            // if cd1 is an extension of cd2
            JavaClassDescription super1 = cd1.getSuperClass();
            while ( super1 != null )
            {
                if ( equals( super1, cd2 ) )
                {
                    return 1;
                }
                super1 = super1.getSuperClass();
                level1++;
            }

            // if cd2 is an extension of cd1
            JavaClassDescription super2 = cd2.getSuperClass();
            while ( super2 != null )
            {
                if ( equals( super2, cd1 ) )
                {
                    return -1;
                }
                super2 = super2.getSuperClass();
                level2++;
            }

            // class do not share the hierarchy, order by hierarchy level
            if ( level1 < level2 )
            {
                return -1;
            }
            else if ( level1 > level2 )
            {
                return 1;
            }
        }
        catch ( MojoExecutionException mee )
        {
            // what shall we do ??
        }

        // last ressort: order by class name
        return cd1.getName().compareTo( cd2.getName() );
    }


    // compare for equality: returns true if both descriptors have the same name
    private boolean equals( JavaClassDescription cd1, JavaClassDescription cd2 )
    {
        return cd1.getName().equals( cd2.getName() );
    }
}
