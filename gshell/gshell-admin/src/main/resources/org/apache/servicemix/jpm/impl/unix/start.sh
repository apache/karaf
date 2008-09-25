#!/bin/sh
#exec 1>${out.file}
#exec 2>${err.file}
exec 1>/dev/null
exec 2>/dev/null
cd ${dir}
nohup ${command} &
echo $! > ${pid.file}
