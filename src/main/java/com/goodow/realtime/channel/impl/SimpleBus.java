/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.util.IdGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class SimpleBus implements Bus {
  private static final Logger log = Logger.getLogger(SimpleBus.class.getName());

  static void checkNotNull(String paramName, Object param) {
    if (param == null) {
      throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }
  }

  private JsonObject handlerMap; // LinkedHashMap<String, LinkedHashSet<Handler<Message>>>
  final JsonObject replyHandlers; // LinkedHashMap<String, Handler<Message>>
  BusHook hook;
  final IdGenerator idGenerator;

  public SimpleBus() {
    handlerMap = Json.createObject();
    replyHandlers = Json.createObject();
    idGenerator = new IdGenerator();
  }

  @Override
  public void close() {
    if (hook == null || hook.handlePreClose()) {
      doClose();
    }
  }

  @Override
  public State getReadyState() {
    return handlerMap == null ? State.CLOSED : State.OPEN;
  }

  @Override
  public String getSessionId() {
    return "@";
  }

  @Override
  public Bus publish(String address, Object msg) {
    internalHandleSendOrPub(false, false, address, msg, null);
    return this;
  }

  @Override
  public Bus publishLocal(String address, Object msg) {
    internalHandleSendOrPub(true, false, address, msg, null);
    return this;
  }

  @Override
  public Registration registerHandler(final String address,
      final Handler<? extends Message> handler) {
    return registerHandlerImpl(false, address, handler);
  }

  @Override
  public Registration registerLocalHandler(final String address,
      final Handler<? extends Message> handler) {
    return registerHandlerImpl(true, address, handler);
  }

  @Override
  public <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler) {
    internalHandleSendOrPub(false, true, address, msg, replyHandler);
    return this;
  }

  @Override
  public <T> Bus sendLocal(String address, Object msg, Handler<Message<T>> replyHandler) {
    internalHandleSendOrPub(true, true, address, msg, replyHandler);
    return this;
  }

  @Override
  public Bus setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }

  protected void doClose() {
    publishLocal(ON_CLOSE, null);
    clearHandlers();
    if (hook != null) {
      hook.handlePostClose();
    }
  }

  protected boolean doRegisterHandler(boolean local, String address,
      Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers == null) {
      handlerMap.set(address, Json.createArray().push(handler));
      return true;
    }
    if (handlers.indexOf(handler) == -1) {
      handlers.push(handler);
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  protected <T> void doSendOrPub(boolean local, boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    checkNotNull("address", address);
    String replyAddress = null;
    if (replyHandler != null) {
      replyAddress = makeUUID();
    }
    MessageImpl message = new MessageImpl(local, send, this, address, replyAddress, msg);
    if (internalHandleReceiveMessage(local, message) && replyHandler != null) {
      replyHandlers.set(replyAddress, replyHandler);
    }
  }

  protected boolean doUnregisterHandler(boolean local, String address,
      Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers == null) {
      return false;
    }
    boolean removed = handlers.removeValue(handler);
    if (handlers.length() == 0) {
      handlerMap.remove(address);
    }
    return removed;
  }

  void clearHandlers() {
    replyHandlers.clear();
    handlerMap.clear();
    handlerMap = null;
  }

  boolean internalHandleReceiveMessage(boolean local, Message message) {
    if (local || hook == null || hook.handleReceiveMessage(message)) {
      doReceiveMessage(message);
      return true;
    }
    return false;
  }

  <T> void internalHandleSendOrPub(boolean local, boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    if (local || hook == null || hook.handleSendOrPub(send, address, msg, replyHandler)) {
      doSendOrPub(local, send, address, msg, replyHandler);
    }
  }

  String makeUUID() {
    return idGenerator.next(36);
  }

  void scheduleHandle(final String address, final Object handler, final Object message) {
    Platform.scheduler().scheduleDeferred(new Handler<Void>() {
      @Override
      public void handle(Void ignore) {
        try {
          Platform.scheduler().handle(handler, message);
        } catch (Throwable e) {
          log.log(Level.WARNING, "Failed to handle on address: " + address, e);
          publishLocal(ON_ERROR, Json.createObject().set("address", address)
              .set("message", message).set("cause", e));
        }
      }
    });
  }

  private void doReceiveMessage(final Message message) {
    final String address = message.address();
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers != null) {
      handlers.forEach(new JsonArray.ListIterator<Object>() {
        @Override
        public void call(int index, Object value) {
          scheduleHandle(address, value, message);
        }
      });
    } else {
      // Might be a reply message
      Object handler = replyHandlers.get(address);
      if (handler != null) {
        replyHandlers.remove(address);
        scheduleHandle(address, handler, message);
      }
    }
  }

  private Registration registerHandlerImpl(final boolean local, final String address,
      final Handler<? extends Message> handler) {
    doRegisterHandler(local, address, handler);
    return new Registration() {
      @Override
      public void unregister() {
        doUnregisterHandler(local, address, handler);
      }
    };
  }
}