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
package com.goodow.realtime.channel.server.impl;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Scheduler;

import org.vertx.java.core.Vertx;

class VertxScheduler implements Scheduler {
  private final Vertx vertx;

  VertxScheduler(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public boolean cancelTimer(int id) {
    return vertx.cancelTimer(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(Object handler, Object event) {
    ((Handler<Object>) handler).handle(event);
  }

  @Override
  public void scheduleDeferred(final Handler<Void> handler) {
    vertx.runOnContext(new org.vertx.java.core.Handler<Void>() {
      @Override
      public void handle(Void event) {
        handler.handle(null);
      }
    });
  }

  @Override
  public int scheduleDelay(int delayMs, final Handler<Void> handler) {
    return (int) vertx.setTimer(delayMs, new org.vertx.java.core.Handler<Long>() {
      @Override
      public void handle(Long event) {
        handler.handle(null);
      }
    });
  }

  @Override
  public int schedulePeriodic(int delayMs, final Handler<Void> handler) {
    return (int) vertx.setPeriodic(delayMs, new org.vertx.java.core.Handler<Long>() {
      @Override
      public void handle(Long event) {
        handler.handle(null);
      }
    });
  }
}