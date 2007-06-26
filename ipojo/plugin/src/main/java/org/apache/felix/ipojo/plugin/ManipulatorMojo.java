package org.apache.felix.ipojo.plugin;

import java.io.File;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Package an OSGi jar "bundle."
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Apache Felix Project</a>
 * @version $Rev$, $Date$
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to build an iPOJO bundle
 */
public class ManipulatorMojo extends AbstractMojo {

    /**
     * The directory for the generated JAR.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String buildDirectory;

    /**
     * The directory containing generated classes.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The name of the generated JAR file.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String jarName;
    
    /**
     * @parameter expression="${metadata}" default-value="metadata.xml"
     */
    private String metadata;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Start bundle manipulation");
        // Get metadata file
        File meta = new File(outputDirectory + "/" + metadata);
        getLog().info("Metadata File : " + meta.getAbsolutePath());
        if(!meta.exists()) {
            throw new MojoExecutionException("the specified metadata file does not exists");
        }

        // Get input bundle
        File in = new File(buildDirectory + "/" + jarName + ".jar" );
        getLog().info("Input Bundle File : " + in.getAbsolutePath());
        if(!in.exists()) {
            throw new MojoExecutionException("the specified bundle file does not exists");
        }
        
        File out = new File(buildDirectory + "/_out.jar");
        
        Pojoization pojo = new Pojoization();
        pojo.pojoization(in, out, meta);
        for(int i = 0; i < pojo.getWarnings().size(); i++) {
            getLog().warn((String) pojo.getWarnings().get(i));
        }
        if(pojo.getErrors().size() > 0 ) { throw new MojoExecutionException((String) pojo.getErrors().get(0)); }
        in.delete();
        out.renameTo(in);
        getLog().info("Bundle manipulation - SUCCESS");
    }

}
