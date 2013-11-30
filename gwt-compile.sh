mvn clean compile gwt:compile -Dgwt.module=com.goodow.realtime.channel.ChannelProd
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

cp target/realtime-channel-0.5.0-SNAPSHOT/channel/channel.nocache.js ../realtime-server/src/main/resources/web/js/channel.js