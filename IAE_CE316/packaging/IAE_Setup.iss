#define AppName "Integrated Assignment Environment"
#define AppVersion "1.0.0"
#define AppExeName "IAE.exe"

[Setup]
AppId={{EA972A78-8FD0-4CE3-8B9E-26EF627C75C9}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=CE316 Team - IUE
DefaultDirName={autopf}\IAE
DefaultGroupName=IAE
OutputDir=..\dist
OutputBaseFilename=IAE_Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\target\installer-image\IAE\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\IAE"; Filename: "{app}\{#AppExeName}"
Name: "{autodesktop}\IAE"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
