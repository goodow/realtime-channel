#!/bin/bash

## Default configuration: https://github.com/goodow/realtime-channel/blob/master/src/main/resources/channel.conf


## Option 1: Build from source and run with Maven 
git clone https://github.com/goodow/realtime-channel.git
cd realtime-channel
mvn clean package vertx:runMod


## Option 2: Run with pre-installed Vert.x
# wget https://raw.githubusercontent.com/goodow/realtime-channel/master/src/main/resources/channel.conf
# vertx runmod com.goodow.realtime~realtime-channel~0.5.5-SNAPSHOT -conf channel.conf