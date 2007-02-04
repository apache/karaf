/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tools.maven.felix.plugin;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.MutablePropertyResolver;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;


/**
 * Starts up Felix, installs this module's bundle artifact with dependent 
 * bundles, and starts them.
 * 
 * @goal run
 * @phase integration-test
 */
public class FelixRunMojo extends AbstractMojo
{
    private static final String JRE_1_3_PACKAGES = 
        "javax.accessibility;" +
        "javax.accessibility.resources;" +
        "javax.naming;" +
        "javax.naming.directory;" +
        "javax.naming.event;" +
        "javax.naming.ldap;" +
        "javax.naming.spi;" +
        "javax.rmi;" +
        "javax.rmi.CORBA;" +
        "javax.sound.midi;" +
        "javax.sound.midi.spi;" +
        "javax.sound.sampled;" +
        "javax.sound.sampled.spi;" +
        "javax.swing;" +
        "javax.swing.border;" +
        "javax.swing.colorchooser;" +
        "javax.swing.event;" +
        "javax.swing.filechooser;" +
        "javax.swing.plaf;" +
        "javax.swing.plaf.basic;" +
        "javax.swing.plaf.basic.resources;" +
        "javax.swing.plaf.metal;" +
        "javax.swing.plaf.metal.resources;" +
        "javax.swing.plaf.multi;" +
        "javax.swing.table;" +
        "javax.swing.text;" +
        "javax.swing.text.html;" +
        "javax.swing.text.html.parser;" +
        "javax.swing.text.rtf;" +
        "javax.swing.tree;" +
        "javax.swing.undo;" +
        "javax.transaction;" +
        "org.omg.CORBA;" +
        "org.omg.CORBA_2_3;" +
        "org.omg.CORBA_2_3.portable;" +
        "org.omg.CORBA.DynAnyPackage;" +
        "org.omg.CORBA.ORBPackage;" +
        "org.omg.CORBA.portable;" +
        "org.omg.CORBA.TypeCodePackage;" +
        "org.omg.CosNaming;" +
        "org.omg.CosNaming.NamingContextPackage;" +
        "org.omg.SendingContext;" +
        "org.omg.stub.java.rmi;version=\"1.3.0\"";

