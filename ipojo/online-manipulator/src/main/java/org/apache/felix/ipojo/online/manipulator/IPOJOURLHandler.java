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
package org.apache.felix.ipojo.online.manipulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerService;

/**
* iPOJO URL Handler allowing installation time manipulation.
* When a bundle is installed with the <code>ipojo:</code> URL
* prefix, the bundle is downloaded and manipulated by this 
* handler.
* The metadata.xml file can either be provided inside the bundle (root,
* or in META-INF) or given in the URL:
* ipojo:URL_BUNDLE!URL_METADATA.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class IPOJOURLHandler extends org.osgi.service.url.AbstractURLStreamHandlerService implements URLStreamHandlerService {
    
    /**
     * The bundle context.
     */
    private BundleContext m_context;
    
    /**
     * The directory storing bundles.
     */
    private File m_temp;
    
    /**
     * Creates a IPOJOURLHandler.
     * Gets the bundle context and create the working
     * directory.
     * @param bc the bundle context
     */
    public IPOJOURLHandler(BundleContext bc) {
        m_context = bc;
        m_temp = m_context.getDataFile("temp");
        if (! m_temp.exists()) {
            m_temp.mkdir();
        }
    }
    
    /**
     * Stops the URL handler:
     * Deletes the working directory.  
     */
    public void stop() {
        File[] files = m_temp.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        m_temp.delete();
    }

    /**
     * Opens a connection using the ipojo url handler.
     * This methods parses the URL and manipulate the given bundle.
     * @param url the url.
     * @return the URL connection on the manipulated bundle
     * @throws IOException  occurs when the bundle cannot be either downloaded, or manipulated or
     * installed correctly.
     * @see org.osgi.service.url.AbstractURLStreamHandlerService#openConnection(java.net.URL)
     */
    public URLConnection openConnection(URL url) throws IOException {
        System.out.println("Processing URL : " + url);

       // Parse the url:
        String full = url.toExternalForm();
        // Remote ipojo://
        if (full.startsWith("ipojo:")) {
            full = full.substring(6);
        }
        // Remove '/' or '//'
        while (full.startsWith("/")) {
            full = full.substring(1);
        }
        
        full = full.trim();
        
        // Now full is like : URL,URL or URL
        String[] urls = full.split("!");
        URL bundleURL = null;
        URL metadataURL = null;
        if (urls.length == 1) {
            // URL form
            System.out.println("Extracted URL : " + urls[0]);
            bundleURL = new URL(urls[0]);
        } else if (urls.length == 2) {
            // URL,URL form
            bundleURL = new URL(urls[0]);
            metadataURL = new URL(urls[1]);
        } else {
            throw new MalformedURLException("The iPOJO url is not formatted correctly, ipojo:bundle_url[!metadata_url] expected");
        }
        
        File bundle = File.createTempFile("ipojo_", ".jar", m_temp);
        save(bundleURL, bundle);
        File metadata = null;
        if (metadataURL != null) {
            metadata = File.createTempFile("ipojo_", ".xml", m_temp);
            save(metadataURL, metadata);   
        } else {
            // Check that the metadata are in the jar file
            JarFile jar = new JarFile(bundle);
            metadata = findMetadata(jar); 
        }
        
        // Pojoization
        Pojoization pojoizator = new Pojoization();
        File out =  new File(m_temp, bundle.getName() + "-ipojo.jar");
        System.out.println("Pojoization " + bundle.exists() + " - " + metadata.exists());
        try {
            pojoizator.pojoization(bundle, out, metadata);
        } catch (Exception e) {
            if (! pojoizator.getErrors().isEmpty()) {
                throw new IOException("Errors occured during the manipulation : " + pojoizator.getErrors());
            }
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        
        if (! pojoizator.getErrors().isEmpty()) {
            throw new IOException("Errors occured during the manipulation : " + pojoizator.getErrors());
        }
        if (! pojoizator.getWarnings().isEmpty()) {
            System.err.println("Warnings occured during the manipulation : " + pojoizator.getWarnings());
        }

        System.out.println("Manipulation done : " + out.exists());
        
        // Cleanup
        bundle.delete();
        if (metadata != null) {
            metadata.delete();
        }
        out.deleteOnExit();
        // Returns the URL Connection
        return out.toURL().openConnection();
        
        
    }

    /**
     * Downloads the content pointed by the given url to
     * the given file.
     * @param url   the url
     * @param file  the file
     * @throws IOException occurs if the content cannot be read 
     * and save inside the file 
     */
    private void save(URL url, File file) throws IOException {
        InputStream is = url.openStream();
        save(is, file);
    }
    
    /**
     * Saves the content of the input stream to the given file.
     * @param is    the input stream to read
     * @param file  the file
     * @throws IOException  occurs if the content cannot be read 
     * and save inside the file  
     */
    private void save(InputStream is, File file) throws IOException {
        FileOutputStream writer = new FileOutputStream(file);
        int cc = 0;
        do {
            int i = is.read();
            if (i == -1) {
                break;
            }
            cc++;
            writer.write(i);
        } while (true);
        System.out.println(cc + " bytes copied");
        is.close();
        writer.close();        
    }
    
    /**
     * Looks for the metadata.xml file in the jar file.
     * Two locations are checked:
     * <ol>
     * <li>the root of the jar file</li>
     * <li>the META-INF directory</li>
     * </ol>
     * @param jar   the jar file
     * @return  the founded file or <code>null</code> if not found.
     * @throws IOException  occurs when the Jar file cannot be read.
     */
    private File findMetadata(JarFile jar) throws IOException {
        JarEntry je = jar.getJarEntry("metadata.xml");
        if (je == null) {
            je = jar.getJarEntry("META-INF/metadata.xml");
        }
        
        if (je == null) {
            System.out.println("Metadata file not found, use annotations only.");
            return null; // Not Found, use annotation only
        } else {
            System.out.println("Metadata file found: " + je.getName());
            File metadata = File.createTempFile("ipojo_", ".xml", m_temp);
            save(jar.getInputStream(je), metadata);
            System.out.println("Metadata file saved to " + metadata.getAbsolutePath());
            return metadata;
        }
        
    }
    
}
