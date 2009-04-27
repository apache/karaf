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
package org.apache.servicemix.kernel.gshell.core;

import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.ShellContextHolder;

public class ShellWrapper implements Shell {

    private Shell delegate;

    public ShellWrapper(Shell delegate) {
        this.delegate = delegate;
    }

    public ShellContext getContext() {
        return delegate.getContext();
    }

    public Object execute(String s) throws Exception {
        ShellContext ctx = ShellContextHolder.get(true);
        try {
            ShellContextHolder.set(getContext());
            return delegate.execute(s);
        } finally {
            ShellContextHolder.set(ctx);
        }
    }

    public Object execute(String s, Object[] objects) throws Exception {
        ShellContext ctx = ShellContextHolder.get(true);
        try {
            ShellContextHolder.set(getContext());
            return delegate.execute(s, objects);
        } finally {
            ShellContextHolder.set(ctx);
        }
    }

    public Object execute(Object... objects) throws Exception {
        ShellContext ctx = ShellContextHolder.get(true);
        try {
            ShellContextHolder.set(getContext());
            return delegate.execute(objects);
        } finally {
            ShellContextHolder.set(ctx);
        }
    }

    public boolean isOpened() {
        return delegate.isOpened();
    }

    public void close() {
        delegate.close();
    }

    public boolean isInteractive() {
        return delegate.isInteractive();
    }

    public void run(Object... objects) throws Exception {
        ShellContext ctx = ShellContextHolder.get(true);
        try {
            ShellContextHolder.set(getContext());
            delegate.run(objects);
        } finally {
            ShellContextHolder.set(ctx);
        }
    }
}
