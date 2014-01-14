/*
 * Copyright 2014 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.objc;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Net;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Platform.Type;
import com.goodow.realtime.core.PlatformFactory;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

class ObjCPlatform implements PlatformFactory {
  /**
   * Registers the Objective-C platform with a default configuration.
   */
  public static void register() {
    Platform.setFactory(new ObjCPlatform());
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
  public native void scheduleDeferred(Handler<Void> handler) /*-[
    [[NSRunLoop mainRunLoop] performSelector:@selector(handleWithId:) target:handler argument:nil order:0 modes:@[NSDefaultRunLoopMode]];
  ]-*/;
  // @formatter:on

  @Override
  public int setPeriodic(int delayMs, Handler<Void> handler) {
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
  
  private native Object setPeriodicNative(int delayMs, Handler<Void> handler) /*-[
    return
    [NSTimer scheduledTimerWithTimeInterval:delayMs/1000 
                                     target:handler
                                   selector:@selector(handleWithId:)
                                   userInfo:nil
                                    repeats:YES];
  ]-*/;
  // @formatter:on
}