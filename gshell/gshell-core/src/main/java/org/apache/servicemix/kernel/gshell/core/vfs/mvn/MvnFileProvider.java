package org.apache.servicemix.kernel.gshell.core.vfs.mvn;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.provider.url.UrlFileProvider;

public class MvnFileProvider extends UrlFileProvider {

    /**
     * Locates a file object, by absolute URI.
     */
    public synchronized FileObject findFile(final FileObject baseFile,
                                            final String uri,
                                            final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        try
        {
            final URL url = new URL(uri);

            URL rootUrl = new URL(url, "/");
            final String key = this.getClass().getName() + rootUrl.toString();
            FileSystem fs = findFileSystem(key, fileSystemOptions);
            if (fs == null)
            {
                String extForm = rootUrl.toExternalForm();
                final FileName rootName = getContext().parseURI(extForm);
                // final FileName rootName =
                //    new BasicFileName(rootUrl, FileName.ROOT_PATH);
                fs = new MvnFileSystem(rootName, fileSystemOptions);
                addFileSystem(key, fs);
            }
            return fs.resolveFile(url.getPath());
        }
        catch (final MalformedURLException e)
        {
            throw new FileSystemException("vfs.provider.url/badly-formed-uri.error", uri, e);
        }
    }


}
