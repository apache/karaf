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


import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.felix.sigil.common.runtime.BundleForm.BundleStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

import static org.apache.felix.sigil.common.runtime.io.Constants.STATUS;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.BUNDLE_NAME;


/**
 * @author dave
 *
 */
public class StatusAction extends Action<Void, BundleStatus[]>
{

    public StatusAction( DataInputStream in, DataOutputStream out ) throws IOException
    {
        super( in, out );
    }


    @Override
    public BundleStatus[] client( Void in ) throws IOException
    {
        writeInt(STATUS);
        flush();
        int num = readInt();
        ArrayList<BundleStatus> ret = new ArrayList<BundleStatus>(num);
        
        for (int i = 0; i < num; i++) {
            BundleStatus s = new BundleStatus();
            s.setId(readLong());
            s.setBundleSymbolicName(readString());
            s.setVersion(readString());
            s.setLocation(readString());
            s.setStatus(readInt());
            ret.add(s);
        }
        
        return ret.toArray(new BundleStatus[num]);
    }


    @Override
    public void server( Framework fw ) throws IOException
    {
        log( "Read status" );
        Bundle[] bundles = fw.getBundleContext().getBundles();
        writeInt( bundles.length );
        for ( Bundle b : bundles ) {
            writeLong(b.getBundleId());
            String bsn = b.getSymbolicName();
            if ( bsn == null )
                bsn = (String) b.getHeaders().get(BUNDLE_NAME);
            
            writeString(bsn);
            writeString((String) b.getHeaders().get( BUNDLE_VERSION ));
            writeString(b.getLocation());
            writeInt(b.getState());
            flush();
        }
        flush();        
    }
}
