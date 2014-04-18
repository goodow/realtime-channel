#!/bin/bash

# Configuration file: https://github.com/goodow/realtime-channel/blob/master/src/main/resources/channel.conf


# Option 1: Build from source and run with Maven 
bower install # fetch realtime-channel.js from https://github.com/goodow/bower-realtime-channel
mvn clean install vertx:runMod


# Option 2: Run with pre-installed Vert.x
# vertx runmod com.goodow.realtime~realtime-channel~0.5.5-SNAPSHOT -conf channel.conf