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
	<xsl:param name="head.title">mlet.title</xsl:param>
	<xsl:include href="common.xsl"/>

	<xsl:template name="domain">
		<xsl:for-each select="Domain[Mbean]">
			<tr>
				<td class="domainline">
					<div class="domainheading"><xsl:value-of select="@name"/></div>
					<table width="100%" cellpadding="0" cellspacing="0" border="0">
						<xsl:call-template name="mbean"/>
					</table>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="MBean" name="mbean">
		<xsl:for-each select="MBean">
			<tr>
				<xsl:variable name="classtype">
					<xsl:if test="(position() mod 2)=1">darkline</xsl:if>
					<xsl:if test="(position() mod 2)=0">clearline</xsl:if>
				</xsl:variable>
				<xsl:variable name="objectname" select="@objectname"/>
				<td class="{$classtype}">
					<a href="mbean?objectname={$objectname}"><xsl:value-of select="$objectname"/></a>
				</td>
				<td align="right" class="{$classtype}">
					<a href="delete?objectname={$objectname}">
						<xsl:call-template name="str">
							<xsl:with-param name="id">mlet.mbean.unregister</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- Main processing template -->
	<xsl:template match="Server" name="main">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">mlet</xsl:with-param>
				</xsl:call-template>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr>
						<td colspan="7" width="100%" class="page_title">
							<xsl:call-template name="str">
								<xsl:with-param name="id">mlet.main.title</xsl:with-param>
							</xsl:call-template>
						</td>
					</tr>
				</table>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<xsl:call-template name="domain"/>
				</table>
				<xsl:call-template name="bottom"/>
			</body>
	</html>
</xsl:template>
</xsl:stylesheet>

