/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.gshell.osgi;

import java.io.PrintWriter;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 3:32:15 PM
 * To change this template use File | Settings | File Templates.
 */
@CommandComponent(id="osgi:install", description="Install bundle")
public class InstallBundle extends OsgiCommandSupport {

    @Argument(required = true, multiValued = true, description = "Bundle URLs")
    List<String> urls;

    protected Object doExecute() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (String url : urls) {
            Bundle bundle = install(url, io.out, io.err);
            if (bundle != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(bundle.getBundleId());
            }
        }
        if (sb.toString().indexOf(',') > 0) {
            io.out.println("Bundle IDs: " + sb.toString());
        } else if (sb.length() > 0) {
            io.out.println("Bundle ID: " + sb.toString());
        }
        return null;
    }

    protected Bundle install(String location, PrintWriter out, PrintWriter err) {
        try {
            return getBundleContext().installBundle(location, null);
        } catch (IllegalStateException ex) {
            err.println(ex.toString());
        } catch (BundleException ex) {
            if (ex.getNestedException() != null) {
                err.println(ex.getNestedException().toString());
            } else {
                err.println(ex.toString());
            }
        } catch (Exception ex) {
            err.println(ex.toString());
        }
        return null;
    }

}
