/*
 * $Header: /cvshome/bundles/bundles.obr/src/bundles/obr/resource/RepositoryImpl.java,v 1.11 2006/04/27 09:00:16 pkriens Exp $
 * 
 * Copyright (c) The OSGi Alliance (2005). All Rights Reserved.
 * 
 * Implementation of certain elements of the OSGi Specification may be subject
 * to third party intellectual property rights, including without limitation,
 * patent rights (such a third party may or may not be a member of the OSGi
 * Alliance). The OSGi Alliance is not responsible and shall not be held
 * responsible in any manner for identifying or failing to identify any or all
 * such third party intellectual property rights.
 * 
 * This document and the information contained herein are provided on an "AS IS"
 * basis and THE OSGI ALLIANCE DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION
 * HEREIN WILL NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL THE
 * OSGI ALLIANCE BE LIABLE FOR ANY LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF
 * USE OF DATA, INTERRUPTION OF BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR
 * EXEMPLARY, INCIDENTIAL, PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN
 * CONNECTION WITH THIS DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS OR DAMAGE.
 * 
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.impl.bundle.obr.resource;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import org.kxml2.io.KXmlParser;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;

import org.xmlpull.v1.*;

/**
 * Implements the basic repository. A repository holds a set of resources.
 * 
 * 
 * @version $Revision: 1.11 $
 */
public class RepositoryImpl implements Repository {
	transient Set			resources		= new HashSet();
	URL						url;
	String					date;
	Set						visited			= new HashSet();
	final static Resource[]	EMPTY_RESOURCE	= new Resource[0];
	String					name			= "Untitled";
	long					lastModified;
	Exception				exception;
	int						ranking=0;

	/**
	 * Each repository is identified by a single URL.
	 * 
	 * A repository can hold referrals to other repositories. These referred
	 * repositories are included at the point of referall.
	 * 
	 * @param url
	 */
	public RepositoryImpl(URL url) {
		this.url = url;
	}

	/**
	 * Refresh the repository from the URL.
	 * 
	 * @throws Exception
	 */
	public boolean refresh() {
		exception = null;
		try {
			resources.clear();
			parseDocument(url);
			visited = null;
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			exception = e;
		}
		return false;
	}

