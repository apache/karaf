/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/AdminPermission.java,v 1.12 2005/05/13 20:32:54 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.*;
import org.apache.felix.framework.FilterImpl;

/**
 * Indicates the caller's authority to perform specific privileged administrative 
 * operations on or to get sensitive information about a bundle.
 * 
 * <ul>
 *   <li>The <code>{@link AdminPermission#METADATA}</code> action allows calls to
 *   	<ul>
 *         <li>{@link Bundle#getHeaders()}
 *         <li>{@link Bundle#getHeaders(String)}
 *         <li>{@link Bundle#getLocation()}
 *         </ul>
 *   <li>The <code>{@link AdminPermission#RESOURCE}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link Bundle#getResource(String)}
 *   		<li>{@link Bundle#getEntry(String)}
 *   		<li>{@link Bundle#getEntryPaths(String)}
 *   		<li>Bundle resource/entry URL creation
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#METADATA}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link Bundle#loadClass(String)}
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#LIFECYCLE}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link BundleContext#installBundle(String)}
 *   		<li>{@link BundleContext#installBundle(String, InputStream)}
 *   		<li>{@link Bundle#update()}
 *   		<li>{@link Bundle#update(InputStream)}
 *   		<li>{@link Bundle#uninstall()}
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#EXECUTE}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link Bundle#start()}
 *   		<li>{@link Bundle#stop()}
 *   		<li>{@link org.osgi.service.startlevel.StartLevel#setBundleStartLevel(Bundle, int)}
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#LISTENER}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link BundleContext#addBundleListener(BundleListener)} for 
 *   				<code>SynchronousBundleListener</code>
 *   		<li>{@link BundleContext#removeBundleListener(BundleListener)} for 
 *   				<code>SynchronousBundleListener</code>
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#PERMISSION}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link org.osgi.service.permissionadmin.PermissionAdmin#setPermissions(String, PermissionInfo[])}
 *   		<li>{@link org.osgi.service.permissionadmin.PermissionAdmin#setDefaultPermissions(PermissionInfo[])}
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#RESOLVE}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link org.osgi.service.packageadmin.PackageAdmin#refreshPackages(Bundle[])}</code>
 *   		<li>{@link org.osgi.service.packageadmin.PackageAdmin#resolveBundles(Bundle[])}</code>
 *   	</ul>
 *   <li>The <code>{@link AdminPermission#STARTLEVEL}</code> action allows calls to
 *   	<ul>
 *   		<li>{@link org.osgi.service.startlevel.StartLevel#setStartLevel(int)}
 *   		<li>{@link org.osgi.service.startlevel.StartLevel#setInitialBundleStartLevel(int)}
 *   	</ul>
 * </ul>
 * 
 * The special action "*" will represent all actions.
 * 
 * @version $Revision: 1.12 $
 */

public final class AdminPermission extends Permission
{
	static final long	serialVersionUID	= 207051004521261705L;

    /**
     * The action string <code>class</code> (Value is "class").
     */
    public final static String CLASS = "class"; //$NON-NLS-1$

    /**
     * The action string <code>execute</code> (Value is "execute").
     */
    public final static String EXECUTE = "execute"; //$NON-NLS-1$
    
    /**
     * The action string <code>lifecycle</code> (Value is "lifecycle").
     */
    public final static String LIFECYCLE = "lifecycle"; //$NON-NLS-1$
    
    /**
     * The action string <code>listener</code> (Value is "listener").
     */
    public final static String LISTENER = "listener"; //$NON-NLS-1$
    
    /**
     * The action string <code>metadata</code> (Value is "metadata").
     */
    public final static String METADATA = "metadata"; //$NON-NLS-1$

    /**
     * The action string <code>permission</code> (Value is "permission").
     */
    public final static String PERMISSION = "permission"; //$NON-NLS-1$
	
    /**
     * The action string <code>resolve</code> (Value is "resolve").
     */
    public final static String RESOLVE = "resolve"; //$NON-NLS-1$

