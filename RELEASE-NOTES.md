<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

## Apache Karaf 4.4.7

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates, improvements, and fixes.

### ChangeLog

#### Bug
    * [KARAF-7695] - karaf-maven-plugin ArchiveMojo sets unexisting File for Artifact
    * [KARAF-7839] - Karaf client ssh idleTimeout not working
    * [KARAF-7843] - It would be nice if the spifly feature was on a start-level other than the default
    * [KARAF-7858] - Upgrade to CXF 3.6.4
    * [KARAF-7873] - Problem when running under a user without a home directory
    * [KARAF-7898] - Karaf client should be updated to use slf4j2
    * [KARAF-7927] - Missing java.nio.file.spi from etc/jre.properties
    * [KARAF-7929] - bin/client script shows a warning from bin/inc
    * [KARAF-7930] - karaf-maven-plugin integration tests are failing

#### Improvement
    * [KARAF-7859] - Multiple bundles provide javax.inject
    * [KARAF-7871] - Add Java 23 support
    * [KARAF-7882] - Upgrade to Pax URL 2.6.15
    * [KARAF-7892] - Start felix fileinstall before configadmin to avoid race condition
    * [KARAF-7893] - InstanceTest should use random generated instance name
    * [KARAF-7938] - Fix distributions LICENSE and NOTICE

#### Dependency upgrade
    * [KARAF-7831] - Upgrade to easymock 5.3.0
    * [KARAF-7832] - Upgrade to commons-io 2.16.1
    * [KARAF-7834] - Upgrade to commons-compress 1.26.2
    * [KARAF-7838] - Upgrade to xbean 4.25
    * [KARAF-7844] - Upgrade to sshd 2.13.1
    * [KARAF-7846] - Upgrade JAXB 2.3.9
    * [KARAF-7847] - Upgrade to eclipselink 2.7.15
    * [KARAF-7851] - Upgrade to commons-lang3 3.15.0
    * [KARAF-7853] - Cleanup commons-collections dependency
    * [KARAF-7854] - Upgrade to jackson 2.17.2
    * [KARAF-7863] - Upgrade to easymock 5.4.0
    * [KARAF-7864] - Upgrade to commons-io 2.17.0
    * [KARAF-7865] - Upgrade to commons-logging 1.3.4
    * [KARAF-7866] - Upgrade to JNA 5.15.0
    * [KARAF-7867] - Upgrade to commons-compress 1.27.1
    * [KARAF-7868] - Upgrade to commons-lang3 3.17.0
    * [KARAF-7869] - Upgrade to sshd 2.13.2
    * [KARAF-7874] - Upgrade to xbean 4.26
    * [KARAF-7875] - Upgrade to dom4j 2.1.4
    * [KARAF-7876] - Upgrade to sshd 2.14.0
    * [KARAF-7877] - Upgrade to ASM 9.7.1
    * [KARAF-7879] - Upgrade to pax-web 8.0.29
    * [KARAF-7888] - Stepup snakeyaml, undertow, xnio and woodstox to solve CVEs 
    * [KARAF-7890] - Upgrade to commons-io 2.18.0
    * [KARAF-7894] - Upgrade to Spring 6.1.14
    * [KARAF-7895] - Upgrade to Spring Security 5.7.12_3
    * [KARAF-7896] - Upgrade to easymock 5.5.0
    * [KARAF-7897] - Upgrade to log4j 2.24.3
    * [KARAF-7900] - Upgrade to JNA 5.16.0
    * [KARAF-7902] - Cleanup maven-dependency-tree
    * [KARAF-7906] - Upgrade to maven-dependency-plugin 3.8.1
    * [KARAF-7907] - Upgrade to maven-deploy-plugin 3.1.3
    * [KARAF-7908] - Upgrade to maven-enforcer-plugin 3.5.0
    * [KARAF-7909] - Upgrade to maven-gpg-plugin 3.2.7
    * [KARAF-7910] - Upgrade to maven-install-plugin 3.1.3
    * [KARAF-7911] - Upgrade to maven-jar-plugin 3.4.2
    * [KARAF-7912] - Upgrade to maven-javadoc-plugin 3.11.2
    * [KARAF-7913] - Upgrade to maven-jxr-plugin 3.6.0
    * [KARAF-7914] - Upgrade to maven-project-info-reports-plugin 3.8.0
    * [KARAF-7915] - Upgrade to maven-release-plugin 3.1.1
    * [KARAF-7916] - Upgrade to maven-surefire-plugin 3.5.2
    * [KARAF-7917] - Upgrade to apache-rat-plugin 0.16.1
    * [KARAF-7918] - Upgrade to build-helper-maven-plugin 3.6.0
    * [KARAF-7919] - Upgrade to exec-maven-plugin 3.5.0
    * [KARAF-7920] - Upgrade to modello-maven-plugin 2.4.0
    * [KARAF-7921] - Upgrade to maven-invoker-plugin 3.9.0
    * [KARAF-7922] - Upgrade to jacoco-maven-plugin 0.8.12
    * [KARAF-7923] - Upgrade to maven-scm-publish-plugin 3.3.0
    * [KARAF-7924] - Upgrade to asciidoctor-maven-plugin 3.1.1
    * [KARAF-7925] - Upgrade to maven-archetype-plugin 3.3.1
    * [KARAF-7926] - Upgrade to Jackson 2.18.2
    * [KARAF-7933] - Upgrade to woodstox-core 6.4.0
    * [KARAF-7934] - Upgrade to Pax Web 8.0.30
    * [KARAF-7935] - Upgrade to Pax Logging 2.2.8
    * [KARAF-7936] - Upgrade to Pax URL 2.6.16

#### Dependency
    * [KARAF-7852] - Upgrade to commons-pool2 2.12.0

#### Documentation
    * [KARAF-7850] - karaf-services-maven-plugin requirement for Commands is not documented

## Apache Karaf 4.4.6

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates, improvements, and fixes.

### ChangeLog

#### Bug
    * [KARAF-7682] - Karaf does not stop when stop command issued
    * [KARAF-7691] - Status check has SocketException
    * [KARAF-7795] - Custom InfoProvider missing with system:info command output
    * [KARAF-7802] - javax.annotation.security is missing from etc/jre.properties
    * [KARAF-7804] - Cannot register more than one HTTP proxy

#### New Feature
    * [KARAF-7747] - Provide Spring 6.1.x/6.0.x features

#### Improvement
    * [KARAF-7799] - Allow merging org.apache.karaf.features.xml definitions

#### Dependency upgrade
    * [KARAF-7800] - Upgrade asciidoctor-maven-plugin to 2.2.5
    * [KARAF-7801] - Upgrade to sshd 2.12.1
    * [KARAF-7808] - Stepup Jetty and pax-web to solve CVE-2024-22201
    * [KARAF-7809] - Bouncycastle 1.76+ is needed for sshd-osgi
    * [KARAF-7810] - Upgrade to commons-logging 1.3.1
    * [KARAF-7811] - Upgrade to commons-compress 1.26.1
    * [KARAF-7812] - Upgrade to Maven Wagon 3.5.3
    * [KARAF-7813] - Upgrade to ASM 9.7
    * [KARAF-7814] - Upgrade to Pax Web 8.0.27 / Jetty 9.4.54.v20240208
    * [KARAF-7815] - Upgrade to Pax Logging 2.2.7/slf4j 2.0.12/log4j 2.23.1
    * [KARAF-7816] - Upgrade to Pax JDBC 1.5.7
    * [KARAF-7817] - Upgrade Spring Security features to 5.7.12/5.5.3/5.1.6
    * [KARAF-7819] - Upgrade to Spring 5.2.25_1 feature
    * [KARAF-7820] - Upgrade feature to Spring 5.3.33
    * [KARAF-7822] - Upgrade to Apache POM 31

## Apache Karaf 4.4.5

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates and fixes.

### ChangeLog

#### Bug
    * [KARAF-6210] - NoClassDefFoundError: org/apache/karaf/shell/api/action/Action when starting Karaf
    * [KARAF-6606] - felix.fileinstall.filename property is not available via ConfigMBean
    * [KARAF-7753] - Key authentication doesn't work with ecdsa keys
    * [KARAF-7769] - Karaf webconsole plugins don't work since Felix WebConsole update
    * [KARAF-7773] - WAB ServletContextListener.contextInitialized invoked multiple times during re-deploy (and with wrong context)
    * [KARAF-7775] - Installing shell feature causes org.apache.karaf.config.core refresh
    * [KARAF-7779] - Problem installing feature with fragment bundle for existing host bundle

#### Improvement
    * [KARAF-7751] - fix additional Reproducible Builds issues

#### Dependency upgrade
    * [KARAF-7727] - Upgrade to org.osgi.util.promise 1.3.0
    * [KARAF-7755] - Upgrade maven-compiler-plugin to 3.11.0
    * [KARAF-7757] - Upgrade maven-enforcer-plugin to 3.4.1
    * [KARAF-7758] - Upgrade maven-surefire-plugin to 3.1.2
    * [KARAF-7759] - Upgrade maven-remote-resources-plugin to 3.1.0
    * [KARAF-7760] - Upgrade maven-source-plugin to 3.3.0
    * [KARAF-7761] - Upgrade maven-resources-plugin to 3.3.1
    * [KARAF-7762] - Upgrade build-helper-maven-plugin to 3.4.0
    * [KARAF-7771] - Upgrade to ASM 9.6
    * [KARAF-7772] - Upgrade to sshd 2.11.0
    * [KARAF-7781] - Upgrade to maven-plugin-annotations 3.10.2
    * [KARAF-7782] - Upgrade to commons-io 2.15.1
    * [KARAF-7783] - Upgrade to commons-logging 1.3.0
    * [KARAF-7784] - Upgrade to JNA 5.14.0
    * [KARAF-7785] - Upgrade to Aries Proxy 1.1.14
    * [KARAF-7786] - Upgrade to Aries SpiFly 1.3.7
    * [KARAF-7787] - Upgrade to commons-compress 1.25.0
    * [KARAF-7788] - Upgrade to commons-lang3 3.14.0
    * [KARAF-7789] - Upgrade to xbean 4.24
    * [KARAF-7790] - Upgrade to jansi 2.4.1
    * [KARAF-7791] - Upgrade to Pax Web 8.0.24 / Jetty 9.4.53.v20231009
    * [KARAF-7793] - Upgrade to PAX Logging 2.2.6

## Apache Karaf 4.4.4

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates and fixes.

### ChangeLog

#### Bug
    * [KARAF-6074] - Race condition between the FeaturesService and FeatureDeploymentListener
    * [KARAF-7669] - Fix --patch-module on Instance startup
    * [KARAF-7678] - ShellTable erroneously clips out text from multi-line column contents
    * [KARAF-7690] - jdk.net package is not exposed
    * [KARAF-7710] - Fix CVE-2023-33201 in BouncyCastle
    * [KARAF-7711] - Fix CVE-2023-24998 in FileUpload
    * [KARAF-7712] - Fix CVE-2023-33008 in Johnzon

#### Improvement
    * [KARAF-5421] - Better error while installing non OSGi bundles
    * [KARAF-6538] - Add exec:groovy command
    * [KARAF-7637] - remove timestamp from generated service metadata
    * [KARAF-7677] - Log the offending bundle if a bundle refresh occurs during assembly
    * [KARAF-7708] - Test and example of JSON configuration
    * [KARAF-7733] - Upgrade to pax-jms 1.1.3
    * [KARAF-7736] - Service PID for a JSON configuration
    * [KARAF-7746] - Improve JVM version check in the shell script

#### Task
    * [KARAF-7681] - Add JDK20 packages in etc/jre.properties
    * [KARAF-7696] - Add support for JDK 20

#### Dependency upgrade
    * [KARAF-7693] - Upgrade to Spring 5.3.29
    * [KARAF-7698] - Upgrade SSHD to 2.10.0
    * [KARAF-7699] - Upgrade commons-io to 2.13.0
    * [KARAF-7701] - Upgrade XBean to 4.23
    * [KARAF-7702] - Upgrade commons-compress to 1.23.0
    * [KARAF-7709] - Upgrade to org.eclipse.equinox.region 1.5.300
    * [KARAF-7714] - Upgrade to Pax Logging 2.2.3
    * [KARAF-7715] - Upgrade to easymock 5.2.0
    * [KARAF-7716] - Upgrade to JNA 5.13.0
    * [KARAF-7717] - Upgrade to Felix Framework Security 2.8.4
    * [KARAF-7719] - Upgrade to Felix SCR 2.2.6
    * [KARAF-7720] - Upgrade to Felix WebConsole 4.8.10
    * [KARAF-7724] - Upgrade to ASM 9.5
    * [KARAF-7725] - Upgrade to org.osgi.service.component* 1.5.1
    * [KARAF-7726] - Upgrade to org.osgi.service.jdbc 1.1.0, Pax Transx 0.5.4, Pax JDBC 1.5.6
    * [KARAF-7727] - Upgrade to org.osgi.util.promise 1.3.0
    * [KARAF-7729] - Upgrade to Aries SpiFly 1.3.6
    * [KARAF-7730] - Upgrade to maven-bundle-plugin 5.1.9
    * [KARAF-7731] - Upgrade to Felix ConfigAdmin plugin interpolation 1.2.8
    * [KARAF-7740] - Upgrade pax-url to 2.6.14
    * [KARAF-7743] - Upgrade to commons-lang 3.13.0
    * [KARAF-7744] - Upgrade to org.eclipse.equinox.coordinator 1.5.200
    * [KARAF-7745] - Upgrade to Pax Web 8.0.22 & Jetty 9.4.52.v20230823
    * [KARAF-7748] - Upgrade to JAXB 2.3.8 and use jaxb-bom

#### Documentation
    * [KARAF-7670] - Some example READMEs use old versions 

## Apache Karaf 4.4.3

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates and important fixes.

### ChangeLog

#### Bug
    * [KARAF-6697] - karaf-maven-plugin verify goal leaks threads
    * [KARAF-7443] - JMX: rmiRegistryHost=0.0.0.0 Runtime IO exception: no such object in table
    * [KARAF-7583] - shell:alias command not found in Apache Karaf 4.4.1
    * [KARAF-7605] - Karaf BoM causes import of org.osgi.service.log to have lower bound 1.5.0 but only version 1.4.0 is available
    * [KARAF-7607] - bin/client fails with SSHD IllegalArgumentException
    * [KARAF-7608] - Override config option by environment variable not possible
    * [KARAF-7610] - JMX: rmiRegistryHost=0.0.0.0 not working

#### New Feature
    * [KARAF-1717] - Add markdown format support in the command helper plugin
    * [KARAF-7068] - Add instance:package command

#### Improvement
    * [KARAF-7601] - Remove two default ssh algorithms no longer available

#### Wish
    * [KARAF-7624] - Enable debug port on all interface

#### Dependency upgrade
    * [KARAF-7411] - Upgrade to easymock 5.0.1
    * [KARAF-7455] - Upgrade to Aries JAX-RS whiteboard 2.0.2
    * [KARAF-7599] - Upgrade to jackson 2.14.1
    * [KARAF-7600] - Upgrade to Pax JDBC 1.5.5
    * [KARAF-7609] - Upgrade to sshd 2.9.2
    * [KARAF-7611] - Upgrade to commons-compress 1.22
    * [KARAF-7613] - Upgrade to Felix ConfigAdmin 1.9.26
    * [KARAF-7615] - Upgrade to Felix WebConsole DS plugin 2.2.0
    * [KARAF-7616] - Upgrade to maven-dependency-tree 3.2.1
    * [KARAF-7617] - Upgrade to xbean 4.22
    * [KARAF-7619] - Upgrade to narayana 5.13.1.Final
    * [KARAF-7620] - Upgrade to Apache POM 28
    * [KARAF-7621] - Upgrade to eclipselink 2.7.11
    * [KARAF-7622] - Upgrade to Pax Logging 2.2.0
    * [KARAF-7625] - Upgrade to CXF 3.5.5
    * [KARAF-7628] - Upgrade to Apache POM 29
    * [KARAF-7630] - Upgrade to Pax Web 8.0.15

#### Documentation
    * [KARAF-7626] - Websocket example references the command http:list instead of web:servlet-list

## Apache Karaf 4.4.2

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates and important fixes and new features.

### ChangeLog

#### Sub-task
    * [KARAF-7554] - Add feature:status command
    * [KARAF-7567] - Add GraphQL example

#### Bug
    * [KARAF-6769] - censor parameter does not work when using alias.
    * [KARAF-7362] - Override of shellPort by environment variable sometime failed during clean start
    * [KARAF-7522] - Duplicate feature name and version cause stack overflow errors
    * [KARAF-7535] - Mis-aligned artifacts for el-api
    * [KARAF-7538] - Assembly Mojo parameter translatedUrls is unusable
    * [KARAF-7550] - MBeanInvocationHandler should throw the cause of InvocationTargetException, not ITE itself
    * [KARAF-7556] - pax-jdbc has mismatched pax-transx feature version
    * [KARAF-7580] - StringIndexOutOfBoundsException in ShellTable

#### New Feature
    * [KARAF-1381] - Use a history file per instance when possible
    * [KARAF-7551] - Extend KarService.install() with noAutoRefreshBundles as Parameter
    * [KARAF-7571] - Create feature for Spring Security 5.7.3

