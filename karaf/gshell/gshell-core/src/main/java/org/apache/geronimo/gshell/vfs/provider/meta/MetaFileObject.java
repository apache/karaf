/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.gshell.vfs.provider.meta;

import org.apache.commons.vfs.FileContentInfoFactory;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.provider.DefaultFileContent;
import org.apache.geronimo.gshell.vfs.provider.meta.data.MetaData;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Meta file object.
 *
 * TODO: remove this file when gshell version is upgraded
 *
 * @version $Rev: 706033 $ $Date: 2008-10-19 17:36:15 +0200 (Sun, 19 Oct 2008) $
 */
public class MetaFileObject
    extends AbstractFileObject
{
    private final MetaFileSystem fileSystem;

    private MetaData data;
    private FileContent content;

    public MetaFileObject(final FileName fileName, final MetaFileSystem fileSystem) {
        super(fileName, fileSystem);

        // Save for uncasted typed access
        this.fileSystem = fileSystem;
    }

    public MetaData getData() {
        if (data == null) {
            throw new IllegalStateException("Meta data has not been attached");
        }

        return data;
    }

    @Override
    protected FileType doGetType() throws Exception {
        return getData().getType();
    }

    @Override
    protected long doGetLastModifiedTime() throws Exception {
        return getData().getLastModified();
    }

    @Override
    protected boolean doIsReadable() throws Exception {
        return data.getBuffer() != null;
    }

    @Override
    protected boolean doIsWriteable() throws Exception {
        return false;
    }

    @Override
    protected FileContentInfoFactory getFileContentInfoFactory() {
        return fileSystem.getFileContentInfoFactory();
    }

    @Override
    protected long doGetContentSize() throws Exception {
        byte[] bytes = data.getBuffer();
        return bytes != null ? bytes.length : 0;
    }

    @Override
    protected InputStream doGetInputStream() throws Exception {
        byte[] bytes = data.getBuffer();
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }

        return null;
    }

    @Override
    protected Map<String,Object> doGetAttributes() {
        return getData().getAttributes();
    }

    @Override
    protected void doSetAttribute(final String name, final Object value) {
        getData().getAttributes().put(name, value);
    }

    protected void doRemoveAttribute(final String name) {
        getData().getAttributes().remove(name);
    }

    @Override
    protected String[] doListChildren() throws Exception {
        return fileSystem.listChildren(getName());
    }

    @Override
    protected void doAttach() throws Exception {
        if (data == null) {
            data = fileSystem.lookupData(this);
        }
        content = new DefaultFileContent(this, getFileContentInfoFactory());
    }

    @Override
    protected void doDetach() throws Exception {
        data = null;
        content = null;
    }

    @Override
    public FileContent getContent() throws FileSystemException {
        super.getContent();
        return content;
    }
}