<?xml version="1.0"?>
<!--
 Copyright (C) MX4J.
 All rights reserved.

 This software is distributed under the terms of the MX4J License version 1.0.
 See the terms of the MX4J License in the documentation provided with this software.

 Author: Carlos Quiroz (tibu@users.sourceforge.net)
 Revision: $Revision: 1.1.1.1 $
																																					-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 	version="1.0">
	<xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
	<xsl:include href="common.xsl"/>
	<xsl:include href="xalan-ext.xsl"/>

	<xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
	<xsl:param name="html.stylesheet.type">text/css</xsl:param>
	<xsl:param name="head.title">emptymbean.title</xsl:param>

	<!-- Main template -->
	<xsl:template match="/">
		<html>
			<xsl:call-template name="head"/>
			<body>
				<table width="100%" cellpadding="0" cellspacing="0" border="0">
					<tr width="100%">
						<td>
						<xsl:call-template name="toprow"/>
						<xsl:call-template name="tabs">
							<xsl:with-param name="selection">mbean</xsl:with-param>
						</xsl:call-template>
							<table width="100%" cellpadding="0" cellspacing="0" border="0">
								<tr>
									<td class="page_title">
										<xsl:call-template name="str">
											<xsl:with-param name="id">emptymbean.title</xsl:with-param>
										</xsl:call-template>
									</td>
								</tr>
								<tr>
									<form action="/constructors">
										<td class="page_title">
										<xsl:call-template name="str">
											<xsl:with-param name="id">emptymbean.querycontructors</xsl:with-param>
										</xsl:call-template>
										<xsl:variable name="str.query">
											<xsl:call-template name="str">
												<xsl:with-param name="id">emptymbean.query</xsl:with-param>
											</xsl:call-template>
										</xsl:variable>
										<input type="input" name="classname"/><input type="submit" value="{$str.query}"/>
										</td></form></tr>
							</table>
						<xsl:call-template name="bottom"/>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>

