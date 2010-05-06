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
package org.apache.felix.gogo.runtime.osgi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

public class Procedural
{
    public String tac() throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            sw.write(s);
            s = rdr.readLine();
        }
        return sw.toString();
    }
    
    public void each(CommandSession session, Collection<Object> list, Function closure)
        throws Exception
    {
        List<Object> args = new ArrayList<Object>();
        args.add(null);
        for (Object x : list)
        {
            args.set(0, x);
            closure.execute(session, args);
        }
    }

    public Object _if(CommandSession session, Function condition, Function ifTrue,
        Function ifFalse) throws Exception
    {
        Object result = condition.execute(session, null);
        if (isTrue(result))
        {
            return ifTrue.execute(session, null);
        }
        else
        {
            if (ifFalse != null)
            {
                return ifFalse.execute(session, null);
            }
        }
        return null;
    }

    public Object _new(String name, Bundle bundle) throws Exception
    {
        if (bundle == null)
        {
            return Class.forName(name).newInstance();
        }
        else
        {
            return bundle.loadClass(name).newInstance();
        }
    }

    private boolean isTrue(Object result)
    {
        if (result == null)
        {
            return false;
        }

        if (result instanceof String && ((String) result).equals(""))
        {
            return false;
        }

        if (result instanceof Boolean)
        {
            return ((Boolean) result).booleanValue();
        }

        return true;
    }
}
