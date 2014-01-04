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
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.VoidHandler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

@SuppressWarnings("rawtypes")
public class SimpleBus implements Bus {
  protected final JsonObject handlerMap; // LinkedHashMap<String, LinkedHashSet<Handler<Message>>>
  protected final JsonObject replyHandlers; // LinkedHashMap<String, Handler<Message>>
  private final IdGenerator idGenerator;
  private final boolean forkLocal;
  protected State state = State.CONNECTING;

  public SimpleBus() {
    this(null);
  }

  public SimpleBus(JsonObject options) {
    handlerMap = Json.createObject();
    replyHandlers = Json.createObject();
    idGenerator = new IdGenerator();
    state = State.OPEN;

    forkLocal =
        options != null && options.has("forkLocal") ? options.getBoolean("forkLocal") : false;
  }

  @Override
  public void close() {
    state = State.CLOSING;
    deliverMessage(Bus.LOCAL_ON_CLOSE, new DefaultMessage<Void>(false, null, Bus.LOCAL_ON_CLOSE,
        null, null));
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
    registerHandlerImpl(address, handler);
    return this;
  }

  @Override
  public <T> Bus send(String address, JsonElement msg, Handler<Message<T>> replyHandler) {
    sendOrPub(true, address, msg, replyHandler);
    return this;
  }

  @Override
  public Bus unregisterHandler(String address, Handler<? extends Message> handler) {
    unregisterHandlerImpl(address, handler);
    return this;
  }

  protected void checkNotNull(String paramName, Object param) {
    if (param == null) {
      throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }
  }

  protected void clearHandlers() {
    replyHandlers.clear();
    handlerMap.clear();
  }

  protected void deliverMessage(String address, Message message) {
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers != null) {
      // We make a copy since the handler might get unregistered from within the
      // handler itself, which would screw up our iteration
      // JsonArray copy = new ArrayList<Handler<Message>>(handlers);
      for (int i = 0, len = handlers.length(); i < len; i++) {
        scheduleHandle(message, handlers.get(i));
      }
    } else {
      // Might be a reply message
      Object handler = replyHandlers.get(address);
      if (handler != null) {
        replyHandlers.remove(address);
        scheduleHandle(message, handler);
      }
    }
  }

  protected boolean isLocalFork(String address) {
    assert address != null;
    return forkLocal && !address.isEmpty() && address.charAt(0) == Bus.LOCAL;
  }

  protected String makeUUID() {
    return idGenerator.next(36);
  }

  @SuppressWarnings("unchecked")
  protected <T> void nativeHandle(T message, Object handler) {
    ((Handler<T>) handler).handle(message);
  }

  protected boolean registerHandlerImpl(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers == null) {
      handlerMap.set(address, Json.createArray().push(handler));
      return true;
    } else if (handlers.indexOf(handler) == -1) {
      handlers.push(handler);
    }
    return false;
  }

  protected void scheduleHandle(final Object message, final Object handler) {
    Platform.scheduleDeferred(new VoidHandler() {
      @Override
      protected void handle() {
        nativeHandle(message, handler);
      }
    });
  }

  @SuppressWarnings("unchecked")
  protected void sendOrPub(boolean send, String address, JsonElement msg, Object replyHandler) {
    checkNotNull("address", address);
    String replyAddress = null;
    if (replyHandler != null) {
      replyAddress = makeUUID();
      replyHandlers.set(replyAddress, replyHandler);
    }
    if (isLocalFork(address)) {
      address = address.substring(1);
      if (replyAddress != null) {
        replyAddress = LOCAL + replyAddress;
      }
    }
    deliverMessage(address, new DefaultMessage(send, this, address, replyAddress, msg));
  }

  protected boolean unregisterHandlerImpl(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers != null) {
      int idx = handlers.indexOf(handler);
      if (idx != -1) {
        handlers.remove(idx);
      }
      if (handlers.length() == 0) {
        handlerMap.remove(address);
        return true;
      }
    }
    return false;
  }
}