	/**
	 * Parse the repository.
	 * 
	 * @param parser
	 * @throws Exception
	 */
	private void parseRepository(XmlPullParser parser) throws Exception {
		try {
			parser.require(XmlPullParser.START_DOCUMENT, null, null);
			parser.nextTag();
			if (parser.getName().equals("bundles"))
				parseOscar(parser);
			else {
				parser.require(XmlPullParser.START_TAG, null, "repository");
				date = parser.getAttributeValue(null, "lastmodified");
				name = parser.getAttributeValue(null, "name");
				if (name == null)
					name = "Untitled";

				while (parser.nextTag() == XmlPullParser.START_TAG) {
					if (parser.getName().equals("resource")) {
						ResourceImpl resource = new ResourceImpl(this, parser);
						resources.add(resource);
					}
					else if (parser.getName().equals("referral"))
						referral(parser);
					else
						throw new IllegalArgumentException(
								"Invalid tag in repository: " + url + " "
										+ parser.getName());
				}
				parser.require(XmlPullParser.END_TAG, null, "repository");
			}
		}
		catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("XML unregognized around: "
					+ e.getLineNumber() + " " + e.getMessage());
		}
	}

	/**
	 * Parse an old style OBR repository.
	 * 
	 * <dtd-version>1.0</dtd-version> <repository> <name>Oscar Bundle
	 * Repository</name> <url>http://oscar-osgi.sourceforge.net/</url>
	 * <date>Fri May 07 16:45:07 CEST 2004</date> <extern-repositories> <!--
	 * Stefano Lenzi (kismet@interfree.it) -->
	 * <url>http://domoware.isti.cnr.it/osgi-obr/niche-osgi-obr.xml</url>
	 * <!--Manuel Palencia (santillan@dit.upm.es) --> <!--
	 * <url>http://jmood.forge.os4os.org/repository.xml</url> --> <!-- Enrique
	 * Rodriguez (erodriguez@apache.org) -->
	 * <url>http://update.cainenable.org/repository.xml</url>
	 * </extern-repositories> </repository> <bundle> <bundle-name>Bundle
	 * Repository</bundle-name> <bundle-description> A bundle repository
	 * service for Oscar. </bundle-description> <bundle-updatelocation>
	 * http://oscar-osgi.sf.net/repo/bundlerepository/bundlerepository.jar
	 * </bundle-updatelocation> <bundle-sourceurl>
	 * http://oscar-osgi.sf.net/repo/bundlerepository/bundlerepository-src.jar
	 * </bundle-sourceurl> <bundle-version>1.1.3</bundle-version>
	 * <bundle-docurl> http://oscar-osgi.sf.net/repo/bundlerepository/
	 * </bundle-docurl> <bundle-category>General</bundle-category>
	 * <import-package package="org.osgi.framework"/> <export-package
	 * package="org.ungoverned.osgi.service.bundlerepository"
	 * specification-version="1.1.0"/> </bundle> *
	 */
	private void parseOscar(XmlPullParser parser) throws Exception {
		parser.require(XmlPullParser.START_TAG, null, "bundles");
		while (true) {
			int event = parser.next();

			// Error ..
			if (event == XmlPullParser.TEXT)
				event = parser.next();

			if (event != XmlPullParser.START_TAG)
				break;

			ResourceImpl resource = new ResourceImpl(this);

			if (parser.getName().equals("bundle")) {
				while (parser.nextTag() == XmlPullParser.START_TAG) {
					String key = parser.getName();
					if (key.equals("import-package")) {
						RequirementImpl requirement = new RequirementImpl(
								"package");
						
						requirement.setOptional(false);
						requirement.setMultiple(false);
						
						String p = parser.getAttributeValue(null, "package");
						StringBuffer sb = new StringBuffer();
						sb.append("(&(package=");
						sb.append(p);
						sb.append(")");
						String version = parser.getAttributeValue(null,
								"specification-version");
						VersionImpl v = new VersionImpl("0");
						if (version != null) {
							sb.append("(version=");
							sb.append(v= new VersionImpl(version));
							sb.append(")");
						}
						sb.append(")");
						requirement.setFilter(sb.toString());
						requirement.setComment("Import-Package: " + p + ";" + v );
						resource.addRequirement(requirement);
						
						parser.nextTag();
					}
					else if (key.equals("export-package")) {
						CapabilityImpl capability = new CapabilityImpl(
								"package");
						capability.addProperty("package", parser
								.getAttributeValue(null, "package"));
						String version = parser.getAttributeValue(null,
								"specification-version");
						if (version != null) {
							capability.addProperty("version", new VersionImpl(
									version));
						}
						resource.addCapability(capability);
						parser.nextTag();
					}
					else {
						String value = parser.nextText().trim();
						if (key.equals("bundle-sourceurl"))
							resource.setSource(new URL(value));
						else if (key.equals("bundle-docurl"))
							resource.setDocumentation(new URL(value));
						else if (key.equals("bundle-updatelocation"))
							resource.setURL(new URL(value));
						else if (key.equals("bundle-description"))
							resource.setDescription(value);
						else if (key.equals("bundle-category"))
							resource.addCategory(value);
						else if (key.equals("bundle-name")) {
							resource.setName(value);
							resource.setPresentationName(value);
						}
						else if (key.equals("bundle-version"))
							resource.setVersion(new VersionImpl(value));
						else {
							resource.put(key, value);
						}
					}
				}
				resources.add(resource);
				parser.require(XmlPullParser.END_TAG, null, "bundle");
			}
			else if (parser.getName().equals("repository")) {
				parser.require(XmlPullParser.START_TAG, null, "repository");
				while (parser.nextTag() == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					if (tag.equals("name")) {
						String name = parser.nextText();
						if (this.name == null)
							this.name = name.trim();
					}
					else if (tag.equals("url"))
						parser.nextText().trim();
					else if (tag.equals("date"))
						parser.nextText().trim();
					else if (tag.equals("extern-repositories")) {
						parser.require(XmlPullParser.START_TAG, null,
								"extern-repositories");
						while (parser.nextTag() == XmlPullParser.START_TAG) {
							if (parser.getName().equals("url"))
								parseDocument(new URL(parser.nextText().trim()));
							else
								throw new IllegalArgumentException(
										"Invalid tag in repository while parsing extern repositories: "
												+ url + " " + parser.getName());
						}
						parser.require(XmlPullParser.END_TAG, null,
								"extern-repositories");
					}
					else
						throw new IllegalArgumentException(
								"Invalid tag in repository: " + url + " "
										+ parser.getName());
				}
				parser.require(XmlPullParser.END_TAG, null, "repository");
			}
			else if (parser.getName().equals("dtd-version")) {
				parser.nextText();
			}
			else
				throw new IllegalArgumentException(
						"Invalid tag in repository: " + url + " "
								+ parser.getName());
		}
		parser.require(XmlPullParser.END_TAG, null, "bundles");
	}

	/**
	 * We have a referral to another repository. Just create another parser and
	 * read it inline.
	 * 
	 * @param parser
	 */
	void referral(XmlPullParser parser) {
		// TODO handle depth!
		try {
			parser.require(XmlPullParser.START_TAG, null, "referral");
			// String depth = parser.getAttributeValue(null, "depth");
			String path = parser.getAttributeValue(null, "url");
			URL url = new URL(this.url, path);
			parseDocument(url);
			parser.next();
			parser.require(XmlPullParser.END_TAG, null, "referral");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse a repository document.
	 * 
	 * @param url
	 * @throws IOException
	 * @throws XmlPullParserException
	 * @throws Exception
	 */
	void parseDocument(URL url) throws IOException, XmlPullParserException,
			Exception {
		if (!visited.contains(url)) {
			visited.add(url);
			try {
				System.out.println("Visiting: " + url);
				InputStream in = null;
				
				if ( url.getPath().endsWith(".zip")) {
					ZipInputStream zin = new ZipInputStream( url.openStream() );
					ZipEntry entry = zin.getNextEntry();
					while ( entry != null ) {
						if ( entry.getName().equals("repository.xml")) {
							in = zin;
							break;
						}
						entry = zin.getNextEntry();
					}
				} else {
					in = url.openStream();
				}
				Reader reader = new InputStreamReader(in);
				XmlPullParser parser = new KXmlParser();
				parser.setInput(reader);
				parseRepository(parser);
			} catch( MalformedURLException e ) {
				System.out.println("Cannot create connection to url");
			}
		}
	}

	public URL getURL() {
		return url;
	}

	/**
	 * @return
	 */
	public Collection getResourceList() {
		return resources;
	}

	public Resource[] getResources() {
		return (Resource[]) getResourceList().toArray(EMPTY_RESOURCE);
	}

	public String getName() {
		return name;
	}

	public Resource getResource(String id) {
		for (Iterator i = getResourceList().iterator(); i.hasNext();) {
			ResourceImpl resource = (ResourceImpl) i.next();
			if (resource.getId().equals(id))
				return resource;
		}
		return null;
	}

	public long getLastModified() {
		return lastModified;
	}

	public int getRanking() {
		return ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

}
