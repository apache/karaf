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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * this class describe all information by bundle.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourcesBundle
{
    /**
     * store the bundle symbolic name.
     */
    private String m_symbolicName;

    /**
     * store the bundle presentation name.
     */
    private String m_presentationName;

    /**
     * store the bundle version.
     */
    private String m_version;

    /**
     * store the bundle URI.
     */
    private String m_uri;

    /**
     * store the bundle description.
     */
    private String m_description;

    /**
     * store the bundle size.
     */
    private String m_size;

    /**
     * store the bundle documentation.
     */
    private String m_documentation;

    /**
     * store the bundle source.
     */
    private String m_source;

    /**
     * store the bundle license.
     */
    private String m_license;

    /**
     * store the bundle id.
     */
    private String m_id;

    /**
     * store the bundle categories.
     */
    private List m_category = new ArrayList();

    /**
     * store the bundle capabilities.
     */
    private List m_capability = new ArrayList();

    /**
     * store the bundle requirement.
     */
    private List m_require = new ArrayList();

    /**
     * get the plugin logger.
     */
    private Log m_logger;


    /**
     * initialize logger.
     * @param log log use by plugin
     */
    public ResourcesBundle( Log log )
    {
        m_logger = log;
    }


    public List getCapability()
    {
        return m_capability;
    }


    public void setCapability( List capability )
    {
        m_capability = capability;
    }


    public List getCategory()
    {
        return m_category;
    }


    public void setCategory( List category )
    {
        m_category = category;
    }


    public String getLicense()
    {
        return m_license;
    }


    public void setLicense( String license )
    {
        m_license = license;
    }


    public String getDescription()
    {
        return m_description;
    }


    public void setDescription( String description )
    {
        m_description = description;
    }


    public String getDocumentation()
    {
        return m_documentation;
    }


    public void setDocumentation( String documentation )
    {
        m_documentation = documentation;
    }


    public String getPresentationName()
    {
        return m_presentationName;
    }


    public void setPresentationName( String name )
    {
        m_presentationName = name;
    }


    public String getSize()
    {
        return m_size;
    }


    public void setSize( String size )
    {
        m_size = size;
    }


    public String getSymbolicName()
    {
        return m_symbolicName;
    }


    public void setSymbolicName( String name )
    {
        m_symbolicName = name;
    }


    public String getUri()
    {
        return m_uri;
    }


    public void setUri( String url )
    {
        m_uri = url;
    }


    public String getVersion()
    {
        return m_version;
    }


    public void setVersion( String version )
    {
        m_version = version;
    }


    public List getRequire()
    {
        return m_require;
    }


    public void setRequire( List require )
    {
        m_require = require;
    }


    public String getSource()
    {
        return m_source;
    }


    public void setSource( String source )
    {
        m_source = source;
    }


    public String getId()
    {
        return m_id;
    }


    public void setId( String id )
    {
        m_id = id;
    }


    /**
     * add a new capability for this bundle description.
     * @param capability the Capability to add
     */
    public void addCapability( Capability capability )
    {
        m_capability.add( capability );
    }


    /**
     * add a new requirement for this bundle description.
     * @param require th Require to add
     */
    public void addRequire( Require require )
    {
        m_require.add( require );
    }


    /**
     * add a new category for this bundle decription.
     * @param category the Category to add
     */
    public void addCategory( Category category )
    {
        m_category.add( category );
    }


    /**
     * transform this object to Node.
     * tranform all sub-object to node also
     * @param father father document for create Node
     * @return node
     */
    public Node getNode( Document father )
    {
        // return the complete resource tree
        if ( !isValid() || getId() == null )
        {
            m_logger.error( "those properties was not defined:" + getInvalidProperties() );
            return null;
        }

        Element resource = father.createElement( "resource" );
        Element description = father.createElement( "description" );
        Element size = father.createElement( "size" );
        Element documentation = father.createElement( "documentation" );
        Element source = father.createElement( "source" );
        Element license = father.createElement( "license" );

        resource.setAttribute( "id", getId() );
        resource.setAttribute( "symbolicname", getSymbolicName() );
        resource.setAttribute( "presentationname", getPresentationName() );
        resource.setAttribute( "uri", getUri() );
        resource.setAttribute( "version", getVersion() );

        XmlHelper.setTextContent( description, getDescription() );
        resource.appendChild( description );

        XmlHelper.setTextContent( size, getSize() );
        resource.appendChild( size );

        if ( getDocumentation() != null )
        {
            XmlHelper.setTextContent( documentation, getDocumentation() );
            resource.appendChild( documentation );
        }

        if ( getSource() != null )
        {
            XmlHelper.setTextContent( source, getSource() );
            resource.appendChild( source );
        }

        if ( getLicense() != null )
        {
            XmlHelper.setTextContent( license, getLicense() );
            resource.appendChild( license );
        }

        List list = getNodeCategories( father );
        for ( int i = 0; i < list.size(); i++ )
        {
            resource.appendChild( ( Node ) list.get( i ) );
        }

        list = getNodeCapabilities( father );
        for ( int i = 0; i < list.size(); i++ )
        {
            resource.appendChild( ( Node ) list.get( i ) );
        }

        list = getNodeRequirement( father );
        for ( int i = 0; i < list.size(); i++ )
        {
            resource.appendChild( ( Node ) list.get( i ) );
        }

        return resource;
    }


    /**
     * this method gets information form pom.xml to complete missing data from those given by user.
     * @param project project information given by maven
     * @param ebi bundle information extracted from bindex
     * @return true
     */
    public boolean construct( MavenProject project, ExtractBindexInfo ebi )
    {

        if ( ebi.getPresentationName() != null )
        {
            setPresentationName( ebi.getPresentationName() );
            if ( project.getName() != null )
            {
                m_logger.debug( "pom property override:<presentationname> " + project.getName() );
            }
        }
        else
        {
            setPresentationName( project.getName() );
        }

        if ( ebi.getSymbolicName() != null )
        {
            setSymbolicName( ebi.getSymbolicName() );
            if ( project.getArtifactId() != null )
            {
                m_logger.debug( "pom property override:<symbolicname> " + project.getArtifactId() );
            }
        }
        else
        {
            setSymbolicName( project.getArtifactId() );
        }

        if ( ebi.getVersion() != null )
        {
            setVersion( ebi.getVersion() );
            if ( project.getVersion() != null )
            {
                m_logger.debug( "pom property override:<version> " + project.getVersion() );
            }
        }
        else
        {
            setVersion( project.getVersion() );
        }

        if ( ebi.getId() != null )
        {
            setId( ebi.getId() );
        }

        if ( ebi.getDescription() != null )
        {
            setDescription( ebi.getDescription() );
            if ( project.getDescription() != null )
            {
                m_logger.debug( "pom property override:<description> " + project.getDescription() );
            }
        }
        else
        {
            setDescription( project.getDescription() );
        }

        if ( ebi.getDocumentation() != null )
        {
            setDocumentation( ebi.getDocumentation() );
            if ( project.getUrl() != null )
            {
                m_logger.debug( "pom property override:<documentation> " + project.getUrl() );
            }
        }
        else
        {
            setDocumentation( project.getUrl() );
        }

        if ( ebi.getSource() != null )
        {
            setSource( ebi.getSource() );
            if ( project.getScm() != null )
            {
                m_logger.debug( "pom property override:<source> " + project.getScm() );
            }
        }
        else
        {
            String src = null;
            if ( project.getScm() != null )
            {
                src = project.getScm().getUrl();
            }
            setSource( src );
        }

        if ( ebi.getLicense() != null )
        {
            setLicense( ebi.getLicense() );
            String lic = null;
            List l = project.getLicenses();
            Iterator it = l.iterator();
            while ( it.hasNext() )
            {
                if ( it.next() != null )
                {
                    m_logger.debug( "pom property override:<license> " + lic );
                    break;
                }
            }
        }
        else
        {
            String lic = null;
            List l = project.getLicenses();
            Iterator it = l.iterator();
            while ( it.hasNext() )
            {
                lic = it.next() + ";";
            }

            setLicense( lic );
        }

        // create the first capability (ie : bundle)
        Capability capability = new Capability();
        capability.setName( "bundle" );
        PElement p = new PElement();
        p.setN( "manifestversion" );
        p.setV( "2" );
        capability.addP( p );

        p = new PElement();
        p.setN( "presentationname" );
        p.setV( getPresentationName() );
        capability.addP( p );

        p = new PElement();
        p.setN( "symbolicname" );
        p.setV( getSymbolicName() );
        capability.addP( p );

        p = new PElement();
        p.setN( "version" );
        p.setT( "version" );
        p.setV( getVersion() );
        capability.addP( p );

        addCapability( capability );

        List capabilities = ebi.getCapabilities();
        for ( int i = 0; i < capabilities.size(); i++ )
        {
            addCapability( ( Capability ) capabilities.get( i ) );
        }

        List requirement = ebi.getRequirement();
        for ( int i = 0; i < requirement.size(); i++ )
        {
            addRequire( ( Require ) requirement.get( i ) );
        }

        // we also add the goupId
        Category category = new Category();
        category.setId( project.getGroupId() );
        addCategory( category );

        return true;
    }


    /**
     * return if the bundle resource is complete.
     * @return false if an information is missing, else true
     */
    public boolean isValid()
    {
        // we must verify required properties are present
        return getPresentationName() != null && getSymbolicName() != null && getVersion() != null && getUri() != null
            && getSize() != null;
    }


    /**
     * test if this bundle has the same symbolicname, and version number.
     * @param symbolicName symbolicName to compare with current bundle
     * @param presentationName presentationName to compare with current bundlde
     * @param version version to compare with current bundle
     * @return true if the information are the same, else false
     */
    public boolean isSameBundleResource( String symbolicName, String version )
    {
        if ( isValid() )
        {
            return ( symbolicName.compareTo( getSymbolicName() ) == 0 ) && ( version.compareTo( getVersion() ) == 0 );
        }

        return false;
    }


    /**
     * return a list of categories transformed to node.
     * @param father father document to create node from same document
     * @return List of Node
     */
    private List getNodeCategories( Document father )
    {
        List listNode = new ArrayList();
        List listCategory = getCategory();
        for ( int i = 0; i < listCategory.size(); i++ )
        {
            listNode.add( ( ( Category ) listCategory.get( i ) ).getNode( father ) );
        }
        return listNode;
    }


    /**
     * return a list of capabilities transformed to node.
     * @param father father document to create node from same document
     * @return List of Node
     */
    private List getNodeCapabilities( Document father )
    {
        List listNode = new ArrayList();
        List listCapability = getCapability();
        for ( int i = 0; i < listCapability.size(); i++ )
        {
            listNode.add( ( ( Capability ) listCapability.get( i ) ).getNode( father ) );
        }
        return listNode;
    }


    /**
     * return a list of requirement transformed to node.
     * @param father father document to create node from same document
     * @return List of Node.
     */
    private List getNodeRequirement( Document father )
    {
        List listNode = new ArrayList();
        List listRequirement = getRequire();
        for ( int i = 0; i < listRequirement.size(); i++ )
        {
            listNode.add( ( ( Require ) listRequirement.get( i ) ).getNode( father ) );
        }
        return listNode;
    }


    /**
     * return the list of properties not define in this bundle resource.
     * @return list of properties not define
     */
    private String getInvalidProperties()
    {
        if ( isValid() )
        {
            if ( getId() == null )
            {
                return "id";
            }

            return "";
        }
        String result = "";
        if ( getPresentationName() == null )
        {
            result = result + "presentationName;";
        }
        if ( getSymbolicName() == null )
        {
            result = result + "symbolicName;";
        }
        if ( getVersion() == null )
        {
            result = result + "version;";
        }
        if ( getUri() == null )
        {
            result = result + "Uri;";
        }
        if ( getSize() == null )
        {
            result = result + "Size";
        }
        return result;
    }

}
