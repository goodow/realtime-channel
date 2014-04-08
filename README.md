realtime-channel [![Build Status](https://travis-ci.org/goodow/realtime-channel.svg?branch=master)](https://travis-ci.org/goodow/realtime-channel)
================

For java and android: realtime-android
=========

## Adding realtime-android to your project

### Maven

```xml
<dependency>
  <groupId>com.goodow.realtime</groupId>
  <artifactId>realtime-android</artifactId>
  <version>0.5.0</version>
</dependency>
```

## Usage

### WebSocket mode
See https://github.com/goodow/realtime-android/blob/master/src/test/java/com/goodow/realtime/java/EventBusDemo.java

### Local mode
See https://github.com/goodow/realtime-android/blob/master/src/test/java/com/goodow/realtime/java/LocalEventBusDemo.java

### Mix mode
todo

**NOTE:** You must register a platform first by invoke JavaPlatform.register() or AndroidPlatform.register()


For iOS and mac osx: GDChannel
=========

## Adding GDChannel to your project

### Cocoapods

[CocoaPods](http://cocoapods.org) is the recommended way to add GDChannel to your project.

1. Add a pod entry for GDChannel to your Podfile `pod 'GDChannel', '~> 0.5'`
2. Install the pod(s) by running `pod install`.
3. Include GDChannel wherever you need it with `#import "GDChannel.h"`.

## Usage

### WebSocket mode
See https://github.com/goodow/realtime-channel/blob/master/Project/GDChannelTests/GDCEventBusTests.m

### Local mode
See https://github.com/goodow/realtime-channel/blob/master/Project/GDChannelTests/GDCLocalEventBusTests.m

### Mix mode
todo


For javascript: good.channel
=========

## Usage
See https://github.com/goodow/realtime-channel/blob/master/src/main/resources/web/index.html
