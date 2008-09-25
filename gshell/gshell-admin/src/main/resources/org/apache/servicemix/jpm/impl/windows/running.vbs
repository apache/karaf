Set objWMIService = GetObject("winmgmts:\\.\root\cimv2")
Set colProcessList = objWMIService.ExecQuery("Select * from Win32_Process Where ProcessId = ${pid}")
intRetVal = 1
For Each objProcess in colProcessList
    intRetVal = 0
Next
WScript.Quit(intRetVal)
