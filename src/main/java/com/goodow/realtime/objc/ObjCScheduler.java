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
import com.goodow.realtime.core.Scheduler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/*-[
 #import "GDChannel.h"
 #import "ComGoodowRealtimeChannelImplDefaultMessage+Adapter.h"
 ]-*/
class ObjCScheduler implements Scheduler {
  // @formatter:off
  private static native void nativeCancelTimer(Object timer) /*-[
    [(NSTimer *)timer invalidate];
  ]-*/;

  private static native <T> void nativeHandle(Object handler, T event) /*-[
    GDCMessageHandler block = (GDCMessageHandler)handler;
    block(event);
  ]-*/;
  
  private static native Object nativeScheduleTimer(int delayMs, boolean repeat, Handler<Void> handler) /*-[
    return
    [NSTimer scheduledTimerWithTimeInterval:delayMs/1000 
                                     target:handler
                                   selector:@selector(handleWithId:)
                                   userInfo:nil
                                    repeats:repeat];
  ]-*/;
  // @formatter:on

  private final AtomicInteger timerId = new AtomicInteger(0);
  private final JsonObject timers = Json.createObject();

  @Override
  public boolean cancelTimer(int id) {
    String key = "" + id;
    if (timers.has(key)) {
      nativeCancelTimer(timers.get(key));
      timers.remove(key);
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(Object handler, Object event) {
    if (handler instanceof Handler) {
      ((Handler<Object>) handler).handle(event);
    } else {
      nativeHandle(handler, event);
    }
  }

  @Override
  // @formatter:off
  public native void scheduleDeferred(Handler<Void> handler) /*-[
    [[NSRunLoop mainRunLoop] performSelector:@selector(handleWithId:) target:handler argument:nil order:0 modes:@[NSDefaultRunLoopMode]];
  ]-*/;
  // @formatter:on

  @Override
  public int scheduleDelay(int delayMs, final Handler<Void> handler) {
    final int id = timerId.getAndIncrement();
    timers.set("" + id, nativeScheduleTimer(delayMs, false, handler));
    return id;
  }

  @Override
  public int schedulePeriodic(int delayMs, Handler<Void> handler) {
    final int id = timerId.getAndIncrement();
    timers.set("" + id, nativeScheduleTimer(delayMs, true, handler));
    return id;
  }
}