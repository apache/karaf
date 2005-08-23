***********************
* Apache Felix README *
***********************

This document is basically a catch-all for general information about the Apache
Felix project.  It is anticipated that this document will be updated from time
to time with new information.

SVN PROJECT STRUCTURE

The Apache Felix project has the following directory structure in order to organize
the project's varied product artifacts.

+ framework
+ sandbox
+ tools

The *framework* directory contains the source and build tree for the OSGi-compliant
framework implementation.  It is, of course, the primary product of this project.

The *sandbox* directory contains a directory tree whereby project committers can 
store various experimental and working versions of code.  Each committer that wishes
to utilize the sandbox should create a subdirectory named after his/her apache username.

e.g.

- sandbox
  + erodriguez
  + rickhall
  + tbennett

The *tools* directory contains a directory tree for various tools that may be developed
in association with this project.  One such tool is the maven-osgi-plugin currently under
development for the MAVEN2 build system.  A tools directory may itself have multiple
products each requiring their own directory.

e.g.

- tools
  - maven2
    + maven-osgi-plugin

As the project matures, we certainly may see other directories created to organize other
types of products.