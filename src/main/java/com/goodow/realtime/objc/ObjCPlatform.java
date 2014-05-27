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

import com.goodow.realtime.core.Diff;
import com.goodow.realtime.core.Net;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Platform.Type;
import com.goodow.realtime.core.PlatformFactory;
import com.goodow.realtime.core.Scheduler;

class ObjCPlatform implements PlatformFactory {
  /**
   * Registers the Objective-C platform with a default configuration.
   */
  public static void register() {
    Platform.setFactory(new ObjCPlatform());
  }

  /*-[
    + (void)load {
      [ComGoodowRealtimeObjcObjCPlatform register__];
    }
  ]-*/;

  private final Net net = new ObjCNet();
  private final ObjCScheduler scheduler = new ObjCScheduler();
  private final ObjCDiff diff = new ObjCDiff();

  @Override
  public Diff diff() {
    return diff;
  }

  @Override
  public Net net() {
    return net;
  }

  @Override
  public Scheduler scheduler() {
    return scheduler;
  }

  @Override
  public Type type() {
    return Type.IOS;
  }
}