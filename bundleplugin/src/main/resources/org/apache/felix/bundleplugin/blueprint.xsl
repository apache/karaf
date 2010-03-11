<?xml version="1.0" encoding="UTF-8"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:bp="http://www.osgi.org/xmlns/blueprint/v1.0.0">
    <xsl:param name="nsh_interface" select="''"/>
    <xsl:param name="nsh_namespace" select="''"/>
	<xsl:output method="text" />

	<xsl:template match="/">


        <xsl:text>Import-Package:org.osgi.service.blueprint;version="[1.0.0,2.0.0)"
        </xsl:text>

        <xsl:if test="not($nsh_interface = '' or $nsh_namespace = '')">
            <xsl:for-each select="descendant-or-self::node() | descendant-or-self::node()/attribute::*">
                <xsl:if test="not(namespace-uri() = 'http://www.osgi.org/xmlns/blueprint/v1.0.0'
                                        or namespace-uri() = 'http://www.w3.org/2001/XMLSchema-instance'
                                        or namespace-uri() = '')">
                    <xsl:value-of select="concat('Import-Service:', $nsh_interface, ';filter=&quot;(', $nsh_namespace, '=', namespace-uri(), ')&quot;')" />
                    <xsl:text>
                    </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>

		<xsl:for-each select="
				//bp:bean/@class
			|	//bp:service/@interface
			|   //bp:service/bp:interfaces/bp:value/text()
 			|	//bp:reference/@interface
			|	//bp:reference-list/@interface
		">
			<xsl:value-of select="concat('Import-Class:', .)" />
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
		        <xsl:when test="contains(., '[')"><xsl:value-of select="concat('Import-Class:', substring-before(., '['))"/></xsl:when>
		        <xsl:otherwise><xsl:value-of select="concat('Import-Class:', .)"/></xsl:otherwise>
			</xsl:choose>
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

        <xsl:for-each select="//bp:service">
            <xsl:choose>
                <xsl:when test="@interface">
                    <xsl:value-of select="concat('Export-Service:', @interface)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="bp:interfaces/bp:value/text()">
                            <xsl:value-of select="concat('Export-Service:', bp:interfaces/bp:value/text())" />
                        </xsl:when>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="bp:service-properties/bp:entry">
                <xsl:value-of select="concat(';', @key, '=&quot;', @value, '&quot;')" />
            </xsl:for-each>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>

        <xsl:for-each select="//bp:reference[@interface] | //bp:reference-list[@interface]">
            <xsl:value-of select="concat('Import-Service:', @interface)" />
            <xsl:choose>
                <xsl:when test="@availability">
                    <xsl:value-of select="concat(';availability:=', @availability)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="/bp:blueprint/@default-availability">
                            <xsl:value-of select="concat(';availability:=', /bp:blueprint/@default-availability)"/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="@filter">
                    <xsl:choose>
                        <xsl:when test="@component-name">
                            <xsl:value-of select="concat(';filter=&quot;(&amp;', @filter, ')(osgi.service.blueprint.compname=',  @component-name, ')&quot;')" />
                         </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat(';filter=&quot;', @filter, '&quot;')" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="@component-name">
                            <xsl:value-of select="concat(';filter=&quot;(osgi.service.blueprint.compname=', @component-name, ')&quot;')" />
                        </xsl:when>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>

