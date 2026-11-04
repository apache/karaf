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

# Security Policy

## Apache Karaf Runtime Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 4.5.x   | :white_check_mark: |
| 4.4.x   | :white_check_mark: |
| 4.3.x   | :x: |
| 4.2.x   | :x: |
| < 4.2.x | :x:                |

## Reporting a Vulnerability

For information on how to report a new security problem please see [here](https://www.apache.org/security/).
Our existing security advisories are published [here](https://karaf.apache.org/documentation.html).

Before you report, please check the security model below: it describes what we consider a vulnerability.

## Security Model

This section describes what Apache Karaf assumes and guarantees about security. It helps you deploy Karaf safely,
and it helps us decide whether a report is a vulnerability. It covers the Karaf runtime built from this repository.
Features and applications that you install on Karaf (for example Apache Camel or Apache CXF) have their own security
model. Report vulnerabilities in them, and in the projects Karaf uses (for example Apache Felix, OPS4J Pax or
Apache MINA SSHD), to those projects.

### What Karaf trusts

- **The Karaf installation.** Anyone who can write to the Karaf directories (`bin`, `etc`, `deploy`, `lib`, `system`
  and `data`) controls Karaf: they can drop a bundle into `deploy`, or change users and ACLs in `etc`. Protect these
  directories with file system permissions. Passwords in `etc/users.properties` are stored in clear text unless you
  set `encryption.enabled = true` in `etc/org.apache.karaf.jaas.cfg`.
- **The local console.** The console started by `bin/karaf` doesn't ask for a password. It runs as the
  `karaf.local.user` user with the `karaf.local.roles` roles set in `etc/system.properties` (by default `admin`,
  `manager`, `viewer` and `systembundles`).
- **The code you install.** Bundles, features, KAR files and the repositories they come from are trusted. Karaf
  doesn't sandbox bundles: they all run in the same JVM, with the permissions of the Karaf process. Only install
  artifacts from repositories you trust (see `etc/org.ops4j.pax.url.mvn.cfg`; the default repositories use HTTPS).

### Network access

Karaf ships with no enabled user. Add users to `etc/users.properties`, or configure another JAAS login module, before
you log in remotely.

| Endpoint                    | Default address                                                                  | Access                                                                 |
| --------------------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| SSH console                 | `0.0.0.0:8101` (`etc/org.apache.karaf.shell.cfg`)                                | `karaf` JAAS realm, the user needs the `ssh` role                      |
| JMX (RMI)                   | `127.0.0.1:1099` and `127.0.0.1:44444` (`etc/org.apache.karaf.management.cfg`)   | `karaf` JAAS realm. TLS is off by default (`secured` property)         |
| WebConsole (when installed) | HTTP service, port `8181`                                                        | `karaf` JAAS realm, the user needs the `admin` role                    |

Karaf doesn't throttle failed logins or lock accounts. Don't expose the SSH or JMX ports to untrusted networks.
Applications that you deploy on the HTTP service are responsible for their own security.

### Roles and ACLs

Karaf authorizes users with their JAAS roles. The default roles mean:

- `admin`: full control of Karaf. An admin can run code and operating system commands (for example with
  `shell:exec`), and read or write any file that the Karaf process can access.
- `manager`: changes the runtime. A manager can manage bundles that aren't system bundles, and edit most
  configurations. Karaf restricts the most direct ways to run code to `admin`, but a manager can still run code in
  other ways, for example by installing or updating a bundle. Only give the `manager` role to users you trust as
  much as admins.
- `viewer`: reads the state of Karaf, for example bundles, features, configurations and logs.
- `systembundles`: needed to start, stop, update or uninstall system bundles, the bundles with a start level below
  `karaf.systemBundlesStartLevel` (50 by default, set in `etc/custom.properties`).
- `ssh`: needed to open an SSH console session.

The default `admingroup` in `etc/users.properties` has all of these roles.

ACLs decide which roles can run each console command (`etc/org.apache.karaf.command.acl.<scope>.cfg`) and each JMX
MBean operation (`etc/jmx.acl*.cfg`). A rule can match the command or operation name only, or also its arguments.
A command that has no ACL can be run by any user who can log in to the console, including a viewer. Karaf ships ACLs
for the most sensitive commands (installing features, bundles and KAR files, running operating system commands,
changing users and ACLs), but not for every command that changes state (for example `log:set`). To restrict the
commands that have no ACL, set `karaf.secured.command.compulsory.roles` in `etc/system.properties`. See the
[security section of the user guide](manual/src/main/asciidoc/user-guide/security.adoc) for details.

### What we treat as a vulnerability

- A client that isn't authenticated runs a command, calls an MBean operation or reads data through the SSH, JMX or
  WebConsole endpoints. This includes flaws in the JAAS login modules that Karaf ships.
- An authenticated user runs a command or MBean operation that the ACLs deny to their roles. This includes getting
  around an argument rule in the ACLs that Karaf ships, for example to change an ACL configuration without the
  `admin` role.
- A user with only the `viewer` role (and `ssh`) installs or runs code, changes users, roles or ACLs, or gets the
  effect of an operation that the shipped ACLs reserve to `manager` or `admin` through a command or operation open
  to them.
- A user without the `systembundles` role starts, stops, updates or uninstalls a system bundle through the bundle
  commands or MBean operations.

### What we treat as hardening

These aren't vulnerabilities. We welcome reports and fix them as regular bugs:

- A `manager` gets `admin` capabilities, such as running code, through the operations that `manager` is allowed to
  use.
- A user gets around an argument rule that you added to an ACL. An argument rule matches the text of each argument
  against a regular expression, and many commands accept the same resource in several forms (an id, a name, an alias
  or a pattern). Karaf doesn't guarantee that your rule covers all of them: test your rules, or use rules on the
  command name when you need a strict limit.
- A command with no ACL changes the state of Karaf, as long as it doesn't give the effect of an operation that the
  shipped ACLs reserve to a higher role.

### What isn't a vulnerability

- Anything a user with the `admin` role can do, including running code or operating system commands, and reading
  or writing files.
- Anything that needs write access to the Karaf installation, or access to the local console.
- Harm done by code that you install (bundles, features or KAR files): it runs with the permissions of the Karaf
  process.
- Users who can log in reading logs and configurations, including any credentials stored in them. Don't give console
  or JMX access to users who must not see them.
- Guessing passwords: Karaf doesn't throttle failed logins, so limit who can reach the SSH and JMX ports.
