Various information about these examples can be found here and on the tutorial
page here: xxxx.  Some notes follow regarding tutorial projects in this example 
directory.

 o The dictionaryservice.itest project demonstrates how to use the maven-felix
   -plugin to perform integration tests.  Look at it's pom to see how to use the
   new plugin which runs bundles inside Felix during the integration-test phase
   of the Maven default lifecycle.
  
   dictionaryservice.itest is a testing bundle with a simple Activator that 
   performs some simple operations on a target dictionaryservice bundle and 
   checks for correct operation.
