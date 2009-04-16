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
package org.apache.felix.das;


import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 * Utility class for injecting objects and invoking
 * methods that are normally invoked by the dependency manager.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class Utils
{

    public static void invoke( Object target, String method )
    {
        try
        {
            Method m = target.getClass().getDeclaredMethod( method, new Class[0] );
            m.setAccessible( true );
            m.invoke( target, new Object[0] );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            junit.framework.Assert.fail( e.getMessage() );
        }
    }


    public static void inject( Object target, Class<?> clazz, Object injectable )
    {

        Field[] fields = target.getClass().getDeclaredFields();

        for ( Field field : fields )
        {
            if ( clazz == field.getType() )
            {
                field.setAccessible( true );
                try
                {
                    field.set( target, injectable );
                }
                catch ( IllegalArgumentException e )
                {
                    e.printStackTrace();
                }
                catch ( IllegalAccessException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }

}