#### Improvement
    * [KARAF-4542] - Add an option to feature:list to list features provided by a given repository
    * [KARAF-5194] - Add feature status to feature:version-list command
    * [KARAF-6110] - Add an option to shutdown Karaf if a boot features fails or repositories not found
    * [KARAF-6319] - Add <stageBootFeatures/> allowing to define the early stage boot features
    * [KARAF-6321] - Be able to "control/enable/disable" CTRL-D and logout in the shell
    * [KARAF-7423] - Add logback configuration option
    * [KARAF-7546] - Add jdk19 section in jre.properties
    * [KARAF-7552] - Improving test coverage for feature commands
    * [KARAF-7568] - Verify scheme in JAAS JDBCUtils
    * [KARAF-7574] - Upgrade to jakarta.el-api 3.0.3

#### Wish
    * [KARAF-7537] - Password displayed in console using repo-list

#### Task
    * [KARAF-7553] - Add additional dependencies to karaf-bom

#### Dependency upgrade
    * [KARAF-7223] - Upgrade maven artifacts to mitigate CVE-2021-26291
    * [KARAF-7523] - Upgrade to jackson 2.13.3
    * [KARAF-7524] - Upgrade to maven-bundle-plugin 5.1.8
    * [KARAF-7525] - Upgrade to Spring 5.3.22
    * [KARAF-7527] - Upgrade to JNA 5.12.1
    * [KARAF-7528] - Upgrade to sshd 2.9.0
    * [KARAF-7529] - Upgrade to maven-javadoc-plugin 3.3.2
    * [KARAF-7530] - Upgrade to maven-project-info-reports-plugin 3.4.0
    * [KARAF-7531] - Upgrade to maven-remote-resources-plugin 3.0.0
    * [KARAF-7532] - Upgrade to exec-maven-plugin 3.1.0
    * [KARAF-7539] - Upgrade to Felix eventadmin 1.6.4
    * [KARAF-7540] - Upgrade to maven-dependency-tree 3.2.0
    * [KARAF-7542] - Upgrade to narayana 5.13.0.Final
    * [KARAF-7557] - Upgrade to Aries SpiFly 1.3.5
    * [KARAF-7558] - Upgrade to Felix ConfigAdmin plugin interpolation 1.2.6
    * [KARAF-7559] - Upgrade to Felix configurator 1.0.16
    * [KARAF-7560] - Upgrade to Felix inventory 1.1.0
    * [KARAF-7561] - Upgrade to Felix WebConsole 4.8.4
    * [KARAF-7562] - Upgrade to sshd 2.9.1
    * [KARAF-7564] - Upgrade to ASM 9.4
    * [KARAF-7566] - Upgrade to Pax Web 8.0.11
    * [KARAF-7569] - Upgrade to Spring 5.3.23
    * [KARAF-7570] - Upgrade to Spring Security 5.6.3
    * [KARAF-7572] - Upgrade to Felix SCR 2.2.4
    * [KARAF-7573] - Upgrade to Aries Proxy 1.1.13
    * [KARAF-7575] - Upgrade to Pax transx 0.5.3
    * [KARAF-7576] - Upgrade to Pax JDBC 1.5.4
    * [KARAF-7578] - Upgrade to maven resolver 1.8.2
    * [KARAF-7579] - Upgrade to Pax URL 2.6.12
    * [KARAF-7581] - Upgrade to commons-pool2 2.11.1
    * [KARAF-7582] - Upgrade to Pax JMS 1.1.1
    * [KARAF-7584] - Upgrade to Felix Http 4.2.2
    * [KARAF-7586] - Upgrade to Pax JMS 1.1.2
    * [KARAF-7589] - Upgrade to maven-assembly-plugin 3.4.2
    * [KARAF-7590] - Upgrade to maven-deploy-plugin 3.0.0
    * [KARAF-7591] - Upgrade to maven-javadoc-plugin 3.4.1
    * [KARAF-7592] - Upgrade to maven-jar-plugin 3.3.0
    * [KARAF-7593] - Upgrade to maven-install-plugin 3.0.1
    * [KARAF-7594] - Upgrade to maven-resources-plugin 3.3.0
    * [KARAF-7595] - Upgrade to maven-antrun-plugin 3.1.0
    * [KARAF-7596] - Upgrade to maven-site-plugin 3.12.1
    * [KARAF-7597] - Upgrade to maven-jxr-plugin 3.3.0
    * [KARAF-7598] - Upgrade to maven-project-info-reports-plugin 3.4.1

## Apache Karaf 4.4.1

This is a maintenance release on the 4.4.x series. It includes a bunch of dependency updates and important fixes especially about config installation embedded in features.

### ChangeLog

#### Bug
    * [KARAF-7389] - Problem installing features with embedded config
    * [KARAF-7431] - Missing package exports for java.net.http and others
    * [KARAF-7444] - java.lang.runtime export is missing
    * [KARAF-7453] - Race condition in threaded ConnectorServerFactory
    * [KARAF-7502] - not possible to install camel-jetty

#### Improvement
    * [KARAF-7451] - Rename config json file extension to cfg.json by default

#### Task
    * [KARAF-6703] - Spec features and cleanup
    * [KARAF-7427] - pax-war pulls in pax-url-war version different from karaf base
    * [KARAF-7432] - Remove unsupported jre.properties

#### Dependency upgrade
    * [KARAF-7434] - Upgrade to maven-bundle-plugin 5.1.6
    * [KARAF-7440] - Upgrade to Felix ConfigAdmin plugin interpolation 1.2.4
    * [KARAF-7441] - Upgrade to Felix Framework 7.0.4
    * [KARAF-7442] - Upgrade to Felix WebConsole 4.8.2
    * [KARAF-7445] - Upgrade to Felix Framework 7.0.5
    * [KARAF-7446] - Upgrade to Spring 5.3.19
    * [KARAF-7447] - Add env variable/system property to change json extension handled by the json config installer
    * [KARAF-7449] - Upgrade to Pax Web 8.0.4
    * [KARAF-7452] - Upgrade to jackson 2.13.3
    * [KARAF-7456] - Upgrade to Felix SCR 2.2.2
    * [KARAF-7457] - Upgrade to maven 3.8.6
    * [KARAF-7458] - Upgrade to maven-dependency-tree 3.1.1
    * [KARAF-7459] - Upgrade to maven wagon 3.5.1
    * [KARAF-7460] - Upgrade to ServiceMix jaxb-runtime 2.3.2_3
    * [KARAF-7461] - Upgrade to ServiceMix jaxb-xjc 2.3.2_2
    * [KARAF-7462] - Upgrade to ServiceMix Spec jaxws-api-2.3 2.3_3
    * [KARAF-7463] - Upgrade to equinox 3.18.0
    * [KARAF-7464] - Upgrade to narayana 5.12.7.Final
    * [KARAF-7465] - Upgrade to geronimo-json_1.1_spec 1.5
    * [KARAF-7467] - Upgrade to org.osgi.util.converter 1.0.9
    * [KARAF-7468] - Upgrade to bouncycastle 1.70
    * [KARAF-7470] - Upgrade to javax.jms-api 2.0.1 and ActiveMQ 5.17.1
    * [KARAF-7472] - Upgrade to bndlib 6.3.1
    * [KARAF-7473] - Upgrade to maven-plugin-annotations 3.6.4
    * [KARAF-7475] - Upgrade to maven-filtering 3.3.0
    * [KARAF-7477] - Upgrade to plexus-utils 3.4.2
    * [KARAF-7478] - Upgrade to xmlunit 1.6
    * [KARAF-7479] - Upgrade to disruptor 1.2.20
    * [KARAF-7480] - Upgrade to org.osgi.service.cdi 1.0.1
    * [KARAF-7483] - Upgrade to maven-assembly-plugin 3.3.0
    * [KARAF-7484] - Upgrade to maven-dependency-plugin 3.3.0
    * [KARAF-7485] - Upgrade to maven-enforcer-plugin 3.1.0
    * [KARAF-7486] - Upgrade to maven-gpg-plugin 3.0.1
    * [KARAF-7487] - Upgrade to maven-jar-plugin 3.2.2
    * [KARAF-7488] - Upgrade to maven-javadoc-plugin 3.3.1
    * [KARAF-7489] - Upgrade to maven-jxr-plugin 3.2.0
    * [KARAF-7490] - Upgrade to maven-project-info-reports-plugin 3.2.2
    * [KARAF-7491] - Upgrade to maven-resources-plugin 3.2.0
    * [KARAF-7492] - Upgrade to maven-site-plugin 3.11.0
    * [KARAF-7493] - Upgrade to maven-war-plugin 3.3.2
    * [KARAF-7494] - Upgrade to build-helper-maven-plugin 3.3.0
    * [KARAF-7495] - Upgrade to exec-maven-plugin 3.0.0
    * [KARAF-7496] - Upgrade to maven-compiler-plugin 3.10.1
    * [KARAF-7497] - Upgrade to modello-maven-plugin 2.0.0
    * [KARAF-7500] - Upgrade stax2-api to 4.2.1
    * [KARAF-7501] - Update woodstox to 6.2.8
    * [KARAF-7503] - Upgrade to Pax Web 8.0.5
    * [KARAF-7504] - Upgrade to Pax Web 8.0.6
    * [KARAF-7507] - Upgrade to Pax URL 2.6.11
    * [KARAF-7509] - Upgrade to maven-assembly-plugin 3.4.0
    * [KARAF-7510] - Upgrade to maven wagon 3.5.2
    * [KARAF-7511] - Upgrade to Pax Logging 2.1.3
    * [KARAF-7513] - Upgrade to Spring 5.3.21
    * [KARAF-7514] - Upgrade to Spring 5.2.22.RELEASE

## Apache Karaf 4.4.0

Apache Karaf 4.4.0 is a major new milestone. It brings:
- OSGi R8
- Pax Web 8.x
- Pax Logging 2.1.x
- JDK 11+
- and much more!

### ChangeLog

#### Bug
    * [KARAF-7248] - java.time.format export is missing
    * [KARAF-7255] - KARAF JsonInstaller throw error on Null dictionary configuration
    * [KARAF-7259] - Karaf Server can't start if space in path (KARAF_HOME)
    * [KARAF-7271] - Upgrade to org.ops4j.pax.url 2.6.8 (BND tools 5.2.0) leads to big memory overhead
    * [KARAF-7276] - LinkageError when receiving SOAP message with attachments
    * [KARAF-7282] - global autoRefresh logic does not work as expected
    * [KARAF-7288] - Karaf client often hangs when executing multiple operations from the command line
    * [KARAF-7295] - CVE-2021-45046 update
    * [KARAF-7306] - Hot deployment (deploy directory) does not work for provisioned blueprints
    * [KARAF-7316] - Fallback distribution in verify mojo always fail to resolve
    * [KARAF-7317] - Support spaces in boot features path
    * [KARAF-7326] - Fix potential partial path traversal
    * [KARAF-7389] - Problem installing features with embedded config
    * [KARAF-7393] - Karaf server SSH connection not happing via ssh.net dll
    * [KARAF-7401] - Java 17 is not supported is minimum javase version
    * [KARAF-7404] - NPE in JaasHelper

#### New Feature
    * [KARAF-7070] - Add kill command to kill a running thread
    * [KARAF-7367] - support Reproducible Builds project.build.outputTimestamp

#### Improvement
    * [KARAF-7278] - Speedup calculating distances on Feature Resource digraph
    * [KARAF-7289] - Provide config property name completion using MetaType information
    * [KARAF-7290] - Set autoRefresh true by default in etc/org.apache.karaf.features.cfg file
    * [KARAF-7304] - Add cleanall supports in Karaf Main
    * [KARAF-7312] - Add support for JMX RMI credentials filter pattern
    * [KARAF-7363] - Allow restriction of signature algorithms in SSH server config

#### Task
    * [KARAF-7266] - JDK 18 support
    * [KARAF-7365] - Upgrade sling-commons-johnzon to 1.2.14
    * [KARAF-7406] - Document Pax Web 8 changes in Karaf manual

#### Dependency upgrade
    * [KARAF-6913] - Upgrade to jansi 2.4.0
    * [KARAF-7051] - Upgrade to sshd 2.8.0
    * [KARAF-7143] - Upgrade to jline 3.21.0
    * [KARAF-7172] - Upgrade to Hibernate 5.6.7.Final
    * [KARAF-7198] - Upgrade to Spring Security 5.5.2
    * [KARAF-7249] - Use individual api/impl bundles instead of cmpn
    * [KARAF-7262] - Upgrade to Pax Logging 2.0.11
    * [KARAF-7265] - Upgrade to Felix FileInstall 3.7.2
    * [KARAF-7269] - Upgrade to Pax Exam 4.13.5
    * [KARAF-7270] - Upgrade to Pax Web 7.3.23
    * [KARAF-7272] - Upgrade to commons-pool2 2.9.0
    * [KARAF-7273] - Upgrade to OpenJPA 3.2.2
    * [KARAF-7274] - Upgrade to javax.annotation-api 1.3.2
    * [KARAF-7275] - Upgrade to Spring 5.3.12
    * [KARAF-7279] - Upgrade to Pax URL 2.6.8
    * [KARAF-7281] - Upgrade to org.osgi.util.function 1.2.0 and org.osgi.util.promise 1.2.0
    * [KARAF-7283] - Upgrade to Pax URL 2.6.10
    * [KARAF-7284] - Upgrade to Pax Swissbox 1.8.5
    * [KARAF-7285] - Upgrade to bndlib 6.1.0
    * [KARAF-7293] - Upgrade to Pax Logging 2.0.12
    * [KARAF-7294] - Upgrade to maven-bundle-plugin 5.1.3
    * [KARAF-7296] - Upgrade Jolokia to 1.7.1
    * [KARAF-7300] - Upgrade to Pax Logging 2.0.13
    * [KARAF-7309] - Upgrade to Pax Logging 2.0.14
    * [KARAF-7313] - Upgrade to Aries Proxy Impl 1.1.12
    * [KARAF-7315] - Upgrade to Felix FileInstall 3.7.4
    * [KARAF-7318] - Upgrade to Spring 5.3.14
    * [KARAF-7319] - Upgrade to Spring Security 5.6.1
    * [KARAF-7320] - Upgrade to maven-bundle-plugin 5.1.4
    * [KARAF-7323] - Upgrade to xbean 4.21
    * [KARAF-7324] - Upgrade to Aries SpiFly 1.3.4
    * [KARAF-7368] - Upgrade to Pax Logging 2.1.2
    * [KARAF-7372] - Upgrade to JNA 5.10.0
    * [KARAF-7373] - Upgrade to Felix ConfigAdmin plugin interpolation 1.2.2
    * [KARAF-7374] - Upgrade to Felix Framework 7.0.3
    * [KARAF-7375] - Upgrade to Felix Framework Security 2.8.3
    * [KARAF-7376] - Upgrade to Felix Http 4.2.0
    * [KARAF-7378] - Upgrade to Felix WebConsole 4.7.2
    * [KARAF-7380] - Upgrade to narayana 5.12.5.Final
    * [KARAF-7382] - Upgrade to org.osgi.service.cm 1.6.1
    * [KARAF-7383] - Upgrade to org.osgi.service.component* 1.5.0
    * [KARAF-7384] - Upgrade to org.osgi.service.event 1.4.1
    * [KARAF-7385] - Upgrade to org.osgi.service.http 1.2.2
    * [KARAF-7386] - Upgrade to org.osgi.service.jdbc 1.0.1
    * [KARAF-7387] - Upgrade to org.osgi.service.log 1.5.0
    * [KARAF-7388] - Upgrade to org.osgi.service.metatype 1.4.1
    * [KARAF-7392] - Upgrade to Pax Web 8.0.2
    * [KARAF-7394] - Upgrade to equinox 3.17.100
    * [KARAF-7395] - Upgrade to Apache POM 25
    * [KARAF-7405] - Upgrade to Felix SCR 2.2.0
    * [KARAF-7407] - Upgrade to equinox 3.17.200
    * [KARAF-7408] - Upgrade to Pax JDBC 1.5.3
    * [KARAF-7410] - Upgrade to Spring 5.2.20.RELEASE
    * [KARAF-7413] - Upgrade to jackson 2.13.2
    * [KARAF-7414] - Upgrade to Jetty 9.4.46.v20220331
    * [KARAF-7415] - Upgrade to Hibernate Validator 7.0.2.Final
    * [KARAF-7416] - Upgrade to ASM 9.3
    * [KARAF-7417] - Upgrade to JNA 5.11.0
    * [KARAF-7418] - Upgrade to Felix ConfigAdmin 1.9.24
    * [KARAF-7419] - Upgrade to Felix Gogo runtime 1.1.6
    * [KARAF-7420] - Upgrade to sling commons johnzon 1.2.14
    * [KARAF-7421] - Upgrade to narayana 5.12.6.Final

## Apache Karaf 4.3.3

Apache Karaf 4.3.3 is a maintenance release on 4.3.x series. It contains major fixes and updates, we encourage users to update to this version.

### ChangeLog

#### Bug
    * [KARAF-6877] - Itest doesn't see alias command
    * [KARAF-7148] - Full start/build with JDK17
    * [KARAF-7152] - Narayana unable to create object store due to ClassNotFoundException on javax.security.cert.X509Certificate
    * [KARAF-7153] - Narayana object store fails to start due to FELIX-6416
    * [KARAF-7157] - Editing a factory config lacking a FILEINSTALL_FILE_NAME prop. produces a file with incorrect filename
    * [KARAF-7164] - Pax-Exam failure to Start Karaf container Java > 8
    * [KARAF-7165] - config:meta doesn't work for factory PIDs
    * [KARAF-7176] - java.* packages export incomplete
    * [KARAF-7190] - SSH session not properly closed by Karaf
    * [KARAF-7231] - High memory consumption in BluePrintServiceState
    * [KARAF-7234] - JMX - exceptions are ignored

