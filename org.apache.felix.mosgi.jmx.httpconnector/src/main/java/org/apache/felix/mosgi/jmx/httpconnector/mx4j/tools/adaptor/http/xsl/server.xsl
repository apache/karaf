<?xml version="1.0"?>
<!--
 Copyright (C) MX4J.
 All rights reserved.

 This software is distributed under the terms of the MX4J License version 1.0.
 See the terms of the MX4J License in the documentation provided with this software.

 Author: Carlos Quiroz (tibu@users.sourceforge.net)
 Revision: $Revision: 1.1.1.1 $
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html" indent="yes" encoding="UTF-8"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">server.title</xsl:param>
	<xsl:include href="common.xsl"/>
	<xsl:include href="xalan-ext.xsl"/>

	<xsl:template name="mbean">
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
					<td class="domainline"/>
					<td width="35%" align="left">
						<a href="mbean?objectname={$objectname}"><xsl:value-of select="@objectname"/></a>
					</td>
					<td width="20%" align="left">
						<xsl:value-of select="@classname"/>
					</td>
					<td width="35%" align="left">
						<xsl:value-of select="@description"/>
					</td>
					<td width="10%" align="right" class="{$classtype}" >
						<a href="delete?objectname={$objectname}">
							<xsl:call-template name="str">
								<xsl:with-param name="id">server.unregister</xsl:with-param>
							</xsl:call-template>
						</a>
					</td>
				</tr>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="Server">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">server</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr class="fronttab">
						<td >
							<xsl:call-template name="str">
								<xsl:with-param name="id">server.mbeans.title</xsl:with-param>
							</xsl:call-template>
						</td>
						<form action="server">
							<td align="right">
								<xsl:call-template name="str">
									<xsl:with-param name="id">server.filter.title</xsl:with-param>
								</xsl:call-template>
								<input type="text" name="querynames" value="*:*"/>
								<input type="submit" value="query"/>
							</td>
						</form>
					</tr>
				</table>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<xsl:call-template name="mbean"/>
				</table>
				<xsl:call-template name="bottom"/>
			</body>
	</html>
</xsl:template>
</xsl:stylesheet>

