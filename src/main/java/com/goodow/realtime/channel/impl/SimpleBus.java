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
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.util.IdGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

@SuppressWarnings("rawtypes")
public class SimpleBus implements Bus {
  protected final JsonObject handlerMap; // Map<String, List<Handler<Message>>>
  protected final JsonObject replyHandlers; // Map<String, Handler<Message>>
  protected State state = State.CONNECTING;
  private final IdGenerator idGenerator;

  public SimpleBus() {
    handlerMap = Json.createObject();
    replyHandlers = Json.createObject();
    idGenerator = new IdGenerator();
    state = State.OPEN;
  }

  @Override
  public void close() {
    state = State.CLOSING;
    publish(Bus.LOCAL + Bus.LOCAL_ON_CLOSE, null);
    state = State.CLOSED;
    clearHandlers();
  }

  @Override
  public State getReadyState() {
    return state;
  }

  @Override
  public Bus publish(String address, JsonElement msg) {
    sendOrPub(false, address, msg, null);
    return this;
  }

  @Override
  public Bus registerHandler(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers == null) {
      handlers = Json.createArray();
      handlers.push(handler);
      handlerMap.set(address, handlers);
    } else if (handlers.indexOf(handler) == -1) {
      handlers.push(handler);
    }
    return this;
  }

  @Override
  public <T> Bus send(String address, JsonElement msg, Handler<Message<T>> replyHandler) {
    sendOrPub(true, address, msg, replyHandler);
    return this;
  }

  @Override
  public Bus unregisterHandler(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.get(address); // List<Handler<Message>>
    if (handlers != null) {
      int idx = handlers.indexOf(handler);
      if (idx != -1) {
        handlers.remove(idx);
      }
      if (handlers.length() == 0) {
        handlerMap.remove(address);
      }
    }
    return this;
  }

  protected void checkNotNull(String paramName, Object param) {
    if (param == null) {
      throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }
  }

  protected void clearHandlers() {
    String[] keys = replyHandlers.keys();
    for (String key : keys) {
      replyHandlers.remove(key);
    }
    keys = handlerMap.keys();
    for (String key : keys) {
      JsonArray handlers = handlerMap.getArray(key);
      for (int i = handlers.length() - 1; i >= 0; i--) {
        handlers.remove(i);
      }
      handlerMap.remove(key);
    }
  }

  protected void deliverMessage(String address, Message message) {
    JsonArray handlers = handlerMap.get(address);
    if (handlers != null) {
      // We make a copy since the handler might get unregistered from within the
      // handler itself, which would screw up our iteration
      // JsonArray copy = new ArrayList<Handler<Message>>(handlers);
      for (int i = 0, len = handlers.length(); i < len; i++) {
        nativeHandle(message, handlers.get(i));
      }
    } else {
      // Might be a reply message
      Object handler = replyHandlers.get(address);
      if (handler != null) {
        replyHandlers.remove(address);
        nativeHandle(message, handler);
      }
    }
  }

  protected String makeUUID() {
    return idGenerator.next(36);
  }

  @SuppressWarnings("unchecked")
  protected <T> void nativeHandle(T message, Object handler) {
    ((Handler<T>) handler).handle(message);
  }

  @SuppressWarnings("unchecked")
  protected void sendOrPub(boolean send, String address, JsonElement msg, Object replyHandler) {
    checkNotNull("address", address);
    String replyAddress = null;
    if (replyHandler != null) {
      replyAddress = makeUUID();
      replyHandlers.set(replyAddress, replyHandler);
    }
    deliverMessage(address, new DefaultMessage(false, this, address, replyAddress, msg));
    return;
  }
}