#### Improvement
    * [KARAF-7106] - Add OSGi/Aries Transaction Control Service bundles to Karaf transaction feature
    * [KARAF-7146] - Clean argument should not remove log folder by default
    * [KARAF-7181] - Add NOTICE file in src distribution
    * [KARAF-7182] - Upgrade LICENSE file mentioning 3rd party software
    * [KARAF-7183] - Avoid binary files in source release artifact
    * [KARAF-7186] - Optionally avoid to fail the verify goal on bundle uninstall/update
    * [KARAF-7219] - Document improved password encryption algorithms

#### Task
    * [KARAF-7225] - ensure karaf can build and run with JDK17

#### Dependency upgrade
    * [KARAF-7158] - Upgrade to Felix ConfigAdmin plugin interpolation 1.1.4
    * [KARAF-7159] - Upgrade to Felix FileInstall 3.7.0
    * [KARAF-7160] - Upgrade to xbean 4.20
    * [KARAF-7161] - Upgrade to Pax Web 7.3.17  / Jetty 9.4.41.v20210516
    * [KARAF-7167] - Upgrade to commons-io 2.10.0
    * [KARAF-7171] - Upgrade to hibernate 5.4.32.Final
    * [KARAF-7173] - Upgrade to openjpa 3.2.0
    * [KARAF-7179] - Upgrade to Pax Web 7.3.18 / Jetty 9.4.42.v20210604
    * [KARAF-7188] - Upgrade to equinox 3.16.300
    * [KARAF-7191] - Upgrade to Felix Http Jetty 4.1.10
    * [KARAF-7192] - Upgrade to Felix Resolver 2.0.4
    * [KARAF-7193] - Upgrade to Felix SCR 2.1.28
    * [KARAF-7194] - Upgrade to Felix WebConsole 4.6.2
    * [KARAF-7195] - Upgrade to narayana 5.12.0.Final
    * [KARAF-7196] - Upgrade to ASM 9.2
    * [KARAF-7197] - Upgrade to Spring 5.3.8
    * [KARAF-7218] - Upgrade to Felix Framework 6.0.5
    * [KARAF-7220] - Upgrade commons-compress to 1.21
    * [KARAF-7222] - Upgrade commons io to 2.11.0
    * [KARAF-7228] - Upgrade to Aries Transaction Blueprint 2.3.0
    * [KARAF-7229] - Upgrade to Aries JPA 2.7.3
    * [KARAF-7230] - Upgrade to Aries Proxy 1.1.11
    * [KARAF-7232] - Upgrade to Spring 5.3.9
    * [KARAF-7233] - Upgrade to Apache POM 24
    * [KARAF-7235] - Upgrade to Pax Web 7.3.19
    * [KARAF-7237] - Upgrade sjf4j to 1.7.32
    * [KARAF-7238] - Upgrade jolokia to 1.7.0
    * [KARAF-7240] - Upgrade bcprov 1.68 artifacts to mitigate CVE-2020-28052
    * [KARAF-7242] - Upgrade to Pax Logging 2.0.10
    * [KARAF-7243] - Upgrade to bouncycastle 1.69
    * [KARAF-7244] - Upgrade to jackson 2.12.5

## Apache Karaf 4.3.2

Apache Karaf 4.3.2 is a maintenance release on 4.3.x series.

### ChangeLog

#### Bug
    * [KARAF-5362] - NPE creating session with a null "in" parameter from a SessionFactory
    * [KARAF-6696] - importing a regular war file containing a module-info.class file in an embedded jar causes a NullPointerException
    * [KARAF-6849] - ShellTable no format rendering is not the same on Windows and Unix
    * [KARAF-6964] - Running bin/shell wrapper:install bug
    * [KARAF-7075] - Enable/disable log:tail command
    * [KARAF-7086] - disable default user karaf in etc/user.properties by default to make the karaf installation more secure
    * [KARAF-7090] - pax-logging default pattern is not fully correct
    * [KARAF-7091] - Missing eecap-16 in config.properties
    * [KARAF-7093] - Console should use karaf.history instead of karaf41.history file name
    * [KARAF-7095] - JDK11+: we should use full lib path when patch-module
    * [KARAF-7096] - When rmiServerHost is 127.0.0.1, RMIServerImpl_Stub still uses hostname's IP
    * [KARAF-7097] - JsonConfigInstaller is continuously updating configurations
    * [KARAF-7098] - JsonConfigInstaller ignores R7 factory configurations
    * [KARAF-7101] - maven cmd: NPE guard for the case that there's no ~/.m2/settings.xml
    * [KARAF-7113] - Scheduler should deal with array service pid
    * [KARAF-7114] - Cannot logon webconsole
    * [KARAF-7129] - Fix race condition in org.apache.karaf.shell.ssh.Activator

#### Improvement
    * [KARAF-7094] - Karaf should provide etc/org.apache.karaf.features.xml by default (even empty)
    * [KARAF-7099] - Provide util for Configuration PIDs
    * [KARAF-7131] - etc/host.key is readable by anyone
    * [KARAF-7132] - Use maven properties in distributionManagement repositories and scm
    * [KARAF-7133] - Allow karaf commands to have return codes

#### Dependency upgrade
    * [KARAF-7087] - Upgrade to Jetty 9.4.39.v20210325
    * [KARAF-7088] - Upgrade to jackson 2.11.4
    * [KARAF-7092] - Upgrade aries-proxy to 1.1.10
    * [KARAF-7103] - Upgrade to xbean 4.19
    * [KARAF-7116] - Upgrade to Pax Web 7.3.14
    * [KARAF-7119] - Upgrade to Jetty 9.4.40.v20210413
    * [KARAF-7120] - Upgrade to Pax Web 7.3.15
    * [KARAF-7122] - Upgrade to Pax Logging 2.0.9
    * [KARAF-7123] - Upgrade to Spring 5.3.5
    * [KARAF-7124] - Upgrade to Spring Security 5.4.5
    * [KARAF-7125] - Upgrade to Spring Security 5.3.3
    * [KARAF-7135] - Upgrade to Pax Web 7.3.16
    * [KARAF-7139] - Upgrade to Spring 5.3.6
    * [KARAF-7140] - Upgrade to easymock 4.3
    * [KARAF-7141] - Upgrade to Felix Http Jetty 4.1.8
    * [KARAF-7142] - Upgrade to narayana 5.11.2.Final
    * [KARAF-7144] - Upgrade to Felix ConfigAdmin 1.9.22

## Apache Karaf 4.3.1

Apache Karaf 4.3.1 is a maintenance release on 4.3.x series.

### ChangeLog

#### Bug
    * [KARAF-6654] - Remote JMX connection not working with security manager
    * [KARAF-6772] - Removing JAASLoginService entry in jetty.xml causes an error (workaround for camel-servlet basic auth) in Karaf 4.2.9
    * [KARAF-6845] - Cannot read resource files with hash sign in the name with Felix Framework
    * [KARAF-6878] - Fix WARNING: package org.apache.karaf.specs.locator not in java.base
    * [KARAF-6895] - ssh-commands not properly return
    * [KARAF-6897] - .kar generated by karaf-maven-plugin does not contain conditional bundles (no offline support)
    * [KARAF-6918] - Karaf server SSH connection not happing via ssh.net dll
    * [KARAF-6923] - Avoid potential XML entity injection in MavenConfigService
    * [KARAF-6924] - Configurations created via web console are always persisted as JSON
    * [KARAF-6948] - Session does not close when using client with command
    * [KARAF-6949] - HTTP proxy service can throw NullPointerException
    * [KARAF-6955] - JMX: With rmiRegistryHost = 127.0.0.1, Karaf should listen only on 127.0.0.1
    * [KARAF-6963] - Stopping Karaf daemon invokes kill -9, resulting in failed systemd service
    * [KARAF-6980] - client/ssh session is not closed anymore until the idleTimeout
    * [KARAF-6988] - Error installing json-flattener with wrap command
    * [KARAF-6989] - Update wrap and classpath pax url features name in the distribution assembly
    * [KARAF-6992] - karaf-maven-plugin does not work with decanter boot feature
    * [KARAF-6998] - Karaf 4.3 Configurations with array property values calls @modified every 2 seconds
    * [KARAF-7008] - client ignores env-variable ORG_APACHE_KARAF_SSH_SSHPORT
    * [KARAF-7012] - Bundles don't resolve due to java.* package imports
    * [KARAF-7030] - Karaf client does not exit/return when directly executing command
    * [KARAF-7032] - JTA specification/package versions and exports are still not perfect
    * [KARAF-7071] - KARAF-6887 Dropped support for JAVA_MAX_MEM
    * [KARAF-7074] - Resolver parallelism can fail on kubernetes as the resolverParallelism is not accurate

#### New Feature
    * [KARAF-6479] - Add features repository JSON format
    * [KARAF-6904] - Add Pax URL Classpath
    * [KARAF-6953] - Globally Prevent AutoRefresh Cascade on Feature Install

#### Improvement
    * [KARAF-6888] - Sort out JMXMP authentication
    * [KARAF-6917] - Remove -Dcom.sun.management.jmxremote by default
    * [KARAF-6925] - Support stronger JAAS Encryption algorithms via spring-security-crypto
    * [KARAF-6951] - Add regex support in features selection in add-features-to-repo goal
    * [KARAF-6956] - Upgrade to Spring 5.2.9.RELEASE_2
    * [KARAF-6957] - Add Spring 5.3.1 support
    * [KARAF-6958] - client.bat - Message "Failed to get the session" shown only for 1ms
    * [KARAF-7061] - Add default message escaping for Log4J2 configuration to help prevent log injection attacks
    * [KARAF-7073] - Upgrade to commons-lang 3.12.0
    * [KARAF-7077] - Add "verbose" option to bundle:find-class command

#### Wish
    * [KARAF-6434] - Add CDI examples

#### Task
    * [KARAF-6894] - Create jetty alias feature
    * [KARAF-7013] - Add OSGi resolvers in Camel example

#### Dependency upgrade
    * [KARAF-6870] - Upgrade to Felix Framework 6.0.4
    * [KARAF-6901] - Upgrade example to use CXF 3.4.0 and Camel 3.6.0
    * [KARAF-6912] - Upgrade to jline 3.17.1
    * [KARAF-6914] - Upgrade to Pax Web 7.3.10
    * [KARAF-6922] - Upgrade to Jetty 9.4.35.v20201120
    * [KARAF-6960] - Upgrade to Pax Web 7.3.11
    * [KARAF-6961] - Upgrade to org.osgi.util.promise 1.1.1
    * [KARAF-6965] - Upgrade to Hibernate 5.4.26.Final
    * [KARAF-6966] - Upgrade to Hibernate Validator 6.1.7.Final
    * [KARAF-6970] - Upgrade to Felix EventAdmin 1.6.0
    * [KARAF-6971] - Upgrade to Felix WebConsole 4.6.0
    * [KARAF-6972] - Upgrade to Felix Utils 1.11.6
    * [KARAF-6973] - Upgrade to Felix Http 4.1.4
    * [KARAF-6974] - Upgrade to Felix Gogo Jline 1.1.8
    * [KARAF-6976] - Upgrade to Felix Resolver 2.0.2
    * [KARAF-6977] - Remove ant bundle from bom
    * [KARAF-6978] - Upgrade to equinox 3.16.100
    * [KARAF-6979] - Upgrade to jline 3.18.0
    * [KARAF-6993] - bndlib cleanup and upgrade to bndlib 5.2.0
    * [KARAF-6994] - Upgrade to Pax Swissbox 1.8.4
    * [KARAF-6995] - Upgrade to Pax URL 2.6.7
    * [KARAF-7009] - Upgrade to Pax Logging 2.0.8
    * [KARAF-7017] - Upgrade to Pax Web 7.3.12
    * [KARAF-7031] - Upgrade to Jetty 9.4.36.v20210114
    * [KARAF-7033] - Upgrade to commons-codec 1.15
    * [KARAF-7034] - Upgrade to Pax CDI 1.1.4
    * [KARAF-7036] - Upgrade to Aries Proxy 1.1.9
    * [KARAF-7037] - Upgrade to Aries Blueprint Core 1.10.3 and Blueprint CM 1.3.2
    * [KARAF-7039] - Upgrade to jackson-databind 2.10.5.1
    * [KARAF-7041] - Upgrade to ASM 9.1
    * [KARAF-7043] - Upgrade to junit 4.13.2
    * [KARAF-7044] - Upgrade to JNA 5.7.0
    * [KARAF-7045] - Upgrade to Felix CM JSON 1.0.6
    * [KARAF-7046] - Upgrade to Felix ConfigAdmin 1.9.20
    * [KARAF-7047] - Upgrade to Felix ConfigAdmin plugin interpolation 1.1.2
    * [KARAF-7048] - Upgrade to Felix EventAdmin 1.6.2
    * [KARAF-7049] - Upgrade to Felix Metatype 1.2.4
    * [KARAF-7050] - Upgrade to Felix SCR 2.1.26
    * [KARAF-7052] - Upgrade to jline 3.19.0
    * [KARAF-7053] - Upgrade to Jetty 9.4.38.v20210224
    * [KARAF-7054] - Upgrade to Pax Web 7.3.13
    * [KARAF-7057] - Update Pax Transx, Pax JMS and Pax JDBC
    * [KARAF-7058] - Upgrade to Hibernate 5.4.29.Final
    * [KARAF-7059] - Upgrade to Hibernate Validator 7.0.1.Final
    * [KARAF-7060] - Upgrade to eclipselink 2.7.8
    * [KARAF-7062] - Upgrade to Spring 4.3.30.RELEASE
    * [KARAF-7063] - Upgrade to Spring 5.2.13.RELEASE
    * [KARAF-7064] - Upgrade to Spring 5.3.4
    * [KARAF-7067] - Provide Spring Security 5.4.2_1 feature
    * [KARAF-7078] - Upgrade to maven-bundle-plugin 5.1.2
    * [KARAF-7079] - Upgrade to JNA 5.8.0
    * [KARAF-7080] - Upgrade to Felix Configurator 1.0.14
    * [KARAF-7081] - Upgrade to Felix Http Jetty 4.1.6
    * [KARAF-7082] - Upgrade to narayana 5.11.0.Final
    * [KARAF-7083] - Upgrade to equinox 3.16.200
    * [KARAF-7085] - Upgrade to Felix Utils 1.11.8

## Apache Karaf 4.3.0

Apache Karaf 4.3.0 is the first release on the 4.3.x series. This release is JDK 11+ compliant, OSGi R7
compliant and brings bunch of fixes and new features.

### ChangeLog

#### Sub-task
    * [KARAF-6591] - Upgrade to Pax Logging 2.0.2
    * [KARAF-6595] - Upgrade to Pax CDI 1.1.3