    private static final String JRE_1_4_PACKAGES = 
        "javax.accessibility;" +
        "javax.imageio;" +
        "javax.imageio.event;" +
        "javax.imageio.metadata;" +
        "javax.imageio.plugins.jpeg;" +
        "javax.imageio.spi;" +
        "javax.imageio.stream;" +
        "javax.naming;" +
        "javax.naming.directory;" +
        "javax.naming.event;" +
        "javax.naming.ldap;" +
        "javax.naming.spi;" +
        "javax.print;" +
        "javax.print.attribute;" +
        "javax.print.attribute.standard;" +
        "javax.print.event;" +
        "javax.rmi;" +
        "javax.rmi.CORBA;" +
        "javax.security.auth;" +
        "javax.security.auth.callback;" +
        "javax.security.auth.kerberos;" +
        "javax.security.auth.login;" +
        "javax.security.auth.spi;" +
        "javax.security.auth.x500;" +
        "javax.sound.midi;" +
        "javax.sound.midi.spi;" +
        "javax.sound.sampled;" +
        "javax.sound.sampled.spi;" +
        "javax.sql;" +
        "javax.swing;" +
        "javax.swing.border;" +
        "javax.swing.colorchooser;" +
        "javax.swing.event;" +
        "javax.swing.filechooser;" +
        "javax.swing.plaf;" +
        "javax.swing.plaf.basic;" +
        "javax.swing.plaf.metal;" +
        "javax.swing.plaf.multi;" +
        "javax.swing.table;" +
        "javax.swing.text;" +
        "javax.swing.text.html;" +
        "javax.swing.text.html.parser;" +
        "javax.swing.text.rtf;" +
        "javax.swing.tree;" +
        "javax.swing.undo;" +
        "javax.transaction;" +
        "javax.transaction.xa;" +
        "javax.xml.parsers;" +
        "javax.xml.transform;" +
        "javax.xml.transform.dom;" +
        "javax.xml.transform.sax;" +
        "javax.xml.transform.stream;" +
        "org.apache.crimson.jaxp;" +
        "org.apache.crimson.parser;" +
        "org.apache.crimson.parser.resources;" +
        "org.apache.crimson.tree;" +
        "org.apache.crimson.tree.resources;" +
        "org.apache.crimson.util;" +
        "org.apache.xalan;" +
        "org.apache.xalan.client;" +
        "org.apache.xalan.extensions;" +
        "org.apache.xalan.lib;" +
        "org.apache.xalan.lib.sql;" +
        "org.apache.xalan.processor;" +
        "org.apache.xalan.res;" +
        "org.apache.xalan.serialize;" +
        "org.apache.xalan.templates;" +
        "org.apache.xalan.trace;" +
        "org.apache.xalan.transformer;" +
        "org.apache.xalan.xslt;" +
        "org.apache.xml.dtm;" +
        "org.apache.xml.dtm.ref;" +
        "org.apache.xml.dtm.ref.dom2dtm;" +
        "org.apache.xml.dtm.ref.sax2dtm;" +
        "org.apache.xml.utils;" +
        "org.apache.xml.utils.res;" +
        "org.apache.xml.utils.synthetic;" +
        "org.apache.xml.utils.synthetic.reflection;" +
        "org.apache.xpath;" +
        "org.apache.xpath.axes;" +
        "org.apache.xpath.compiler;" +
        "org.apache.xpath.functions;" +
        "org.apache.xpath.objects;" +
        "org.apache.xpath.operations;" +
        "org.apache.xpath.patterns;" +
        "org.apache.xpath.res;" +
        "org.ietf.jgss;" +
        "org.omg.CORBA;" +
        "org.omg.CORBA_2_3;" +
        "org.omg.CORBA_2_3.portable;" +
        "org.omg.CORBA.DynAnyPackage;" +
        "org.omg.CORBA.ORBPackage;" +
        "org.omg.CORBA.portable;" +
        "org.omg.CORBA.TypeCodePackage;" +
        "org.omg.CosNaming;" +
        "org.omg.CosNaming.NamingContextExtPackage;" +
        "org.omg.CosNaming.NamingContextPackage;" +
        "org.omg.Dynamic;" +
        "org.omg.DynamicAny;" +
        "org.omg.DynamicAny.DynAnyFactoryPackage;" +
        "org.omg.DynamicAny.DynAnyPackage;" +
        "org.omg.IOP;" +
        "org.omg.IOP.CodecFactoryPackage;" +
        "org.omg.IOP.CodecPackage;" +
        "org.omg.Messaging;" +
        "org.omg.PortableInterceptor;" +
        "org.omg.PortableInterceptor.ORBInitInfoPackage;" +
        "org.omg.PortableServer;" +
        "org.omg.PortableServer.CurrentPackage;" +
        "org.omg.PortableServer.POAManagerPackage;" +
        "org.omg.PortableServer.POAPackage;" +
        "org.omg.PortableServer.portable;" +
        "org.omg.PortableServer.ServantLocatorPackage;" +
        "org.omg.SendingContext;" +
        "org.omg.stub.java.rmi;" +
        "org.w3c.dom;" +
        "org.w3c.dom.css;" +
        "org.w3c.dom.events;" +
        "org.w3c.dom.html;" +
        "org.w3c.dom.stylesheets;" +
        "org.w3c.dom.traversal;" +
        "org.w3c.dom.views;" +
        "org.xml.sax;" +
        "org.xml.sax.ext;" +
        "org.xml.sax.helpers;" +
        "version=\"1.4.0\"";

