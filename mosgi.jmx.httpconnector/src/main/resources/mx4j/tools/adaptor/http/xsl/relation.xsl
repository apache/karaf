<?xml version="1.0"?>
<!--
 Copyright (C) MX4J.
 All rights reserved.

 This software is distributed under the terms of the MX4J License version 1.0.
 See the terms of the MX4J License in the documentation provided with this software.

 Author: Bronwen Cassidy (shadow12@users.sourceforge.net)
 Revision: $Revision: 1.1.1.1 $
 																																					-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>

  <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
  <xsl:param name="html.stylesheet.type">text/css</xsl:param>
  <xsl:param name="head.title">relation.title</xsl:param>
  <xsl:include href="common.xsl"/>

   <xsl:template match="relation-type-name" name="relationtypename">
       <table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td colspan="7" width="50%" class="mbeans">
					<xsl:call-template name="str">
						<xsl:with-param name="id">relation.typename.title</xsl:with-param>
					</xsl:call-template>
				</td>
				<td colspan="7" width="50%" class="mbeans"><xsl:value-of select="@name"/></td>
            </tr>
			<tr class="darkline">
				<td>
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">relation.metadata.title</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
			</tr>
			<tr><xsl:apply-templates select="./relation-meta-data"/></tr>
			<tr class="darkline">
				<td>
					<div class="tableheader">
						<xsl:call-template name="str">
							<xsl:with-param name="id">relation.relationids.title</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
			</tr>
			<tr>
				<td class="domainline">
					<xsl:apply-templates select="./relation-id"/>
				</td>
			</tr>
		</table>
  </xsl:template>

  <xsl:template match="relation-meta-data" name="meta-data">
   <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <xsl:variable name="position">
		  <xsl:if test="(position() mod 2)=1">darkline</xsl:if>
		  <xsl:if test="(position() mod 2)=0">clearline</xsl:if>
	  </xsl:variable>
      <tr width="100%" border="1">
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.rolename.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./role-name"/></td>
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.classname.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./mbean-classname"/></td>
	  </tr>
	  <tr  border="1">
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.description.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./description"/></td>
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.mindegree.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./min-degree"/></td>
	  </tr>
	  <tr border="1">
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.maxdegree.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./max-degree"/></td>
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.readable.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./is-readable"/></td>
		<td class="{$position}"><b>
			<xsl:call-template name="str">
				<xsl:with-param name="id">relation.writable.title</xsl:with-param>
			</xsl:call-template></b>
		</td><td><xsl:apply-templates select="./is-writable"/></td>
	  </tr>
	</table>
  </xsl:template>

  <xsl:template match="role-name" name="roleName">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="mbean-classname" name="classname">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="description" name="description">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="min-degree" name="minimum">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="max-degree" name="maximum">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="is-readable" name="reading">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="is-writable" name="writing">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="default" name="default">
      <xsl:value-of select="text()"/>
  </xsl:template>

  <xsl:template match="relation-id" name="relationId">
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <xsl:variable name="position">
        <xsl:if test="(position() mod 2)=1">darkline</xsl:if>
        <xsl:if test="(position() mod 2)=0">clearline</xsl:if>
      </xsl:variable>
    	<tr>
        <td class="{$position}" align="justify">
			<xsl:value-of select="text()"/>
        </td>
      </tr>
	</table>
  </xsl:template>

  <xsl:template match="RelationServer">
    <html>
    	<xsl:call-template name="head"/>
      <body>
      	<xsl:call-template name="toprow"/>
        <xsl:call-template name="tabs">
        	<xsl:with-param name="selection">relation</xsl:with-param>
        </xsl:call-template>
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr>
          	<td colspan="7" width="100%" align="center" class="fronttab">
				<xsl:call-template name="str">
					<xsl:with-param name="id">relation.inprogress</xsl:with-param>
				</xsl:call-template>
			</td>
          </tr>
        </table>
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
		  <xsl:apply-templates select="./default"/>
          <xsl:apply-templates select="./relation-type-name"/>
        </table>
        <xsl:call-template name="bottom"/>
      </body>
  </html>
</xsl:template>
</xsl:stylesheet>

