#!/bin/sh
nohup java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/d/dump/ -Xms1536m -Xmx1536m -Xss256k -Xmn1000m -XX:PermSize=100m -XX:MaxPermSize=256m -XX:MaxTenuringThreshold=10 -XX:PretenureSizeThreshold=1m -XX:SurvivorRatio=8 -XX:NewRatio=4 -XX:ParallelGCThreads=4 -XX:-DisableExplicitGC -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseParNewGC -Duser.timezone=GMT+08 -jar ccweb-start.jar >ccweb.log 2>&1 &
#nohup java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/d/dump/ -Duser.timezone=GMT+08 -jar ccweb-start.jar >ccweb.log 2>&1 &
#nohup java -XX:TieredStopAtLevel=1 -Xverify:none -Dspring.jmx.enabled=true -Dspring.liveBeansView.mbeanDomain -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -jar ccweb-start.jar
echo $! > tpid
echo Start ccweb-start.jar Success!
exit