#### Bug
    * [KARAF-5628] - Corrupt gc.log due to unseparated VM settings
    * [KARAF-5753] - Karaf won't start correctly on HP-UX
    * [KARAF-5990] - Blacklisted dependent features should be skipped during assembly generation
    * [KARAF-6024] - Blacklisted dependent repositories should be skipped during assembly generation
    * [KARAF-6039] - maven-resources-plugin in same pom twice
    * [KARAF-6085] - karaf-maven-plugin verify mojo builds invalid repository URL on Windows
    * [KARAF-6119] - karaf-maven-plugin assembly goal doesn't handle locked snapshots
    * [KARAF-6143] - Subshell and first complete modes don't work
    * [KARAF-6144] - Some bundle:* commands doesn't output on System.out
    * [KARAF-6153] - Instance fails to start
    * [KARAF-6156] - Support extending Karaf JAAS Principal classes
    * [KARAF-6160] - NPE when setting configCfgStore=false in the org.apache.karaf.features.cfg
    * [KARAF-6170] - karaf-maven-plugin doesn't support "https" remoteRepos URI accembly
    * [KARAF-6171] - batch command line mode does not work
    * [KARAF-6186] - Downgrade to wagon 3.2.0
    * [KARAF-6189] - Don't overwrite JAVA_HOME on gentoo system
    * [KARAF-6197] - Dot at the end of PATH_WITH_JAVA in karaf-wrapper.conf break PATH variable
    * [KARAF-6198] - Spaces in the path stored in KARAF_ETC will prohibit service start-up
    * [KARAF-6199] - Upgrade Hibernate feature to work with Java 11
    * [KARAF-6201] - Karaf staticcm should export org.osgi.service.cm package with version 1.6
    * [KARAF-6202] - Update to osgi specification new coordinates
    * [KARAF-6205] - bin\client.bat only passes 8 or 9 arguments to java
    * [KARAF-6207] - bootFeatures sometimes being ignored
    * [KARAF-6208] - Fix scr:* commands
    * [KARAF-6224] - Race condition in BaseActivator on first launch
    * [KARAF-6226] - config core: ConfigRepository requires access to TypedProperties
    * [KARAF-6229] - karaf-maven-plugin (>= 4.2.2) deploy/install zip twice 
    * [KARAF-6232] - the karaf-maven-plugin features-add-to-repository goal creates fixed snapshot versions for bundles referenced via -SNAPSHOT version in a feature descriptor
    * [KARAF-6235] - Date.getTime() can be changed to System.currentTimeMillis()
    * [KARAF-6237] - Karaf Scheduler reschedule failed because the function lost the job reference (After reschedule job is null)
    * [KARAF-6238] - org.apache.karaf.profile.assembly.Builder#downloadLibraries is not synchronized
    * [KARAF-6239] - Duplicated Resource have been added to the Set<Resource> bundlesInRegion during compute
    * [KARAF-6252] - NPE when trying to remove repo
    * [KARAF-6253] - Typo in documentation
    * [KARAF-6254] - Karaf shell scripts don't work on Solaris 10
    * [KARAF-6256] - Parsing string arrays in org.apache.karaf.management.cfg doesn't ignore spaces
    * [KARAF-6257] - client.bat no longer working on Windows due to KARAF-6205
    * [KARAF-6258] - Do not print error for user interrupted script
    * [KARAF-6259] - SCR feature fails to install when using Equinox framework
    * [KARAF-6270] - client could be more verbose on config error
    * [KARAF-6274] - Karaf 4.2.5 maven plugin breaks if no archives are generated
    * [KARAF-6276] - Bundle update results in leaking update*.jar files
    * [KARAF-6279] - Upgrade to Pax Web 7.2.10
    * [KARAF-6290] - Karaf Wrapper does not properly manage process on Solaris
    * [KARAF-6299] - JDK11: don't expose javax.activation package from system bundle 0
    * [KARAF-6325] - Jetty client issue?
    * [KARAF-6326] - instance:start does not work under jdk-11
    * [KARAF-6329] - NPE on shutdown
    * [KARAF-6337] - ConcurrentModificationException when executing commands
    * [KARAF-6341] - Karaf does not start if TERM=linux
    * [KARAF-6344] - AsyncLogger fails due to bug in pax-logging
    * [KARAF-6351] - The classes command fails with package-less classes
    * [KARAF-6356] - Using instance commands corrupts org.apache.karaf.shell.cfg
    * [KARAF-6357] - client sh/bat no longer working in case of specify encryption.enabled = true
    * [KARAF-6358] - jre.properties exports javax.annotation packages as 1.0 vs 1.3
    * [KARAF-6359] - Clients can log in with encrypted passwords
    * [KARAF-6361] - jre.properties lists CORBA packages for jre-11
    * [KARAF-6362] - [karaf-maven-plugin] client goal:only the fist command in script file could be executed
    * [KARAF-6363] - ConfigRepository.update changes configuration file location
    * [KARAF-6365] - KARAF_LOG fails if directory doesn't exist
    * [KARAF-6369] - Upgrade to pax-logging 1.11.0
    * [KARAF-6382] - Upgrade to pax-jdbc 1.4.0, pax-jms 1.0.5 and pax-transx 0.4.4
    * [KARAF-6385] - WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
    * [KARAF-6386] - Race condition in initialization of Activators (Port already in use: 1099)
    * [KARAF-6393] - Make sure extracted data from OBR source stays in destination directory
    * [KARAF-6410] - FeatureProcessor selects wrong override bundle
    * [KARAF-6413] - Windows Service fails to start initially
    * [KARAF-6417] - AutoEncryptionSupport has hardcoded users.properties
    * [KARAF-6444] - karaf-maven-plugin:client goal doesn't work due to missing setter
    * [KARAF-6445] - [karaf-maven-plugin] client goal: ensure commands in script file will be executed in expected order
    * [KARAF-6449] - karaf-service.sh does not work until executed from its folder
    * [KARAF-6450] - Upgrade to Jackson 2.9.10
    * [KARAF-6451] - Upgrade to Jackson 2.10.0
    * [KARAF-6456] - KarafTestSupport should check if etc/org.ops4j.pax.logging.cfg resource exists
    * [KARAF-6457] - KarafTestSupport should not "force" the version variables
    * [KARAF-6462] - Unresolvable dependency to org.knopflerfish.kf6:log-API:jar:5.0.0 using karaf-maven-plugin:
    * [KARAF-6472] - Blacklisted features may be processed wrong with different blacklist ranges
    * [KARAF-6476] - ClassLoader and Memory leak
    * [KARAF-6480] - Permgen JVM options still being used on Windows
    * [KARAF-6498] - StaticCM doesn't work with SCR
    * [KARAF-6501] - Restoring the wiring of fragment bundles with multiple hosts
    * [KARAF-6505] - Unable to override bundle's dependency attribute by specifying it in the source feature.xml descriptor
    * [KARAF-6510] - Change wrap URL handler bundle start level to 10
    * [KARAF-6517] - LDAPLoginModule + SSL + connection timeout problem
    * [KARAF-6519] - Config MBean update operation should "really" update (not just add properties)
    * [KARAF-6523] - Cleanly destroy the CXF server in the REST/SOAP examples
    * [KARAF-6525] - bin/shell.bat|sh could not find org.osgi.framework.FrameworkUtil
    * [KARAF-6535] - in bin/client script JAVA_OPTS are never populated to default values
    * [KARAF-6542] - Refreshing sshd cause the ssh service to be unavailable
    * [KARAF-6543] - Upgrade jline to 3.13.2 (Bug: Cannot run program "infocmp": CreateProcess error=2)
    * [KARAF-6593] - Assembly bundles unused/unneeded slf4j-api 
    * [KARAF-6596] - Bad line ending in karaf.bat affecting Windows startup with JDK9+
    * [KARAF-6597] - MutiPartInputStreamParser usage causes CNFE
    * [KARAF-6598] - Upgrade to CXF 3.3.5
    * [KARAF-6600] - Change default Maven repository to use https
    * [KARAF-6602] - History shell command is broken
    * [KARAF-6604] - Remove dependency opendmk_jmxremote_optional_jar dependency
    * [KARAF-6613] - Paste doesn't work in ssh terminal
    * [KARAF-6614] - man command leads to java.lang.reflect.InvocationTargetException
    * [KARAF-6624] - Error starting on JDK 13 & JDK 14
    * [KARAF-6649] - Documentation uses wrong markup in section 5.9.1. and 5.9.2.
    * [KARAF-6650] - Error parsing the bundle.info file, if 'h' is last character in line
    * [KARAF-6715] - Wrong exports of javax.transaction package from jre.properties
    * [KARAF-6716] -   Change in KAR installation behaviour
    * [KARAF-6731] - Align hibernate bundle version with feature version and upgrade to hibernate 5.4.17.Final
    * [KARAF-6763] - Disallow calling getMBeansFromURL
    * [KARAF-6764] - Compilation with OpenJDK 11 failed because of Unit tests with JPM
    * [KARAF-6776] - scheduler doesn't work in featuresBoot
    * [KARAF-6784] - Karaf docker images not stopped correctly
    * [KARAF-6822] - “NoSuchMethodErrors” due to multiple versions of org.codehaus.plexus:plexus-utils:jar
    * [KARAF-6836] - Restart issue with subsystem runtime
    * [KARAF-6852] - RmiServerPort (44444) does not rise at startup
    * [KARAF-6879] - Fix log:display/log:tail commands

#### New Feature
    * [KARAF-2925] - Add JMXMP support
    * [KARAF-6182] - Add override property to <config/> in feature
    * [KARAF-6236] - Add karaf:dockerfile & karaf:docker Maven goals
    * [KARAF-6247] - Add web:install command
    * [KARAF-6277] - Upgrade to felix-http 4.0.8
    * [KARAF-6289] - Add spring-messaging feature
    * [KARAF-6378] - Add OSGi R7 Configurator (Felix) bundle in config feature
    * [KARAF-6418] - Add flag to mark transitive dependencies with dependency="true"
    * [KARAF-6574] - Upgrade pax-logging to 1.11.4
    * [KARAF-6611] - Add gitpod.yml for better onboarding experience
    * [KARAF-6676] - Support OSGi R7 factory configurations with factory PID and name
    * [KARAF-6680] - Support OSGi R7 JSON configuration format

#### Improvement
    * [KARAF-2894] - Add option to feature:uninstall to cleanup feature configs/configfiles
    * [KARAF-3467] - Create a BOM (Bill of Material) for Karaf
    * [KARAF-4609] - Be able to override ConfigAdmin properties with System/JVM properties
    * [KARAF-5772] - HTTP proxy should be able to support several addresses with balancing policies
    * [KARAF-6157] - ensure karaf-maven-plugin can honor start-level for bootBundles
    * [KARAF-6159] - Allow to override/blacklist some features for VerifyMojo
    * [KARAF-6167] - Add skip option to karaf-maven-plugin
    * [KARAF-6177] - improve shell:exit help message
    * [KARAF-6183] - FeaturesProcessorImpl improvement for bundle override
    * [KARAF-6209] - Add SortedProperties class to karaf tooling
    * [KARAF-6220] - add principal info to audit logs
    * [KARAF-6222] - add MAX_CONCURRENT_SESSIONS option to Karaf ssh server
    * [KARAF-6230] - Prevent relative path in config install command and ConfigMBean
    * [KARAF-6234] - Handle null reference in MetaServiceCaller.withMetaTypeService()
    * [KARAF-6241] - introduce new property EnabledProtocals for org.apache.karaf.management.cfg
    * [KARAF-6245] - Update war example README.md
    * [KARAF-6296] - Upgrade to Jasypt 1.9.3
    * [KARAF-6301] - Please log remote socket address/port in ShutdownSocketThread 
    * [KARAF-6323] - Add jetty-proxy bundle in http/jetty feature
    * [KARAF-6340] - Add filter attribute on command @Reference annotation
    * [KARAF-6346] - Support FATAL level for log console commands
    * [KARAF-6350] - Add support for elliptic keys in the PublicKeyLoginModule
    * [KARAF-6353] - Sanitize ShutdownSocketThread command log
    * [KARAF-6370] - StaticInstallSupport should log update/uninstall bundle when throwing UnsupportedOperationException
    * [KARAF-6381] - Update Commons BeanUtils
    * [KARAF-6383] - Replace not-yet-commons-ssl with BouncyCastle
    * [KARAF-6384] - Add support for encrypted key password for SSH
    * [KARAF-6388] - Update default encryption algorithm to SHA-256
    * [KARAF-6402] - ConfigMBean should reflect config:edit command for factory
    * [KARAF-6420] - Disable JAAS deployer by default
    * [KARAF-6421] - Trim JAAS config values before they are used
    * [KARAF-6428] - bin/status should display only "Running ..." or "Not Running ..."
    * [KARAF-6431] - Maven plugin feature to define custom bundle location prefixes
    * [KARAF-6447] - Added more information to the thrown exception for malformed url.
    * [KARAF-6452] - Add a CXF (SOAP/REST) example with SCR
    * [KARAF-6458] - Add option to ignore PartialResultExceptions from Active Directory
    * [KARAF-6570] - Be able to override the Karaf distribution in KarafTestSupport
    * [KARAF-6579] - Improve the jaas command to be able to add a new realm
    * [KARAF-6580] - Provide Spring Security 5.3.x feature
    * [KARAF-6594] - introduce new property enabledCipherSuites for org.apache.karaf.management.cfg
    * [KARAF-6634] - Prevent JMX rebinding
    * [KARAF-6660] - Update to Commons Lang 3.10
    * [KARAF-6672] - Update to Commons Compress 1.20
    * [KARAF-6675] - Upgrade Apache RAT plugin to 0.13
    * [KARAF-6698] - VerifyMojo performance improvement
    * [KARAF-6722] - Spring cleaning — remove unsupported Spring versions
    * [KARAF-6737] - Disallow calling getMBeansFromURL
    * [KARAF-6795] - Add feature:upgrade alias
    * [KARAF-6812] - example name depth really long, causes problems on win64
    * [KARAF-6816] - Add activation-api bundle in the static-framework feature
    * [KARAF-6856] - Make webconsole feature independent of Pax Web
    * [KARAF-6869] - Upgrade to Pax Logging 2.0.6
    * [KARAF-6887] - Remove setting JAVA_MAX_MEM defaults in bin/inc 

#### Task
    * [KARAF-6154] - Add a example with direct features generate, custom distro and docker image all in a row
    * [KARAF-6155] - Add an itest to check startup using equinox
    * [KARAF-6219] - Add MDC/Sift example (commented) in provided etc/org.ops4j.pax.logging.cfg
    * [KARAF-6298] - master can't build with JDK11 again
    * [KARAF-6327] - Build using JDK 12
    * [KARAF-6331] - Move Spring 5.0.x features in spring-legacy features repository
    * [KARAF-6333] - Add Karaf jaas based authentication example
    * [KARAF-6544] - Use atomic CXF features in examples
    * [KARAF-6575] - Upgrade Aries JAX-RS Whiteboard to 1.0.6
    * [KARAF-6826] - update to aries.spi 1.2.4
    * [KARAF-6828] - Ensure karaf:run (maven goal) can find a pre-built artifact
    * [KARAF-6829] - JDK15 support
    * [KARAF-6832] - Upgrade to Aries Proxy 1.1.8
    * [KARAF-6838] - bin\start.bat  doesn't work if no data directory in Karaf kit
    * [KARAF-6847] - Update examples to be R7 focus (annotations, ...)
    * [KARAF-6855] - JMX over SSL doesn't work anymore

