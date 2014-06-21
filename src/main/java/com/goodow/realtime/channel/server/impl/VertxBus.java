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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.impl.SimpleBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.impl.JreJsonArray;
import com.goodow.realtime.json.impl.JreJsonObject;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VertxBus implements Bus {
  private static final Logger log = Logger.getLogger(VertxBus.class.getName());

  @SuppressWarnings("unchecked")
  static Object unwrapMsg(Object vertxMessage) {
    if (vertxMessage instanceof JsonObject) {
      return new JreJsonObject(((JsonObject) vertxMessage).toMap());
    } else if (vertxMessage instanceof JsonArray) {
      return new JreJsonArray(((JsonArray) vertxMessage).toList());
    } else {
      return vertxMessage;
    }
  }

  static Object wrapMsg(Object realtimeMessage) {
    if (realtimeMessage instanceof JreJsonObject) {
      return new JsonObject(((JreJsonObject) realtimeMessage).toNative());
    } else if (realtimeMessage instanceof JreJsonArray) {
      return new JsonArray(((JreJsonArray) realtimeMessage).toNative());
    } else {
      return realtimeMessage;
    }
  }

  private final Bus localBus;
  private final EventBus eb;
  private State state;
  private BusHook hook;

  public VertxBus(EventBus eb) {
    this.eb = eb;
    state = State.OPEN;
    localBus = new SimpleBus();
  }

  @Override
  public void close() {
    if (hook == null || hook.handlePreClose()) {
      state = State.CLOSING;
      localBus.close();
      eb.close(new org.vertx.java.core.Handler<AsyncResult<Void>>() {
        @Override
        public void handle(AsyncResult<Void> ar) {
          if (ar.succeeded()) {
            state = State.CLOSED;
            if (hook != null) {
              hook.handlePostClose();
            }
          } else {
            log.log(Level.SEVERE, "Failed to close EventBus", ar.cause());
          }
        }
      });
    }
  }

  @Override
  public State getReadyState() {
    return state;
  }

  @Override
  public String getSessionId() {
    return "vertx";
  }

  @Override
  public VertxBus publish(String address, Object msg) {
    if (hook == null || hook.handleSendOrPub(false, address, msg, null)) {
      eb.publish(address, wrapMsg(msg));
    }
    return this;
  }

  @Override
  public Bus publishLocal(String address, Object msg) {
    return localBus.publishLocal(address, msg);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Registration registerHandler(final String address,
      final Handler<? extends Message> handler) {
    if (hook != null && !hook.handlePreRegister(address, handler)) {
      return Registration.EMPTY;
    }
    final org.vertx.java.core.Handler<org.vertx.java.core.eventbus.Message> vertxHandler =
        wrapHandler(handler);
    eb.registerHandler(address, vertxHandler, new org.vertx.java.core.Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.failed()) {
          log.log(Level.SEVERE, "Failed to register handler on event bus", ar.cause());
        }
      }
    });
    return new Registration() {
      @Override
      public void unregister() {
        if (hook == null || hook.handleUnregister(address)) {
          eb.unregisterHandler(address, vertxHandler,
              new org.vertx.java.core.Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> ar) {
                  if (ar.failed()) {
                    log.log(Level.SEVERE, "Failed to unregister handler on event bus", ar.cause());
                  }
                }
              });
        }
      }
    };
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Registration registerLocalHandler(final String address,
      Handler<? extends Message> handler) {
    return localBus.registerLocalHandler(address, handler);
  }

  @Override
  public <T> VertxBus send(String address, Object msg, final Handler<Message<T>> replyHandler) {
    if (hook == null || hook.handleSendOrPub(true, address, msg, replyHandler)) {
      eb.send(address, wrapMsg(msg), wrapHandler(replyHandler));
    }
    return this;
  }

  @Override
  public <T> Bus sendLocal(String address, Object msg, Handler<Message<T>> replyHandler) {
    return localBus.sendLocal(address, msg, replyHandler);
  }

  @Override
  public VertxBus setHook(BusHook hook) {
    this.hook = hook;
    if (hook != null && state == State.OPEN) {
      hook.handleOpened();
    }
    return this;
  }

  BusHook getHook() {
    return hook;
  }

  @SuppressWarnings("rawtypes")
  private org.vertx.java.core.Handler<org.vertx.java.core.eventbus.Message> wrapHandler(
      final Handler<? extends Message> handler) {
    return handler == null ? null
        : new org.vertx.java.core.Handler<org.vertx.java.core.eventbus.Message>() {
          @SuppressWarnings("unchecked")
          @Override
          public void handle(org.vertx.java.core.eventbus.Message message) {
            VertxMessage event = new VertxMessage(VertxBus.this, message);
            if (hook == null || hook.handleReceiveMessage(event)) {
              ((Handler<Message>) handler).handle(event);
            }
          }
        };
  }
}