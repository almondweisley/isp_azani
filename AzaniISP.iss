; ============================================================================
;  AzaniISP.iss  -  Inno Setup script for "Azani ISP Setup.exe".
;
;  build.bat runs this after jpackage has made dist\Azani ISP, so run
;  build.bat rather than this file. Needs Inno Setup 6 (winget install JRSoftware.InnoSetup).
;
;  The setup installs the app with its own Java and its own MySQL server, plus
;  the source code and the SQL scripts in a "source" folder, so a marker needs
;  neither Java nor MySQL. The data lives in %LOCALAPPDATA%\Azani ISP.
; ============================================================================

#define AppName    "Azani ISP"
#define AppExe     "Azani ISP.exe"
; build.bat passes its VERSION in as /DAppVersion=...
#ifndef AppVersion
  #define AppVersion "1.0"
#endif

[Setup]
; AppId identifies the app to Windows: keep it the same in every version, so a new setup upgrades the old one
AppId={{3F6B2756-A896-4935-B9CD-FD9D5A810EF5}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppName}
DefaultDirName={autopf}\{#AppName}
DisableProgramGroupPage=yes
; No admin rights needed (installs for the current user); an admin can still pick "all users"
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
; The bundled Java runtime is 64-bit
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=dist
OutputBaseFilename={#AppName} Setup
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\{#AppExe}

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; The program: the exe, AzaniISP.jar, the driver and the Java runtime
Source: "dist\{#AppName}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; Readable copies for the marker. db.properties stays out: the jar already holds it.
Source: "README.md";            DestDir: "{app}\source"; Flags: ignoreversion
Source: "db\*.java";            DestDir: "{app}\source\db"; Flags: ignoreversion
Source: "model\*.java";         DestDir: "{app}\source\model"; Flags: ignoreversion
Source: "dao\*.java";           DestDir: "{app}\source\dao"; Flags: ignoreversion
Source: "service\*.java";       DestDir: "{app}\source\service"; Flags: ignoreversion
Source: "ui\*.java";            DestDir: "{app}\source\ui"; Flags: ignoreversion
Source: "azani_db_ddl.sql";     DestDir: "{app}\source\database"; Flags: ignoreversion
Source: "latest_dml_azani.sql"; DestDir: "{app}\source\database"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autoprograms}\{#AppName} source code"; Filename: "{app}\source"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[UninstallRun]
; Close the app, then its MySQL server, so their files can be removed.
; azani-mysqld.exe is the renamed server: any other MySQL on the PC is left alone.
Filename: "{sys}\taskkill.exe"; Parameters: "/F /IM ""{#AppExe}"""; Flags: runhidden; RunOnceId: "StopApp"
Filename: "{sys}\taskkill.exe"; Parameters: "/F /IM azani-mysqld.exe"; Flags: runhidden; RunOnceId: "StopDatabase"

[UninstallDelete]
; The data the app made on first start (see db\LocalServer.java)
Type: filesandordirs; Name: "{localappdata}\{#AppName}"
