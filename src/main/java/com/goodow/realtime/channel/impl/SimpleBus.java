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
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class SimpleBus implements Bus {
  public static abstract class BusProxy implements Bus {
    protected final SimpleBus delegate;
    protected BusHook hook;

    public BusProxy(SimpleBus delegate) {
      this.delegate = delegate;
    }

    @Override
    public void close() {
      delegate.close();
    }

    @Override
    public State getReadyState() {
      return delegate.getReadyState();
    }

    @Override
    public SimpleBus publish(String address, Object msg) {
      return delegate.publish(address, msg);
    }

    @Override
    public HandlerRegistration registerHandler(String address, Handler<? extends Message> handler) {
      return delegate.registerHandler(address, handler);
    }

    @Override
    public <T> SimpleBus send(String address, Object msg, Handler<Message<T>> replyHandler) {
      return delegate.send(address, msg, replyHandler);
    }

    @Override
    public Bus setHook(BusHook hook) {
      this.hook = hook;
      return this;
    }
  }

  public static final String MODE_MIX = "forkLocal";
  private static final Logger log = Logger.getLogger(SimpleBus.class.getName());

  protected static void checkNotNull(String paramName, Object param) {
    if (param == null) {
      throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }
  }

  protected final JsonObject handlerMap; // LinkedHashMap<String, LinkedHashSet<Handler<Message>>>
  protected final JsonObject replyHandlers; // LinkedHashMap<String, Handler<Message>>
  private final IdGenerator idGenerator;
  private JsonObject options;
  private boolean forkLocal;
  protected State state = State.CONNECTING;
  protected BusHook hook;

  public SimpleBus() {
    this(null);
  }

  public SimpleBus(JsonObject options) {
    handlerMap = Json.createObject();
    replyHandlers = Json.createObject();
    idGenerator = new IdGenerator();
    state = State.OPEN;

    setOptions(options);
  }

  @Override
  public void close() {
    if (hook == null || hook.handlePreClose()) {
      doClose();
    }
  }

  public JsonObject getOptions() {
    return options == null ? null : options.copy();
  }

  @Override
  public State getReadyState() {
    return state;
  }

  @Override
  public SimpleBus publish(String address, Object msg) {
    internalHandleSendOrPub(false, address, msg, null);
    return this;
  }

  @Override
  public HandlerRegistration registerHandler(final String address,
      final Handler<? extends Message> handler) {
    doRegisterHandler(address, handler);
    return new HandlerRegistration() {
      @Override
      public void unregisterHandler() {
        doUnregisterHandler(address, handler);
      }
    };
  }

  @Override
  public <T> SimpleBus send(String address, Object msg, Handler<Message<T>> replyHandler) {
    internalHandleSendOrPub(true, address, msg, replyHandler);
    return this;
  }

  @Override
  public SimpleBus setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }

  public void setOptions(JsonObject options) {
    this.options = options;
    forkLocal = options == null || !options.has(MODE_MIX) ? false : options.getBoolean(MODE_MIX);
  }

  protected void clearHandlers() {
    replyHandlers.clear();
    handlerMap.clear();
  }

  protected void doClose() {
    state = State.CLOSING;
    doReceiveMessage(new DefaultMessage<Void>(false, null, LOCAL_ON_CLOSE, null, null));
    state = State.CLOSED;
    clearHandlers();
    if (hook != null) {
      hook.handlePostClose();
    }
  }

  protected void doReceiveMessage(final Message message) {
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

  protected boolean doRegisterHandler(String address, Handler<? extends Message> handler) {
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

  @SuppressWarnings("unchecked")
  protected <T> void doSendOrPub(boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    checkNotNull("address", address);
    String replyAddress = null;
    if (replyHandler != null) {
      replyAddress = makeUUID();
    }
    boolean isLocal = isLocalFork(address);
    DefaultMessage message =
        new DefaultMessage(send, this, isLocal ? address.substring(LOCAL.length()) : address,
            isLocal && replyHandler != null ? (LOCAL + replyAddress) : replyAddress, msg);
    if (internalHandleReceiveMessage(message) && replyHandler != null) {
      replyHandlers.set(replyAddress, replyHandler);
    }
  }

  protected boolean doUnregisterHandler(String address, Handler<? extends Message> handler) {
    assert address != null : "address shouldn't be null";
    assert handler != null : "handler shouldn't be null";
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

  protected boolean internalHandleReceiveMessage(Message message) {
    if (hook == null || hook.handleReceiveMessage(message)) {
      doReceiveMessage(message);
      return true;
    }
    return false;
  }

  protected boolean isLocalFork(String address) {
    assert address != null : "address shouldn't be null";
    return forkLocal && address.startsWith(LOCAL);
  }

  protected String makeUUID() {
    return idGenerator.next(36);
  }

  protected void scheduleHandle(final String address, final Object handler, final Object event) {
    Platform.scheduler().scheduleDeferred(new Handler<Void>() {
      @Override
      public void handle(Void ignore) {
        try {
          Platform.scheduler().handle(handler, event);
        } catch (Throwable e) {
          log.log(Level.WARNING, "Failed to handle on address: " + address, e);
          doReceiveMessage(new DefaultMessage<JsonObject>(false, null, LOCAL_ON_ERROR, null, Json
              .createObject().set("address", address).set("event", event).set("cause", e)));
        }
      }
    });
  }

  private <T> void internalHandleSendOrPub(boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    if (hook == null || hook.handleSendOrPub(send, address, msg, replyHandler)) {
      doSendOrPub(send, address, msg, replyHandler);
    }
  }
}