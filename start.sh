#!/bin/bash

pid=$(ps -ef | grep "java" | grep "hibachi.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

app_jarfile=hibachi.jar
if [ ! -f "$app_jarfile" ]; then
    app_jarfile=target/hibachi.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

nohup java -jar $app_jarfile $* > hibachi-stdout.log 2>&1 &

sleep 2

pid=$(ps -ef | grep "java" | grep "hibachi" | awk '{print $2}')

if [ -x ${pid} ]; then
   echo -e "No hibachi.jar process found - check hibachi-stdout.log"
   exit 1
else
   echo -e "Start successful - check hibachi-stdout.log"
   exit 0
fi
