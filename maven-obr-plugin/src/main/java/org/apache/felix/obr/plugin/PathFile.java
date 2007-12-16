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
package org.apache.felix.obr.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;

/**
 * this class provide some functions to simplify file manipulation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PathFile {

    /**
     * full filename.
     */
    private String m_fullFilename;

    /**
     * store only the filename of the file.
     */
    private String m_fileName;

    /**
     * store only the path of this file.
     */
    private String m_pathFile;

    /**
     * store the base Directory in case of relative path.
     */
    private String m_baseDir;

    /**
     * store the protocol used (ie:file, http...).
     */
    private String m_protocol;

    /**
     * if the path is relative or absolute.
     */
    private boolean m_relative;

    /**
     * if this file is a folder.
     */
    private boolean m_folder;

    /**
     * if the file exist or not.
     */
    private boolean m_exist;

    /**
     * if this file is a file (not a folder).
     */
    private boolean m_file;

    /**
     * if this filename is valid or incomplete.
     */
    private boolean m_valid;

    /**
     * build all the attribute information.
     * @param filename path to the file
     */
    public PathFile(String filename) {

        this.m_fullFilename = filename;
        if (filename == null) {
            this.m_valid = false;
            return;
        }
        this.m_valid = true;
        m_protocol = extractProtocol(filename);
        m_pathFile = extractPathFile(filename);
        if (m_pathFile.startsWith("//")) {
            // avoid problems on Unix like system
            m_pathFile = m_pathFile.substring(1);
        }
        m_fileName = extractFileName(filename);
        m_relative = extractRelative();
        if (!m_relative && (getProtocol().compareTo("file") == 0 || getProtocol().compareTo("") == 0)) {
            File f = new File(getOnlyAbsoluteFilename());
            m_file = f.isFile();
            m_folder = f.isDirectory();
            m_exist = f.exists();
            if (m_folder) {
                m_pathFile = m_pathFile + m_fileName + File.separator;
                m_fileName = "";
            }
        }
        if (m_exist) {
            m_protocol = "file";
        } else {
            if (m_fileName.compareTo("") == 0) {
                m_folder = true;
                m_file = false;
            } else {
                m_folder = false;
                m_file = true;
            }

        }

        // add the '/' before the complete path if it is absolute path
        if (!this.isRelative() && !m_pathFile.startsWith("/")) { m_pathFile = "/".concat(m_pathFile); }

    }

    /**
     * get if the filename is relative or absolute.
     * @return true if the path is relative, else false
     */
    private boolean extractRelative() {
        if (m_pathFile.startsWith("." + File.separator, 1) || m_pathFile.startsWith(".." + File.separator, 1)) {
            m_pathFile = m_pathFile.substring(1);
            m_valid = false;
            return true;
        }

        return false;
    }

    /**
     * get only the name from the filename.
     * @param fullFilename full filename
     * @return the name of the file or folder
     */
    private String extractFileName(String fullFilename) {
        int index = fullFilename.lastIndexOf('/'); // Given path 
        return fullFilename.substring(index + 1, fullFilename.length());
    }

    /**
     * get the path from the filename.
     * @param fullFilename full filename
     * @return the path of the file
     */
    private String extractPathFile(String fullFilename) {
        String substring;
        if (extractFileName(fullFilename).compareTo("") == 0) {
            // it is a folder
            substring = fullFilename;
        } else {
            substring = fullFilename.substring(0, fullFilename.indexOf(extractFileName(fullFilename)));
        }

        if (getProtocol().compareTo("") != 0) {
            substring = substring.substring(5);
        }

        return substring;
    }

    /**
     * determine which protocol is used.
     * @param filename the full fileneme
     * @return "file" or "http" or ""
     */
    private String extractProtocol(String filename) {
        if (filename.startsWith("file:")) { return "file"; }
        if (filename.startsWith("http:")) { return "http"; }
        return "";
    }

    /**
     * set the base directory.
     * @param baseDir new value for the base directory
     */
    public void setBaseDir(String baseDir) {
        this.m_baseDir = baseDir;
        if (isRelative() && this.m_fullFilename != null) {
            this.m_valid = true;
            if (getProtocol().compareTo("file") == 0 || getProtocol().compareTo("") == 0) {
                File f = new File(getOnlyAbsoluteFilename());
                m_file = f.isFile();
                m_folder = f.isDirectory();
                m_exist = f.exists();
            }
            if (m_exist) {
                m_protocol = "file";
            }
        }

    }

    /**
     * get the base directory.
     * @return base directory
     */
    public String getBaseDir() {
        return this.m_baseDir;
    }

    public boolean isValid() {
        return m_valid;
    }

    public boolean isRelative() {
        return m_relative;
    }

    public boolean isExists() {
        return m_exist;
    }

    public boolean isFile() {
        return m_file;
    }

    public boolean isFolder() {
        return m_folder;
    }

    /**
     * get a File which points on the same file.
     * @return a File object
     */
    public File getFile() {
        if (!this.isValid()) { return null; }
        String path = PathFile.uniformSeparator(this.getOnlyAbsoluteFilename());
        if (File.separatorChar == '\\') { path = path.replace('\\', '/'); }
        File f = new File(path);
        return f;
    }

    /**
     * get an URI which points on the same file.
     * @return an URI object
     */
    public URI getUri() {
        if (!this.isValid()) { return null; }
        String path = PathFile.uniformSeparator(getAbsoluteFilename());
        if (File.separatorChar == '\\') { 
        	path = path.replace('\\', '/');
        }

        path = path.replaceAll(" ", "%20");

        URI uri = null;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {        	
            System.err.println("Malformed URI: " + path);
            System.err.println(e.getMessage());
            return null;
        }
        return uri;
    }

    /**
     * get protocol + relative path of this file.
     * @return the relative path or null if it is not valid
     */
    public String getRelativePath() {
        if (!this.isValid()) { return null; }

        return getProtocol() + ":/" + getOnlyRelativePath();
    }

    /**
     * get only (without protocol) relative path of this file.
     * @return the relative path or null if it is not valid
     */
    public String getOnlyRelativePath() {
        if (!this.isValid()) { return null; }
        if (this.isRelative()) {
            return m_pathFile;

        } else {
            if (m_baseDir != null) {
                // System.err.println(m_pathFile);
                // System.err.println(m_baseDir);
                if (m_pathFile.startsWith(m_baseDir)) {
                    /*
                     * String ch1 = m_pathFile; String ch2 = m_baseDir; System.err.println(ch1); System.err.println(ch2); System.err.println("."+File.separator+ch1.substring(ch2.length()));
                     */
                    return "." + File.separator + m_pathFile.substring(m_baseDir.length());
                }
            }
            return m_pathFile;
        }
    }

    /**
     * calcul absolute path from relative path.
     * @param baseDir base directory
     * @param path path to convert
     * @return the absolute path or null
     */
    private String calculAbsolutePath(String baseDir, String path) {
        if (path.startsWith(".." + File.separatorChar)) {
            String base = baseDir;
            int lastIndex;
            lastIndex = base.lastIndexOf(File.separator);
            if (lastIndex == base.length()) {
                base = base.substring(0, base.length() - 1);
                lastIndex = base.lastIndexOf(File.separator);
            }
            if (lastIndex < base.length()) {
                return calculAbsolutePath(base.substring(0, lastIndex + 1), path.substring(3));
            } else {
                return null;
            }
        } else if (path.startsWith("." + File.separatorChar)) {
            String res;
            if (File.separatorChar == '\\') {
                res = path.replaceFirst(".", baseDir.replace('\\', '/'));
            } else {
                res = path.replaceFirst(".", baseDir);
            }

            return PathFile.uniformSeparator(res);
        } else {
            return PathFile.uniformSeparator(baseDir + path);
        }
    }

    /**
     * get only (without protocol) absolute path (without filename).
     * @return absolute path
     */
    public String getOnlyAbsolutePath() {
        if (!this.isValid()) { return null; }
        if (isRelative()) {
            return calculAbsolutePath(m_baseDir, m_pathFile);
        } else {
            return m_pathFile;
        }
    }

    /**
     * get protocol + absolute path (without filename).
     * @return absolute path
     */
    public String getAbsolutePath() {

        if (isRelative()) {
            return getProtocol() + ":/" + calculAbsolutePath(m_baseDir, m_pathFile);
        } else {
            if (getProtocol().compareTo("") == 0 || m_pathFile == null) {
                return m_pathFile;
            } else {
                return getProtocol() + ":" + m_pathFile;
            }
        }
    }

    /**
     * get only (without protocol) absolute path + filename.
     * @return absolute filename
     */
    public String getOnlyAbsoluteFilename() {
        if (getOnlyAbsolutePath() != null && getFilename() != null) {
            return getOnlyAbsolutePath() + getFilename();
        } else {
            return null;
        }
    }

    /**
     * get protocol + absolute path + filename.
     * @return absolute filenama
     */
    public String getAbsoluteFilename() {
        if (getAbsolutePath() != null && getFilename() != null) {
            return getAbsolutePath() + getFilename();
        } else {
            return null;
        }
    }

    /**
     * get only (without protocol) relative path + filename.
     * @return relative filename
     */
    public String getOnlyRelativeFilename() {
        if (!this.isValid()) { return ""; }

        return getOnlyRelativePath() + getFilename();

    }

    /**
     * get protocol + relative path + filename.
     * @return relative filename
     */
    public String getRelativeFilename() {
        if (!this.isValid()) { return ""; }

        if (this.isRelative()) {
            return getRelativePath() + getFilename();
        } else {
            return getAbsoluteFilename();
        }
    }

    public String getFilename() {
        return m_fileName;
    }

    public String getProtocol() {
        return m_protocol;
    }

    /**
     * create all the directories not also present in the current path.
     * @return true if all directories was created, else false
     */
    public boolean createPath() {
        File path = new File(this.getOnlyAbsolutePath());
        if (path.exists()) { return true; }
        return path.mkdirs();
    }

    /**
     * create all the directories not also present in the current path and the file.
     * @return true it was created, else false
     */
    public boolean createFile() {
        File path = new File(this.getOnlyAbsolutePath());
        if (!path.exists()) {
            if (!this.createPath()) { return false; }
        }
        path = new File(this.getOnlyAbsoluteFilename());
        try {
            return path.createNewFile();
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * delete the current file.
     * @return true if it was deleted, else false
     */
    public boolean delete() {
        File path = new File(this.getAbsoluteFilename());
        if (path.exists()) {
            return path.delete();
        } else {
            return true;
        }

    }

    private static final String REGEXP_BACKSLASH = "\\\\";

    /**
     * replace all '\' by '\\' in the given string.
     * @param path string where replace the search pattern
     * @return string replaced
     */
    public static String doubleSeparator(String path) {
        // double the '\' in the path
        if (path != null && File.separatorChar == '\\') {
            return path.replaceAll(REGEXP_BACKSLASH, REGEXP_BACKSLASH + REGEXP_BACKSLASH);
        } else {
            return null;
        }
    }

    /**
     * file separator('\' or '/') by the one of the current system.
     * @param path string where replace the search pattern
     * @return string replaced
     */
    public static String uniformSeparator(String path) {
        if (File.separatorChar == '\\') {
            if (path.startsWith("/")) {
                return path.substring(1).replace('/', File.separatorChar);
            } else {
                return path.replace('/', File.separatorChar);
            }
        } else {
            return path.replace('\\', File.separatorChar);
        }
    }

    /**
     * copy file from src to dest.
     * @param src source file
     * @param dest destination file
     * @return true if the file was correctly copied, else false
     */
    public static boolean copyFile(PathFile src, PathFile dest) {
        FileChannel in = null;
        FileChannel out = null;

        if (!src.isExists()) {
            System.err.println("src file must exist: " + src.getAbsoluteFilename());
            return false;
        }
        if (!dest.isExists()) {
            dest.createFile();
        }
        try {
            in = new FileInputStream(src.getOnlyAbsoluteFilename()).getChannel();
            out = new FileOutputStream(dest.getOnlyAbsoluteFilename()).getChannel();

            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    return false;
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

}