    /**
     * The action string <code>resource</code> (Value is "resource").
     */
    public final static String RESOURCE = "resource"; //$NON-NLS-1$
    
    /**
     * The action string <code>startlevel</code> (Value is "startlevel").
     */
    public final static String STARTLEVEL = "startlevel"; //$NON-NLS-1$

    /**
     * The action string <code>extensionLifecycle</code> (Value is "extensionLifecycle").
     */
    public final static String EXTENSIONLIFECYCLE = "extensionLifecycle"; //$NON-NLS-1$

    private final static int ACTION_CLASS				= 0x00000001;
    private final static int ACTION_EXECUTE				= 0x00000002;
    private final static int ACTION_LIFECYCLE			= 0x00000004;
    private final static int ACTION_LISTENER			= 0x00000008;
    private final static int ACTION_METADATA			= 0x00000010;
    private final static int ACTION_PERMISSION			= 0x00000020;
    private final static int ACTION_RESOLVE				= 0x00000040;
    private final static int ACTION_RESOURCE			= 0x00000080;
    private final static int ACTION_STARTLEVEL			= 0x00000100;
	private final static int ACTION_EXTENSIONLIFECYCLE	= 0x00000200;
    private final static int ACTION_ALL = 
		ACTION_CLASS 				|
		ACTION_EXECUTE 				|
		ACTION_LIFECYCLE 			|
		ACTION_LISTENER 			|
    	ACTION_METADATA 			|
		ACTION_PERMISSION			|
		ACTION_RESOLVE 				|
		ACTION_RESOURCE 			|
		ACTION_STARTLEVEL			|
		ACTION_EXTENSIONLIFECYCLE;
    private final static int ACTION_NONE = 0;
	
	/**
	 * Indicates that this AdminPermission refers to all bundles
	 * @serial
	 */
	private boolean wildcard;
	
	/**
	 * An x.500 distinguished name used to match a bundle's signature - only used if
	 * wildcard is false and bundle = null
	 * @serial
	 */
	private String filter;

    /**
     * The actions in canonical form.
     *
     * @serial
     */
    private String actions = null;

    /**
     * The actions mask.
     */
	private transient int action_mask = ACTION_NONE;

	/**
	 * The bundle governed by this AdminPermission - only used if 
	 * wildcard is false and filter == null
	 */
	private transient Bundle bundle;

    /**
     * If this AdminPermission was constructed with a bundle, this dictionary holds
     * the properties of that bundle, used to match a filter in implies.
     * This is not initialized until necessary, and then cached in this object.
     */
    private transient Dictionary bundleProperties;
    
    /**
     * If this AdminPermission was constructed with a filter, this dictionary holds
     * a Filter matching object used to evaluate the filter in implies.
     * This is not initialized until necessary, and then cached in this object
     */
    private transient Filter filterImpl;
    
	/**
     * Creates a new <code>AdminPermission</code> object that matches 
     * all bundles and has all actions.  Equivalent to 
     * AdminPermission("*","*");
     */
    public AdminPermission()
    {
    	this("*",AdminPermission.ACTION_ALL); //$NON-NLS-1$
    }
    
    /**
     * Creates a new <code>AdminPermission</code> object for use by the <code>Policy</code>
     * object to instantiate new <code>Permission</code> objects.
     * 
     * Null arguments are equivalent to "*"
     *
     * @param filter an X.500 Distinguished Name suffix or "*" to match all bundles
     * @param actions <code>class</code>, <code>execute</code>, <code>lifecycle</code>, 
     * <code>listener</code>, <code>metadata</code>, <code>permission</code>, <code>resolve</code>, 
     * <code>resource</code>, <code>startlevel</code>, or "*" to indicate all actions
     */
    public AdminPermission(String filter, String actions)
    {
    	//arguments will be null if called from a PermissionInfo defined with
    	//no args
    	this(
    			(filter == null ? "*" : filter), //$NON-NLS-1$
				getMask((actions == null ? "*" : actions)) //$NON-NLS-1$
				);
    }