#### Dependency upgrade
    * [KARAF-5770] - Support OSGi R7
    * [KARAF-5820] - Upgrade to Felix Resolver 2.0.0
    * [KARAF-5823] - Upgrade to sshd 2.5.1
    * [KARAF-5825] - Upgrade to equinox 3.15.100
    * [KARAF-5949] - Upgrade to Felix Framework 6.0.3
    * [KARAF-5951] - Upgrade to Felix Resolver 2.0.0
    * [KARAF-5957] - Upgrade to equinox 3.13.200
    * [KARAF-6188] - Upgrade to Felix ConfigAdmin 1.9.14
    * [KARAF-6190] - Upgrade to Spring 5.1.5.RELEASE
    * [KARAF-6191] - Upgrade to Spring 4.3.22.RELEASE
    * [KARAF-6192] - Upgrade to Aries JPA 2.7.2
    * [KARAF-6193] - Upgrade to Felix SCR 2.1.16
    * [KARAF-6194] - Upgrade to awaitility 3.1.6
    * [KARAF-6195] - Upgrade to ASM 7.1
    * [KARAF-6196] - Upgrade to jline 3.10.0
    * [KARAF-6212] - Upgrade to hibernate-validator 6.0.16.Final
    * [KARAF-6213] - Upgrade to pax-jdbc 1.3.5 / pax-transx 0.4.3
    * [KARAF-6214] - Upgrade to Aries Blueprint Core 1.10.2
    * [KARAF-6216] - Upgrade to equinox 3.13.300
    * [KARAF-6217] - Upgrade to narayana 5.9.4.Final
    * [KARAF-6218] - Upgrade to Geronimo Connector 3.1.4
    * [KARAF-6228] - Upgrade to Felix maven-bundle-plugin 4.2.0
    * [KARAF-6248] - Upgrade to Quartz 2.3.1
    * [KARAF-6260] - Upgrade to XBean 4.13
    * [KARAF-6261] - Upgrade to Felix WebConsole MemoryUsage plugin 1.0.10
    * [KARAF-6262] - Upgrade to Maven 3.6.1
    * [KARAF-6263] - Upgrade to jansi 1.18
    * [KARAF-6264] - Upgrade to narayana 5.9.5.Final
    * [KARAF-6265] - Upgrade to jline 3.11.0
    * [KARAF-6266] - Upgrade to commons-lang3 3.9
    * [KARAF-6272] - Upgrade to Pax Web 7.2.9 and Jetty 9.4.18
    * [KARAF-6280] - Upgrade to Spring 4.3.23.RELEASE
    * [KARAF-6281] - Upgrade to Spring 5.0.13.RELEASE
    * [KARAF-6282] - Upgrade to Spring 5.1.6.RELEASE
    * [KARAF-6283] - Upgrade to Spring Security 5.1.5.RELEASE
    * [KARAF-6286] - Upgrade to Apache Felix Webconsole 4.3.12
    * [KARAF-6287] - Downgrade to equinox 3.12.100
    * [KARAF-6293] - Upgrade to XBean 4.14
    * [KARAF-6300] - Upgrade to Pax Logging 1.10.2
    * [KARAF-6302] - Upgrade to ActiveMQ 5.15.9
    * [KARAF-6303] - Upgrade to aspectj 1.9.4
    * [KARAF-6304] - Upgrade to ant 1.10.5
    * [KARAF-6305] - Upgrade to cglib 3.2.9
    * [KARAF-6306] - Upgrade to JNA 5.3.1
    * [KARAF-6307] - Upgrade to commons-beanutils 1.9.3
    * [KARAF-6308] - Upgrade to commons-codec 1.12
    * [KARAF-6309] - Upgrade to commons-fileupload 1.4
    * [KARAF-6310] - Upgrade to commons-pool2 2.6.2
    * [KARAF-6311] - Upgrade to jolokia 1.6.1
    * [KARAF-6312] - Upgrade to serp 1.15.1
    * [KARAF-6313] - Upgrade to Pax JMS 1.0.4
    * [KARAF-6314] - Upgrade to bndlib 3.5.0
    * [KARAF-6315] - Upgrade to Spring 4.3.24.RELEASE
    * [KARAF-6316] - Upgrade to Spring 5.0.14.RELEASE
    * [KARAF-6317] - Upgrade to Spring 5.1.7.RELEASE
    * [KARAF-6324] - Upgrade to Pax Web 7.2.11
    * [KARAF-6332] - Upgrade to Spring 5.1.8.RELEASE
    * [KARAF-6335] - Upgrade to Aries Proxy 1.1.5
    * [KARAF-6338] - Upgrade to Aries Spi Fly 1.2.2
    * [KARAF-6339] - Upgrade to javax.annotation-api 1.3.1
    * [KARAF-6347] - Upgrade to Felix ConfigAdmin 1.9.16
    * [KARAF-6348] - Upgrade to Felix HTTP 4.0.10
    * [KARAF-6349] - Upgrade to ant 1.10.6
    * [KARAF-6371] - Upgrade to Jetty 9.4.20.v20190813
    * [KARAF-6372] - Upgrade to jline 3.12.1
    * [KARAF-6373] - Upgrade to CXF 3.3.2
    * [KARAF-6391] - Update Commons Compress
    * [KARAF-6394] - Upgrade to JNA 5.4.0
    * [KARAF-6395] - Upgrade to maven-bundle-plugin 4.2.1
    * [KARAF-6396] - Upgrade to Felix WebConsole 4.3.16
    * [KARAF-6398] - Upgrade to narayana 5.9.7.Final
    * [KARAF-6403] - Upgrade to Pax Logging 1.11.2
    * [KARAF-6406] - Upgrade to Spring 5.1.9.RELEASE
    * [KARAF-6407] - Upgrade to Spring 5.0.15.RELEASE
    * [KARAF-6408] - Upgrade to Spring 4.3.25.RELEASE
    * [KARAF-6409] - Upgrade to ant 1.10.7
    * [KARAF-6411] - Upgrade to commons-codec 1.13
    * [KARAF-6415] - Upgrade to Aries Proxy 1.1.6
    * [KARAF-6423] - Upgrade JAXB to 2.3.3
    * [KARAF-6426] - Upgrade to hibernate-validator 6.0.17.Final
    * [KARAF-6446] - Upgrade to Jetty 9.4.22 and Pax Web 7.2.12
    * [KARAF-6461] - Upgrade to Pax JDBC 1.4.2
    * [KARAF-6464] - Upgrade to Aries SPI Fly 1.2.3
    * [KARAF-6465] - Upgrade to istack-commons-runtime 3.0.9
    * [KARAF-6466] - Upgrade to Felix HTTP Jetty 4.0.14
    * [KARAF-6467] - Upgrade to Maven 3.6.2
    * [KARAF-6468] - Upgrade to narayana 5.9.8.Final
    * [KARAF-6469] - Upgrade to jline 3.13.0
    * [KARAF-6470] - Upgrade to ASM 7.2
    * [KARAF-6471] - Upgrade to Pax Logging 1.11.3
    * [KARAF-6489] - Upgrade to CXF 3.3.4 & Camel 2.24.2
    * [KARAF-6497] - Upgrade to XBean 4.15
    * [KARAF-6499] - Upgrade to narayana 5.10.0.Final
    * [KARAF-6502] - Upgrade to Hibernate 5.2.18.Final
    * [KARAF-6503] - Upgrade to hibernate-validator 6.0.18.Final
    * [KARAF-6504] - Upgrade to Hibernate 5.4.8.Final
    * [KARAF-6506] - Provide Spring 5.2.0.RELEASE features
    * [KARAF-6520] - Upgrade to Jackson 2.10.1
    * [KARAF-6545] - Upgrade to OpenJPA 3.1.1
    * [KARAF-6546] - Upgrade to JNA 5.5.0
    * [KARAF-6547] - Upgrade to Felix Utils 1.11.4
    * [KARAF-6548] - Upgrade to maven 3.6.3
    * [KARAF-6549] - Upgrade to hibernate-validator 6.1.0.Final
    * [KARAF-6550] - Upgrade to narayana 5.10.1.Final
    * [KARAF-6551] - Upgrade to ops4j-base 1.5.1
    * [KARAF-6552] - Upgrade to PAX JDBC 1.4.4
    * [KARAF-6553] - Upgrade to PAX CDI 1.1.2
    * [KARAF-6554] - Upgrade to PAX URL 2.6.2
    * [KARAF-6555] - Upgrade to PAX JMS 1.0.6
    * [KARAF-6556] - Upgrade to istack-commons-runtime 3.0.10
    * [KARAF-6557] - Upgrade to maven-assembly-plugin 3.2.0
    * [KARAF-6558] - Upgrade to maven-compiler-plugin 3.8.1
    * [KARAF-6559] - Upgrade to maven-jar-plugin 3.2.0
    * [KARAF-6560] - Upgrade to maven-javadoc-plugin 3.1.1
    * [KARAF-6561] - Upgrade to maven-jxr-plugin 3.0.0
    * [KARAF-6562] - Upgrade to maven-remote-resources-plugin 1.6.0
    * [KARAF-6563] - Upgrade to maven-site-plugin 3.8.2
    * [KARAF-6564] - Upgrade to maven-source-plugin 3.2.0
    * [KARAF-6565] - Upgrade to maven-surefire-plugin 2.22.2
    * [KARAF-6566] - Upgrade to maven-war-plugin 3.2.3
    * [KARAF-6567] - Upgrade to maven-invoker-plugin 3.2.1
    * [KARAF-6568] - Upgrade to maven-plugin-plugin 3.6.0
    * [KARAF-6569] - Upgrade to maven-archetype-plugin 3.1.2
    * [KARAF-6578] - Upgrade to Pax Web 7.2.13
    * [KARAF-6581] - Upgrade to ActiveMQ 5.15.11
    * [KARAF-6582] - Upgrade to aspectj bundle 1.9.6_1
    * [KARAF-6583] - Upgrade to camel 2.24.3
    * [KARAF-6584] - Upgrade to jackson 2.10.2
    * [KARAF-6585] - Upgrade to eclipselink 2.7.5
    * [KARAF-6586] - Upgrade to jolokia 1.6.2
    * [KARAF-6587] - Upgrade to easymock 4.1
    * [KARAF-6588] - Upgrade to Felix Inventory 1.0.6
    * [KARAF-6589] - Upgrade to Spring 5.2.2.RELEASE
    * [KARAF-6603] - Upgrade to Pax Web 7.2.14
    * [KARAF-6609] - Upgrade to Pax Web 7.2.15 and Jetty 9.4.28.v20200408
    * [KARAF-6622] - Upgrade to PAX Exam 4.13.2
    * [KARAF-6628] - Upgrade to jline 3.14.1
    * [KARAF-6630] - Upgrade to junit 4.13
    * [KARAF-6663] - Upgrade to Felix Gogo jline 1.1.6
    * [KARAF-6664] - Upgrade to Felix HTTP jetty 4.0.16
    * [KARAF-6665] - Upgrade to XBean 4.16
    * [KARAF-6666] - Upgrade to easymock 4.2
    * [KARAF-6667] - Upgrade to equinox 3.15.200
    * [KARAF-6668] - Upgrade to narayana 5.10.4.Final
    * [KARAF-6669] - Upgrade to Pax Exam 4.13.3
    * [KARAF-6670] - Upgrade to ASM 8.0.1
    * [KARAF-6681] - Upgrade to Felix Fileinstall 3.6.6
    * [KARAF-6682] - Upgrade to Felix Http Jetty 4.0.18
    * [KARAF-6683] - Upgrade to Felix SCR 2.1.20
    * [KARAF-6684] - Upgrade to Felix WebConsole 4.5.0
    * [KARAF-6685] - Upgrade to maven-antrun-plugin 3.0.0
    * [KARAF-6686] - Upgrade to maven-dependency-plugin 3.1.2
    * [KARAF-6687] - Upgrade to maven-javadoc-plugin 3.2.0
    * [KARAF-6688] - Upgrade to maven-remote-resources-plugin 1.7.0
    * [KARAF-6689] - Upgrade to maven-site-plugin 3.9.0
    * [KARAF-6690] - Upgrade to maven-source-plugin 3.2.1
    * [KARAF-6691] - Upgrade to build-helper-maven-plugin 3.1.0
    * [KARAF-6692] - Upgrade to modello-maven-plugin 1.11
    * [KARAF-6693] - Upgrade to jacoco-maven-plugin 0.8.5
    * [KARAF-6701] - Upgrade to xbean 4.17
    * [KARAF-6707] - Upgrade to PAX JMS 1.0.7
    * [KARAF-6709] - Upgrade to Apache pom parent 23
    * [KARAF-6713] - Upgrade to Felix WebConsole 4.5.2
    * [KARAF-6719] - Upgrade to jackson 2.10.4
    * [KARAF-6721] - Upgrade to Spring 5.1.14.RELEASE and 5.2.5.RELEASE due to CVE-2020-5398
    * [KARAF-6723] - Upgrade to commons-codec 1.14
    * [KARAF-6724] - Upgrade to hibernate-validator 6.1.5.Final
    * [KARAF-6725] - Upgrade to Hibernate 5.4.15.Final
    * [KARAF-6726] - Upgrade to eclipselink 2.7.7
    * [KARAF-6728] - Upgrade to Aries Proxy API 1.1.1 & Aries Proxy 1.1.7
    * [KARAF-6729] - Upgrade to Pax Web 7.2.16
    * [KARAF-6730] - Upgrade to Hibernate 5.4.16.Final
    * [KARAF-6785] - Upgrade to Pax Web 7.2.18
    * [KARAF-6788] - Upgrade to Felix FileInstall 3.6.8
    * [KARAF-6798] - Upgrade to commons-io 2.7
    * [KARAF-6799] - Upgrade to JNA 5.6.0
    * [KARAF-6800] - Upgrade to commons-lang 3.11
    * [KARAF-6801] - Upgrade to Felix ConfigAdmin 1.9.18
    * [KARAF-6802] - Upgrade to Felix HTTP Jetty 4.0.20
    * [KARAF-6803] - Upgrade to Felix WebConsole 4.5.4
    * [KARAF-6804] - Upgrade to geronimo-atinject_1.0_spec 1.2
    * [KARAF-6805] - Upgrade to Jetty 9.4.30.v20200611
    * [KARAF-6806] - Upgrade to Jackson Databind 2.10.5
    * [KARAF-6809] - Upgrade to Pax Logging 2.0.5
    * [KARAF-6815] - Upgrade to maven-bundle-plugin
    * [KARAF-6817] - Upgrade to Equinox 3.15.300
    * [KARAF-6818] - Upgrade to narayana 5.10.5.Final
    * [KARAF-6819] - Upgrade to jline 3.16.0
    * [KARAF-6825] - Upgrade to bouncycastle 1.66
    * [KARAF-6833] - Upgrade to Felix SCR 2.1.22
    * [KARAF-6841] - Upgrade to ant 1.10.8
    * [KARAF-6842] - Upgrade to Pax Web 7.3.9
    * [KARAF-6843] - Upgrade to SCR 2.1.24
    * [KARAF-6844] - Upgrade to Camel 3.5.0
    * [KARAF-6857] - Upgrade to Spring 5.2.9.RELEASE
    * [KARAF-6859] - Upgrade to Spring 4.3.29.RELEASE
    * [KARAF-6860] - Upgrade to Pax Exam 4.13.4
    * [KARAF-6863] - Upgrade to ASM 9.0
    * [KARAF-6871] - Upgrade to XBean 4.18
    * [KARAF-6875] - Upgrade to Felix Gogo Runtime 1.1.4
    * [KARAF-6876] - Upgrade to Felix Http 4.1.2
    * [KARAF-6880] - Upgrade to commons-io 2.8.0
    * [KARAF-6881] - Upgrade to Equinox 3.16.0
    * [KARAF-6882] - Upgrade to narayana 5.10.6.Final
    * [KARAF-6884] - Upgrade to Hibernate Validator 6.1.6.Final
    * [KARAF-6885] - Upgrade to Hibernate 5.4.22.Final
    * [KARAF-6886] - Upgrade to junit 4.13.1
    * [KARAF-6889] - Upgrade to Aries SpiFly 1.3.2
    * [KARAF-6890] - Upgrade to HTTP Client 4.5.13 (CVE-2020-13956)

#### Documentation
    * [KARAF-6516] - Broken links to examples
    * [KARAF-6652] - Some links in README.md broken
    * [KARAF-6821] - Command jdbc:ds-factories is not documented
    * [KARAF-6850] - Type-o in JDNI vs JNDI in datasource description of JDBCLoginModule


## Apache Karaf 4.3.0.RC1

Apache Karaf 4.3.0.RC1 is the first release candidate on the 4.3.x series. It upgrades to OSGi R7 support.
4.3.0.RC1 is not GA. The purpose is to allow users to test and review the runtime, preparing the 4.3.0 GA
release.

### ChangeLog

As it's a 4.3.0.RC1 and the Release Notes will be updated with the 4.3.0 GA, you can find content of 4.3.0.RC1
on Jira: 

https://issues.apache.org/jira/sr/jira.issueviews:searchrequest-printable/temp/SearchRequest.html?jqlQuery=project+%3D+KARAF+AND+status+in+%28Resolved%2C+Closed%29+AND+fixVersion+%3D+4.3.0+ORDER+BY+priority+DESC%2C+updated+DESC&tempMax=1000

## Apache Karaf 4.2.3

Apache Karaf 4.2.3 is a major update on the 4.2.x series. It brings several fixes, updates and improvements.

### ChangeLog

#### Bug
    * [KARAF-5086] - Java 8 default methods cause IncompatibleClassChangeError in blueprint
    * [KARAF-6005] - Resolve of bundle with version range in a feature only works with ".m2" and not with "system" directory
    * [KARAF-6037] - DB connection and acquire lock to be retried few times before giving up
    * [KARAF-6050] - package org.apache.karaf.specs.locator not in java.base
    * [KARAF-6052] - Don't use cellar to illustreate provisioning commands (feature:*) in documentation
    * [KARAF-6057] - karaf-plugin breaks with maven-install-plugin-2.5.2
    * [KARAF-6058] - Even key based auth enabled, ssh still prompt for password
    * [KARAF-6066] - Karaf Client doesn't respond to some keystrokes on Windows
    * [KARAF-6069] - Distribution pom is not uploaded on Central
    * [KARAF-6074] - Race condition between the FeaturesService and FeatureDeploymentListener
    * [KARAF-6076] - Blueprint loading fails with Saxon or Xalan
    * [KARAF-6077] - Feature verify fails with UnsupportedOperationException on some features
    * [KARAF-6080] - Duplicate configuration randomly created on the first start in ConfigurationAdmin
    * [KARAF-6084] - Startup bundles do not resolve correctly when compiled with Java 11 during assembly
    * [KARAF-6086] - Profile builder should use ${karaf.etc} instead of ${karaf.home}/etc
    * [KARAF-6089] - Deadlock with FeatureDeploymentListener
    * [KARAF-6093] - Error putting an attachment on a SOAP message via CXF due to javax.xml.soap
    * [KARAF-6103] - When doing a "mvn compile" the karaf-maven-plugin causes a IOException
    * [KARAF-6106] - LinkageError due to javax.activation not in kernel anymore
    * [KARAF-6107] - client doesn't find JAVA_HOME on Windows
    * [KARAF-6112] - Table rendering is not correct on client on windows
    * [KARAF-6116] - karaf-maven-plugin adds incorrect snapshot version to repository
    * [KARAF-6120] - Karaf Profiles don't parse on Windows
    * [KARAF-6123] - Karaf scheduler can't be easily configured with a JDBC job store
    * [KARAF-6124] - Instance Creation on Windows fails due to unsupported operation
    * [KARAF-6125] - Audit library creates incorrect dates due to timezones
    * [KARAF-6130] - Documentation points to wrong examples location

#### New Feature
    * [KARAF-5873] - Add spring-security 5.0.7.RELEASE feature

#### Improvement
    * [KARAF-6051] - Add operation to get features contained in a feature repository
    * [KARAF-6062] - CLASSPATH error when using KARAF with JDK11 on windows
    * [KARAF-6090] - kar extract should ignore path containing .. relative path
    * [KARAF-6108] - Service wrapper should add selected JAVA_HOME also to the PATH variable.
    * [KARAF-6109] - Enhance bundle report to reflect effective installed features for custom distributions

#### Test
    * [KARAF-6139] - JmsTest is failing

#### Wish
    * [KARAF-6060] - shell:wc -l should only print line count, without leading spaces

#### Task
    * [KARAF-5901] - Create Aries JAXRS whiteboard example
    * [KARAF-6100] - Align narayana version with pax-* projects

