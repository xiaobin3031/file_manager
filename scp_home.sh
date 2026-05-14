#! /bin/bash

cd web
npm run build
cp -r dist/* ../server/src/main/resources/static

cd ../server
mvn clean install -DskipTests

scp target/home-0.0.1-SNAPSHOT.jar xiaobin@192.168.50.133:/home/xiaobin/home/
