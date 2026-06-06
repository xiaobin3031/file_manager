#! /bin/bash

remoteIp=192.168.50.133
user=xiaobin

cd web
npm run build
cp -r dist/* ../server/src/main/resources/static

cd ../server
mvn clean install -DskipTests

echo "准备上传文件"
scp target/home-0.0.1-SNAPSHOT.jar $user@$remoteIp:/home/xiaobin/home/

echo "准备执行重启命令"
ssh $user@$remoteIp "sudo systemctl restart home-app"
