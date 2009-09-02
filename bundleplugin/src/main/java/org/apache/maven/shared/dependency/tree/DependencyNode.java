package org.apache.maven.shared.dependency.tree;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.SerializingDependencyNodeVisitor;

/**
 * Represents an artifact node within a Maven project's dependency tree.
 * 
 * @author Edwin Punzalan
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyNode.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public class DependencyNode
{
    // constants --------------------------------------------------------------

    /**
     * State that represents an included dependency node.
     * 
     * @since 1.1
     */
    public static final int INCLUDED = 0;

    /**
     * State that represents a dependency node that has been omitted for duplicating another dependency node.
     * 
     * @since 1.1
     */
    public static final int OMITTED_FOR_DUPLICATE = 1;

    /**
     * State that represents a dependency node that has been omitted for conflicting with another dependency node.
     * 
     * @since 1.1
     */
    public static final int OMITTED_FOR_CONFLICT = 2;

    /**
     * State that represents a dependency node that has been omitted for introducing a cycle into the dependency tree.
     * 
     * @since 1.1
     */
    public static final int OMITTED_FOR_CYCLE = 3;
    
    // classes ----------------------------------------------------------------
    
    /**
     * Utility class to concatenate a number of parameters with separator tokens.   
     */
    private static class ItemAppender
    {
        private StringBuffer buffer;
        
        private String startToken;
        
        private String separatorToken;
        
        private String endToken;
        
        private boolean appended;
        
        public ItemAppender( StringBuffer buffer, String startToken, String separatorToken, String endToken )
        {
            this.buffer = buffer;
            this.startToken = startToken;
            this.separatorToken = separatorToken;
            this.endToken = endToken;
            
            appended = false;
        }

        public ItemAppender append( String item )
        {
            appendToken();
            
            buffer.append( item );
            
            return this;
        }
        
        public ItemAppender append( String item1, String item2 )
        {
            appendToken();
            
            buffer.append( item1 ).append( item2 );
            
            return this;
        }
        
        public void flush()
        {
            if ( appended )
            {
                buffer.append( endToken );
                
                appended = false;
            }
        }
        
        private void appendToken()
        {
            buffer.append( appended ? separatorToken : startToken );
            
            appended = true;
        }
    }

    // fields -----------------------------------------------------------------

    /**
     * The artifact that is attached to this dependency node.
     */
    private final Artifact artifact;

    /**
     * The list of child dependency nodes of this dependency node.
     */
    private final List children;

    /**
     * The parent dependency node of this dependency node.
     */
    private DependencyNode parent;

    /**
     * The state of this dependency node. This can be either <code>INCLUDED</code>,
     * <code>OMITTED_FOR_DUPLICATE</code>, <code>OMITTED_FOR_CONFLICT</code> or <code>OMITTED_FOR_CYCLE</code>.
     * 
     * @see #INCLUDED
     * @see #OMITTED_FOR_DUPLICATE
     * @see #OMITTED_FOR_CONFLICT
     * @see #OMITTED_FOR_CYCLE
     */
    private int state;

    /**
     * The artifact related to the state of this dependency node. For dependency nodes with a state of
     * <code>OMITTED_FOR_DUPLICATE</code> or <code>OMITTED_FOR_CONFLICT</code>, this represents the artifact that
     * was conflicted with. For dependency nodes of other states, this is always <code>null</code>.
     */
    private Artifact relatedArtifact;
    
    /**
     * The scope of this node's artifact before it was updated due to conflicts, or <code>null</code> if the artifact
     * scope has not been updated.
     */
    private String originalScope;

    /**
     * The scope that this node's artifact was attempted to be updated to due to conflicts, or <code>null</code> if
     * the artifact scope has not failed being updated.
     */
    private String failedUpdateScope;

    /**
     * The version of this node's artifact before it was updated by dependency management, or <code>null</code> if the
     * artifact version has not been managed.
     */
    private String premanagedVersion;
    
    /**
     * The scope of this node's artifact before it was updated by dependency management, or <code>null</code> if the
     * artifact scope has not been managed.
     */
    private String premanagedScope;

    private VersionRange versionSelectedFromRange;
    
    private List availableVersions;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new dependency node for the specified artifact with an included state.
     * 
     * @param artifact
     *            the artifact attached to the new dependency node
     * @throws IllegalArgumentException
     *             if the parameter constraints were violated
     * @since 1.1
     */
    public DependencyNode( Artifact artifact )
    {
        this( artifact, INCLUDED );
    }

    /**
     * Creates a new dependency node for the specified artifact with the specified state.
     * 
     * @param artifact
     *            the artifact attached to the new dependency node
     * @param state
     *            the state of the new dependency node. This can be either <code>INCLUDED</code> or
     *            <code>OMITTED_FOR_CYCLE</code>.
     * @throws IllegalArgumentException
     *             if the parameter constraints were violated
     * @since 1.1
     */
    public DependencyNode( Artifact artifact, int state )
    {
        this( artifact, state, null );
    }

    /**
     * Creates a new dependency node for the specified artifact with the specified state and related artifact.
     * 
     * @param artifact
     *            the artifact attached to the new dependency node
     * @param state
     *            the state of the new dependency node. This can be either <code>INCLUDED</code>,
     *            <code>OMITTED_FOR_DUPLICATE</code>, <code>OMITTED_FOR_CONFLICT</code> or
     *            <code>OMITTED_FOR_CYCLE</code>.
     * @param relatedArtifact
     *            the artifact related to the state of this dependency node. For dependency nodes with a state of
     *            <code>OMITTED_FOR_DUPLICATE</code> or <code>OMITTED_FOR_CONFLICT</code>, this represents the
     *            artifact that was conflicted with. For dependency nodes of other states, this should always be
     *            <code>null</code>.
     * @throws IllegalArgumentException
     *             if the parameter constraints were violated
     * @since 1.1
     */
    public DependencyNode( Artifact artifact, int state, Artifact relatedArtifact )
    {
        if ( artifact == null )
        {
            throw new IllegalArgumentException( "artifact cannot be null" );
        }

        if ( state < INCLUDED || state > OMITTED_FOR_CYCLE )
        {
            throw new IllegalArgumentException( "Unknown state: " + state );
        }

        boolean requiresRelatedArtifact = ( state == OMITTED_FOR_DUPLICATE || state == OMITTED_FOR_CONFLICT );

        if ( requiresRelatedArtifact && relatedArtifact == null )
        {
            throw new IllegalArgumentException( "Related artifact is required for states "
                            + "OMITTED_FOR_DUPLICATE and OMITTED_FOR_CONFLICT" );
        }

        if ( !requiresRelatedArtifact && relatedArtifact != null )
        {
            throw new IllegalArgumentException( "Related artifact is only required for states "
                            + "OMITTED_FOR_DUPLICATE and OMITTED_FOR_CONFLICT" );
        }

        this.artifact = artifact;
        this.state = state;
        this.relatedArtifact = relatedArtifact;

        children = new ArrayList();
    }
    
    /**
     * Creates a new dependency node.
     * 
     * @deprecated As of 1.1, replaced by {@link #DependencyNode(Artifact, int, Artifact)}
     */
    DependencyNode()
    {
        artifact = null;
        children = new ArrayList();
    }

    // public methods ---------------------------------------------------------

    /**
     * Applies the specified dependency node visitor to this dependency node and its children.
     * 
     * @param visitor
     *            the dependency node visitor to use
     * @return the visitor result of ending the visit to this node
     * @since 1.1
     */
    public boolean accept( DependencyNodeVisitor visitor )
    {
        if ( visitor.visit( this ) )
        {
            boolean visiting = true;

            for ( Iterator iterator = getChildren().iterator(); visiting && iterator.hasNext(); )
            {
                DependencyNode child = (DependencyNode) iterator.next();

                visiting = child.accept( visitor );
            }
        }

        return visitor.endVisit( this );
    }

    /**
     * Adds the specified dependency node to this dependency node's children.
     * 
     * @param child
     *            the child dependency node to add
     * @since 1.1
     */
    public void addChild( DependencyNode child )
    {
        children.add( child );
        child.parent = this;
    }

    /**
     * Removes the specified dependency node from this dependency node's children.
     * 
     * @param child
     *            the child dependency node to remove
     * @since 1.1
     */
    public void removeChild( DependencyNode child )
    {
        children.remove( child );
        child.parent = null;
    }

    /**
     * Gets the parent dependency node of this dependency node.
     * 
     * @return the parent dependency node
     */
    public DependencyNode getParent()
    {
        return parent;
    }

    /**
     * Gets the artifact attached to this dependency node.
     * 
     * @return the artifact
     */
    public Artifact getArtifact()
    {
        return artifact;
    }
    
    /**
     * Gets the depth of this dependency node within its hierarchy.
     * 
     * @return the depth
     * @deprecated As of 1.1, depth is computed by node hierarchy. With the introduction of node
     *             visitors and filters this method can give misleading results. For example, consider
     *             serialising a tree with a filter using a visitor: this method would return the
     *             unfiltered depth of a node, whereas the correct depth would be calculated by the
     *             visitor.
     */
    public int getDepth()
    {
        int depth = 0;
        
        DependencyNode node = getParent();
        
        while ( node != null )
        {
            depth++;
            
            node = node.getParent();
        }
        
        return depth;
    }

    /**
     * Gets the list of child dependency nodes of this dependency node.
     * 
     * @return the list of child dependency nodes
     */
    public List getChildren()
    {
        return Collections.unmodifiableList( children );
    }

    public boolean hasChildren()
    {
        return children.size() > 0;
    }

    /**
     * Gets the state of this dependency node.
     * 
     * @return the state: either <code>INCLUDED</code>, <code>OMITTED_FOR_DUPLICATE</code>,
     *         <code>OMITTED_FOR_CONFLICT</code> or <code>OMITTED_FOR_CYCLE</code>.
     * @since 1.1
     */
    public int getState()
    {
        return state;
    }

    /**
     * Gets the artifact related to the state of this dependency node. For dependency nodes with a state of
     * <code>OMITTED_FOR_CONFLICT</code>, this represents the artifact that was conflicted with. For dependency nodes
     * of other states, this is always <code>null</code>.
     * 
     * @return the related artifact
     * @since 1.1
     */
    public Artifact getRelatedArtifact()
    {
        return relatedArtifact;
    }
    
    /**
     * Gets the scope of this node's artifact before it was updated due to conflicts.
     * 
     * @return the original scope, or <code>null</code> if the artifact scope has not been updated
     * @since 1.1
     */
    public String getOriginalScope()
    {
        return originalScope;
    }

    /**
     * Sets the scope of this node's artifact before it was updated due to conflicts.
     * 
     * @param originalScope
     *            the original scope, or <code>null</code> if the artifact scope has not been updated
     * @since 1.1
     */
    public void setOriginalScope( String originalScope )
    {
        this.originalScope = originalScope;
    }

    /**
     * Gets the scope that this node's artifact was attempted to be updated to due to conflicts.
     * 
     * @return the failed update scope, or <code>null</code> if the artifact scope has not failed being updated
     * @since 1.1
     */
    public String getFailedUpdateScope()
    {
        return failedUpdateScope;
    }

    /**
     * Sets the scope that this node's artifact was attempted to be updated to due to conflicts.
     * 
     * @param failedUpdateScope
     *            the failed update scope, or <code>null</code> if the artifact scope has not failed being updated
     * @since 1.1
     */
    public void setFailedUpdateScope( String failedUpdateScope )
    {
        this.failedUpdateScope = failedUpdateScope;
    }
    
    /**
     * Gets the version of this node's artifact before it was updated by dependency management.
     * 
     * @return the premanaged version, or <code>null</code> if the artifact version has not been managed
     * @since 1.1
     */
    public String getPremanagedVersion()
    {
        return premanagedVersion;
    }

    /**
     * Sets the version of this node's artifact before it was updated by dependency management.
     * 
     * @param premanagedVersion
     *            the premanaged version, or <code>null</code> if the artifact version has not been managed
     * @since 1.1
     */
    public void setPremanagedVersion( String premanagedVersion )
    {
        this.premanagedVersion = premanagedVersion;
    }
    
    /**
     * Gets the scope of this node's artifact before it was updated by dependency management.
     * 
     * @return the premanaged scope, or <code>null</code> if the artifact scope has not been managed
     * @since 1.1
     */
    public String getPremanagedScope()
    {
        return premanagedScope;
    }
    
    /**
     * Sets the scope of this node's artifact before it was updated by dependency management.
     * 
     * @param premanagedScope
     *            the premanaged scope, or <code>null</code> if the artifact scope has not been managed
     * @since 1.1
     */
    public void setPremanagedScope( String premanagedScope )
    {
        this.premanagedScope = premanagedScope;
    }

    /**
     * If the version was selected from a range this method will return the range.
     * 
     * @return the version range before a version was selected, or <code>null</code> if the artifact had a explicit
     *         version.
     * @since 1.2
     */
    public VersionRange getVersionSelectedFromRange()
    {
        return versionSelectedFromRange;
    }
    
    public void setVersionSelectedFromRange( VersionRange versionSelectedFromRange )
    {
        this.versionSelectedFromRange = versionSelectedFromRange;
    }

    /**
     * If the version was selected from a range this method will return the available versions when making the decision.
     * 
     * @return {@link List} &lt; {@link String} > the versions available when a version was selected from a range, or
     *         <code>null</code> if the artifact had a explicit version.
     * @since 1.2
     */
    public List getAvailableVersions()
    {
        return availableVersions;
    }
    
    public void setAvailableVersions( List availableVersions )
    {
        this.availableVersions = availableVersions;
    }

    /**
     * Changes the state of this dependency node to be omitted for conflict or duplication, depending on the specified
     * related artifact.
     * 
     * <p>
     * If the related artifact has a version equal to this dependency node's artifact, then this dependency node's state
     * is changed to <code>OMITTED_FOR_DUPLICATE</code>, otherwise it is changed to <code>OMITTED_FOR_CONFLICT</code>.
     * Omitting this dependency node also removes all of its children.
     * </p>
     * 
     * @param relatedArtifact
     *            the artifact that this dependency node conflicted with
     * @throws IllegalStateException
     *             if this dependency node's state is not <code>INCLUDED</code>
     * @throws IllegalArgumentException
     *             if the related artifact was <code>null</code> or had a different dependency conflict id to this
     *             dependency node's artifact
     * @see #OMITTED_FOR_DUPLICATE
     * @see #OMITTED_FOR_CONFLICT
     * @since 1.1
     */
    public void omitForConflict( Artifact relatedArtifact )
    {
        if ( getState() != INCLUDED )
        {
            throw new IllegalStateException( "Only INCLUDED dependency nodes can be omitted for conflict" );
        }

        if ( relatedArtifact == null )
        {
            throw new IllegalArgumentException( "Related artifact cannot be null" );
        }

        if ( !relatedArtifact.getDependencyConflictId().equals( getArtifact().getDependencyConflictId() ) )
        {
            throw new IllegalArgumentException( "Related artifact has a different dependency conflict id" );
        }

        this.relatedArtifact = relatedArtifact;

        boolean duplicate = false;
        if ( getArtifact().getVersion() != null )
        {
            duplicate = getArtifact().getVersion().equals( relatedArtifact.getVersion() );
        }
        else if ( getArtifact().getVersionRange() != null )
        {
            duplicate = getArtifact().getVersionRange().equals( relatedArtifact.getVersionRange() );
        }
        else
        {
            throw new RuntimeException( "Artifact version and version range is null: " + getArtifact() );
        }

        state = duplicate ? OMITTED_FOR_DUPLICATE : OMITTED_FOR_CONFLICT;

        removeAllChildren();
    }

    /**
     * Changes the state of this dependency node to be omitted for a cycle in the dependency tree.
     * 
     * <p>
     * Omitting this node sets its state to <code>OMITTED_FOR_CYCLE</code> and removes all of its children.
     * </p>
     * 
     * @throws IllegalStateException
     *             if this dependency node's state is not <code>INCLUDED</code>
     * @see #OMITTED_FOR_CYCLE
     * @since 1.1
     */
    public void omitForCycle()
    {
        if ( getState() != INCLUDED )
        {
            throw new IllegalStateException( "Only INCLUDED dependency nodes can be omitted for cycle" );
        }

        state = OMITTED_FOR_CYCLE;

        removeAllChildren();
    }
    
    /**
     * Gets an iterator that returns this dependency node and it's children in preorder traversal.
     * 
     * @return the preorder traversal iterator
     * @see #preorderIterator()
     */
    public Iterator iterator()
    {
        return preorderIterator();
    }

    /**
     * Gets an iterator that returns this dependency node and it's children in preorder traversal.
     * 
     * @return the preorder traversal iterator
     * @see DependencyTreePreorderIterator
     */
    public Iterator preorderIterator()
    {
        return new DependencyTreePreorderIterator( this );
    }

    /**
     * Gets an iterator that returns this dependency node and it's children in postorder traversal.
     * 
     * @return the postorder traversal iterator
     * @see DependencyTreeInverseIterator
     */
    public Iterator inverseIterator()
    {
        return new DependencyTreeInverseIterator( this );
    }

    /**
     * Returns a string representation of this dependency node.
     * 
     * @return the string representation
     * @see #toString()
     * @since 1.1
     */
    public String toNodeString()
    {
        StringBuffer buffer = new StringBuffer();

        boolean included = ( getState() == INCLUDED );

        if ( !included )
        {
            buffer.append( '(' );
        }

        buffer.append( artifact );
        
        ItemAppender appender = new ItemAppender( buffer, included ? " (" : " - ", "; ", included ? ")" : "" );

        if ( getPremanagedVersion() != null )
        {
            appender.append( "version managed from ", getPremanagedVersion() );
        }
            
        if ( getPremanagedScope() != null )
        {
            appender.append( "scope managed from ", getPremanagedScope() );
        }
        
        if ( getOriginalScope() != null )
        {
            appender.append( "scope updated from ", getOriginalScope() );
        }
        
        if ( getFailedUpdateScope() != null )
        {
            appender.append( "scope not updated to ", getFailedUpdateScope() );
        }
        
        if ( getVersionSelectedFromRange() != null )
        {
            appender.append( "version selected from range ", getVersionSelectedFromRange().toString() );
            appender.append( "available versions ", getAvailableVersions().toString() );
        }
        
        switch ( state )
        {
            case INCLUDED:
                break;
                
            case OMITTED_FOR_DUPLICATE:
                appender.append( "omitted for duplicate" );
                break;

            case OMITTED_FOR_CONFLICT:
                appender.append( "omitted for conflict with ", relatedArtifact.getVersion() );
                break;

            case OMITTED_FOR_CYCLE:
                appender.append( "omitted for cycle" );
                break;
        }
        
        appender.flush();
        
        if ( !included )
        {
            buffer.append( ')' );
        }

        return buffer.toString();
    }
    
    /**
     * Returns a string representation of this dependency node and its children, indented to the specified depth.
     * 
     * <p>
     * As of 1.1, this method ignores the indentation depth and simply delegates to <code>toString()</code>.
     * </p>
     * 
     * @param indentDepth
     *            the indentation depth
     * @return the string representation
     * @deprecated As of 1.1, replaced by {@link #toString()}
     */
    public String toString( int indentDepth )
    {
        return toString();
    }
    
    // Object methods ---------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        // TODO: probably better using commons-lang HashCodeBuilder
        
        int hashCode = 1;
        
        hashCode = hashCode * 31 + getArtifact().hashCode();
        // DefaultArtifact.hashCode does not consider scope
        hashCode = hashCode * 31 + nullHashCode( getArtifact().getScope() );

        // TODO: use parent's artifact to prevent recursion - how can we improve this?
        hashCode = hashCode * 31 + nullHashCode( nullGetArtifact( getParent() ) );
        
        hashCode = hashCode * 31 + getChildren().hashCode();
        hashCode = hashCode * 31 + getState();
        hashCode = hashCode * 31 + nullHashCode( getRelatedArtifact() );
        hashCode = hashCode * 31 + nullHashCode( getPremanagedVersion() );
        hashCode = hashCode * 31 + nullHashCode( getPremanagedScope() );
        hashCode = hashCode * 31 + nullHashCode( getOriginalScope() );
        hashCode = hashCode * 31 + nullHashCode( getFailedUpdateScope() );
        hashCode = hashCode * 31 + nullHashCode( getVersionSelectedFromRange() );
        hashCode = hashCode * 31 + nullHashCode( getAvailableVersions() );

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals( Object object )
    {
        // TODO: probably better using commons-lang EqualsBuilder
        
        boolean equal;

        if ( object instanceof DependencyNode )
        {
            DependencyNode node = (DependencyNode) object;

            equal = getArtifact().equals( node.getArtifact() );
            // DefaultArtifact.hashCode does not consider scope
            equal &= nullEquals( getArtifact().getScope(), node.getArtifact().getScope() );
            
            // TODO: use parent's artifact to prevent recursion - how can we improve this?
            equal &= nullEquals( nullGetArtifact( getParent() ), nullGetArtifact( node.getParent() ) );
            
            equal &= getChildren().equals( node.getChildren() );
            equal &= getState() == node.getState();
            equal &= nullEquals( getRelatedArtifact(), node.getRelatedArtifact() );
            equal &= nullEquals( getPremanagedVersion(), node.getPremanagedVersion() );
            equal &= nullEquals( getPremanagedScope(), node.getPremanagedScope() );
            equal &= nullEquals( getOriginalScope(), node.getOriginalScope() );
            equal &= nullEquals( getFailedUpdateScope(), node.getFailedUpdateScope() );
            equal &= nullEquals( getVersionSelectedFromRange(), node.getVersionSelectedFromRange() );
            equal &= nullEquals( getAvailableVersions(), node.getAvailableVersions() );
        }
        else
        {
            equal = false;
        }

        return equal;
    }

    /**
     * Returns a string representation of this dependency node and its children.
     * 
     * @return the string representation
     * @see #toNodeString()
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringWriter writer = new StringWriter();
        accept( new SerializingDependencyNodeVisitor( writer ) );
        return writer.toString();
    }

    // private methods --------------------------------------------------------

    /**
     * Removes all of this dependency node's children.
     */
    private void removeAllChildren()
    {
        for ( Iterator iterator = children.iterator(); iterator.hasNext(); )
        {
            DependencyNode child = (DependencyNode) iterator.next();

            child.parent = null;
        }

        children.clear();
    }

    /**
     * Computes a hash-code for the specified object.
     * 
     * @param a
     *            the object to compute a hash-code for, possibly <code>null</code>
     * @return the computed hash-code
     */
    private int nullHashCode( Object a )
    {
        return ( a == null ) ? 0 : a.hashCode();
    }

    /**
     * Gets whether the specified objects are equal.
     * 
     * @param a
     *            the first object to compare, possibly <code>null</code>
     * @param b
     *            the second object to compare, possibly <code>null</code>
     * @return <code>true</code> if the specified objects are equal
     */
    private boolean nullEquals( Object a, Object b )
    {
        return ( a == null ? b == null : a.equals( b ) );
    }
    
    /**
     * Gets the artifact for the specified node.
     * 
     * @param node
     *            the dependency node, possibly <code>null</code>
     * @return the node's artifact, or <code>null</code> if the specified node was <code>null</code>
     */
    private static Artifact nullGetArtifact( DependencyNode node )
    {
        return ( node != null ) ? node.getArtifact() : null;
    }
}
