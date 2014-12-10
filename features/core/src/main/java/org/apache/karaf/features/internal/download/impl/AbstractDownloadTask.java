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
package org.apache.karaf.features.internal.download.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.StreamProvider;

public abstract class AbstractDownloadTask extends DefaultFuture<AbstractDownloadTask> implements Runnable, StreamProvider {

    protected final String url;
    protected ScheduledExecutorService executorService;

    public AbstractDownloadTask(ScheduledExecutorService executorService, String url) {
        this.executorService = executorService;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public File getFile() throws IOException {
        Object v = getValue();
        if (v instanceof File) {
            return (File) v;
        } else if (v instanceof IOException) {
            throw (IOException) v;
        } else {
            return null;
        }
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(getFile());
    }

    public void setFile(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        setValue(file);
    }

    public void setException(IOException exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }
        setValue(exception);
    }

}
