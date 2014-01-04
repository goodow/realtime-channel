package com.goodow.realtime.objc;

import com.goodow.realtime.channel.impl.SimpleBus;
import com.goodow.realtime.core.Handler;

/*-[
 #import "GDChannel.h"
 ]-*/
class ObjCSimpleBus extends SimpleBus {
  @SuppressWarnings("unchecked")
  protected static <T> void handle(T message, Object handler) {
    if (handler instanceof Handler) {
      ((Handler<T>) handler).handle(message);
    } else {
      _nativeHandle(message, handler);
    }
  }

  // @formatter:off 
  private static native <T> void _nativeHandle(T message, Object handler) /*-[
    GDCMessageBlock block = (GDCMessageBlock)handler;
    block(message);
  ]-*/;
  // @formatter:on

  @Override
  protected <T> void nativeHandle(T message, Object handler) {
    ObjCSimpleBus.handle(message, handler);
  }
}
