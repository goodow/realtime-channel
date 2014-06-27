#!/bin/bash
set -ev

#mvn compile gwt:compile -Dgwt.module=com.goodow.realtime.channel.ChannelProd \
#    -Dgwt.disableCastChecking=true -Dgwt.disableClassMetadata=true \
#    -Dgwt.compiler.optimizationLevel=9 -Dgwt.compiler.enableClosureCompiler=true
#    -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

java -cp ./target/classes:./src/main/java:$HOME/.m2/repository/com/google/gwt/gwt-user/2.7.0-SNAPSHOT/gwt-user-2.7.0-SNAPSHOT.jar:$HOME/.m2/repository/com/google/gwt/gwt-dev/2.7.0-SNAPSHOT/gwt-dev-2.7.0-SNAPSHOT.jar:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA.jar:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA-sources.jar:$HOME/.m2/repository/org/json/json/20090211/json-20090211.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT-sources.jar \
    com.google.gwt.dev.Compiler -war target/realtime-channel-0.5.5-SNAPSHOT \
    -XnoclassMetadata -XnocheckCasts -XjsInteropMode JS -XclosureCompiler -optimize 9 \
    com.goodow.realtime.channel.ChannelProd

cp target/realtime-channel-0.5.5-SNAPSHOT/channel/channel.nocache.js bower-realtime-channel/realtime-channel.js
