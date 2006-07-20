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

	<!-- Overall parameters -->
	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">create.title</xsl:param>

	<!-- Request parameters -->
	<xsl:param name="request.objectname"/>
	<xsl:param name="request.class"/>

	<xsl:include href="common.xsl"/>

	<!-- Operation processing template -->
	<xsl:template name="operation" >
		<xsl:for-each select="Operation">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td width="100%" class="page_title">
					<xsl:call-template name="str">
						<xsl:with-param name="id">create.operation.title</xsl:with-param>
						<xsl:with-param name="p0"><xsl:value-of select="$request.class"/></xsl:with-param>
						<xsl:with-param name="p1"><xsl:value-of select="$request.objectname"/></xsl:with-param>
					</xsl:call-template>
				</td>
			</tr>
			<tr class="darkline">
				<td>
					<xsl:if test="@result='success'">
						<xsl:call-template name="str">
							<xsl:with-param name="id">create.operation.success</xsl:with-param>
						</xsl:call-template>
					</xsl:if>
					<xsl:if test="@result='error'">
						<xsl:call-template name="str">
							<xsl:with-param name="id">create.operation.error</xsl:with-param>
							<xsl:with-param name="p0"><xsl:value-of select="@errorMsg"/></xsl:with-param>
						</xsl:call-template>
					</xsl:if>
				 </td>
			</tr>
			<xsl:call-template name="serverview"/>
		</table>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="MBeanOperation">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<xsl:call-template name="toprow"/>
				<xsl:call-template name="tabs">
					<xsl:with-param name="selection">mbean</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="operation"/>
				<xsl:call-template name="bottom"/>
			</body>
	</html>
</xsl:template>
</xsl:stylesheet>