    private static final String JRE_1_5_PACKAGES = 
        "javax.accessibility;" +
        "javax.activity;" +
        "javax.imageio;" +
        "javax.imageio.event;" +
        "javax.imageio.metadata;" +
        "javax.imageio.plugins.bmp;" +
        "javax.imageio.plugins.jpeg;" +
        "javax.imageio.spi;" +
        "javax.imageio.stream;" +
        "javax.management;" +
        "javax.management.loading;" +
        "javax.management.modelmbean;" +
        "javax.management.monitor;" +
        "javax.management.openmbean;" +
        "javax.management.relation;" +
        "javax.management.remote;" +
        "javax.management.remote.rmi;" +
        "javax.management.timer;" +
        "javax.naming;" +
        "javax.naming.directory;" +
        "javax.naming.event;" +
        "javax.naming.ldap;" +
        "javax.naming.spi;" +
        "javax.print;" +
        "javax.print.attribute;" +
        "javax.print.attribute.standard;" +
        "javax.print.event;" +
        "javax.rmi;" +
        "javax.rmi.CORBA;" +
        "javax.rmi.ssl;" +
        "javax.security.auth;" +
        "javax.security.auth.callback;" +
        "javax.security.auth.kerberos;" +
        "javax.security.auth.login;" +
        "javax.security.auth.spi;" +
        "javax.security.auth.x500;" +
        "javax.security.sasl;" +
        "javax.sound.midi;" +
        "javax.sound.midi.spi;" +
        "javax.sound.sampled;" +
        "javax.sound.sampled.spi;" +
        "javax.sql;" +
        "javax.sql.rowset;" +
        "javax.sql.rowset.serial;" +
        "javax.sql.rowset.spi;" +
        "javax.swing;" +
        "javax.swing.border;" +
        "javax.swing.colorchooser;" +
        "javax.swing.event;" +
        "javax.swing.filechooser;" +
        "javax.swing.plaf;" +
        "javax.swing.plaf.basic;" +
        "javax.swing.plaf.metal;" +
        "javax.swing.plaf.multi;" +
        "javax.swing.plaf.synth;" +
        "javax.swing.table;" +
        "javax.swing.text;" +
        "javax.swing.text.html;" +
        "javax.swing.text.html.parser;" +
        "javax.swing.text.rtf;" +
        "javax.swing.tree;" +
        "javax.swing.undo;" +
        "javax.transaction;" +
        "javax.transaction.xa;" +
        "javax.xml;" +
        "javax.xml.datatype;" +
        "javax.xml.namespace;" +
        "javax.xml.parsers;" +
        "javax.xml.transform;" +
        "javax.xml.transform.dom;" +
        "javax.xml.transform.sax;" +
        "javax.xml.transform.stream;" +
        "javax.xml.validation;" +
        "javax.xml.xpath;" +
        "org.ietf.jgss;" +
        "org.omg.CORBA;" +
        "org.omg.CORBA_2_3;" +
        "org.omg.CORBA_2_3.portable;" +
        "org.omg.CORBA.DynAnyPackage;" +
        "org.omg.CORBA.ORBPackage;" +
        "org.omg.CORBA.portable;" +
        "org.omg.CORBA.TypeCodePackage;" +
        "org.omg.CosNaming;" +
        "org.omg.CosNaming.NamingContextExtPackage;" +
        "org.omg.CosNaming.NamingContextPackage;" +
        "org.omg.Dynamic;" +
        "org.omg.DynamicAny;" +
        "org.omg.DynamicAny.DynAnyFactoryPackage;" +
        "org.omg.DynamicAny.DynAnyPackage;" +
        "org.omg.IOP;" +
        "org.omg.IOP.CodecFactoryPackage;" +
        "org.omg.IOP.CodecPackage;" +
        "org.omg.Messaging;" +
        "org.omg.PortableInterceptor;" +
        "org.omg.PortableInterceptor.ORBInitInfoPackage;" +
        "org.omg.PortableServer;" +
        "org.omg.PortableServer.CurrentPackage;" +
        "org.omg.PortableServer.POAManagerPackage;" +
        "org.omg.PortableServer.POAPackage;" +
        "org.omg.PortableServer.portable;" +
        "org.omg.PortableServer.ServantLocatorPackage;" +
        "org.omg.SendingContext;" +
        "org.omg.stub.java.rmi;" +
        "org.omg.stub.javax.management.remote.rmi;" +
        "org.w3c.dom;" +
        "org.w3c.dom.bootstrap;" +
        "org.w3c.dom.css;" +
        "org.w3c.dom.events;" +
        "org.w3c.dom.html;" +
        "org.w3c.dom.ls;" +
        "org.w3c.dom.ranges;" +
        "org.w3c.dom.stylesheets;" +
        "org.w3c.dom.traversal;" +
        "org.w3c.dom.views;" +
        "org.xml.sax;" +
        "org.xml.sax.ext;" +
        "org.xml.sax.helpers;" +
        "version=\"1.5.0\"";

