Set objWMIService = GetObject("winmgmts:\\.\root\cimv2")
Set objConfig = objWMIService.Get("Win32_ProcessStartup").SpawnInstance_
objConfig.ShowWindow = SW_HIDE
objConfig.CreateFlags = 8
intReturn = objWMIService.Get("Win32_Process").Create("${command}", "${dir}", objConfig, intProcessID)
If intReturn = 0 Then
    Set objOutputFile = CreateObject("Scripting.fileSystemObject").CreateTextFile("${pid.file}", TRUE)
    objOutputFile.WriteLine(intProcessID)
    objOutputFile.Close
End If
WScript.Quit(intReturn)
