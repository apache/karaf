/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.osgi.framework.ServiceReference;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.httpconnector.HttpConnectorActivator;


/**
 * XSLTPostProcessor pass the document through an XSLT transformation
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 * the HttpAdaptor through a XSL transformation" extends="mx4j.tools.adaptor.http.ProcessorMBean"
 */
public class XSLTProcessor
	implements ProcessorMBean, XSLTProcessorMBean, URIResolver
{
	TransformerFactory factory = null;

	private Map templatesCache = new HashMap();

	private String path = "mx4j/tools/adaptor/http/xsl";

	private File root = null;

	private Map mimeTypes = new HashMap();

	/** Indicated whether the file are read from a file */
	private boolean useJar = true;

	private boolean useCache = true;

	private ClassLoader targetClassLoader = ClassLoader.getSystemClassLoader();

	private String defaultPage = "serverbydomain";

	/**
	 * The locale is set with the default as en_US since it is the
	 * one bundled
	 */
	private Locale locale = new Locale("en", "");

	public XSLTProcessor()
	{
		factory = TransformerFactory.newInstance();
		factory.setURIResolver(this);
		mimeTypes.put(".gif", "image/gif");
		mimeTypes.put(".jpg", "image/jpg");
		mimeTypes.put(".png", "image/png");
		mimeTypes.put(".tif", "image/tiff");
		mimeTypes.put(".tiff", "image/tiff");
		mimeTypes.put(".ico", "image/ico");
		mimeTypes.put(".html", "text/html");
		mimeTypes.put(".htm", "text/html");
		mimeTypes.put(".txt", "text/plain");
		mimeTypes.put(".xml", "text/xml");
		mimeTypes.put(".xsl", "text/xsl");
		mimeTypes.put(".css", "text/css");
		mimeTypes.put(".js", "text/x-javascript");
		mimeTypes.put(".jar", "application/java-archive");
	}

	public void writeResponse(HttpOutputStream out, HttpInputStream in, Document document) throws IOException
	{
		out.setCode(HttpConstants.STATUS_OKAY);
		out.setHeader("Content-Type", "text/html");
		// added some caching attributes to fornce not to cache
		out.setHeader("Cache-Control", "no-cache");
		out.setHeader("expires", "now");
		out.setHeader("pragma", "no-cache");
		out.sendHeaders();
		Transformer transformer = null;
		String path = preProcess(in.getPath());

		if (in.getVariable("template") != null)
		{
			transformer = createTransformer(in.getVariable("template") + ".xsl");
		}
		else
		{
			transformer = createTransformer(path + ".xsl");
		}

		if (transformer != null)
		{
			// added so that the document() function works
			transformer.setURIResolver(this);
			// The variables are passed to the XSLT as (param.name, value)
			Map variables = in.getVariables();
			Iterator j = variables.keySet().iterator();
			while (j.hasNext())
			{
				String key = (String)j.next();
				Object value = variables.get(key);
				if (value instanceof String) {
					transformer.setParameter("request." + key, value);
				}
				if (value instanceof String[]) {
					String[] allvalues = (String[])value;
					// not a good solution, only the first one is presented
					transformer.setParameter("request." + key, allvalues[0]);
				}

			}
			if (!variables.containsKey("locale"))
			{
				transformer.setParameter("request.locale", locale.toString());
			}
			try
			{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				XSLTProcessor.log(LogService.LOG_DEBUG,"transforming " + path,null);
				transformer.transform(new DOMSource(document), new StreamResult(output));
				output.writeTo(out);
			}
			catch (TransformerException e)
			{
				XSLTProcessor.log(LogService.LOG_ERROR,"Transformation exception ", e);
			}
		}
		else
		{
			XSLTProcessor.log(LogService.LOG_WARNING,"Transformer for path " + path + " not found",null);
		}
	}

	protected Transformer createTransformer(String path)
	{
		Transformer transformer = null;
		try
		{
			if (useCache && templatesCache.containsKey(path))
			{
				transformer = ((Templates)templatesCache.get(path)).newTransformer();
			}
			else
			{
				InputStream file = getInputStream(path);
				if (file != null)
				{
					XSLTProcessor.log(LogService.LOG_INFO,"Creating template for path "+path, null);
					Templates template = factory.newTemplates(new StreamSource(file));
					transformer = template.newTransformer();
					if (useCache)
					{
						templatesCache.put(path, template);
					}
				}
				else
				{
					XSLTProcessor.log(LogService.LOG_WARNING,"template for path "+path+" not found", null);
				}
			}
		}
		catch (TransformerConfigurationException e)
		{
			XSLTProcessor.log(LogService.LOG_ERROR,"Exception during template construction", e);
		}
		return transformer;
	}

	protected void processHttpException(HttpInputStream in, HttpOutputStream out, HttpException e) throws IOException
	{
		out.setCode(e.getCode());
		out.setHeader("Content-Type", "text/html");
		out.sendHeaders();
		// hardcoded dir :-P
		Transformer transformer = createTransformer("error.xsl");
		transformer.setURIResolver(this);
		Document doc = e.getResponseDoc();
		if (doc != null)
		{
			try
			{
				if (!in.getVariables().containsKey("locale"))
				{
					transformer.setParameter("request.locale", locale.toString());
				}
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				transformer.transform(new DOMSource(doc), new StreamResult(output));
				output.writeTo(out);
			}
			catch (TransformerException ex)
			{
				XSLTProcessor.log(LogService.LOG_ERROR,"Exception during error output", ex);
			}
		}
	}

	public void writeError(HttpOutputStream out, HttpInputStream in, Exception e) throws IOException
	{
		Exception t = e;
		if (e instanceof RuntimeMBeanException)
		{
			t = ((RuntimeMBeanException)e).getTargetException();
		}
		XSLTProcessor.log(LogService.LOG_INFO,"Processing error " + t.getMessage(),null);
		if (t instanceof HttpException)
		{
			processHttpException(in, out, (HttpException)e);
		}
		else if ((t instanceof MBeanException) && (((MBeanException)t).getTargetException() instanceof HttpException))
		{
			processHttpException(in, out, (HttpException)((MBeanException)t).getTargetException());
		}
		else if ((t instanceof ReflectionException) && (((ReflectionException)t).getTargetException() instanceof HttpException))
		{
			processHttpException(in, out, (HttpException)((ReflectionException)t).getTargetException());
		}
		else
		{
			out.setCode(HttpConstants.STATUS_INTERNAL_ERROR);
			out.setHeader("Content-Type", "text/html");
			out.sendHeaders();
		}
	}

	public String preProcess(String path) {
		if (path.equals("/"))
		{
			path = "/" + defaultPage;
		}
		return path;
	}

	public String notFoundElement(String path, HttpOutputStream out, HttpInputStream in) throws IOException, HttpException
	{
		File file = new File(this.path, path);
		XSLTProcessor.log(LogService.LOG_INFO,"Processing file request " + file,null);
		String name = file.getName();
		int extensionIndex = name.lastIndexOf('.');
		String mime = null;
		if (extensionIndex < 0)
		{
			XSLTProcessor.log(LogService.LOG_WARNING,"Filename has no extensions " + file.toString(),null);
			mime = "text/plain";
		}
		else
		{
			String extension = name.substring(extensionIndex, name.length());
			if (mimeTypes.containsKey(extension))
			{
				mime = (String)mimeTypes.get(extension);
			} else {
				XSLTProcessor.log(LogService.LOG_WARNING,"MIME type not found " + extension,null);
				mime = "text/plain";
			}
		}
		try
		{
			XSLTProcessor.log(LogService.LOG_DEBUG,"Trying to read file " + file,null);
			BufferedInputStream fileIn = new BufferedInputStream(getInputStream(path));
			ByteArrayOutputStream outArray = new ByteArrayOutputStream();
			BufferedOutputStream outBuffer = new BufferedOutputStream(outArray);
			int piece = 0;
			while ((piece = fileIn.read()) >= 0)
			{
				outBuffer.write(piece);
			}
			outBuffer.flush();
			out.setCode(HttpConstants.STATUS_OKAY);
			out.setHeader("Content-type", mime);
			out.sendHeaders();
			XSLTProcessor.log(LogService.LOG_DEBUG,"File output " + mime,null);
			outArray.writeTo(out);
			fileIn.close();
		}
		catch (Exception e)
		{
			XSLTProcessor.log(LogService.LOG_WARNING,"Exception loading file " + file, e);
			throw new HttpException(HttpConstants.STATUS_NOT_FOUND, "file " + file + " not found");
		}
		return null;
	}

	protected InputStream getInputStream(String path)
	{
		InputStream file = null;
		if (!useJar)
		{
			try
			{
				// load from a dir
				file = new FileInputStream(new File(this.root, path));
			}
			catch (FileNotFoundException e)
			{
				XSLTProcessor.log(LogService.LOG_ERROR,"File not found", e);
			}
		}
		else
		{
			// load from a jar
			String targetFile = this.path;
			// workaround, should tought of somehting better
			if (path.startsWith("/"))
			{
				targetFile += path;
			} else {
				targetFile += "/" + path;
			}
			if (root != null)
			{
				file = targetClassLoader.getResourceAsStream(targetFile);
			}
			if (file == null)
			{
				ClassLoader cl=getClass().getClassLoader();
				if (cl == null)
				{
					file = ClassLoader.getSystemClassLoader().getResourceAsStream(targetFile);
				}
				else
				{
					file = getClass().getClassLoader().getResourceAsStream(targetFile);
				}
				file = getClass().getClassLoader().getResourceAsStream(targetFile);
			}
		}

		return file;
	}

	public Source resolve(String href, String base)
	{
		StreamSource source = new StreamSource(getInputStream(href));
		// this works with saxon7/saxon6.5.2/xalan
		source.setSystemId(href);
		return source;
	}

	public void setFile(String file)
	{
		if (file != null)
		{

			File target = new File(file);
			if (!target.exists())
			{
				XSLTProcessor.log(LogService.LOG_WARNING,"Target file " + file + " does not exist, defaulting to previous",null);
				return;
			}
			if (target.isDirectory())
			{
				useJar = false;
				XSLTProcessor.log(LogService.LOG_INFO, "Using " + file + " as the root dir", null);
				this.root = target;
				return;
			}
			if (target.isFile() && (target.getName().endsWith(".jar") ||
				(target.getName().endsWith(".zip"))))
			{
				try {
					URL url = target.toURL();
					targetClassLoader = new URLClassLoader(new URL[] {url});
					XSLTProcessor.log(LogService.LOG_INFO,"Using compressed file " + url + " as the root file",null);
					this.root = target;
					useJar = true;
				} catch (MalformedURLException e) {
					XSLTProcessor.log(LogService.LOG_WARNING,"Unable to create class loader", e);
				}
			}
			else
			{
				XSLTProcessor.log(LogService.LOG_WARNING,"Target file " + file + " does not exist, defaulting to previous",null);
			}
		}
	}

	public String getFile()
	{
		return (root != null)?root.getName():null;
	}

	public String getPathInJar()
	{
		return path;
	}

	public void setPathInJar(String path)
	{
		this.path = path;
	}

	public String getDefaultPage()
	{
		return defaultPage;
	}

	public void setDefaultPage(String defaultPage)
	{
		this.defaultPage = defaultPage;
	}

	public boolean isUseJar()
	{
		return useJar;
	}

	public boolean isUsePath()
	{
		return !useJar;
	}

	public void addMimeType(String extension, String type)
	{
		if (extension != null && type != null)
		{
			XSLTProcessor.log(LogService.LOG_INFO,"Added MIME type " + type + " for extension " + extension,null);
			mimeTypes.put(extension, type);
		}
	}

	public void setUseCache(boolean useCache)
	{
		this.useCache = useCache;
	}

	public boolean isUseCache()
	{
		return useCache;
	}

	public String getName()
	{
		return "XSLT Processor";
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setLocaleString(String locale) {
		if (locale == null || locale.length()==0) {
		  this.locale = new Locale("en", "");
		}
		else
		{
		  // split locale based on underbar
		  StringTokenizer tknzr = new StringTokenizer(locale,"_");
		  String language = tknzr.nextToken();
		  String country = "";
		  String variant = "";
		  if (tknzr.hasMoreTokens())
		    country = tknzr.nextToken();
		  if (tknzr.hasMoreTokens())
		    variant = tknzr.nextToken();
		  this.locale = new Locale(language,country,variant);
		}
	}

    private static void log(int prio, String message, Throwable t){
    if (HttpConnectorActivator.bc!=null){
    ServiceReference logSR=HttpConnectorActivator.bc.getServiceReference(LogService.class.getName());
    if (logSR!=null){
      ((LogService)HttpConnectorActivator.bc.getService(logSR)).log(prio, message, t);
    }else{
      System.out.println("No Log Service");
    }
    }else{
      System.out.println("mx4j.tools.adapatoir.http.XSLTProcessor.log: No bundleContext");
    }
  }

}
