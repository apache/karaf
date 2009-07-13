/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class ZipResource implements Resource {
    ZipFile  zip;
    ZipEntry entry;
    long     lastModified;
    String   extra;

    ZipResource(ZipFile zip, ZipEntry entry, long lastModified) {
        this.zip = zip;
        this.entry = entry;
        this.lastModified = lastModified;
        byte[] data = entry.getExtra();
        if (data != null)
            this.extra = new String(data);
    }

    public InputStream openInputStream() throws IOException {
        return zip.getInputStream(entry);
    }

    public String toString() {
        return ":" + entry.getName() + ":";
    }

    public static ZipFile build(Jar jar, File file) throws ZipException,
            IOException {
        return build(jar, file, null);
    }

    public static ZipFile build(Jar jar, File file, Pattern pattern)
            throws ZipException, IOException {

        try {
            ZipFile zip = new ZipFile(file);
            nextEntry: for (Enumeration<? extends ZipEntry> e = zip.entries(); e
                    .hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                if (pattern != null) {
                    Matcher m = pattern.matcher(entry.getName());
                    if (!m.matches())
                        continue nextEntry;
                }
                if (!entry.isDirectory()) {
                    long time = entry.getTime();
                    if (time <= 0)
                        time = file.lastModified();
                    jar.putResource(entry.getName(), new ZipResource(zip,
                            entry, time), true);
                }
            }
            return zip;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Problem opening JAR: "
                    + file.getAbsolutePath());
        }
    }

    public void write(OutputStream out) throws IOException {
        FileResource.copy(this, out);
    }

    public long lastModified() {
        return lastModified;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

}
