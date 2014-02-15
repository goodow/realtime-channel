mvn clean compile gwt:compile -Dgwt.module=com.goodow.realtime.channel.ChannelProd \
    -Dgwt.disableCastChecking=true -Dgwt.disableClassMetadata=true \
    -Dgwt.compiler.optimizationLevel=9 -Dgwt.compiler.enableClosureCompiler=true
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

cp target/realtime-channel-0.5.5-SNAPSHOT/channel/channel.nocache.js ../realtime-server/src/main/resources/web/js/channel.js
