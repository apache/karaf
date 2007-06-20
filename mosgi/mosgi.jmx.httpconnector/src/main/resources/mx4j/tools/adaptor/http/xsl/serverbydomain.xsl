<?xml version="1.0"?>
<!--

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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 	version="1.0">
	<xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
	<xsl:include href="common.xsl"/>
	<xsl:include href="xalan-ext.xsl"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">serverbydomain.title</xsl:param>

	<!-- Invoked when a query error is produced -->
	<xsl:template match="Domain" name="error">
		<xsl:for-each select="Exception">
			<tr>
				<td class="serverbydomain_domainline" colspan="5">
					<xsl:call-template name="str">
						<xsl:with-param name="id">serverbydomain.error.query</xsl:with-param>
						<xsl:with-param name="p0">
								<xsl:value-of select="@errorMsg"/>
							</xsl:with-param>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- Invoked to display each domain -->
	<xsl:template match="Domain" name="domain">
		<xsl:for-each select="Domain">
			<xsl:sort data-type="text" order="ascending" select="@name"/>
			<tr>
				<td class="serverbydomain_domainline" colspan="5">
						<xsl:call-template name="str">
							<xsl:with-param name="id">serverbydomain.domain.label</xsl:with-param>
							<xsl:with-param name="p0">
								<xsl:value-of select="@name"/>
							</xsl:with-param>
						</xsl:call-template>
				</td>
				<xsl:call-template name="mbean"/>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- invoked for each mbean -->
	<xsl:template match="MBean" name="mbean">
		<xsl:for-each select="MBean">
			<xsl:sort data-type="text" order="ascending" select="@objectname"/>
				<xsl:variable name="classtype">
					<xsl:if test="(position() mod 2)=1">darkline</xsl:if>
					<xsl:if test="(position() mod 2)=0">clearline</xsl:if>
				</xsl:variable>
				<xsl:variable name="objectname">
					<xsl:call-template name="uri-encode">
						<xsl:with-param name="uri" select="@objectname"/>
					</xsl:call-template>
				</xsl:variable>
				<tr class="{$classtype}" width="100%">
					<td width="35%" align="left" class="serverbydomain_row">
						<a href="mbean?objectname={$objectname}"><xsl:value-of select="@objectname"/></a>
					</td>
					<td width="20%" align="left" class="serverbydomain_row">
						<p><xsl:value-of select="@classname"/></p>
					</td>
					<td width="35%" align="left" class="serverbydomain_row">
						<p><xsl:value-of select="@description"/></p>
					</td>
					<td width="10%" align="right" class="serverbydomain_row">
						<p>
						<a href="delete?objectname={$objectname}">
						<xsl:call-template name="str">
							<xsl:with-param name="id">serverbydomain.mbean.unregister</xsl:with-param>
						</xsl:call-template>
						</a></p>
					</td>
				</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- Main template -->
	<xsl:template match="Server">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr width="100%">
						<td>
						<xsl:call-template name="toprow"/>
						<xsl:call-template name="tabs">
							<xsl:with-param name="selection">server</xsl:with-param>
						</xsl:call-template>
						<xsl:variable name="query">
							<xsl:call-template name="str">
								<xsl:with-param name="id">serverbydomain.server.query</xsl:with-param>
							</xsl:call-template>
						</xsl:variable>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">

								<tr>
									<td class="page_title">
										<xsl:call-template name="str">
											<xsl:with-param name="id">serverbydomain.server.title</xsl:with-param>
										</xsl:call-template>
									</td>
									<form action="serverbydomain">
									<td align="right" class="page_title">
										<xsl:call-template name="str"><xsl:with-param name="id">serverbydomain.server.filter</xsl:with-param></xsl:call-template><input type="text" name="querynames" value="*:*"/><input type="submit" value="{$query}"/>
									</td>
									</form>
								</tr>
							</table>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<xsl:call-template name="domain"/>
								<xsl:call-template name="error"/>
							</table>
						<xsl:call-template name="bottom"/>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

