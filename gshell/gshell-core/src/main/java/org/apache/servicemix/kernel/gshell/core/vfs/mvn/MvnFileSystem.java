package org.apache.servicemix.kernel.gshell.core.vfs.mvn;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.provider.url.UrlFileSystem;

public class MvnFileSystem extends UrlFileSystem {

    protected MvnFileSystem(FileName fileName, FileSystemOptions fileSystemOptions) {
        super(fileName, fileSystemOptions);
    }

    protected FileObject createFile(FileName fileName) {
        return new MvnFileObject(this, fileName);
    }
}
