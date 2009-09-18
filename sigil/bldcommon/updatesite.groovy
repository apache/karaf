import java.util.jar.JarFile

def processFeature(file,writer) {
  def jar = new JarFile(file)

  if (jar.getEntry( "feature.xml" ) != null) {
    def path = 'features/' + file.getName()
    def attrs = jar.getManifest().getMainAttributes()

    def id = attrs.getValue('Bundle-SymbolicName')
    def version = attrs.getValue('Bundle-Version')

    writer.println( '  <feature url="' + path + '" id="' + id + '" version="' + version + '">' )
    writer.println( '    <category name="Sigil"/>')
    writer.println( '  </feature>' )
  }

  jar.close()
}

def findJars(dir,closure) {
  dir.eachFileRecurse() { f ->
    if (f ==~ /.*jar$/) closure( f )
  }
}


def generateUpdateSite(dir,site) {
  def writer = new PrintWriter( site )
  writer.println('<site>')

  findJars( dir, { f -> processFeature( f, writer ) } )

  writer.println('  <category-def name="Sigil" label="Sigil-Core"> ')
  writer.println('    <description>')
  writer.println('      Sigil is an SDK for developing OSGi applications')
  writer.println('    </description>')
  writer.println('  </category-def>')
  writer.println('</site>')

  writer.close()
}

args.each{ ant.echo(message:it) }

def dir = new File( args[0] )
def site = new File( args[1] )

generateUpdateSite(dir, site)
