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
package org.apache.felix.scrplugin;


import java.io.InputStream;
import java.util.*;

import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.tags.*;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProviderManager;
import org.apache.felix.scrplugin.tags.cl.ClassLoaderJavaClassDescription;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;


/**
 * The <code>JavaClassDescriptorManager</code> must be implemented to provide
 * access to the java sources to be scanned for descriptor annotations and
 * JavaDoc tags, the descriptors of components from the class path and the
 * location of the generated class files to be able to add the bind and unbind
 * methods.
 */
public abstract class JavaClassDescriptorManager
{

    /** The maven log. */
    protected final Log log;

    /** The classloader used to compile the classes. */
    private final ClassLoader classloader;

    /** A cache containing the java class descriptions hashed by classname. */
    private final Map<String, JavaClassDescription> javaClassDescriptions = new HashMap<String, JavaClassDescription>();

    /**
     * Supports mapping of built-in and custom java anntoations to {@link JavaTag} implementations.
     */
    private final AnnotationTagProviderManager annotationTagProviderManager;

    /** Parse Javadocs? */
    private final boolean parseJavadocs;

    /** Process Annotations? */
    private final boolean processAnnotations;


    /**
     * Construct a new manager.
     * @param log
     * @param annotationTagProviders List of annotation tag providers
     * @param parseJavadocs Should the javadocs be parsed?
     * @param processAnnotations Should the annotations be processed?
     * @throws SCRDescriptorFailureException
     */
    public JavaClassDescriptorManager( final Log log, final ClassLoader classLoader,
        final String[] annotationTagProviders, final boolean parseJavadocs, final boolean processAnnotations )
        throws SCRDescriptorFailureException
    {
        this.processAnnotations = processAnnotations;
        this.parseJavadocs = parseJavadocs;
        this.log = log;
        this.annotationTagProviderManager = new AnnotationTagProviderManager( annotationTagProviders );
        this.classloader = classLoader;
        ClassUtil.classLoader = this.classloader;
    }


    /**
     * Returns the QDox JavaSource instances representing the source files
     * for which the Declarative Services and Metatype descriptors have to be
     * generated.
     *
     * @throws SCRDescriptorException May be thrown if an error occurrs gathering
     *      the java sources.
     */
    protected abstract JavaSource[] getSources() throws SCRDescriptorException;


    /**
     * Returns a map of component descriptors which may be extended by the java
     * sources returned by the {@link #getSources()} method.
     *
     * @throws SCRDescriptorException May be thrown if an error occurrs gethering
     *      the component descriptors.
     */
    protected abstract Map<String, Component> getComponentDescriptors() throws SCRDescriptorException;


    /**
     * Returns the absolute filesystem path to the directory where the classes
     * compiled from the java source files (see {@link #getSources()}) have been
     * placed.
     * <p>
     * This method is called to find the class files to which bind and unbind
     * methods are to be added.
     */
    public abstract String getOutputDirectory();


    /**
     * Return the log.
     */
    public Log getLog()
    {
        return this.log;
    }


    /**
     * Return the class laoder.
     */
    public ClassLoader getClassLoader()
    {
        return this.classloader;
    }


    /**
     * @return Annotation tag provider manager
     */
    public AnnotationTagProviderManager getAnnotationTagProviderManager()
    {
        return this.annotationTagProviderManager;
    }


    /**
     * Returns <code>true</code> if this class descriptor manager is parsing
     * JavaDoc tags.
     */
    public boolean isParseJavadocs()
    {
        return parseJavadocs;
    }


    /**
     * Returns <code>true</code> if this class descriptor manager is parsing
     * Java 5 annotations.
     */
    public boolean isProcessAnnotations()
    {
        return processAnnotations;
    }


    /**
     * Parses the descriptors read from the given input stream. This method
     * may be called by the {@link #getComponentDescriptors()} method to parse
     * the descriptors gathered in an implementation dependent way.
     *
     * @throws SCRDescriptorException If an error occurrs reading the descriptors
     *      from the stream.
     */
    protected Components parseServiceComponentDescriptor( InputStream file ) throws SCRDescriptorException
    {
        return ComponentDescriptorIO.read( file );
    }


    /**
     * Return all source descriptions of this project.
     * @return All contained java class descriptions.
     */
    public JavaClassDescription[] getSourceDescriptions() throws SCRDescriptorException
    {
        final JavaClass[] javaClasses = getJavaClassesFromSources();
        final JavaClassDescription[] descs = new JavaClassDescription[javaClasses.length];
        for ( int i = 0; i < javaClasses.length; i++ )
        {
            descs[i] = this.getJavaClassDescription( javaClasses[i].getFullyQualifiedName() );
        }
        return descs;
    }


    /**
     * Get a java class description for the class.
     * @param className
     * @return The java class description.
     * @throws SCRDescriptorException
     */
    public JavaClassDescription getJavaClassDescription( String className ) throws SCRDescriptorException
    {
        JavaClassDescription result = this.javaClassDescriptions.get( className );
        if ( result == null )
        {
            this.log.debug( "Searching description for: " + className );
            int index = 0;
            final JavaClass[] javaClasses = getJavaClassesFromSources();
            while ( result == null && index < javaClasses.length )
            {
                final JavaClass javaClass = javaClasses[index];
                if ( javaClass.getFullyQualifiedName().equals( className ) )
                {
                    try
                    {
                        // check for java annotation descriptions - fallback to QDox if none found
                        Class<?> clazz = this.classloader.loadClass( className );
                        if ( this.processAnnotations
                            && getAnnotationTagProviderManager().hasScrPluginAnnotation( javaClass ) )
                        {
                            this.log.debug( "Found java annotation description for: " + className );
                            result = new AnnotationJavaClassDescription( clazz, javaClasses[index], this );
                        }
                        else if ( this.parseJavadocs )
                        {
                            this.log.debug( "Found qdox description for: " + className );
                            result = new QDoxJavaClassDescription( clazz, javaClasses[index], this );
                        }
                    }
                    catch ( ClassNotFoundException e )
                    {
                        throw new SCRDescriptorException( "Unable to load class", className, 0 );
                    }
                }
                else
                {
                    index++;
                }
            }
            if ( result == null )
            {
                try
                {
                    this.log.debug( "Generating classloader description for: " + className );
                    result = new ClassLoaderJavaClassDescription( this.classloader.loadClass( className ), this
                        .getComponentDescriptors().get( className ), this );
                }
                catch ( ClassNotFoundException e )
                {
                    throw new SCRDescriptorException( "Unable to load class", className, 0);
                }
            }
            this.javaClassDescriptions.put( className, result );
        }
        return result;
    }


    /**
     * Get a list of all {@link JavaClass} definitions four all source files (including nested/inner classes)
     * @return List of {@link JavaClass} definitions
     */
    private JavaClass[] getJavaClassesFromSources() throws SCRDescriptorException
    {
        final JavaSource[] sources = this.getSources();
        final List<JavaClass> classes = new ArrayList<JavaClass>();
        for ( int i = 0; i < sources.length; i++ )
        {
            for ( int j = 0; j < sources[i].getClasses().length; j++ )
            {
                final JavaClass clazz = sources[i].getClasses()[j];
                classes.add( clazz );
                for ( int k = 0; k < clazz.getNestedClasses().length; k++ )
                {
                    final JavaClass nestedClass = clazz.getNestedClasses()[k];
                    classes.add( nestedClass );
                }
            }
        }
        return classes.toArray( new JavaClass[classes.size()] );
    }

}