    /**
     * Creates a new <code>AdminPermission</code> object for use by the <code>Policy</code>
     * object to instantiate new <code>Permission</code> objects.
     * 
     * @param bundle A bundle
     * @param actions <code>class</code>, <code>execute</code>, <code>lifecycle</code>, 
     * <code>listener</code>, <code>metadata</code>, <code>permission</code>, <code>resolve</code>, 
     * <code>resource</code>, <code>startlevel</code>, or "*" to indicate all actions
     */
    public AdminPermission(Bundle bundle, String actions) {
    	super(bundle.toString());
    	this.bundle = bundle;
    	this.wildcard = false;
    	this.filter = null;
    	this.action_mask = getMask(actions);
    }
 
    /**
     * Package private constructor used by AdminPermissionCollection.
     *
     * @param filter name filter
     * @param action_mask mask
     */
    AdminPermission(String filter, int action_mask) {
    	super(filter);
    	
    	//name must be either * or a filter
    	if (filter.equals("*")) { //$NON-NLS-1$
    		this.wildcard = true;
    		this.filter = null;
    	} else {
			this.wildcard = false;
			this.filter = filter;
    	}
    	this.bundle = null;
    	this.action_mask = action_mask;
    }

    /**
     * Parse action string into action mask.
     *
     * @param actions Action string.
     * @return action mask.
     */
    private static int getMask(String actions) {
    
    	boolean seencomma = false;

    	int mask = ACTION_NONE;

    	if (actions == null) {
    		return mask;
    	}

    	char[] a = actions.toCharArray();

    	int i = a.length - 1;
    	if (i < 0)
    		return mask;

    	while (i != -1) {
    		char c;

    		// skip whitespace
    		while ((i!=-1) && ((c = a[i]) == ' ' ||
    				c == '\r' ||
					c == '\n' ||
					c == '\f' ||
					c == '\t'))
    			i--;

    		// check for the known strings
    		int matchlen;

    		if (i >= 4 && 
					(a[i-4] == 'c' || a[i-4] == 'C') &&
					(a[i-3] == 'l' || a[i-3] == 'L') &&
					(a[i-2] == 'a' || a[i-2] == 'A') &&
					(a[i-1] == 's' || a[i-1] == 'S') &&
					  (a[i] == 's' ||   a[i] == 'S'))
			{
				matchlen = 5;
				mask |= ACTION_CLASS;
	
    		} else if (i >= 6 && 
					(a[i-6] == 'e' || a[i-6] == 'E') &&
					(a[i-5] == 'x' || a[i-5] == 'X') &&
					(a[i-4] == 'e' || a[i-4] == 'E') &&
					(a[i-3] == 'c' || a[i-3] == 'C') &&
					(a[i-2] == 'u' || a[i-2] == 'U') &&
					(a[i-1] == 't' || a[i-1] == 'T') &&
					  (a[i] == 'e' ||   a[i] == 'E'))
			{
				matchlen = 7;
				mask |= ACTION_EXECUTE;
				
			} else if (i >= 17 && 
					(a[i-17] == 'e' || a[i-17] == 'E') &&
					(a[i-16] == 'x' || a[i-16] == 'X') &&
					(a[i-15] == 't' || a[i-15] == 'T') &&
					(a[i-14] == 'e' || a[i-14] == 'E') &&
					(a[i-13] == 'n' || a[i-13] == 'N') &&
					(a[i-12] == 's' || a[i-12] == 'S') &&
					(a[i-11] == 'i' || a[i-11] == 'I') &&
					(a[i-10] == 'o' || a[i-10] == 'O') &&
					(a[i-9] == 'n' || a[i-9] == 'N') &&
					(a[i-8] == 'l' || a[i-8] == 'L') &&
					(a[i-7] == 'i' || a[i-7] == 'I') &&
					(a[i-6] == 'f' || a[i-6] == 'F') &&
					(a[i-5] == 'e' || a[i-5] == 'E') &&
					(a[i-4] == 'c' || a[i-4] == 'C') &&
					(a[i-3] == 'y' || a[i-3] == 'Y') &&
					(a[i-2] == 'c' || a[i-2] == 'C') &&
					(a[i-1] == 'l' || a[i-1] == 'L') &&
					  (a[i] == 'e' ||   a[i] == 'E'))
    		{
    			matchlen = 18;
    			mask |= ACTION_EXTENSIONLIFECYCLE;

    		} else if (i >= 8 && 
					(a[i-8] == 'l' || a[i-8] == 'L') &&
					(a[i-7] == 'i' || a[i-7] == 'I') &&
					(a[i-6] == 'f' || a[i-6] == 'F') &&
					(a[i-5] == 'e' || a[i-5] == 'E') &&
					(a[i-4] == 'c' || a[i-4] == 'C') &&
					(a[i-3] == 'y' || a[i-3] == 'Y') &&
					(a[i-2] == 'c' || a[i-2] == 'C') &&
					(a[i-1] == 'l' || a[i-1] == 'L') &&
					  (a[i] == 'e' ||   a[i] == 'E'))
			{
				matchlen = 9;
				mask |= ACTION_LIFECYCLE;
				
			} else if (i >= 7 && 
					(a[i-7] == 'l' || a[i-7] == 'L') &&
					(a[i-6] == 'i' || a[i-6] == 'I') &&
					(a[i-5] == 's' || a[i-5] == 'S') &&
					(a[i-4] == 't' || a[i-4] == 'T') &&
					(a[i-3] == 'e' || a[i-3] == 'E') &&
					(a[i-2] == 'n' || a[i-2] == 'N') &&
					(a[i-1] == 'e' || a[i-1] == 'E') &&
					  (a[i] == 'r' ||   a[i] == 'R'))
			{
				matchlen = 8;
				mask |= ACTION_LISTENER;
			
			} else if (i >= 7 && 
    				(a[i-7] == 'm' || a[i-7] == 'M') &&
    	            (a[i-6] == 'e' || a[i-6] == 'E') &&
    	            (a[i-5] == 't' || a[i-5] == 'T') &&
    	            (a[i-4] == 'a' || a[i-4] == 'A') &&
    	            (a[i-3] == 'd' || a[i-3] == 'D') &&
    	            (a[i-2] == 'a' || a[i-2] == 'A') &&
					(a[i-1] == 't' || a[i-1] == 'T') &&
					  (a[i] == 'a' ||   a[i] == 'A'))
    		{
    			matchlen = 8;
    			mask |= ACTION_METADATA;

    		} else if (i >= 9 && 
					(a[i-9] == 'p' || a[i-9] == 'P') &&
					(a[i-8] == 'e' || a[i-8] == 'E') &&
					(a[i-7] == 'r' || a[i-7] == 'R') &&
					(a[i-6] == 'm' || a[i-6] == 'M') &&
					(a[i-5] == 'i' || a[i-5] == 'I') &&
					(a[i-4] == 's' || a[i-4] == 'S') &&
					(a[i-3] == 's' || a[i-3] == 'S') &&
					(a[i-2] == 'i' || a[i-2] == 'I') &&
					(a[i-1] == 'o' || a[i-1] == 'O') &&
					  (a[i] == 'n' ||   a[i] == 'N'))
    		{
    			matchlen = 10;
    			mask |= ACTION_PERMISSION;
			
    		} else if (i >= 6 && 
					(a[i-6] == 'r' || a[i-6] == 'R') &&
					(a[i-5] == 'e' || a[i-5] == 'E') &&
					(a[i-4] == 's' || a[i-4] == 'S') &&
					(a[i-3] == 'o' || a[i-3] == 'O') &&
					(a[i-2] == 'l' || a[i-2] == 'L') &&
					(a[i-1] == 'v' || a[i-1] == 'V') &&
					  (a[i] == 'e' ||   a[i] == 'E'))
    		{
    			matchlen = 7;
    			mask |= ACTION_RESOLVE;
    			
    		} else if (i >= 7 && 
    					(a[i-7] == 'r' || a[i-7] == 'R') &&
						(a[i-6] == 'e' || a[i-6] == 'E') &&
						(a[i-5] == 's' || a[i-5] == 'S') &&
						(a[i-4] == 'o' || a[i-4] == 'O') &&
						(a[i-3] == 'u' || a[i-3] == 'U') &&
						(a[i-2] == 'r' || a[i-2] == 'R') &&
						(a[i-1] == 'c' || a[i-1] == 'C') &&
						  (a[i] == 'e' ||   a[i] == 'E'))
			{
    			matchlen = 8;
    			mask |= ACTION_RESOURCE;

    		} else if (i >= 9 && 
					(a[i-9] == 's' || a[i-9] == 'S') &&
					(a[i-8] == 't' || a[i-8] == 'T') &&
					(a[i-7] == 'a' || a[i-7] == 'A') &&
					(a[i-6] == 'r' || a[i-6] == 'R') &&
					(a[i-5] == 't' || a[i-5] == 'T') &&
					(a[i-4] == 'l' || a[i-4] == 'L') &&
					(a[i-3] == 'e' || a[i-3] == 'E') &&
					(a[i-2] == 'v' || a[i-2] == 'V') &&
					(a[i-1] == 'e' || a[i-1] == 'E') &&
					  (a[i] == 'l' ||   a[i] == 'L'))
    		{
    			matchlen = 10;
    			mask |= ACTION_STARTLEVEL;

    		} else if (i >= 0 && 
					(a[i] == '*'))
    		{
    			matchlen = 1;
    			mask |= ACTION_ALL;

			} else {
				// parse error
				throw new IllegalArgumentException(
						"invalid permission: " + actions);
        }

        // make sure we didn't just match the tail of a word
        // like "ackbarfstartlevel".  Also, skip to the comma.
        seencomma = false;
        while (i >= matchlen && !seencomma) {
        	switch(a[i-matchlen]) {
        		case ',':
        			seencomma = true;
        			/*FALLTHROUGH*/
        		case ' ': case '\r': case '\n':
        		case '\f': case '\t':
        			break;
        		default:
        			throw new IllegalArgumentException(
        					"invalid permission: " + actions);
        	}
        	i--;
        }

        // point i at the location of the comma minus one (or -1).
        i -= matchlen;
    }

    if (seencomma) {
        throw new IllegalArgumentException("invalid permission: " +
                        actions);
    }

    return mask;
    }
     
