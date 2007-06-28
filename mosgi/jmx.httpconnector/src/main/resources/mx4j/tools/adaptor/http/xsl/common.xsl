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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<!-- Import xalan extensions -->
		<xsl:import href="xalan-ext.xsl" />

	<!-- Defin string variables -->
	<xsl:param name="request.locale">en</xsl:param>

	<xsl:variable name="strings" select="document(concat('strings_', $request.locale, '.xml'))" />

	<!-- Common head template -->
	<xsl:template name="head">
		<xsl:if test="$head.title">
			<title>
				<xsl:call-template name="str">
					<xsl:with-param name="id">
						<xsl:value-of select="$head.title" />
					</xsl:with-param>
				</xsl:call-template>
			</title>
		</xsl:if>

		<xsl:if test="$html.stylesheet">
			<link rel="stylesheet" href="{$html.stylesheet}"
			type="{$html.stylesheet.type}" />
		</xsl:if>

		<meta http-equiv="Expires" content ="0"/>
		<meta http-equiv="Pragma" content="no-cache"/>
		<meta http-equiv="Cache-Control" content="no-cache"/>
		<meta name="generator" content="MX4J HttpAdaptor, JMX, JMX implementation" />

	</xsl:template>

	<!-- Common title template -->
	<xsl:template name="toprow">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="darker" colspan="2"/>
			</tr>

			<tr>
				<td class="topheading">
					<xsl:call-template name="str">
						<xsl:with-param name="id">common.title</xsl:with-param>
					</xsl:call-template>
					<br/>
					<div class="subtitle">
					<xsl:call-template name="str">
						<xsl:with-param name="id">common.subtitle</xsl:with-param>
					</xsl:call-template>
					</div>
				</td>

				<td class="topheading" align="right">
					<xsl:variable name="str.logo">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.logo</xsl:with-param>
						</xsl:call-template>
					</xsl:variable>
					<xsl:variable name="str.logo.gif">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.logo.gif</xsl:with-param>
							</xsl:call-template>
					</xsl:variable>
					<xsl:variable name="str.site">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.site</xsl:with-param>
						</xsl:call-template>
					</xsl:variable>
					<xsl:variable name="str.logo.width">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.logo.width</xsl:with-param>
						</xsl:call-template>
					</xsl:variable>
					<xsl:variable name="str.logo.height">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.logo.height</xsl:with-param>
						</xsl:call-template>
					</xsl:variable>
					<a href="http://{$str.site}"><img src="{$str.logo.gif}" width="{$str.logo.width}" height="{$str.logo.height}" border="0" alt="{$str.logo}" />
					</a>
				</td>
			</tr>

			<tr>
				<td class="darker" colspan="2" />
			</tr>
		</table>

		<br />
	</xsl:template>

	<!-- Common bottom template -->
	<xsl:template name="bottom">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="fronttab">&#160;</td>
			</tr>

			<tr>
				<td class="darker" />
			</tr>

			<tr>
				<td>
					<div align="center" class="footer">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.bottom.banner</xsl:with-param>
							<xsl:with-param name="p0">
								<a href="http://mx4j.sourceforge.net">MX4J</a>
							</xsl:with-param>
						</xsl:call-template>
					</div>
				</td>
			</tr>
		</table>
	</xsl:template>

	<!-- Common tabs template -->
	<xsl:template name="tabs">
		<xsl:param name="selection" select="." />

		<xsl:variable name="server.class">
			<xsl:choose>
				<xsl:when test="$selection='server'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="timer.class">
			<xsl:choose>
				<xsl:when test="$selection='timer'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="mbean.class">
			<xsl:choose>
				<xsl:when test="$selection='mbean'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="monitor.class">
			<xsl:choose>
				<xsl:when test="$selection='monitor'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="relation.class">
			<xsl:choose>
				<xsl:when test="$selection='relation'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="mlet.class">
			<xsl:choose>
				<xsl:when test="$selection='mlet'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="about.class">
			<xsl:choose>
				<xsl:when test="$selection='about'">fronttab</xsl:when>
				<xsl:otherwise>backtab</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<table cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="{$server.class}">
					<xsl:if test="not ($selection='server')">
						<a href="/serverbydomain" class="tabs">
							<xsl:call-template name="str">
								<xsl:with-param name="id">common.tabs.serverview</xsl:with-param>
							</xsl:call-template>
						</a>
					</xsl:if>

					<xsl:if test="$selection='server'">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.serverview</xsl:with-param>
						</xsl:call-template>
					</xsl:if>
				</td>

				<td width="2"/>

				<td class="{$mbean.class}">
					<a href="/empty?template=emptymbean" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.mbeanview</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>

				<td width="2"/>

				<td class="{$timer.class}">
					<a href="/serverbydomain?instanceof=javax.management.timer.Timer&amp;template=timer" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.timerview</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>

				<td width="2"/>

				<td class="{$monitor.class}">
					<a href="/serverbydomain?instanceof=javax.management.monitor.Monitor&amp;template=monitor" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.monitorview</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>

				<td width="2"/>

				<td class="{$relation.class}">
					<a href="/relation?instanceof=javax.management.relation.Relation&amp;template=relation" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.relationview</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>

				<td width="2"/>

				<td class="{$mlet.class}">
					<a href="/serverbydomain?instanceof=javax.management.loading.MLetMBean&amp;template=mlet" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.mletview</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>

				<td width="2"/>

				<td class="{$about.class}">
					<a href="/mbean?objectname=JMImplementation%3Atype%3DMBeanServerDelegate&amp;template=about" class="tabs">
						<xsl:call-template name="str">
							<xsl:with-param name="id">common.tabs.about</xsl:with-param>
						</xsl:call-template>
					</a>
				</td>
			</tr>
		</table>
	</xsl:template>

	<xsl:template name="serverview">
		<tr>
			<td class="darkline" align="right">
				<a href="/">
					<xsl:call-template name="str">
						<xsl:with-param name="id">common.serverview.return</xsl:with-param>
					</xsl:call-template>
				</a>
			</td>
		</tr>
	</xsl:template>

	<xsl:template name="mbeanview">
		<xsl:param name="objectname" />
		<xsl:param name="colspan">1</xsl:param>
		<xsl:param name="text">common.mbeanview.return</xsl:param>

		<tr>
			<td class="darkline" align="right" colspan="{$colspan}">
				<xsl:variable name="objectname-encode">
					<xsl:call-template name="uri-encode">
						<xsl:with-param name="uri" select="$objectname" />
					</xsl:call-template>
				</xsl:variable>

				<a href="/mbean?objectname={$objectname-encode}">
					<xsl:call-template name="str">
						<xsl:with-param name="id"><xsl:value-of select="$text" /></xsl:with-param>
					</xsl:call-template>
				</a>
			</td>
		</tr>
	</xsl:template>

	<xsl:template name="aggregation-navigation">
		<xsl:param name="url"/>
		<xsl:param name="total"/>
		<xsl:param name="step"/>
		<xsl:param name="start"/>
		<xsl:param name="str.prefix">common</xsl:param>

		<xsl:if test="$total&gt;$step">
			<xsl:variable name="isfirst">
				<xsl:choose>
					<xsl:when test='$start=0'>true</xsl:when>
					<xsl:when test='$start&gt;0'>false</xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="islast">
				<xsl:choose>
					<xsl:when test='$total&lt;=($step + $start)'>true</xsl:when>
					<xsl:otherwise>false</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<tr><td/></tr>
			<tr><td>
				<xsl:choose>
					<xsl:when test="$isfirst='false'">
					<a href="{$url}&amp;start=0">
						<xsl:call-template name="str">
							<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.first')"/></xsl:with-param>
						</xsl:call-template>
					</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="str">
								<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.first')"/></xsl:with-param>
							</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
				 -
				<xsl:choose>
					<xsl:when test="$isfirst='false'">
						<xsl:variable name="previndex" select="($start - $step)"/>
						<a href="{$url}&amp;start={$previndex}">
						<xsl:call-template name="str">
							<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.previous')"/></xsl:with-param>
						</xsl:call-template>
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="str">
							<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.previous')"/></xsl:with-param>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
				 -
				<xsl:choose>
					<xsl:when test="$islast='false'">
						<xsl:variable name="nextindex" select="($start + $step)"/>
						<a href="{$url}&amp;start={$nextindex}">
							<xsl:call-template name="str">
								<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.next')"/></xsl:with-param>
							</xsl:call-template>
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="str">
							<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.next')"/></xsl:with-param>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
				 -
				<xsl:choose>
					<xsl:when test="$islast='false'">
						<xsl:variable name="lastindex" select="($total - ($total mod $step))"/>
						<a href="{$url}&amp;start={$lastindex}">
							<xsl:call-template name="str">
								<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.last')"/></xsl:with-param>
							</xsl:call-template>
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="str">
							<xsl:with-param name="id"><xsl:value-of select="concat($str.prefix, '.navigation.last')"/></xsl:with-param>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			</tr>
		</xsl:if>
	</xsl:template>

	<!-- Finds a string in the strings file by id. It can replace two params -->
	<xsl:template name="str">
		<xsl:param name="id"/>
		<xsl:param name="p0"/>
		<xsl:param name="p1"/>

		<xsl:variable name="str" select="$strings//str[@id=$id]" />

		<!-- This is a bit lame, should be improved -->
		<xsl:variable name="temp">
			<xsl:call-template name="replace-param">
				<xsl:with-param name="text" select="$str" />
				<xsl:with-param name="paramText">{0}</xsl:with-param>
				<xsl:with-param name="paramValue">
					<xsl:copy-of select="$p0" />
				</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$p1">
				<xsl:call-template name="replace-param">
					<xsl:with-param name="text" select="$temp" />
					<xsl:with-param name="paramText">{1}</xsl:with-param>
					<xsl:with-param name="paramValue">
						<xsl:copy-of select="$p1" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy-of select="$temp"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Replaces paramText by paramValue in text -->
	<xsl:template name="replace-param">
		<xsl:param name="text"/>
		<xsl:param name="paramText"/>
		<xsl:param name="paramValue"/>

		<xsl:choose>
			<xsl:when test="contains($text, $paramText)">
				<xsl:copy-of select="substring-before($text, $paramText)" />
				<xsl:copy-of select="$paramValue" />
				<xsl:copy-of select="substring-after($text, $paramText)" />
			</xsl:when>

			<xsl:otherwise>
					<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
</xsl:template>
</xsl:stylesheet>


