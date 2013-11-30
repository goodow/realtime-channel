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
package com.goodow.realtime.channel;

import com.goodow.realtime.channel.util.IdGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.core.WebSocket.State;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class EventBus {
  public static interface EventBusHandler {
    void onClose();

    void onOpen();
  }

  public static final char LOCAL = '@';
  private static final Logger log = Logger.getLogger(EventBus.class.getName());
  private WebSocket webSocket = WebSocket.EMPTY;
  private final Map<String, List<EventHandler<JsonElement>>> handlerMap;
  private final Map<String, EventHandler<JsonElement>> replyHandlers;
  private State state = State.CONNECTING;
  private String sessionID;
  private int pingTimerID = 0;
  private int pingInterval;
  private EventBusHandler eventBusHandler;
  private final IdGenerator idGenerator;

  public EventBus() {
    handlerMap = new HashMap<String, List<EventHandler<JsonElement>>>();
    replyHandlers = new HashMap<String, EventHandler<JsonElement>>();
    idGenerator = new IdGenerator();
  }

  public EventBus(String url, JsonObject options) {
    this();
    webSocket = Platform.get().net().createWebSocket(url, options);
    if (options != null) {
      pingInterval = options.get("vertxbus_ping_interval");
    }
    if (pingInterval == 0) {
      pingInterval = 5 * 1000;
    }

    webSocket.setListen(new WebSocket.WebSocketHandler() {
      @Override
      public void onClose() {
        state = State.CLOSED;
        fireEvent(false);
      }

      @Override
      public void onError(String error) {
        log.warning(error);
      }

      @Override
      public void onMessage(String message) {
        JsonObject json = Json.parseObject(message);
        JsonElement body = json.get("body");
        final String replyAddress = json.getString("replyAddress");
        String address = json.getString("address");
        EventHandler<JsonElement> replyHandler = null;
        if (replyAddress != null) {
          replyHandler = new EventHandler<JsonElement>() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public void handler(JsonElement reply, EventHandler replyHandler) {
              // Send back reply
              send(replyAddress, reply, replyHandler);
            }
          };
        }
        deliverMessage(address, body, replyHandler);
      }

      @Override
      public void onOpen() {
        // Send the first ping then send a ping every 5 seconds
        sendPing();
        pingTimerID = Platform.get().setPeriodic(5000, new Handler<Integer>() {
          @Override
          public void handle(Integer event) {
            sendPing();
          }
        });
        state = State.OPEN;
        fireEvent(true);
      }
    });
  }

  public void close() {
    checkOpen();
    if (pingTimerID > 0) {
      Platform.get().cancelTimer(pingTimerID);
    }
    state = State.CLOSING;
    webSocket.close();
  }

  public State getReadyState() {
    return state;
  }

  public void login(String username, String password, final EventHandler<JsonObject> replyHandler) {
    JsonObject message = Json.createObject();
    message.set("username", username);
    message.set("password", password);
    sendOrPub("send", "vertx.basicauthmanager.login", message, new EventHandler<JsonObject>() {
      @Override
      public void handler(JsonObject reply, EventHandler<JsonObject> _replyHandler) {
        if ("ok".equals(reply.getString("status"))) {
          sessionID = reply.getString("sessionID");
        }
        if (replyHandler != null) {
          reply.remove("sessionID");
          replyHandler.handler(reply, null);
        }
      }
    });
  }

  public void publish(String address, JsonElement message) {
    sendOrPub("publish", address, message, null);
  }

  @SuppressWarnings("unchecked")
  public <T extends JsonElement> void registerHandler(String address, EventHandler<T> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    if (address.charAt(0) != LOCAL) {
      checkOpen();
    }
    List<EventHandler<JsonElement>> handlers = handlerMap.get(address);
    if (handlers == null) {
      handlers = new ArrayList<EventHandler<JsonElement>>();
      handlers.add((EventHandler<JsonElement>) handler);
      handlerMap.put(address, handlers);
      if (address.charAt(0) != LOCAL) {
        // First handler for this address so we should register the connection
        JsonObject msg = Json.createObject();
        msg.set("type", "register");
        msg.set("address", address);
        webSocket.send(msg.toJsonString());
      }
    } else {
      handlers.add((EventHandler<JsonElement>) handler);
    }
  }

  public <T extends JsonElement> void send(String address, JsonElement message,
      EventHandler<T> replyHandler) {
    sendOrPub("send", address, message, replyHandler);
  }

  public void setListener(EventBusHandler handler) {
    this.eventBusHandler = handler;
  }

  public <T extends JsonElement> void unregisterHandler(String address, EventHandler<T> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    if (address.charAt(0) != LOCAL) {
      checkOpen();
    }
    List<EventHandler<JsonElement>> handlers = handlerMap.get(address);
    if (handlers != null) {
      handlers.remove(handler);
      if (handlers.isEmpty()) {
        // No more local handlers so we should unregister the connection
        if (address.charAt(0) != LOCAL) {
          JsonObject msg = Json.createObject();
          msg.set("type", "unregister");
          msg.set("address", address);
          webSocket.send(msg.toJsonString());
        }
        handlerMap.remove(address);
      }
    }
  }

  // @formatter:off
  private native void __jsniFireEvent(boolean isOpen) /*-{
    isOpen ? (this.__gwtex_wrap.onopen && this.__gwtex_wrap.onopen()) :
        (this.__gwtex_wrap.onclose && this.__gwtex_wrap.onclose());
  }-*/;
  // @formatter:on

  private void checkNotNull(String paramName, Object param) {
    if (param == null) {
      throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }
  }

  private void checkOpen() {
    if (state != State.OPEN) {
      throw new IllegalStateException("INVALID_STATE_ERR");
    }
  }

  private void deliverMessage(String address, JsonElement body, EventHandler<JsonElement> replyTo) {
    List<EventHandler<JsonElement>> handlers = handlerMap.get(address);
    if (handlers != null) {
      // We make a copy since the handler might get unregistered from within the
      // handler itself, which would screw up our iteration
      List<EventHandler<JsonElement>> copy = new ArrayList<EventHandler<JsonElement>>(handlers);
      for (EventHandler<JsonElement> handler : copy) {
        handler.handler(body, replyTo);
      }
    } else {
      // Might be a reply message
      EventHandler<JsonElement> handler = replyHandlers.get(address);
      if (handler != null) {
        replyHandlers.remove(address);
        handler.handler(body, replyTo);
      }
    }
  }

  private void fireEvent(boolean isOpen) {
    switch (Platform.get().type()) {
      case HTML:
        __jsniFireEvent(isOpen);
        break;
      default:
        if (eventBusHandler != null) {
          if (isOpen) {
            eventBusHandler.onOpen();
          } else {
            eventBusHandler.onClose();
          }
        }
    }
  }

  private String makeUUID() {
    return idGenerator.next(36);
  }

  @SuppressWarnings("unchecked")
  private void sendOrPub(String sendOrPub, String address, JsonElement message,
      EventHandler<? extends JsonElement> replyHandler) {
    checkNotNull("address", address);
    if (address.charAt(0) == LOCAL) {
      sendOrPubLocal(address.substring(1), message, replyHandler);
      return;
    }
    checkOpen();
    JsonObject envelope = Json.createObject();
    envelope.set("type", sendOrPub);
    envelope.set("address", address);
    envelope.set("body", message);
    if (sessionID != null) {
      envelope.set("sessionID", sessionID);
    }
    if (replyHandler != null) {
      String replyAddress = makeUUID();
      envelope.set("replyAddress", replyAddress);
      replyHandlers.put(replyAddress, (EventHandler<JsonElement>) replyHandler);
    }
    webSocket.send(envelope.toJsonString());
  }

  @SuppressWarnings("unchecked")
  private void sendOrPubLocal(String address, JsonElement message,
      EventHandler<? extends JsonElement> replyHandler) {
    EventHandler<JsonElement> replyTo = null;
    if (replyHandler != null) {
      final String replyAddress = makeUUID();
      replyHandlers.put(replyAddress, (EventHandler<JsonElement>) replyHandler);
      replyTo = new EventHandler<JsonElement>() {
        @SuppressWarnings({"rawtypes"})
        @Override
        public void handler(JsonElement reply, EventHandler replyHandler) {
          // Send back reply
          sendOrPubLocal(replyAddress, reply, replyHandler);
        }
      };
    }
    JsonElement body =
        message.isArray() ? Json.parseArray(message.toJsonString()) : Json.parseObject(message
            .toJsonString());
    deliverMessage(address, body, replyTo);
  }

  private void sendPing() {
    JsonObject msg = Json.createObject();
    msg.set("type", "ping");
    webSocket.send(msg.toJsonString());
  }
}