    /**
     * Called by <code><@link AdminPermission#implies(Permission)></code> on an AdminPermission
     * which was constructed with a Bundle.  This method loads a dictionary with the
     * filter-matchable properties of this bundle.  The dictionary is cached so this lookup
     * only happens once.
     * 
     * This method should only be called on an AdminPermission which was constructed with a 
     * bundle
     * 
     * @return a dictionary of properties for this bundle
     */
    private Dictionary getProperties() {
    	if (bundleProperties == null) {
    		bundleProperties = new Hashtable();

    		AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
		    		//set Id
		    		bundleProperties.put("id",new Long(bundle.getBundleId())); //$NON-NLS-1$
		    		
		    		//set location
		    		bundleProperties.put("location",bundle.getLocation()); //$NON-NLS-1$
		    		
		    		//set name
		    		if (bundle.getSymbolicName() != null)
		    			bundleProperties.put("name",bundle.getSymbolicName()); //$NON-NLS-1$
		    		
		    		//set signers
//		    		bundleProperties.put("signer",new SignerWrapper(bundle)); //$NON-NLS-1$
		    		
		    		return null;
				}
			});
    	}     		
    	return bundleProperties;
    }
/*
    private static class SignerWrapper extends Object {
    	private Bundle bundle;
    	private String pattern;
    	public SignerWrapper(String pattern) {
    		this.pattern = pattern;    			
    	}
    	SignerWrapper(Bundle bundle) {
    		this.bundle = bundle;
    	}
    	
		public boolean equals(Object o) {
			if (!(o instanceof SignerWrapper))
				return false;
			SignerWrapper other = (SignerWrapper) o;
			AbstractBundle matchBundle = (AbstractBundle) (bundle != null ? bundle : other.bundle);
			String matchPattern = bundle != null ? other.pattern : pattern;
			return matchBundle.getBundleData().matchDNChain(matchPattern);
		}
    }
*/    

    /**
     * Called by <tt><@link AdminPermission#implies(Permission)></tt> on an AdminPermission
     * which was constructed with a filter.  This method loads a FilterImpl with the
     * filter specification of this AdminPermission.  The filter is cached so this work
     * only happens once.
     * 
     * This method should only be called on an AdminPermission which was constructed with a 
     * filter
     * 
     * @return a filterImpl for this bundle
     */
    private Filter getFilterImpl() {
        if (filterImpl == null) {
            try {
                int pos = filter.indexOf("signer"); //$NON-NLS-1$
                if (pos != -1){ 
                
                    //there may be a signer attribute 
                    StringBuffer filterBuf = new StringBuffer(filter);
                    int numAsteriskFound = 0; //use as offset to replace in buffer
                    
                    int walkbackPos; //temp pos

                    //find occurences of (signer= and escape out *'s
                    while (pos != -1) {

                        //walk back and look for '(' to see if this is an attr
                        walkbackPos = pos-1; 
                        
                        //consume whitespace
                        while(walkbackPos >= 0 && Character.isWhitespace(filter.charAt(walkbackPos))) {
                            walkbackPos--;
                        }
                        if (walkbackPos <0) {
                            //filter is invalid - FilterImpl will throw error
                            break;
                        }
                        
                        //check to see if we have unescaped '('
                        if (filter.charAt(walkbackPos) != '(' || (walkbackPos > 0 && filter.charAt(walkbackPos-1) == '\\')) {
                            //'(' was escaped or not there
                            pos = filter.indexOf("signer",pos+6); //$NON-NLS-1$
                            continue;
                        }                   
                        pos+=6; //skip over 'signer'

                        //found signer - consume whitespace before '='
                        while (Character.isWhitespace(filter.charAt(pos))) {
                            pos++;
                        }

                        //look for '='
                        if (filter.charAt(pos) != '=') {
                            //attr was signerx - keep looking
                            pos = filter.indexOf("signer",pos); //$NON-NLS-1$
                            continue;
                        }
                        pos++; //skip over '='
                        
                        //found signer value - escape '*'s
                        while (!(filter.charAt(pos) == ')' && filter.charAt(pos-1) != '\\')) {
                            if (filter.charAt(pos) == '*') {
                                filterBuf.insert(pos+numAsteriskFound,'\\');
                                numAsteriskFound++;
                            }
                            pos++;
                        }

                        //end of signer value - look for more?
                        pos = filter.indexOf("signer",pos); //$NON-NLS-1$
                    } //end while (pos != -1)
                    filter = filterBuf.toString();
                } //end if (pos != -1)

                filterImpl = new FilterImpl(filter);
            } catch (InvalidSyntaxException e) {
                //we will return null
            }
        }           
        return filterImpl;
    }

    /**
     * Determines if the specified permission is implied by this object.
     * This method throws an exception if the specified permission was not
     * constructed with a bundle.
     * 
     * <p>This method returns <code>true</code> if
     * The specified permission is an AdminPermission AND
     * <ul>
     * 	<li>this object's filter is an X.500 Distinguished name suffix that 
     * matches the specified permission's bundle OR
     * 	<li>this object's filter is "*" OR
     * 	<li>this object's bundle is a equal to the specified permission's
     * bundle
     * </ul>
     * AND this object's actions include all of the specified permission's actions 	 
     *
     * Special case: if the specified permission was constructed with "*", then this method
     * returns <code>true</code> if this object's filter is "*" and this object's actions include
     * all of the specified permission's actions
     * 
     * @param p The permission to interrogate.
     *
     * @return <code>true</code> if the specified permission is implied by
     * this object; <code>false</code> otherwise.
     * @throws RuntimeException if specified permission was not constructed with
     * a bundle or "*"
     */
    public boolean implies(Permission p)
    {
    	if (!(p instanceof AdminPermission))
    		return false;
    	AdminPermission target = (AdminPermission)p;
    	//check actions first - much faster
    	if ((action_mask & target.action_mask)!=target.action_mask)
    		return false;
    	//if passed in a filter, puke
    	if (target.filter != null)
    		throw new RuntimeException("Cannot imply a filter");
    	//special case - only wildcard implies wildcard
    	if (target.wildcard)
    		return wildcard;

    	//check our name 
    	if (filter != null) {
    		//it's a filter
    		Filter filterImpl = getFilterImpl();
			return filterImpl != null && filterImpl.match(target.getProperties());
    	} else if (wildcard) {
    		//it's "*"
    		return true;
    	} else {
    		//it's a bundle id
    		return bundle.equals(target.bundle);
    	}
    	    	
    }
    
    /**
     * Returns the canonical string representation of the <code>AdminPermission</code> actions.
     *
     * <p>Always returns present <code>AdminPermission</code> actions in the following order:
     * <code>CLASS</code>, <code>EXECUTE</code>, <code>LIFECYCLE</code>, <code>LISTENER</code>, 
     * <code>METADATA</code>, <code>PERMISSION</code>, <code>RESOLVE</code>, <code>RESOURCE</code>, 
     * <code>STARTLEVEL</code>.
     * @return Canonical string representation of the <code>AdminPermission</code> actions.
     */
	public String getActions() {
		if (actions == null) {
			if (action_mask == ACTION_ALL) {
				actions = "*"; //$NON-NLS-1$
			} else {
				StringBuffer sb = new StringBuffer();
				
				if ((action_mask & ACTION_CLASS) == ACTION_CLASS) {
					sb.append(CLASS);
					sb.append(',');
				}

				if ((action_mask & ACTION_EXECUTE) == ACTION_EXECUTE) {
					sb.append(EXECUTE);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_LIFECYCLE) == ACTION_LIFECYCLE) {
					sb.append(LIFECYCLE);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_LISTENER) == ACTION_LISTENER) {
					sb.append(LISTENER);
					sb.append(',');
				}
				
				if ((action_mask & ACTION_METADATA) == ACTION_METADATA) {
					sb.append(METADATA);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_PERMISSION) == ACTION_PERMISSION) {
					sb.append(PERMISSION);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_RESOLVE) == ACTION_RESOLVE) {
					sb.append(RESOLVE);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_RESOURCE) == ACTION_RESOURCE) {
					sb.append(RESOURCE);
					sb.append(',');
				}
	
				if ((action_mask & ACTION_STARTLEVEL) == ACTION_STARTLEVEL) {
					sb.append(STARTLEVEL);
					sb.append(',');
				}

				if ((action_mask & ACTION_EXTENSIONLIFECYCLE) == ACTION_EXTENSIONLIFECYCLE) {
					sb.append(EXTENSIONLIFECYCLE);
					sb.append(',');
				}

				//remove trailing comma
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length()-1);
				}
				
				actions = sb.toString();
			}
		}
		return actions;
	}
	
    /**
     * Determines the equality of two <code>AdminPermission</code> objects. <p>Two 
     * <code>AdminPermission</code> objects are equal.
     *
     * @param obj The object being compared for equality with this object.
     * @return <code>true</code> if <code>obj</code> is equivalent to this 
     * <code>AdminPermission</code>; <code>false</code> otherwise.
     */
    public boolean equals(Object obj)
    {
        if (obj == this) {
        	return true;
        }
        
        if (!(obj instanceof AdminPermission))
        {
            return false;
        }
        
        AdminPermission a = (AdminPermission) obj;

        return (action_mask == a.action_mask) &&
        		(wildcard == a.wildcard) &&
        		(bundle == null ? a.bundle == null : (a.bundle == null ? false : bundle.getBundleId() == a.bundle.getBundleId())) &&
				(filter == null ? a.filter == null : filter.equals(a.filter));
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return Hash code value for this object.
     */
	public int hashCode() {
		return getName().hashCode() ^ getActions().hashCode();
	}

	private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
		// Write out the actions. The superclass takes care of the name
		// call getActions to make sure actions field is initialized
		if (actions == null)
			getActions();
		if (filter == null && !wildcard)
			throw new UnsupportedOperationException("cannot serialize");
		s.defaultWriteObject();
	}

	/**
	 * readObject is called to restore the state of this permission from a
	 * stream.
	 */
	private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
		// Read in the action, then initialize the rest
		s.defaultReadObject();
		action_mask = getMask(actions);
	}

    /**
     * Returns a new <code>PermissionCollection</code> object suitable for storing
     * <code>AdminPermission</code>s.
     * 
     * @return A new <code>PermissionCollection</code> object.
     */
    public PermissionCollection newPermissionCollection()
    {
        return(new AdminPermissionCollection());
    }

	/**
	 * Stores a collection of <code>AdminPermission</code>s.
	 */
	private final class AdminPermissionCollection extends PermissionCollection
	{
		private static final long serialVersionUID = 3906372644575328048L;
		/**
	     * Collection of permissions.
	     *
	     * @serial
	     */
		private Hashtable permissions;
	
	    /**
	     * Create an empty AdminPermissions object.
	     *
	     */
	
	    public AdminPermissionCollection()
	    {
	        permissions = new Hashtable();        
	    }

	    /**
	     * Adds a permission to the <code>AdminPermission</code> objects. The key for 
	     * the hashtable is the name
	     *
	     * @param permission The <code>AdminPermission</code> object to add.
	     *
	     * @exception IllegalArgumentException If the permission is not an
	     * <code>AdminPermission</code> instance.
	     *
	     * @exception SecurityException If this <code>AdminPermissionCollection</code>
	     * object has been marked read-only.
	     */
	    public void add(Permission permission)
	    {
	        if (! (permission instanceof AdminPermission))
	            throw new IllegalArgumentException("invalid permission: "+
	                                               permission);
	        if (isReadOnly())
	            throw new SecurityException("attempt to add a Permission to a " +
	                                        "readonly AdminCollection");
	        AdminPermission ap = (AdminPermission) permission;
	    	AdminPermission existing = (AdminPermission) permissions.get(ap.getName());
	    	if (existing != null){
	    		int oldMask = existing.action_mask;
	    		int newMask = ap.action_mask;
	        
	    		if (oldMask != newMask) {
	    			permissions.put(existing.getName(),
	    					new AdminPermission(existing.getName(), oldMask | newMask));
	    		}
	    	} else {
	    		permissions.put(ap.getName(), ap);
	    	}
	    }
	
	
	    /**
	     * Determines if the specified permissions implies the permissions
	     * expressed in <code>permission</code>.
	     *
	     * @param permission The Permission object to compare with the <code>AdminPermission</code>
	     *  objects in this collection.
	     *
	     * @return <code>true</code> if <code>permission</code> is implied by an 
	     * <code>AdminPermission</code> in this collection, <code>false</code> otherwise.
	     */
	    public boolean implies(Permission permission)
	    {
	        if (!(permission instanceof AdminPermission))
	            return(false);
	
	        AdminPermission target = (AdminPermission) permission;
	        
	        //just iterate one by one
	        Iterator permItr = permissions.values().iterator();
	        
	        while(permItr.hasNext())
	        	if (((AdminPermission)permItr.next()).implies(target))
	        		return true;
	        return false;
	    }
	 
	
	    /**
	     * Returns an enumeration of all <code>AdminPermission</code> objects in the
	     * container.
	     *
	     * @return Enumeration of all <code>AdminPermission</code> objects.
	     */
	
	    public Enumeration elements()
	    {
	        return(Collections.enumeration(permissions.values()));
	    }
	}
}