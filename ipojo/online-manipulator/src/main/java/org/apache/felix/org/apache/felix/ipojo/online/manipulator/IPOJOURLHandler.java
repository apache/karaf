package org.apache.felix.org.apache.felix.ipojo.online.manipulator;

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

public class IPOJOURLHandler extends org.osgi.service.url.AbstractURLStreamHandlerService implements URLStreamHandlerService {
    
    private BundleContext m_context;
    private File m_temp;
    
    public IPOJOURLHandler(BundleContext bc) {
        m_context = bc;
        m_temp = m_context.getDataFile("temp");
        if (! m_temp.exists()) {
            m_temp.mkdir();
        }
    }
    
    public void stop() {
        File[] files = m_temp.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        m_temp.delete();
    }

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
        String urls[] = full.split("!");
        URL bundleURL = null;
        URL metadataURL = null;
        if (urls.length == 1) {
            // URL form
            System.out.println("Extracted URL : " + urls[0]);
            bundleURL = new URL(urls[0]);
        } else if (urls.length == 2){
            // URL,URL form
            bundleURL = new URL(urls[0]);
            metadataURL = new URL(urls[1]);
        } else {
            throw new MalformedURLException("The iPOJO url is not formatted correctly, ipojo://bundle_url[!metadata_url] expected");
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
        File out =  new File(m_temp, bundle.getName()+"-ipojo.jar");
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

    private void save(URL url, File file) throws IOException {
        InputStream is = url.openStream();
        save(is, file);
    }
    
    private void save(InputStream is, File file) throws IOException {
        FileOutputStream writer = new FileOutputStream(file);
        int cc = 0;
        do {
            int i = is.read();
            if (i == -1)
                break;
            cc++;
            writer.write(i);
        } while (true);
        System.out.println(cc + " bytes copied");
        is.close();
        writer.close();        
    }
    
    private File findMetadata(JarFile jar) throws IOException {
        JarEntry je = jar.getJarEntry("metadata.xml");
        if (je == null) {
            je = jar.getJarEntry("META-INF/metadata.xml");
        }
        
        if (je == null) {
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