#### Dependency upgrade
    * [KARAF-6071] - Upgrade to pax-transx 0.4.1
    * [KARAF-6079] - Upgrade to pax-url 2.6.1
    * [KARAF-6087] - Upgrade to Aries Proxy Impl 1.1.4
    * [KARAF-6088] - Upgrade to pax-web 7.2.7
    * [KARAF-6091] - upgrade jaxb 2.3.2
    * [KARAF-6094] - Upgrade to itstack-commons-runtime 3.0.8
    * [KARAF-6095] - Upgrade to Aries JPA 2.7.1
    * [KARAF-6096] - Upgrade to Felix Gogo 1.1.4
    * [KARAF-6097] - Upgrade to Maven Wagon 3.3.1
    * [KARAF-6098] - Upgrade to awaitility 3.1.5
    * [KARAF-6099] - Upgrade to Narayana 5.9.2.Final
    * [KARAF-6101] - Release train: pax-cdi 1.1.0, pax-jms 1.0.3, pax-jdbc 1.3.2, pax-transx 0.4.2, pax-web 7.2.7
    * [KARAF-6102] - Align some minor dependencies with pax-* projects
    * [KARAF-6105] - Upgrade to Hibernate 5.2.17.Final
    * [KARAF-6127] - Upgrade to Felix Framework 5.6.12
    * [KARAF-6128] - Upgrade to javax.annotation 1.3
    * [KARAF-6129] - Upgrade to Pax JDBC 1.3.3
    * [KARAF-6131] - Upgrade to Pax JDBC 1.3.4
    * [KARAF-6132] - Upgrade to Pax CDI 1.1.1
    * [KARAF-6133] - Upgrade to Pax Web 7.2.8
    * [KARAF-6134] - Upgrade to Spring 5.1.4.RELEASE
    * [KARAF-6135] - Upgrade to Spring 5.0.12.RELEASE
    * [KARAF-6137] - Upgrade to eclipselink 2.7.4
    * [KARAF-6138] - Upgrade to hibernate-validator 6.0.14.Final

## Apache Karaf 4.2.2

Apache Karaf 4.2.2 is a major update on the 4.2.x series. It brings lot of fixes, updates and improvements
especially about Java 11 support and much more.

### ChangeLog

#### Bug
    * [KARAF-4819] - System Property karaf.clean.all Doesn't Work
    * [KARAF-5469] - Installation of a non blueprint feature triggers the installation of the blueprint compatibility bundle
    * [KARAF-5509] - http-whiteboard resources are not published properly
    * [KARAF-5567] - Trying to access ServerInfo service results in bundle wiring issue
    * [KARAF-5703] - pax jdbc config 1.2.0 fails to register XA Datasource
    * [KARAF-5886] - Improve LD_LIBRARY_PATH loading
    * [KARAF-5887] - NPE when starting karaf using bin/shell
    * [KARAF-5897] - ensure javax.xml.stream api works for both JDK8 and 11
    * [KARAF-5902] - Fix example itests
    * [KARAF-5908] - Service Wrapper fails to start on Windows when JAVA_HOME contains whitespace 
    * [KARAF-5909] - Starting Karaf 4.2.1 won't start as Windows service
    * [KARAF-5911] - Restrict XML entity on provided XMLFactoryInput
    * [KARAF-5912] - Don't need to use jaxb jars for JDK8
    * [KARAF-5916] - ssh terminal rendering is not correct using ssh client
    * [KARAF-5917] - EventLoggerTest is broken for some timezones.
    * [KARAF-5931] - More than one Datrasource Configuration does not work
    * [KARAF-5932] - karaf-maven-plugin doesn't use user-supplied settings.xml
    * [KARAF-5939] - karaf-assembly packaging fails with m-i-p 3.0.0-M1
    * [KARAF-5962] - Regression when installing some features containing fragments 4.2.0 -> 4.2.1
    * [KARAF-5980] - SSH output broken
    * [KARAF-5983] - Default ssh port (in the activator) should be 8101
    * [KARAF-5989] - Error while sending email because of javax.activation
    * [KARAF-5990] - Blacklisted dependent features should be skipped during assembly generation
    * [KARAF-5997] - Build fails with Java11 because exam 4.13-SNAPSHOT unavailable
    * [KARAF-6003] - IllegalStateException: Need active coordination - cannot use JPA and Jasypt
    * [KARAF-6004] - karaf-maven-plugin:assembly results in ArrayIndexOutOfBoundsException
    * [KARAF-6020] - Command feature:info fails showing conditional dependencies
    * [KARAF-6021] - Running karaf-maven-plugin in java 11 fails
    * [KARAF-6024] - Blacklisted dependent repositories should be skipped during assembly generation
    * [KARAF-6027] - `log:get` does not report the right log level for loggers that contain numbers
    * [KARAF-6033] - Command "shell wrapper:install" fails
    * [KARAF-6039] - maven-resources-plugin in same pom twice
    * [KARAF-6041] - Upgrade to Aries Proxy Impl 1.1.3
    * [KARAF-6042] - WebSocketExampleTest is flaky
    * [KARAF-6044] - KARAF_LOG env variable generates stack trace in test cases
    * [KARAF-6048] - Camel Test case prone to endless loop

#### New Feature
    * [KARAF-5789] - Add Felix HTTP feature
    * [KARAF-6001] - Upgrade to Pax Web 7.2.4 / Jetty 9.4.12.v20180830
    * [KARAF-6036] - Add Spring 5.1.3.RELEASE support

#### Improvement
    * [KARAF-4095] - Introduce KARAF_LOG env variable
    * [KARAF-4336] - Add support for ordering of CLI scripts and commands in karaf-maven-plugin
    * [KARAF-5232] - Give meaningful names to threads
    * [KARAF-5263] - org.apache.karaf.shell.cfg et al still reference obsolete 'karaf.admin.role'
    * [KARAF-5906] - Update to Apache Felix Webconsole 4.3.8 and latest jQuery UI 1.12.1
    * [KARAF-5919] - Be able to store Http Proxy list in order to keep configuration after restart
    * [KARAF-5937] - karaf-maven-plugin verify doesn't explain why verification failed
    * [KARAF-5940] - Assembly and feature archetype itest is failing
    * [KARAF-5979] - SSH role types should be configurable
    * [KARAF-5981] - Docker build.sh should support exploded distribution
    * [KARAF-5982] - Add additional running mode in the docker-entrypoint.sh
    * [KARAF-5988] - add javax.annotation api when using JDK11
    * [KARAF-5991] - add jaxb api into Karaf specs
    * [KARAF-5995] - ensure there is a way to quit bin/client "cmd"
    * [KARAF-6014] - Improve jdbc:* commands
    * [KARAF-6022] - Add times support in Karaf scheduler

#### Test
    * [KARAF-5845] - JMXSecurityTest.testJMXSecurityAs[Admin|Manager] is flaky
    * [KARAF-5929] - fix several itests failed with JDK11
    * [KARAF-5933] - use JAXB2.3 with JDK 9/10/11

#### Task
    * [KARAF-5882] - ensure karaf-soap-example can build and run with JDK11
    * [KARAF-5893] - Add Camel examples
    * [KARAF-5913] - Add config management exemple
    * [KARAF-5985] - ensure we can see up-to-the-minute log when using bin/client "log:tail"
    * [KARAF-5987] - don't use java.security.acl classes since they will be removed from java12
    * [KARAF-6011] - Add websocket example
    * [KARAF-6018] - use org.glassfish.jaxb instead of com.sun.xml.bind
    * [KARAF-6023] - upgrade to pax-exam 4.13.0

#### Dependency upgrade
    * [KARAF-5806] - Upgrade to easymock 4.0.1
    * [KARAF-5927] - Upgrade to XBean 4.12
    * [KARAF-5934] - Upgrade to maven-bundle-plugin 4.1.0
    * [KARAF-5935] - Upgrade to maven-invoker-plugin 3.1.0 and maven-plugin-plugin 3.5.2
    * [KARAF-5936] - Upgrade to maven-scm-publish-plugin 3.0.0 and asciidoctor-maven-plugin 1.5.6
    * [KARAF-5941] - Upgrade to JNA 4.5.2
    * [KARAF-5942] - Upgrade to Aries Blueprint Core 1.10.1
    * [KARAF-5943] - Upgrade to Aries Blueprint CM 1.3.1
    * [KARAF-5945] - Upgrade to commons-compress 1.18
    * [KARAF-5946] - Upgrade to commons-lang3 3.8.1
    * [KARAF-5947] - Upgrade to Felix ConfigAdmin 1.9.6
    * [KARAF-5948] - Upgrade to Felix Framework Security 2.6.1
    * [KARAF-5950] - Upgrade to Felix Metatype 1.2.2
    * [KARAF-5952] - Upgrade to Felix SCR 2.1.8
    * [KARAF-5953] - Upgrade to Felix WebConsole Plugin DS 2.1.0
    * [KARAF-5954] - Upgrade to geronimo-atinject_1.0_spec 1.1
    * [KARAF-5955] - Upgrade to maven-dependency-tree 3.0.1
    * [KARAF-5956] - Upgrade to Maven Wagon 3.2.0
    * [KARAF-5958] - Upgrade to hibernate-validator 6.0.13.Final
    * [KARAF-5959] - Upgrade to Ant ServiceMix bundle 1.10.3_1
    * [KARAF-5960] - Upgrade to Apache POM 21
    * [KARAF-5961] - Upgrade to ASM 7.0
    * [KARAF-6015] - Upgrade to Aries SpiFly 1.2
    * [KARAF-6028] - Upgrade to Felix ConfigAdmin 1.9.10
    * [KARAF-6029] - Upgrade to Felix SCR 2.1.14
    * [KARAF-6030] - Upgrade to awaitility 3.1.3
    * [KARAF-6031] - Upgrade to easymock 4.0.2
    * [KARAF-6032] - Upgrade to pax-jms 1.0.2
    * [KARAF-6034] - Upgrade to Spring 4.3.21.RELEASE
    * [KARAF-6035] - Upgrade to Spring 5.0.10.RELEASE
    * [KARAF-6043] - Upgrade to Pax Exam 4.13.1
    * [KARAF-6045] - Upgrade to Pax Web 7.2.5
    * [KARAF-6046] - Upgrade to Felix Utils 1.11.2

#### Documentation
    * [KARAF-5900] - Use the asciidoctor-maven-plugin and custom ASF theme for the manual generation

## Apache Karaf 4.2.1

Apache Karaf 4.2.1 is a major update on the 4.2.x series. It brings bunch of fixes, dependencies updates
and new features, especially:

* new assembly tooling to create Karaf Docker images
* new Docker feature allowing you to manipulate Docker directly from a Karaf instance
* Better Java 9/10/11 support
* new examples directly as part of the Karaf distribution
* improved KarafTestSupport allowing you to easily implement your itests

### ChangeLog

#### Bug
    * [KARAF-4996] - Missing packages in created instances 
    * [KARAF-5422] - Feature Repository with Spaces in Path
    * [KARAF-5683] - Completion is "weird" on Windows 8
    * [KARAF-5689] - Console is broken after Ctrl+C
    * [KARAF-5690] - Add missing jaxb endorsed / osgi classes
    * [KARAF-5692] - Alias not honoured in config:edit --factory --alias
    * [KARAF-5694] - strip url to ensure it's a valid one which could download
    * [KARAF-5695] - Starting Karaf Container 4.2.0 in Ubuntu 17.10 with OpenJDK 9 fails
    * [KARAF-5696] - Java detection is broken on windows
    * [KARAF-5697] - feature:start and feature:stop should be able to select multiple features
    * [KARAF-5699] - Upgrade to jolokia 1.5.0
    * [KARAF-5701] - feature installation: Crash and ResolutionException
    * [KARAF-5705] - Java 10 issues with jetty
    * [KARAF-5729] - Karaf won't start on Solaris 11 and AIX 7.2
    * [KARAF-5748] - Command results are not printed anymore unless they are strings
    * [KARAF-5749] - Possible shell crash when executing malformed script
    * [KARAF-5750] - Karaf console not calling Converter for custom gogo commands
    * [KARAF-5753] - Karaf won't start correctly on HP-UX
    * [KARAF-5760] - VerifyMojo should allow blacklisting feature repositories
    * [KARAF-5765] - karaf-service script not working on HP-UX
    * [KARAF-5768] - karaf-service script not working on AIX platforms
    * [KARAF-5781] - Properties edit doesn't conserve the existing ones
    * [KARAF-5791] - need to check the blacklist when we add feature repo through JMX
    * [KARAF-5798] - Karaf slave instance does not write pid or port file until it becomes master
    * [KARAF-5809] - 'simple' host.key files no longer work
    * [KARAF-5840] - Karaf specs activator is missing when used with wrapper
    * [KARAF-5842] - Console unusable in docker
    * [KARAF-5850] - JPA features should provide the engine capability
    * [KARAF-5851] - Remove heading spaces in the cfg files
    * [KARAF-5862] - org.apache.karaf.specs.java.xml doesn't work with IBM JDK

#### New Feature
    * [KARAF-5761] - Print better usage of commands in karaf shell
    * [KARAF-5867] - Provide openjpa 3.0.0 support
    * [KARAF-5870] - Upgrade to Hibernate Validator 6.0.12.Final
    * [KARAF-5871] - Upgrade to ASM 6.2.1
    * [KARAF-5872] - Upgrade to Spring 5.0.8.RELEASE

#### Improvement
    * [KARAF-3235] - Provide karaf itest common bundle
    * [KARAF-5363] - Add --no-start option to kar:install, kar cfg and kar MBean
    * [KARAF-5644] - Add docker feature
    * [KARAF-5685] - Add ProfileMBean
    * [KARAF-5700] - handle \* scope specifically for ACL match
    * [KARAF-5706] - Upgrade to Felix Utils 1.11.0
    * [KARAF-5742] - Possibility to configure colors for karaf shell
    * [KARAF-5752] - Add bundle ID in bundle:classes output
    * [KARAF-5759] - Add an option to config:list to list only the PIDs
    * [KARAF-5778] - NPE in the ssh client if TERM is null
    * [KARAF-5787] - Improve scheduler whiteboard to avoid ClassCastException
    * [KARAF-5796] - Heap dump needs to end in .hprof w/ newer JDK
    * [KARAF-5804] - FastDateFormatTest fails on EDT (jdk 1.8.0_151) 
    * [KARAF-5805] - Add feature required item field to JmxFeature CompositeData
    * [KARAF-5839] - Add assertServiceAvailable() in KarafTestSupport
    * [KARAF-5847] - org.apache.felix.coordinator could be installed with configadmin, to prevent its refreshes
    * [KARAF-5868] - be able to remove properties during distribution assembly

#### Test
    * [KARAF-5845] - JMXSecurityTest.testJMXSecurityAsManager is flaky
    * [KARAF-5846] - ConfigManagedServiceFactoryTest.updateProperties is flacky

#### Task
    * [KARAF-2511] - Review and update documentation
    * [KARAF-5764] - ensure we can build and run Karaf master with JDK11

#### Dependency upgrade
    * [KARAF-5698] - Upgrade to Felix Gogo Runtime / JLine 1.0.12
    * [KARAF-5710] - Upgrade to Felix Resolver 1.16.0
    * [KARAF-5713] - Upgrade to Maven API 3.5.3
    * [KARAF-5714] - Upgrade to ServiceMix Spec Locator 2.10
    * [KARAF-5715] - Upgrade to XBean 4.8
    * [KARAF-5716] - Upgrade to awaitability 3.1.0
    * [KARAF-5717] - Upgrade to easymock 3.6
    * [KARAF-5718] - Upgrade to Equinox 3.12.100
    * [KARAF-5719] - Upgrade to Jansi 1.17.1
    * [KARAF-5720] - Upgrade to JLine 3.7.1
    * [KARAF-5721] - Upgrade to Pax JMS 1.0.1
    * [KARAF-5722] - Upgrade to ASM 6.2 & Aries Proxy Impl 1.1.2
    * [KARAF-5723] - Upgrade to Pax JDBC 1.3.0
    * [KARAF-5726] - Upgrade to Aries Proxy version Java 10 compliant
    * [KARAF-5728] - Upgrade to Pax Web 7.1.1 & Jetty 9.4.10.v20180503
    * [KARAF-5732] - Upgrade to Felix ConfigAdmin 1.9.0
    * [KARAF-5733] - Upgrade to Felix EventAdmin 1.5.0
    * [KARAF-5734] - Upgrade to Felix Metatype 1.2.0
    * [KARAF-5735] - Upgrade to Felix SCR 2.1.0
    * [KARAF-5736] - Upgrade to Narayana 5.8.1.Final
    * [KARAF-5737] - Upgrade to Aries JPA 2.7.0
    * [KARAF-5738] - Upgrade to maven-resources-plugin 3.1.0
    * [KARAF-5745] - Upgrade to Spring 5.0.5.RELEASE
    * [KARAF-5758] - Update to Hibernate Validator 6.0.10.Final
    * [KARAF-5766] - Upgrade to Felix Connect 0.2.0
    * [KARAF-5771] - Upgrade to Pax Transx 0.3.0
    * [KARAF-5779] - Upgrade to Spring 4.3.17.RELEASE and 5.0.6.RELEASE
    * [KARAF-5800] - Upgrade to Felix Gogo 1.1.0
    * [KARAF-5807] - Upgrade to Pax Exam 4.12.0
    * [KARAF-5812] - Upgrade to Spring 4.3.18.RELEASE
    * [KARAF-5813] - Upgrade to Spring 5.0.7.RELEASE
    * [KARAF-5815] - Upgrade to commons-compress 1.17
    * [KARAF-5816] - Upgrade to Aries Transaction Blueprint 2.2.0
    * [KARAF-5817] - Upgrade to maven-bundle-plugin 3.5.1
    * [KARAF-5818] - Upgrade to Felix ConfigAdmin 1.9.2
    * [KARAF-5821] - Upgrade to Maven API 3.5.4
    * [KARAF-5822] - Upgrade to Maven Wagon 3.1.0
    * [KARAF-5824] - Upgrade to awaitility 3.1.1
    * [KARAF-5826] - Upgrade to narayana 5.9.0.Final
    * [KARAF-5827] - Upgrade to jline 3.9.0
    * [KARAF-5829] - Upgrade to Xerces 2.12.0
    * [KARAF-5830] - Upgrade to tagsoup 1.2.1
    * [KARAF-5831] - Upgrade to maven-enforcer-plugin 3.0.0-M2
    * [KARAF-5832] - Upgrade to maven-jar-plugin 3.1.0
    * [KARAF-5833] - Upgrade to maven-project-info-reports-plugin 3.0.0
    * [KARAF-5834] - Upgrade to maven-site-plugin 3.7.1
    * [KARAF-5835] - Upgrade to maven-surefire-plugin 2.22.0
    * [KARAF-5836] - Upgrade to maven-war-plugin 3.2.2
    * [KARAF-5837] - Upgrade to maven-jacoco-plugin 0.8.1
    * [KARAF-5838] - Upgrade to eclipselink 2.7.2
    * [KARAF-5841] - Upgrade to Pax Web 7.2.1
    * [KARAF-5849] - Upgrade to Pax Transx 0.4.0
    * [KARAF-5856] - Upgrade to Pax Web 7.2.2
    * [KARAF-5857] - Upgrade to maven-compiler-plugin 3.8.0
    * [KARAF-5858] - Upgrade to Felix ConfigAdmin 1.9.4
    * [KARAF-5859] - Upgrade to Hibernate Validator 6.0.11.Final
    * [KARAF-5861] - Upgrade to Pax Web 7.2.3 / Jetty 9.4.11.v20180605
    * [KARAF-5865] - Upgrade to eclipselink 2.7.3
    * [KARAF-5866] - Upgrade to Felix SCR 2.1.2
    * [KARAF-5869] - Upgrade to awaitility 3.1.2

