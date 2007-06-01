; Project supplied Inno Setup File 

#define MyAppName "${app}"
#define MyAppVerName "${app} ${app.version}"
#define MyAppVersion "${app.version}"
#define MyAppPublisher "${app.author}"
#define MyAppURL "${app.url}"
#define MyAppExeName "${app}w.exe"
#define MyAppCompanyName "${app.company.name}"
#define MyAppDescription "${app.description}"
#define MyAppCopyright "Copyright (c) ${app.copyright.year} ${app.company.name}"

; Set this constant to the path where your installation image resides
#define SourceBase "${image.basedir}"

[Setup]
AppName={#MyAppName}
AppVerName={#MyAppVerName}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\${app}-${app.version}
DefaultGroupName={#MyAppName}
LicenseFile={#SourceBase}\${app.license.name}
OutputDir="${installer.output.directory}"
OutputBaseFilename=${app.final.name}
SetupIconFile={#SourceBase}\${app.icon.name}
Compression=lzma
SolidCompression=true
VersionInfoCompany={#MyAppCompanyName}
VersionInfoDescription={#MyAppDescription}
AppCopyright={#MyAppCopyright}
ShowLanguageDialog=yes
AppVersion={#MyAppVersion}

[Languages]
Name: eng; MessagesFile: compiler:Default.isl

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}; Flags: unchecked

[Files]
; bin directory targets
Source: {#SourceBase}\bin\${app}w.exe; DestDir: {app}\bin; Tasks: ; Languages: 
Source: {#SourceBase}\bin\${app}.exe; DestDir: {app}\bin
Source: {#SourceBase}\bin\bootstrapper.jar; DestDir: {app}\bin; DestName: bootstrapper.jar
Source: {#SourceBase}\bin\felix.jar; DestDir: {app}\bin; DestName: felix.jar
Source: {#SourceBase}\bin\logger.jar; DestDir: {app}\bin; DestName: logger.jar
Source: {#SourceBase}\bin\daemon.jar; DestDir: {app}\bin; DestName: daemon.jar
; bundle directory targets
Source: {#SourceBase}\bundle\*; DestDir: {app}\bundle; Flags: ignoreversion recursesubdirs createallsubdirs
; conf directory targets
Source: {#SourceBase}\conf\log4j.properties; DestDir: {app}\conf; DestName: log4j.properties
Source: {#SourceBase}\conf\bootstrapper.properties; DestDir: {app}\conf; DestName: bootstrapper.properties
Source: {#SourceBase}\conf\config.properties; DestDir: {app}\conf; DestName: config.properties
; top level directory targets 
Source: {#SourceBase}\${app.license.name}; DestDir: {app}; DestName: ${app.license.name}
Source: {#SourceBase}\${app.readme.name}; DestDir: {app}; DestName: ${app.readme.name}
Source: {#SourceBase}\${app.icon.name}; DestDir: {app}; DestName: ${app.icon.name}
; empty var & lib\ext directory structure
Source: {#SourceBase}\var\*; DestDir: "{app}\var\"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: {#SourceBase}\lib\ext; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
; lib directory targets
${app.lib.jars}
${docs.directive}
${sources.directive}
${notice.file}

[Icons]
Name: {group}\Service Settings; Filename: {app}\bin\${app}w.exe; Parameters: //ES//${app}; IconIndex: 0
Name: {userdesktop}\{#MyAppName}; Filename: {app}\bin\${app}w.exe; Tasks: desktopicon; Parameters: //ES//${app}; IconIndex: 0; Languages: 
Name: {group}\Tray Monitor; Filename: {app}\bin\${app}w.exe; Parameters: //MS//${app}; IconIndex: 0
Name: {group}\Test Service; Filename: {app}\bin\${app}.exe; IconIndex: 0

[Run]
Filename: {app}\bin\${app}.exe; WorkingDir: {app}\bin; Tasks: ; Languages: ; Parameters: "//IS//${app.displayname} --Description=""${app.description} Service ${app.version} - ${app.url}"" --DisplayName=${app.displayname} --Install=""{app}\bin\${app}.exe"" --StartMode=jvm --StopMode=jvm --StartClass=org.apache.directory.daemon.ProcrunBootstrapper --StartMethod prunsrvStart --StartParams=""{app}"" --StopClass=org.apache.directory.daemon.ProcrunBootstrapper --StopMethod prunsrvStop --StopParams=""{app}"" --Startup=manual --JvmOptions=""-D${app}.home={app}"" --Classpath=""{app}\bin\bootstrapper.jar;{app}\conf;{app}\bin\logger.jar;{app}\bin\daemon.jar"" --LogPath=""{app}\var\log"" --LogPrefix=${app}.log --LogLevel=debug --StdOutput=""{app}\var\log\${app}-stdout.log"" --StdError=""{app}\var\log\${app}-stderr.log"""; Flags: runhidden
Filename: {app}\bin\${app}w.exe; Parameters: //ES//${app.displayname}; WorkingDir: {app}\bin; Flags: postinstall nowait; Description: Runs the configuration manager for the ${app} windows service

[Registry]

[UninstallRun]
Filename: {app}\bin\${app}.exe; WorkingDir: {app}\bin; Parameters: //DS//${app.displayname}