    private static final String JRE_1_6_PACKAGES = 
        "java.applet;" +
        "java.awt;" +
        "java.awt.color;" +
        "java.awt.datatransfer;" +
        "java.awt.dnd;" +
        "java.awt.dnd.peer;" +
        "java.awt.event;" +
        "java.awt.font;" +
        "java.awt.geom;" +
        "java.awt.im;" +
        "java.awt.image;" +
        "java.awt.image.renderable;" +
        "java.awt.im.spi;" +
        "java.awt.peer;" +
        "java.awt.print;" +
        "java.beans;" +
        "java.beans.beancontext;" +
        "java.io;" +
        "java.lang;" +
        "java.lang.annotation;" +
        "java.lang.instrument;" +
        "java.lang.management;" +
        "java.lang.ref;" +
        "java.lang.reflect;" +
        "java.math;" +
        "java.net;" +
        "java.nio;" +
        "java.nio.channels;" +
        "java.nio.channels.spi;" +
        "java.nio.charset;" +
        "java.nio.charset.spi;" +
        "java.rmi;" +
        "java.rmi.activation;" +
        "java.rmi.dgc;" +
        "java.rmi.registry;" +
        "java.rmi.server;" +
        "java.security;" +
        "java.security.acl;" +
        "java.security.cert;" +
        "java.security.interfaces;" +
        "java.security.spec;" +
        "java.sql;" +
        "java.text;" +
        "java.text.spi;" +
        "java.util;" +
        "java.util.concurrent;" +
        "java.util.concurrent.atomic;" +
        "java.util.concurrent.locks;" +
        "java.util.jar;" +
        "java.util.logging;" +
        "java.util.prefs;" +
        "java.util.regex;" +
        "java.util.spi;" +
        "java.util.zip;" +
        "javax.accessibility;" +
        "javax.activation;" +
        "javax.activity;" +
        "javax.annotation;" +
        "javax.annotation.processing;" +
        "javax.imageio;" +
        "javax.imageio.event;" +
        "javax.imageio.metadata;" +
        "javax.imageio.plugins.bmp;" +
        "javax.imageio.plugins.jpeg;" +
        "javax.imageio.spi;" +
        "javax.imageio.stream;" +
        "javax.jws;" +
        "javax.jws.soap;" +
        "javax.lang.model;" +
        "javax.lang.model.element;" +
        "javax.lang.model.type;" +
        "javax.lang.model.util;" +
        "javax.management;" +
        "javax.management.loading;" +
        "javax.management.modelmbean;" +
        "javax.management.monitor;" +
        "javax.management.openmbean;" +
        "javax.management.relation;" +
        "javax.management.remote;" +
        "javax.management.remote.rmi;" +
        "javax.management.timer;" +
        "javax.naming;" +
        "javax.naming.directory;" +
        "javax.naming.event;" +
        "javax.naming.ldap;" +
        "javax.naming.spi;" +
        "javax.print;" +
        "javax.print.attribute;" +
        "javax.print.attribute.standard;" +
        "javax.print.event;" +
        "javax.rmi;" +
        "javax.rmi.CORBA;" +
        "javax.rmi.ssl;" +
        "javax.script;" +
        "javax.security.auth;" +
        "javax.security.auth.callback;" +
        "javax.security.auth.kerberos;" +
        "javax.security.auth.login;" +
        "javax.security.auth.spi;" +
        "javax.security.auth.x500;" +
        "javax.security.sasl;" +
        "javax.smartcardio;" +
        "javax.sound.midi;" +
        "javax.sound.midi.spi;" +
        "javax.sound.sampled;" +
        "javax.sound.sampled.spi;" +
        "javax.sql;" +
        "javax.sql.rowset;" +
        "javax.sql.rowset.serial;" +
        "javax.sql.rowset.spi;" +
        "javax.swing;" +
        "javax.swing.border;" +
        "javax.swing.colorchooser;" +
        "javax.swing.event;" +
        "javax.swing.filechooser;" +
        "javax.swing.plaf;" +
        "javax.swing.plaf.basic;" +
        "javax.swing.plaf.metal;" +
        "javax.swing.plaf.multi;" +
        "javax.swing.plaf.synth;" +
        "javax.swing.table;" +
        "javax.swing.text;" +
        "javax.swing.text.html;" +
        "javax.swing.text.html.parser;" +
        "javax.swing.text.rtf;" +
        "javax.swing.tree;" +
        "javax.swing.undo;" +
        "javax.tools;" +
        "javax.transaction;" +
        "javax.transaction.xa;" +
        "javax.xml;" +
        "javax.xml.bind;" +
        "javax.xml.bind.annotation;" +
        "javax.xml.bind.annotation.adapters;" +
        "javax.xml.bind.attachment;" +
        "javax.xml.bind.helpers;" +
        "javax.xml.bind.util;" +
        "javax.xml.crypto;" +
        "javax.xml.crypto.dom;" +
        "javax.xml.crypto.dsig;" +
        "javax.xml.crypto.dsig.dom;" +
        "javax.xml.crypto.dsig.keyinfo;" +
        "javax.xml.crypto.dsig.spec;" +
        "javax.xml.datatype;" +
        "javax.xml.namespace;" +
        "javax.xml.parsers;" +
        "javax.xml.soap;" +
        "javax.xml.stream;" +
        "javax.xml.stream.events;" +
        "javax.xml.stream.util;" +
        "javax.xml.transform;" +
        "javax.xml.transform.dom;" +
        "javax.xml.transform.sax;" +
        "javax.xml.transform.stax;" +
        "javax.xml.transform.stream;" +
        "javax.xml.validation;" +
        "javax.xml.ws;" +
        "javax.xml.ws.handler;" +
        "javax.xml.ws.handler.soap;" +
        "javax.xml.ws.http;" +
        "javax.xml.ws.soap;" +
        "javax.xml.ws.spi;" +
        "javax.xml.xpath;" +
        "org.ietf.jgss;" +
        "org.jcp.xml.dsig.internal;" +
        "org.jcp.xml.dsig.internal.dom;" +
        "org.omg.CORBA;" +
        "org.omg.CORBA_2_3;" +
        "org.omg.CORBA_2_3.portable;" +
        "org.omg.CORBA.DynAnyPackage;" +
        "org.omg.CORBA.ORBPackage;" +
        "org.omg.CORBA.portable;" +
        "org.omg.CORBA.TypeCodePackage;" +
        "org.omg.CosNaming;" +
        "org.omg.CosNaming.NamingContextExtPackage;" +
        "org.omg.CosNaming.NamingContextPackage;" +
        "org.omg.Dynamic;" +
        "org.omg.DynamicAny;" +
        "org.omg.DynamicAny.DynAnyFactoryPackage;" +
        "org.omg.DynamicAny.DynAnyPackage;" +
        "org.omg.IOP;" +
        "org.omg.IOP.CodecFactoryPackage;" +
        "org.omg.IOP.CodecPackage;" +
        "org.omg.Messaging;" +
        "org.omg.PortableInterceptor;" +
        "org.omg.PortableInterceptor.ORBInitInfoPackage;" +
        "org.omg.PortableServer;" +
        "org.omg.PortableServer.CurrentPackage;" +
        "org.omg.PortableServer.POAManagerPackage;" +
        "org.omg.PortableServer.POAPackage;" +
        "org.omg.PortableServer.portable;" +
        "org.omg.PortableServer.ServantLocatorPackage;" +
        "org.omg.SendingContext;" +
        "org.omg.stub.java.rmi;" +
        "org.omg.stub.javax.management.remote.rmi;" +
        "org.w3c.dom;" +
        "org.w3c.dom.bootstrap;" +
        "org.w3c.dom.css;" +
        "org.w3c.dom.events;" +
        "org.w3c.dom.html;" +
        "org.w3c.dom.ls;" +
        "org.w3c.dom.ranges;" +
        "org.w3c.dom.stylesheets;" +
        "org.w3c.dom.traversal;" +
        "org.w3c.dom.views;" +
        "org.w3c.dom.xpath;" +
        "org.xml.sax;" +
        "org.xml.sax.ext;" +
        "org.xml.sax.helpers;" +
        "version=\"1.6.0\"";