## Apache Karaf 4.2.0

Apache Karaf 4.2.0 is the first GA release on the 4.2.x series. We encourage all users to upgrade to this
 new stable series, bringing a lot of fixes, improvements and new features.

### ChangeLog

#### Bug
    * [KARAF-5342] - No reference to branding-ssh.properties in console branding section
    * [KARAF-5384] - Optional dependencies in MINA SSHD Core cause system bundle refreshes
    * [KARAF-5473] - Karaf SSH session timing out
    * [KARAF-5554] - the karaf.secured.command.compulsory.roles shouldn't apply for alias commands
    * [KARAF-5559] - log:tail kills ssh & karaf when root logger is in DEBUG
    * [KARAF-5563] - Enf-of-line display problem with the ShellTable on windows
    * [KARAF-5566] - Features installed through prerequisites lead to errors when uninstalling features
    * [KARAF-5569] - Cannot pass commands to client script when sftpEnabled=false
    * [KARAF-5573] - Karaf on Windows does not pass the version check when JAVA_HOME contains whitespace
    * [KARAF-5581] - bin/client -u karaf -p karaf can login if we enable jasypt for jaas 
    * [KARAF-5585] - Verify mojo configure pax-url-mvn with non existent settings.xml
    * [KARAF-5591] - Blacklisted features should be considered as dependencies and/or conditionals
    * [KARAF-5592] - Karaf shell unexpected exit when Ctrl + C during log:display or select text then press Enter
    * [KARAF-5610] - Build problems with JDK9
    * [KARAF-5611] - karaf.bat still uses endorsed dirs with Java 9 install
    * [KARAF-5634] - karaf/karaf.bat scripts do not handle lib.next->lib update correctly
    * [KARAF-5639] - NPE during instance:start
    * [KARAF-5641] - Karaf boot scripts need to deal with JDK10 version patterns
    * [KARAF-5642] - karaf:deploy goal broken
    * [KARAF-5645] - Karaf crashes when using the character ']' in the console
    * [KARAF-5646] - Support env:XXX subtitution missing for system.properties
    * [KARAF-5647] - start, stop, shell, status and client fail on Solaris Sparc 11
    * [KARAF-5657] - client.bat doesn't work on Windows
    * [KARAF-5667] - Installing the audit-log feature never ends
    * [KARAF-5670] - pax-web throws an exception when running with a security manager
    * [KARAF-5671] - Demo profiles still use "old style" pax-logging configuration
    * [KARAF-5672] - Servlets urls are displayed without the http context path
    * [KARAF-5673] - karaf-maven-plugin can be very long to apply profile
    * [KARAF-5678] - Existing configfiles (in kar) may be overwritten when building assembly
    * [KARAF-5688] - XML parsing fails when xerces is installed on JDK 8

#### New Feature
    * [KARAF-1677] - Unpacked KAR deployment
    * [KARAF-5614] - Add HttpRedirect/Proxy service with http:redirect/proxy command & MBean
    * [KARAF-5629] - Add new karaf commands shell:elif and shell:else
    * [KARAF-5635] - Integrate WebConsole Memory Usage plugin
    * [KARAF-5665] - Sometimes the command description does not show when listing commands with "TAB"
    * [KARAF-5680] - Provide support for xml parsers deployed as bundle on Java 9

#### Improvement
    * [KARAF-2688] - Karaf info - Add memory details about perm gen pool
    * [KARAF-4496] - UserPrincipal lookup in the JAAS' BackingEngine
    * [KARAF-5448] - Fix Java 9 warnings
    * [KARAF-5558] - Be able to configure the Quartz Scheduler
    * [KARAF-5568] - Karaf Commands cannot have return codes
    * [KARAF-5578] - Add repo URL for sling
    * [KARAF-5588] - Increase max number of threads in the scheduler by default
    * [KARAF-5604] - karaf:features-generate-descriptor takes long when faced with complex feature dependencies
    * [KARAF-5627] - Upgrade to PAX-JMS 0.3.0
    * [KARAF-5677] - deploy goal throws NPE with artifactLocations is not provided
    * [KARAF-5679] - Upgrade to Hibernate Validator 6.0.9.Final

#### Task
    * [KARAF-5586] - Upgrade to Hibernate-validator 5.4.2

#### Dependency upgrade
    * [KARAF-5574] - Upgrade to Pax Web 7.0.0/Jetty 9.4.6
    * [KARAF-5584] - Upgrade to SSHD 1.7.0
    * [KARAF-5595] - Upgrade toJLine 3.6.0 and Jansi 1.17
    * [KARAF-5596] - Upgrade to Spring 5.0.3.RELEASE
    * [KARAF-5597] - Upgrade to Spring 4.3.14.RELEASE
    * [KARAF-5599] - Upgrade Narayana to version 5.7.2.Final
    * [KARAF-5602] - Upgrade to Spring Security 4.2.4.RELEASE
    * [KARAF-5605] - Upgrade to OpenJPA 2.4.2
    * [KARAF-5606] - Upgrade to EclipseLink 2.7.1
    * [KARAF-5607] - Upgrade to Hibernate 5.2.9.Final
    * [KARAF-5612] - Upgrade to blueprint-core 1.9.0, blueprint-cm-1.2.0, blueprint-spring-0.6.0 and blueprint-spring-extender-0.4.0
    * [KARAF-5616] - Upgrade to SCR 2.0.14
    * [KARAF-5617] - Upgrade to JNA 4.5.1
    * [KARAF-5618] - Upgrade to Aries JMX Blueprint 1.2.0
    * [KARAF-5619] - Upgrade to Aries JMX Core 1.1.8 & JMX Whiteboard 1.2.0
    * [KARAF-5622] - Upgrade to commons-compress 1.16.1
    * [KARAF-5623] - Upgrade to maven-bundle-plugin 3.5.0
    * [KARAF-5624] - Upgrade to jline 3.6.1
    * [KARAF-5625] - Upgrade to Pax Swissbox 1.8.3
    * [KARAF-5631] - Upgrade to PAX-CDI 1.0.0
    * [KARAF-5658] - Upgrade to Spring 5.0.4.RELEASE
    * [KARAF-5668] - Upgrade to JLine 3.6.2
    * [KARAF-5675] - Upgrade to XBean 4.7

## Apache Karaf 4.2.0.M2

 Apache Karaf 4.2.0.M2 is a the second technical preview of the 4.2.x series. It's not yet a GA release. It
 brings a lot of improvements and new features, in preparation for the first 4.2.0 GA release.

### ChangeLog

#### Bug
    * [KARAF-2792] - shared cm-properties empty for second bundle
    * [KARAF-3875] - Karaf scheduler should wrap QuartzException in exported SchedulerException
    * [KARAF-3976] - Broken compatibility with 3.x jdbc DataSources
    * [KARAF-4181] - blacklist.properties and overrides.properties are not properties file
    * [KARAF-4662] - Unable to create Karaf Cave 4.0.0 Kar file
    * [KARAF-4684] - karaf-maven-plugin assembly goal fails to find nested features with explicit version containing qualifier
    * [KARAF-4912] - Cannot register Servlet via http-whiteboard under Java 9
    * [KARAF-5203] - KAR:Create missing bundles that are marked conditional
    * [KARAF-5210] - Seemingly random NPEs from Aether resolver
    * [KARAF-5372] - startup.properties doesn't respect overrides
    * [KARAF-5446] - Fragment bundles are not resolved properly when installing/restarting the container
    * [KARAF-5452] - [SCR] Karaf can't activate/deactivate SCR components via JMX
    * [KARAF-5455] - remove redundant sshRole comment 
    * [KARAF-5458] - karaf-maven-plugin fails to assemble artifacts if only available within local reactor
    * [KARAF-5461] - incorrect filter in EncryptionSupport of jaas modules
    * [KARAF-5464] - karaf.bat file is missing KARAF_SYSTEM_OPTS property
    * [KARAF-5466] - Karaf does not start on JDK 9.0.1
    * [KARAF-5467] - Karaf doesn't recognize Java 9 on Ubuntu 16.04
    * [KARAF-5470] - Karaf fails build with Java 9.0.1
    * [KARAF-5472] - Karaf RmiRegistryFactory throws a warning with Java 9
    * [KARAF-5478] - Provide a Version class to check Karaf version used.
    * [KARAF-5480] - The webconsole gogo plugin is broken
    * [KARAF-5495] - Upgrade SyncopeBackingEngineFactory to support Syncope 2.x
    * [KARAF-5496] - NPEs in SyncopeLoginModule if "version" is not specified
    * [KARAF-5498] - SyncopeLoginModule parses roles instead of groups for Syncope 2.0.x
    * [KARAF-5505] - Jetty version out of date
    * [KARAF-5508] - Error using OSGi JAX RS Connector in Java 9
    * [KARAF-5527] - the karaf.secured.command.compulsory.roles should only affect command ACL rules
    * [KARAF-5528] - Karaf feature deployer should stop refreshed bundles together with the updated ones
    * [KARAF-5533] - KarArtifactInstaller does not properly detect already installed KAR files
    * [KARAF-5541] - ensure check the compulsory.roles even there's no ACL for a specific command scope
    * [KARAF-5542] - Installing a feature triggers restarting previous ones
    * [KARAF-5546] - incorrect acl rules for system:start-level
    * [KARAF-5547] - Blueprint namespace handlers cause warning to be printed

#### Dependency upgrade
    * [KARAF-5412] - Upgrade to ASM 6.0
    * [KARAF-5488] - Upgrade to Felix Framework 5.6.10
    * [KARAF-5489] - Upgrade to commons-io 2.6
    * [KARAF-5490] - Upgrade to JNA 4.5.0
    * [KARAF-5491] - Upgrade to commons-compress 1.15
    * [KARAF-5516] - Upgrade to commons-lang3 3.7
    * [KARAF-5517] - Upgrade to Apache Felix Metatype 1.1.6
    * [KARAF-5518] - Upgrade to Apache Felix WebConsole DS plugin 2.0.8
    * [KARAF-5519] - Upgrade to Apache Felix WebConsole EventAdmin plugin 1.1.8
    * [KARAF-5520] - Upgrade to Maven dependencies 3.5.2
    * [KARAF-5521] - Upgrade to Maven Wagon 3.0.0
    * [KARAF-5522] - Upgrade to easymock 3.5.1
    * [KARAF-5523] - Upgrade to Equinox 3.12.50
    * [KARAF-5524] - Upgrade to maven-dependency-tree 3.0.1
    * [KARAF-5525] - Upgrade to PAX tinybundle 3.0.0 
    * [KARAF-5531] - Upgrade to maven-compiler-plugin 3.7.0
    * [KARAF-5532] - Upgrade to maven-dependency-plugin 3.0.2
    * [KARAF-5535] - Upgrade to maven-javadoc-plugin 3.0.0
    * [KARAF-5536] - Upgrade to maven-war-plugin 3.2.0
    * [KARAF-5537] - Upgrade to modello-maven-plugin 1.9.1
    * [KARAF-5538] - Upgrade to maven-invoker-plugin 3.0.1
    * [KARAF-5539] - Upgrade to maven-archetype-plugin 3.0.1
    * [KARAF-5549] - Upgrade to JLine 3.5.4
    * [KARAF-5550] - Upgrade to pax-url 2.5.4
    * [KARAF-5551] - Upgrade to Pax Web 6.1.0

#### Improvement
    * [KARAF-3674] - Document and improve scheduler feature
    * [KARAF-4329] - Consider bundles from override.properties while creating the assembly
    * [KARAF-5273] - karaf-maven-plugin assembly should take feature wildcards
    * [KARAF-5323] - Set multi-location for created configurations
    * [KARAF-5339] - Allow to define blacklisted bundles in a profile
    * [KARAF-5418] - SSH public key authentication from LDAP
    * [KARAF-5448] - Fix Java 9 warnings
    * [KARAF-5456] - introduce a property karaf.shell.history.file.maxSize to configure the history file size on disk
    * [KARAF-5476] - Reduce number of logins when using the webconsole
    * [KARAF-5486] - Add a command to change job scheduling
    * [KARAF-5494] - Fix performance issue generating service metadata, change logging
    * [KARAF-5506] - ensure we also check the ACL for alias cmds before auto-completer
    * [KARAF-5511] - Proper Provide-Capability for org.apache.karaf.jaas.modules.EncryptionService
    * [KARAF-5529] - Rewrite SCR management layer to more closely follow the real object model
    * [KARAF-5544] - Provide bundle consistency report from custom Karaf distribution
    * [KARAF-5548] - Improve the find-class command to support package names

#### New Feature
    * [KARAF-5307] - Add SchedulerMBean to mimic scheduler shell commands
    * [KARAF-5447] - Support Spring 5.0.x
    * [KARAF-5475] - Provide a security audit log
    * [KARAF-5485] - Be able to disable the sftp server

#### Proposal
    * [KARAF-5376] - Processor mechanism for feature definitions (a.k.a. "better overrides")

#### Task
    * [KARAF-5468] - Clean up AssemblyMojo

## Apache Karaf 4.2.0.M1

 Apache Karaf 4.2.0.M1 is a technical preview of the 4.2.x series. It's not yet a GA release. It
 brings a lot of improvements and new features, including Java 9 support.

### ChangeLog

