package org.apache.felix.ipojo.tinybundles;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.ops4j.pax.swissbox.tinybundles.core.BundleAs;
import org.ops4j.pax.swissbox.tinybundles.core.targets.BundleAsFile;


public class BundleAsiPOJO implements BundleAs<URL> {

    private File m_metadata;
    private File m_file;


    public static BundleAs<URL> asiPOJOBundle(File file, File metadata) {
        return (new BundleAsiPOJO(file, metadata));
    }

    public BundleAsiPOJO (File file, File metadata) {
        m_metadata = metadata;
        m_file = file;
        if (! metadata.exists()) {
            throw new RuntimeException("METADATA NOT FOUND");
        }
    }

    public URL make(InputStream arg0) {
        Pojoization pojoizator = new Pojoization();
        try {
            File fout = File.createTempFile( "tinybundle_", ".jar" );
            fout.deleteOnExit();
            File out = new BundleAsFile(fout).make(arg0);
            pojoizator.pojoization(out, m_file, m_metadata);


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
            return m_file.toURL();
        } catch (Exception e) {
            List<String> list = (List<String>) pojoizator.getErrors();
            if (list != null) {
                for (String s : list) {
                    System.err.println(s);
                }
            }
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

}