    /**
     * The name of the felix cache profile.
     * 
     * @parameter expression=”${run.felix.cache.profile}” default-value="test"
     * @description the felix.cache.profile property value
     */
    private String felixCacheProfile;
    
    /**
     * The location of the felix bundle cache directory.
     * 
     * @parameter expression=”${run.felix.cache.dir}” default-value="${basedir}/target/.felix"
     * @description the felix.cache.dir property value
     */
    private File felixCacheDir;
    
    /**
     * The location of the test bundle.
     * 
     * @parameter expression="${run.felix.test.bundle}" 
     *            default-value="${basedir}/target/${project.artifactId}-${project.version}.jar"
     */
    private File felixTestBundle;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private List exclusions;
    
    /**
     * The felix container used to run the integration tests.
     */
    private Felix felixContainer = new Felix();


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // -------------------------------------------------------------------
        // Clean out the old cache directory if it exists
        // -------------------------------------------------------------------
        
        if ( felixCacheDir.exists() )
        {
            try
            {
                FileUtils.forceDelete( felixCacheDir );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "failed to delete old Felix cache directory: " 
                    + felixCacheDir.getAbsolutePath() );
            }
        }

        // -------------------------------------------------------------------
        // Build the properties we will stuff into the resolver
        // -------------------------------------------------------------------

        Properties props = new Properties();
        props.put( "felix.cache.dir", felixCacheDir.getAbsolutePath() );
        props.put( "felix.cache.profile", felixCacheProfile );
        props.put( "felix.embedded.execution", "true" );
        
        StringBuffer buf = new StringBuffer();
        buf.append( "org.osgi.framework; version=1.3.0, " );
        buf.append( "org.osgi.service.packageadmin; version=1.2.0, " );
        buf.append( "org.osgi.service.startlevel; version=1.0.0, " );
        buf.append( "org.osgi.service.url; version=1.0.0, " );

        String version = System.getProperty( "java.version" );
        if ( version.indexOf( "1.3" ) != -1 )
        {
            buf.append( JRE_1_3_PACKAGES );
        }
        else if ( version.indexOf( "1.4" ) != -1  ) 
        {
            buf.append( JRE_1_4_PACKAGES );
        }
        else if ( version.indexOf( "1.5" ) != -1  )
        {
            buf.append( JRE_1_5_PACKAGES );
        }
        else if ( version.indexOf( "1.6" ) != -1  )
        {
            buf.append( JRE_1_6_PACKAGES );
        }
        else
        {
            throw new IllegalStateException( "java.version = " + version + " is not recognized" );
        }
        
        props.put( "org.osgi.framework.system.packages", buf.toString() );
        try
        {
            props.put( "felix.auto.start.1", getAutoStart() );
        }
        catch ( MalformedURLException e )
        {
            e.printStackTrace();
        }
        
        // -------------------------------------------------------------------
        // Start up Felix with resolver and shut it down
        // -------------------------------------------------------------------

        MutablePropertyResolver resolver = new MutablePropertyResolverImpl(props);
        felixContainer.start( resolver, new ArrayList() );
        getLog().info( "-=============================-" );
        getLog().info( "| Felix: successfully started |" );
        getLog().info( "-=============================-" );
        felixContainer.shutdown();
        getLog().info( "-==============================-" );
        getLog().info( "| Felix: successfully shutdown |" );
        getLog().info( "-==============================-" );
    }
    

    public String getAutoStart() throws MalformedURLException
    {
        List included = new ArrayList();
        List excluded = new ArrayList();
        if ( exclusions == null )
        {
            exclusions = Collections.EMPTY_LIST;
        }
        
        StringBuffer buf = new StringBuffer();
        StringBuffer keyBuf = new StringBuffer();
        for ( Iterator ii = project.getDependencyArtifacts().iterator(); ii.hasNext(); /**/) 
        {
            Artifact dep = ( Artifact ) ii.next();
            keyBuf.setLength( 0 );
            keyBuf.append( dep.getGroupId() ).append( ":" ).append( dep.getArtifactId() );
            String depKey = keyBuf.toString();
            
            // -------------------------------------------------------------------
            // Add only provided dependent artifacts that have not been excluded
            // -------------------------------------------------------------------
            
            if ( dep.getScope().equalsIgnoreCase( "provided" ) )
            {
                if ( dep.getArtifactId().equalsIgnoreCase( "org.osgi.core" ) ||
                     exclusions.contains( depKey ) )
                {
                    excluded.add( depKey );
                    continue;
                }
                
                included.add( depKey );
                buf.append( dep.getFile().toURL() );
                buf.append( " " );
            }
        }
        
        keyBuf.setLength( 0 );
        keyBuf.append( project.getGroupId() ).append( ":" ).append( project.getArtifactId() );
        String depKey = keyBuf.toString();
        included.add( depKey );
        buf.append( felixTestBundle.toURL() );
        
        // -------------------------------------------------------------------
        // Report what was included and what was excluded
        // -------------------------------------------------------------------
        
        getLog().info( "" );
        getLog().info( "\t\tJars/Bundles Included for Autostart" ); 
        getLog().info( "\t\t-----------------------------------" );
        getLog().info( "" );
        for ( Iterator ii = included.iterator(); ii.hasNext(); /**/ )
        {
            getLog().info( "\t\t" + ( String ) ii.next() );
        }
        getLog().info( "" );
        getLog().info( "" );
        getLog().info( "\t\tJars/Bundles Excluded from Autostart" );
        getLog().info( "\t\t------------------------------------" );
        getLog().info( "" );
        for ( Iterator ii = excluded.iterator(); ii.hasNext(); /**/ )
        {
            getLog().info( "\t\t" + ( String ) ii.next() );
        }
        getLog().info( "" );
        getLog().info( "" );
        return buf.toString();
    }
}
