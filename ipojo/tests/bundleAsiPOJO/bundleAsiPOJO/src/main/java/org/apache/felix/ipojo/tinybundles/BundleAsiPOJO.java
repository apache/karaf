package org.apache.felix.ipojo.tinybundles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.ops4j.pax.swissbox.tinybundles.core.BuildableBundle;
import org.ops4j.pax.swissbox.tinybundles.core.metadata.RawBuilder;
import org.ops4j.pax.swissbox.tinybundles.core.metadata.UIDProvider;


public class BundleAsiPOJO implements BuildableBundle {
    
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private File m_metadata;
    private File m_file;


    public static BuildableBundle asiPOJOBundle(File file, File metadata) {
        return (new BundleAsiPOJO(file, metadata));
    }
    
    public static BuildableBundle asiPOJOBundle(File metadata) {
        try {
            File file = File.createTempFile("tinybundle_ipojo", ".jar");
            return asiPOJOBundle(file, metadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BundleAsiPOJO (File file, File metadata) {
        m_metadata = metadata;
        m_file = file;
        if (metadata != null && !metadata.exists()) {
            throw new RuntimeException("METADATA NOT FOUND");
        }
    }

    public InputStream pojoize(File in) {
        Pojoization pojoizator = new Pojoization();
        try {
            pojoizator.pojoization(in, m_file, m_metadata);
            List<String> list = (List<String>) pojoizator.getErrors();
            if (list != null  && ! list.isEmpty()) {
                for (String s : list) {
                    System.err.println("[ERROR]" + s);
                }
                throw new RuntimeException("Errors occurs during pojoization " + list);
            }

            list = (List<String>) pojoizator.getWarnings();
            if (list != null  && ! list.isEmpty()) {
                for (String s : list) {
                    System.err.println("[WARNING]" + s);
                }
            }
            return new FileInputStream(m_file);
        } catch (Exception e) {
            List<String> list = (List<String>) pojoizator.getErrors();
            if (list != null) {
                for (String s : list) {
                    System.err.println(s);
                }
            }
            e.printStackTrace();
            throw new RuntimeException(e.getMessage() + " : " + list);
        }

    }

    public InputStream build(Map<String, URL> resources, Map<String, String> headers) {
        InputStream in = new RawBuilder().build(resources,
                new HashMap<String, String>());
        try {
            Properties p = new Properties();
            p.putAll(headers);
            InputStream bnd =  BndUtils.createBundle(in, p, "BuildByTinyBundles"
                    + UIDProvider.getUID());
            File tmp = File.createTempFile("tinybundle_", ".jar");
            tmp.deleteOnExit();
            copy(bnd, new FileOutputStream(tmp));
            return pojoize(tmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Copy bytes from an InputStream to an OutputStream.
     * @param input the InputStream to read from
     * @param output the OutputStream to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(
            InputStream input,
            OutputStream output)
                throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
