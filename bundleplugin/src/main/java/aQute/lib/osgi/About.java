/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

/**
 * This package contains a number of classes that assists by analyzing JARs and
 * constructing bundles.
 * 
 * The Analyzer class can be used to analyze an existing bundle and can create a
 * manifest specification from proposed (wildcard) Export-Package,
 * Bundle-Includes, and Import-Package headers.
 * 
 * The Builder class can use the headers to construct a JAR from the classpath.
 * 
 * The Verifier class can take an existing JAR and verify that all headers are
 * correctly set. It will verify the syntax of the headers, match it against the
 * proper contents, and verify imports and exports.
 * 
 * A number of utility classes are available.
 * 
 * Jar, provides an abstraction of a Jar file. It has constructors for creating
 * a Jar from a stream, a directory, or a jar file. A Jar, keeps a collection
 * Resource's. There are Resource implementations for File, from ZipFile, or from
 * a stream (which copies the data). The Jar tries to minimize the work during
 * build up so that it is cheap to use. The Resource's can be used to iterate 
 * over the names and later read the resources when needed.
 * 
 * Clazz, provides a parser for the class files. This will be used to define the
 * imports and exports.
 * 
 * A key component in this library is the Map. Headers are translated to Maps of Maps. OSGi
 * header syntax is like:
 * <pre>
 * 	  header = clause ( ',' clause ) *
 *    clause = file ( ';' file ) * ( parameter ) *
 *    param  = attr '=' value | directive ':=' value
 * </pre>
 * These headers are translated to a Map that contains all headers (the order is
 * maintained). Each additional file in a header definition will have its own
 * entry (only native code does not work this way). The clause is represented
 * as another map. The ':' of directives is considered part of the name. This
 * allows attributes and directives to be maintained in the clause map. 
 * 
 * @version $Revision: 1.1 $
 */
public class About {

}
