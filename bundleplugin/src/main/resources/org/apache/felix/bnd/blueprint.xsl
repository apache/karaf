<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:bp="http://www.osgi.org/xmlns/blueprint/v1.0.0">
	<xsl:output method="text" />

	<xsl:template match="/">

		<!-- Match all attributes that holds a class or a comma delimited 
		     list of classes and print them -->

		<xsl:for-each select="
				//bp:bean/@class 
			|	//bp:service/@interface 
			|   //bp:service/bp:interfaces/bp:value/text()
 			|	//bp:reference/@interface
			|	//bp:reference-list/@interface
		">
			<xsl:value-of select="." />
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

		<xsl:for-each select="
				//bp:bean/bp:argument/@type
		    |	//bp:list/@value-type 
    		|	//bp:set/@value-type 
    		|	//bp:array/@value-type 
			|   //bp:map/@key-type
			|   //bp:map/@value-type
		">
		    <xsl:choose>
		        <xsl:when test="contains(., '[')"><xsl:value-of select="substring-before(., '[')"/></xsl:when>
		        <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
			</xsl:choose>
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

	</xsl:template>


</xsl:stylesheet>

