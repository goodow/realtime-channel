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
package com.goodow.realtime.html;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Scheduler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

class HtmlScheduler implements Scheduler {
  // @formatter:off 
  private static native <T> void nativeHandle(Object handler, T event) /*-{
    handler(@org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(event));
  }-*/;
  // @formatter:on

  private int timerId = 0;
  private final JsonObject timers = Json.createObject();

  @Override
  public boolean cancelTimer(int id) {
    String key = "" + id;
    if (timers.has(key)) {
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
  public void scheduleDeferred(final Handler<Void> handler) {
    com.google.gwt.core.client.Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        handler.handle(null);
      }
    });
  }

  @Override
  public int scheduleDelay(int delayMs, final Handler<Void> handler) {
    final String key = "" + timerId++;
    RepeatingCommand cmd = new RepeatingCommand() {
      @Override
      public boolean execute() {
        if (timers.has(key)) {
          timers.remove(key);
          handler.handle(null);
        }
        return false;
      }
    };
    timers.set(key, cmd);
    com.google.gwt.core.client.Scheduler.get().scheduleFixedDelay(cmd, delayMs);
    return timerId;
  }

  @Override
  public int schedulePeriodic(int delayMs, final Handler<Void> handler) {
    final String key = "" + timerId++;
    RepeatingCommand cmd = new RepeatingCommand() {
      @Override
      public boolean execute() {
        if (timers.has(key)) {
          handler.handle(null);
          return true;
        }
        return false;
      }
    };
    timers.set(key, cmd);
    com.google.gwt.core.client.Scheduler.get().scheduleFixedPeriod(cmd, delayMs);
    return timerId;
  }
}