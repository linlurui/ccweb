#!/bin/sh
#nohup java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/d/dump/ -Xms2g -Xmx2g -Xss256k -Xmn600m -XX:PermSize=100m -XX:MaxPermSize=256m -XX:MaxTenuringThreshold=15 -XX:SurvivorRatio=8 -XX:NewRatio=4 -XX:ParallelGCThreads=8 -XX:-DisableExplicitGC -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Duser.timezone=GMT+08 -jar ccweb-start.jar >ccweb.log 2>&1 &
#nohup java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/d/dump/ -Duser.timezone=GMT+08 -jar ccweb-start.jar >ccweb.log 2>&1 &
#nohup java -XX:TieredStopAtLevel=1 -Xverify:none -Dspring.output.ansi.enabled=always -Dcom.sun.management.jmxremote -Dspring.jmx.enabled=true -Dspring.liveBeansView.mbeanDomain -Dspring.application.admin.enabled=true -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -jar ccweb-start.jar
echo $! > tpid
echo Start ccweb-start.jar Success!
exit
