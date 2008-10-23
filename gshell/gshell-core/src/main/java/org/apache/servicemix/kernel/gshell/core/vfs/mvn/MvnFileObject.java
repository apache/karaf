package org.apache.servicemix.kernel.gshell.core.vfs.mvn;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.url.UrlFileObject;
import org.apache.commons.vfs.provider.URLFileName;

public class MvnFileObject extends UrlFileObject {

    public MvnFileObject(MvnFileSystem fs, FileName fileName) {
        super(fs, fileName);
    }

    protected URL createURL(final FileName name) throws MalformedURLException, FileSystemException, URIException
    {
        String url;
        if (name instanceof URLFileName)
        {
            URLFileName urlName = (URLFileName) getName();

            // TODO: charset
            url = urlName.getURIEncoded(null);
        }
        else
        {
            url = getName().getURI();
        }
        if (url.startsWith("mvn:///")) {
            url = "mvn:" + url.substring("mvn:///".length());
        }
        return new URL(url);
    }

}
