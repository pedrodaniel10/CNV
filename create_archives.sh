#!/usr/bin/env bash
rm -r lb-bin.zip web-server.zip
cd databaselib

if mvn clean install; then
    cd ..
    cd loadbalancer
    if mvn clean install; then
	cd ..

        cp -r loadbalancer/target/classes lb-bin
	cp -r databaselib/target/classes db-lib
        zip -r lb-bin.zip lb-bin db-lib
        zip -r web-server.zip web-server db-lib
        rm -rf lb-bin db-lib
    else
	cd ..
    fi
else
    cd ..
fi
