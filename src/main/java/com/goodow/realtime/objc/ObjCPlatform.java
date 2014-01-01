package com.goodow.realtime.objc;

import com.goodow.realtime.core.Net;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.VoidHandler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

public class ObjCPlatform extends Platform {
  /**
   * Registers the Objective-C platform with a default configuration.
   */
  public static ObjCPlatform register() {
    ObjCPlatform platform = new ObjCPlatform();
    Platform.setPlatform(platform);
    return platform;
  }

  private final AtomicInteger timerId = new AtomicInteger(1);
  private final JsonObject timers = Json.createObject();

  private final Net net = new ObjCNet();

  @Override
  public boolean cancelTimer(int id) {
    if (timers.has("" + id)) {
      cancelTimer(timers.get("" + id));
      timers.remove("" + id);
      return true;
    }
    return false;
  }

  @Override
  public Net net() {
    return net;
  }

  @Override
  // @formatter:off
  public native void scheduleDeferred(VoidHandler handler) /*-[
    [[NSRunLoop mainRunLoop] performSelector:@selector(handleWithId:) target:handler argument:nil order:0 modes:@[NSDefaultRunLoopMode]];
  ]-*/;
  // @formatter:on

  @Override
  public int setPeriodic(int delayMs, VoidHandler handler) {
    final int id = timerId.getAndIncrement();
    timers.set("" + id, setPeriodicNative(delayMs, handler));
    return id;
  }

  @Override
  public Type type() {
    return Type.IOS;
  }

  // @formatter:off
  private native void cancelTimer(Object timer) /*-[
    [(NSTimer *)timer invalidate];
  ]-*/;
  
  private native Object setPeriodicNative(int delayMs, VoidHandler handler) /*-[
    return
    [NSTimer scheduledTimerWithTimeInterval:delayMs/1000 
                                     target:handler
                                   selector:@selector(handleWithId:)
                                   userInfo:nil
                                    repeats:YES];
  ]-*/;
  // @formatter:on
}