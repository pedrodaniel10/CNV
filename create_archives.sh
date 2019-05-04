#!/usr/bin/env bash
rm -r lb-bin.zip web-server.zip
cd loadbalancer

if mvn clean install; then
    cd ..
    cp -r loadbalancer/target/classes lb-bin
    zip -r lb-bin.zip lb-bin
    zip -r web-server.zip web-server
    rm -rf lb-bin
else
    cd ..
fi