#!/bin/sh
#exec 1>${out.file}
#exec 2>${err.file}
exec 1>/dev/null
exec 2>/dev/null
if [ "x${dir}" != "x" ]; then
    cd ${dir}
fi
nohup ${command} &
echo $! > ${pid.file}
