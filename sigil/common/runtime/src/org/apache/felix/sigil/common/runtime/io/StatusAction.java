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

package org.apache.felix.sigil.common.runtime.io;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

import static org.apache.felix.sigil.common.runtime.io.Constants.STATUS;
import static org.osgi.framework.Constants.BUNDLE_VERSION;


/**
 * @author dave
 *
 */
public class StatusAction extends Action<Void, Map<Long, String>>
{

    public StatusAction( InputStream in, OutputStream out ) throws IOException
    {
        super( in, out );
    }


    @Override
    public Map<Long, String> client( Void in ) throws IOException
    {
        writeInt(STATUS);
        int num = readInt();
        HashMap<Long, String> map = new HashMap<Long, String>(num);
        
        for (int i = 0; i < num; i++) {
            long id = readLong();
            String symbol = readString();
            map.put( id, symbol );
        }
        
        return map;
    }


    @Override
    public void server( Framework fw ) throws IOException
    {
        Bundle[] bundles = fw.getBundleContext().getBundles();
        writeInt( bundles.length );
        for ( Bundle b : bundles ) {
            writeLong(b.getBundleId());
            String symbol = b.getSymbolicName() + ":" + b.getHeaders().get( BUNDLE_VERSION );
            writeString(symbol);
        }
    }

}
