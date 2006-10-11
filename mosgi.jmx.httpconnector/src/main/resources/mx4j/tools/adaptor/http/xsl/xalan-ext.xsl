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
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0"
	xmlns:encoder="/java.net.URLEncoder"
	xmlns:java="http://xml.apache.org/xslt/java"
	exclude-result-prefixes="java">

	<xsl:template name="uri-encode">
		<xsl:param name="uri"/>
			<xsl:choose>
				<xsl:when test="function-available('java:java.net.URLEncoder.encode')">
					<xsl:value-of select="java:java.net.URLEncoder.encode($uri)"/>
				</xsl:when>
				<xsl:when test="function-available('encoder:encode')">
					<xsl:value-of select="encoder:encode($uri)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:message>No encode function available</xsl:message>
					<xsl:value-of select="$uri"/>
				</xsl:otherwise>
			</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