#### Bug
    * [KARAF-3347] - 'LATEST' placeholder is not resolved correctly for descriptors and repositories
    * [KARAF-3429] - always use proxy server listed in maven settings.xml when installing features
    * [KARAF-3531] - SimpleMavenResolver does not handle wrap: prefix in mvn urls
    * [KARAF-3875] - Karaf scheduler should wrap QuartzException in exported SchedulerException
    * [KARAF-4174] - NullPointerException when running obr:info on a bundle served by cave
    * [KARAF-4380] - Remove blueprint feature in standard distribution
    * [KARAF-4490] - LDAPLoginModule use authentication to check user password
    * [KARAF-4603] - Nashorn support in Karaf
    * [KARAF-4655] - karaf-maven-plugin add-features-to-repo goal can't add Camel feature
    * [KARAF-4985] - Karaf does not start with JDK 9 in Windows 
    * [KARAF-4988] - Refreshing a feature repository from webconsole fails
    * [KARAF-5031] - Subshell doesn't show in prompt
    * [KARAF-5051] - Command "shell wrapper:install" fails
    * [KARAF-5073] - OpenSSHGeneratorFileKeyProvider is unable to write SSH keys
    * [KARAF-5078] - Shell crash
    * [KARAF-5091] - log:get does not show correct level
    * [KARAF-5094] - Remove -server option in Karaf scripts
    * [KARAF-5096] - Karaf 4.1.1 Console Issues Over SSH (PuTTY)
    * [KARAF-5103] - Quick start fails at the step "feature:install camel-spring"
    * [KARAF-5105] - Issue with bin/shell command in karaf 4.1.1
    * [KARAF-5106] - karaf-maven-plugin hangs the build (probably when having cyclic deps in the features def)
    * [KARAF-5109] - endorsed and ext directories are not set properly when using instance start
    * [KARAF-5115] - Error while installing cxf
    * [KARAF-5116] - Defining karaf.log.console as a log4j2 log level causes exceptions
    * [KARAF-5119] - log:tail on OSX does not display updates without user input and exits shell on ctrl + c
    * [KARAF-5120] - etc/org.apache.karaf.shell.cfg is "raw", all comments are lost in the distribution
    * [KARAF-5121] - blueprint created by jms:create is not correct
    * [KARAF-5123] - Executing feature:repo-remove can leave karaf in an invalid state
    * [KARAF-5124] - NPE when location information is included in console logging pattern
    * [KARAF-5128] - Upgrade to aries.proxy 1.1.1
    * [KARAF-5134] - Instance org.apache.karaf.features.cfg refers to 4.1.1-SNAPSHOT
    * [KARAF-5138] - CTRL-D on a connected instance exits from the root one
    * [KARAF-5143] - Command cannot be executed via SSH when property "karaf.shell.init.script" (etc/system.properties) has its default value
    * [KARAF-5144] - java.lang.RuntimeException: Command name evaluates to null: $.jline.terminal
    * [KARAF-5147] - Upgrade to pax-web-6.0.4
    * [KARAF-5164] - karaf-maven-plugin fails to verify artifacts if only available within local reactor
    * [KARAF-5165] - Custom Distributions: Pax-Web gets installed twice
    * [KARAF-5167] - Instance etc folder is not sync automatically
    * [KARAF-5171] - Upgrade to ServiceMix Specs 2.9.0
    * [KARAF-5174] - Uninstalling feature using liquibase-slf4j crashes karaf
    * [KARAF-5176] - Fix support for characters entered while executing a command
    * [KARAF-5179] - Setting the karaf.restart.jvm property to true causes system halt commands to behave as reboots
    * [KARAF-5180] - The framework is restarted and sometimes spits an exception when refreshing a fragment
    * [KARAF-5181] - NPE while running "threads --tree" command from console
    * [KARAF-5182] - Console command log:list returns "null"
    * [KARAF-5184] - ClassLoader leak when org.apache.karaf.shell.core bundle is refreshed
    * [KARAF-5196] - Strongly consider removing -XX:+UnsyncloadClass from start scripts
    * [KARAF-5197] - Features deployed from a KAR file do not respect the feature's install setting
    * [KARAF-5199] - Karaf installs both version of the feature (old and new) in case if referencing feature contains wrapped bundle with package import
    * [KARAF-5206] - Karaf doesn't start after not clean reboot, because stored PID corresponds to running process
    * [KARAF-5207] - Features 1.4 namespace not supported by the features deployer
    * [KARAF-5211] - NPE in StoredWiringResolver if BundleEvent.UNRESOLVED handled before BundleEvent.RESOLVED event
    * [KARAF-5216] - Exiting karaf shell, mess the bash shell
    * [KARAF-5218] - bin/client exists when typing CTRL-C
    * [KARAF-5221] - karaf-maven-plugin's pidsToExtract handled incorrectly
    * [KARAF-5223] - "Error in initialization script" messages printed to the main console when clients connect through ssh
    * [KARAF-5229] - The download manager may generate wrong jar with custom urls
    * [KARAF-5234] - Update BUILDING file to reference Java 8
    * [KARAF-5245] - Running karaf.bat inside a "Program Files (x86)" directory
    * [KARAF-5247] - java.lang.InterruptedException after logout command in shell
    * [KARAF-5250] - SNAPSHOT metadata doesn't match SNAPSHOT artifacts after mvn deploy
    * [KARAF-5252] - Upgrade Narayana to version 5.6.3.Final
    * [KARAF-5255] - Upgrade to pax-web-6.0.6
    * [KARAF-5259] - Duplicate log entries displayed when using log:tail
    * [KARAF-5260] - log:tail default should start at the end of the file
    * [KARAF-5264] - Clean up maven dependencies
    * [KARAF-5267] - Karaf does not work correctly after log:tail
    * [KARAF-5271] - Improve JDBC generic lock to better support network glitches
    * [KARAF-5276] - Do not use right prompt by default
    * [KARAF-5279] - InterruptedException when updating the shell.core bundle
    * [KARAF-5283] - Karaf in offline (no internet) environment - NamespaceHandler bugs
    * [KARAF-5298] - config:update doesn't create the cfg file in the etc folder
    * [KARAF-5304] - checkRootInstance function in karaf script fails under AIX
    * [KARAF-5305] - FeatureConfigInstaller writes incorrect config if append=true and file already exists
    * [KARAF-5311] - NPE in karaf-maven-plugin when specifying descriptor by file url
    * [KARAF-5312] - bin/stop script output some unwanted message on mac
    * [KARAF-5313] - Exception when deleting a .cfg file from hot deploy directory 
    * [KARAF-5314] - The performance of profile builder used by karaf maven plugin has reduced significantly in 4.1 compared to 4.0
    * [KARAF-5315] - Race condition during shutdown using SIGTERM
    * [KARAF-5317] - "Exception in thread "SIGWINCH handler" java.lang.UnsupportedOperationException" occurs when resizing the console while `log:tail` is run
    * [KARAF-5320] - Karaf Command Arguments escapes backslash characters
    * [KARAF-5326] - variables in cfg files are expanded
    * [KARAF-5327] - Threads not stopped on karaf.restart + bundle(0).stop()
    * [KARAF-5328] - NPE is thrown when execute source command from client/ssh
    * [KARAF-5330] - Require a specific role to access the SSH console
    * [KARAF-5331] - Use shell command access control lists during command completion
    * [KARAF-5332] - bin/stop script fails when KARAF_DEBUG is set
    * [KARAF-5333] -  UnsupportedCharsetException: cp65001 and unprintable characters from karaf 4.1.2 console
    * [KARAF-5334] - Fix broken shell.support.table.ShellTableTest on Windows
    * [KARAF-5337] - karaf-maven-plugin generates an "override.properties" instead of "overrides.properties"
    * [KARAF-5338] - Unable to access the local JMX server on OSX
    * [KARAF-5340] - A "Set<LocalDependency>" cannot contain a "Artifact" in Dependency31Helper
    * [KARAF-5343] - Upgrade to pax-web-6.0.7
    * [KARAF-5344] - Remote shell *really* doesn't like you resizing the console window
    * [KARAF-5352] - KARAF_ETC envvar ignored
    * [KARAF-5355] - The scripts triggered with {{scheduler::schedule}} command fail to execute
    * [KARAF-5361] - shell:watch is broken
    * [KARAF-5371] - Race condition between FeatureService and Fileinstall
    * [KARAF-5373] - Karaf-maven-plugin fails to create feature file
    * [KARAF-5374] - karaf-maven-plugin can't configure the start-level for the startupBundles
    * [KARAF-5375] - feature:stop command does not stop the bundles
    * [KARAF-5377] - Speed up repository loading
    * [KARAF-5382] - Karaf shell session.readLine consumes backslashes
    * [KARAF-5385] - shutdown -f command can't exit the karaf
    * [KARAF-5387] - Build fail on JLineTerminal
    * [KARAF-5388] - create dump doesn't include log file anymore
    * [KARAF-5390] - tar.gz archives contains invalid data in demos\web\src\main\webapp\WEB-INF\karaf\system\org\apache\felix
    * [KARAF-5394] - maven-metadata-local.xml in KARs cause SAXParseException
    * [KARAF-5395] - Improve memory consumption during resolution
    * [KARAF-5398] - The "cd" command should not attempt to complete multiple directories
    * [KARAF-5404] - CLI autocompletion issue
    * [KARAF-5406] - CLI error on window resize on Linux(Wayland)
    * [KARAF-5411] - Client doesn't prompt for user if no user.properties file
    * [KARAF-5413] - Missing explicit version in features
    * [KARAF-5414] - Features mentioned in feature.xml stubs aren't taken into account in dependency calculations
    * [KARAF-5420] - Bad console behavior when dealing with the input stream with the exec command
    * [KARAF-5423] - Karaf is flagged as vulnerable to CVE-2015-5262
    * [KARAF-5425] - ArrayIndexOutOfBoundsException running history | grep
    * [KARAF-5435] - BundleException when installing a bundle by API when the FeatureService install a feature
    * [KARAF-5436] - Factory configurations file in etc/ are not deleted when the configuration is deleted
    * [KARAF-5440] - No override facility for properties in system.properties

#### Dependency
    * [KARAF-5345] - Upgrade to pax-jms-0.1.0 and ActiveMQ 5.15.0

#### Dependency upgrade
    * [KARAF-4921] - Upgrade to pax-logging 1.10.0
    * [KARAF-4991] - Upgrade to Narayana 5.5.2.Final
    * [KARAF-5085] - Upgrade to Aries JPA 2.6.1
    * [KARAF-5087] - Upgrade to Spring 4.3.7.RELEASE
    * [KARAF-5090] - Update equinox to 3.11.3
    * [KARAF-5112] - Upgrade to jansi 1.16
    * [KARAF-5113] - Upgrade to jline 3.3.0
    * [KARAF-5114] - Upgrade to gogo 1.0.6
    * [KARAF-5132] - Cellar: Upgrade Hazelcast to 3.8.2
    * [KARAF-5146] - Upgrade to Narayana 5.6.0.Final
    * [KARAF-5149] - Upgrade to JNA 4.4.0
    * [KARAF-5150] - Upgrade to Aries Blueprint Core 1.8.1
    * [KARAF-5151] - Upgrade to Aries Transaction Manager 1.3.3
    * [KARAF-5152] - Upgrade to commons-compress 1.14
    * [KARAF-5153] - Upgrade to Felix BundleRepository 2.0.10
    * [KARAF-5154] - Upgrade to Felix Framework 5.6.4
    * [KARAF-5155] - Upgrade to Felix HttpLite 0.1.6
    * [KARAF-5157] - Upgrade to Felix Resolver 1.14.0
    * [KARAF-5158] - Upgrade to Felix SCR 2.0.10
    * [KARAF-5159] - Upgrade to Felix WebConsole 4.3.4
    * [KARAF-5160] - Upgrade to Equinox Region 1.2.101.v20150831-1342
    * [KARAF-5214] - Upgrade to Pax Logging 1.10.1
    * [KARAF-5219] - Upgrade Narayana to version 5.6.2.Final
    * [KARAF-5220] - Cellar-Kubernetes: Bump to Kubernetes-client 2.4.1
    * [KARAF-5231] - Upgrade to jline 3.3.1
    * [KARAF-5248] - Upgrade to blueprint-core 1.8.2
    * [KARAF-5249] - Upgrade to blueprint spring 0.4.0
    * [KARAF-5253] - Update pax-jdbc to 1.1.0
    * [KARAF-5256] - Upgrade to Felix SCR 2.0.12
    * [KARAF-5257] - Upgrade to sshd 1.6.0
    * [KARAF-5258] - Upgrade to Pax Exam 4.11.0
    * [KARAF-5268] - Upgrade to commons-logging 1.2
    * [KARAF-5269] - Upgrade to commons-lang3 3.6
    * [KARAF-5278] - Update to felix framework 5.6.6
    * [KARAF-5281] - Upgrade to Spring 4.3.10.RELEASE
    * [KARAF-5288] - Cellar: Bump to Kubernetes-client 2.5.9
    * [KARAF-5289] - Upgrade to jline 3.4.0
    * [KARAF-5291] - Upgrade Narayana to version 5.6.4.Final
    * [KARAF-5293] - Upgrade to Apache POM 18
    * [KARAF-5309] - Upgrade to directory server 2.0.0-M24
    * [KARAF-5310] - Upgrade to maven surefire plugin 2.20 to get colored output
    * [KARAF-5349] - Upgrade to pax-jdbc-1.2.0
    * [KARAF-5359] - Upgrade to JLine 3.5.0
    * [KARAF-5360] - Upgrade to Felix Gogo Runtime / JLine 1.0.8
    * [KARAF-5365] - Upgrade to Aries Subsystem 2.0.10
    * [KARAF-5366] - Upgrade to Felix ConfigAdmin 1.8.16
    * [KARAF-5367] - Upgrade to Felix EventAdmin 1.4.10
    * [KARAF-5368] - Upgrade to Felix Framework & Main 5.6.8
    * [KARAF-5369] - Upgrade to Felix Metatype 1.1.4
    * [KARAF-5370] - Upgrade to Felix Resolver 1.14.0
    * [KARAF-5401] - Upgrade to Aries Blueprint Spring 0.5.0
    * [KARAF-5419] - Upgrade to Aries Blueprint Core 1.8.3
    * [KARAF-5429] - Upgrade Narayana to version 5.7.0.Final
    * [KARAF-5430] - Upgrade to Spring 4.0.9.RELEASE & 4.3.12.RELEASE
    * [KARAF-5431] - Upgrade to Felix Gogo Runtime / JLine 1.0.10
    * [KARAF-5432] - Upgrade to Felix Utils 1.10.4 and FileInstall 3.6.4
    * [KARAF-5439] - Upgrade Narayana to version 5.7.1.Final

#### Documentation
    * [KARAF-5357] - Help string for feature:stop is incorrect

#### Improvement
    * [KARAF-3825] - Add ability to shutdown Karaf with a disabled shutdown port
    * [KARAF-4417] - Display a summary for the verify goal
    * [KARAF-4418] - Ability to exclude a set of features from the verify goal
    * [KARAF-4748] - Make Felix Resolver Threads configurable
    * [KARAF-4785] - Use the scr gogo commands and provide completion
    * [KARAF-4803] - Allow to turn off Karaf configuration persistence manager
    * [KARAF-4932] - Remove blueprint compat and blueprint annotations bundles
    * [KARAF-4973] - Refactoring of features extension
    * [KARAF-5004] - Discover the artifact type instead of relying on the artifact type/classifier string (kar / features / bundle)
    * [KARAF-5023] - Improve config commands to better support substituted and typed properties
    * [KARAF-5072] - Add setting to ssh server for forcing a provided key
    * [KARAF-5080] - Use the full ttop command from gogo-jline
    * [KARAF-5102] - org.ops4j.pax.logging.cfg contains non-ASCII character
    * [KARAF-5104] - karaf:run should support a features set
    * [KARAF-5118] - Make SSHD server threads configurable
    * [KARAF-5126] - Use awaitility and matchers in JmsTest
    * [KARAF-5131] - XA + JMS support
    * [KARAF-5162] - Code can be simplified using new Map methods
    * [KARAF-5168] - Replace old-style loops with foreach loops or streams
    * [KARAF-5169] - Remove redundant type information
    * [KARAF-5170] - Use try-with-resources
    * [KARAF-5173] - Some tests could benefit from a common CallbackHandler
    * [KARAF-5178] - Code can be simplified using lambdas
    * [KARAF-5185] - Karaf enterprise feature shall omit the jpa feature in favor of the aries jpa feature
    * [KARAF-5205] - Add -r/--refresh option to bundle:update command
    * [KARAF-5208] - Improve feature:install error message
    * [KARAF-5222] - Make possible to force the start of a karaf instance even if another one has been detected as running.
    * [KARAF-5230] - Support version range when installing features
    * [KARAF-5235] - Remove null values from AssemblyMojo configuration
    * [KARAF-5241] - Improve RBAC logging for JMX
    * [KARAF-5243] - add -p option for bin/client
    * [KARAF-5266] - log commands should limit number of lines printed instead of number of log entries
    * [KARAF-5272] - Enhance the features deployer so that it performs a real upgrade
    * [KARAF-5280] - Shell should not display the welcome message again when it is restarted
    * [KARAF-5282] - SyncopeLoginModule should support Syncope 2.x response format
    * [KARAF-5286] - Separate server key generation from key reading
    * [KARAF-5287] - Provide a way to hide passwords in shell
    * [KARAF-5292] - uneeded dependency to dbcp in eclipselink feature
    * [KARAF-5294] - Cleanup Maven repository
    * [KARAF-5308] - Remove RepositoryImpl lazy loading as we always load it upfront anyway
    * [KARAF-5316] - Jaas Encryption should be easier to use
    * [KARAF-5319] - the jetty feature in karaf shouldn't depend on pax-jetty feature
    * [KARAF-5363] - Add --no-start option to kar:install, kar cfg and kar MBean
    * [KARAF-5380] - Fix typo in JDBC lock implementation
    * [KARAF-5400] - Remove usage of felix scr compatibility bundle
    * [KARAF-5407] - Allow feature:info to print the xml for a given feature
    * [KARAF-5426] - Print type of wiring resource
    * [KARAF-5427] - Add RBAC support for reflection invocation and redirections in the console
    * [KARAF-5437] - Use named thread pools to help identifying threads
    * [KARAF-5443] - Add a completer for bundle symbolic names
    * [KARAF-5445] - Completers should be followed by a space when complete

#### New Feature
    * [KARAF-2401] - Improve log coloring
    * [KARAF-3270] - Add command/MBean operation to give current user and his roles
    * [KARAF-4188] - Add support for Systemd's watchdog
    * [KARAF-5008] - Provide Maven diagnostic commands
    * [KARAF-5074] - Support for typed config files (as in Felix ConfigAdmin config files) in features
    * [KARAF-5082] - Allow the use of external data for features configuration
    * [KARAF-5107] - Allow hooking into the feature installation process
    * [KARAF-5129] - JMS Pooling and better Artemis support
    * [KARAF-5172] - Add simple LDAPBackingEngine
    * [KARAF-5175] - Provide a debugs option for the karaf script to make it easier to debug karaf startup sequence
    * [KARAF-5306] - Add scheduler:trigger command
    * [KARAF-5354] - The log:get and log:set commands should support etc/log4j2.xml configuration
    * [KARAF-5416] - Remove support for ext and endorsed libraries for Java 9 compatibility

#### Task
    * [KARAF-5125] - Upgrade to Narayana 5.5.6.Final
    * [KARAF-5148] - Replace use of org.json
    * [KARAF-5225] - Add Narayana dependencies to DependencyManagement
    * [KARAF-5226] - Add Hibernate-validator dependency to DependencyManagement
    * [KARAF-5227] - Use an explicit Awaitility version property 
    * [KARAF-5396] - Ensure Karaf can build with JDK9 GA(build 9+181)
    * [KARAF-5417] - Trim down distributions

#### Test
    * [KARAF-4936] - FeatureTest#repoRefreshCommand failure
