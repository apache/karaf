Set objWMIService = GetObject("winmgmts:\\.\root\cimv2")
Set objConfig = objWMIService.Get("Win32_ProcessStartup").SpawnInstance_
objConfig.ShowWindow = SW_HIDE
objConfig.CreateFlags = 8
If StrLen("${dir}") > 0 Then
    intReturn = objWMIService.Get("Win32_Process").Create("${command}", "${dir}", objConfig, intProcessID)
Else
    intReturn = objWMIService.Get("Win32_Process").Create("${command}", Null, objConfig, intProcessID)
End If
If intReturn = 0 Then
    Set objOutputFile = CreateObject("Scripting.fileSystemObject").CreateTextFile("${pid.file}", TRUE)
    objOutputFile.WriteLine(intProcessID)
    objOutputFile.Close
End If
WScript.Quit(intReturn)
