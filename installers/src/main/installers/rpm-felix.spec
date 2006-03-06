Summary: ${app.display.name} Server 
Name: ${app}
Version: ${app.version}
Release: ${app.release}
License: ${app.license.type}
Group: System Environment/Daemons
URL: ${app.url}
Source0: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

%description
${app.description}

%prep
echo $RPM_BUILD_ROOT
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT
cp -rf ${image.basedir} $RPM_BUILD_ROOT/%{name}-%{version}
cd $RPM_BUILD_ROOT
tar -zcvf /usr/src/redhat/SOURCES/%{name}-%{version}.tar.gz %{name}-%{version}

%setup -q

%build
cd $RPM_BUILD_ROOT/%{name}-%{version}

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/conf
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/lib/ext
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/var/log
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/var/run
mkdir -p $RPM_BUILD_ROOT/usr/local/${app}-%{version}/var/partitions
touch $RPM_BUILD_ROOT/usr/local/${app}-%{version}/var/log/${app}-stderr.log
touch $RPM_BUILD_ROOT/usr/local/${app}-%{version}/var/log/${app}-stdout.log
mkdir -p $RPM_BUILD_ROOT/etc/init.d
${mk.docs.dirs}
${mk.sources.dirs}

pwd
install -m 755 ${image.basedir}/bin/${app} $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin/${app}
install -m 644 ${image.basedir}/bin/bootstrapper.jar $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin/bootstrapper.jar
install -m 644 ${image.basedir}/bin/logger.jar $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin/logger.jar
install -m 644 ${image.basedir}/bin/daemon.jar $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin/daemon.jar
install -m 644 ${image.basedir}/conf/bootstrapper.properties $RPM_BUILD_ROOT/usr/local/${app}-%{version}/conf/bootstrapper.properties
install -m 644 ${image.basedir}/conf/log4j.properties $RPM_BUILD_ROOT/usr/local/${app}-%{version}/conf/log4j.properties
install -m 744 ${image.basedir}/bin/${server.init} $RPM_BUILD_ROOT/etc/init.d/${app}
install -m 744 ${image.basedir}/bin/felix.jar $RPM_BUILD_ROOT/usr/local/${app}-%{version}/bin/felix.jar
install -m 644 ${image.basedir}/${app.license.name} $RPM_BUILD_ROOT/usr/local/${app}-%{version}
install -m 644 ${image.basedir}/${app.readme.name} $RPM_BUILD_ROOT/usr/local/${app}-%{version}
install -m 644 ${image.basedir}/${app.icon} $RPM_BUILD_ROOT/usr/local/${app}-%{version}
${install.append.libs}
${install.docs}
${install.sources}
${install.notice.file}

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc ${app.license.name} ${app.readme.name}

/etc/init.d/${app}
/usr/local/${app}-%{version}/bin/${app}
/usr/local/${app}-%{version}/bin/bootstrapper.jar
/usr/local/${app}-%{version}/bin/logger.jar
/usr/local/${app}-%{version}/bin/daemon.jar
/usr/local/${app}-%{version}/bin/felix.jar
/usr/local/${app}-%{version}/conf/log4j.properties
/usr/local/${app}-%{version}/conf/bootstrapper.properties
/usr/local/${app}-%{version}/conf/server.xml
/usr/local/${app}-%{version}/lib/ext
/usr/local/${app}-%{version}/var/run
/usr/local/${app}-%{version}/var/log
/usr/local/${app}-%{version}/var/partitions
/usr/local/${app}-%{version}/var/log/${app}-stderr.log
/usr/local/${app}-%{version}/var/log/${app}-stdout.log
/usr/local/${app}-%{version}/${app.readme.name}
/usr/local/${app}-%{version}/${app.license.name}
/usr/local/${app}-%{version}/${app.icon}
${verify.append.libs}
${verify.docs}
${verify.sources}
${verify.notice.file}
