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

import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class WebSocketBusClient extends SimpleBus {
  private static final Logger log = Logger.getLogger(WebSocketBusClient.class.getName());
  private final String url;
  private final JsonObject options;
  private final int pingInterval;
  private final WebSocket.WebSocketHandler webSocketHandler;
  private WebSocket webSocket;
  private String sessionID;
  private int pingTimerID = 0;
  private boolean reconnect = true;

  public WebSocketBusClient(String url, JsonObject options) {
    super(options);
    state = State.CONNECTING;
    this.url = url;
    this.options = options;
    pingInterval =
        options != null && options.has("vertxbus_ping_interval") ? (int) options
            .getNumber("vertxbus_ping_interval") : 5 * 1000;

    webSocketHandler = new WebSocket.WebSocketHandler() {
      @Override
      public void onClose(JsonObject reason) {
        state = State.CLOSED;
        assert pingTimerID > 0 : "pingTimerID should > 0";
        Platform.cancelTimer(pingTimerID);
        deliverMessage(LOCAL_ON_CLOSE, new DefaultMessage<JsonObject>(false, null, LOCAL_ON_CLOSE,
            null, reason));
        if (reconnect) {
          reconnect();
        }
      }

      @Override
      public void onError(String error) {
        reconnect = false;
        deliverMessage(LOCAL_ON_ERROR, new DefaultMessage<JsonObject>(false, null, LOCAL_ON_ERROR,
            null, Json.createObject().set("message", error)));
      }

      @Override
      public void onMessage(String msg) {
        JsonObject json = Json.parse(msg);
        String address = json.getString("address");
        @SuppressWarnings({"unchecked"})
        DefaultMessage message =
            new DefaultMessage(false, WebSocketBusClient.this, address, json
                .getString("replyAddress"), json.get("body"));
        deliverMessage(address, message);
      }

      @Override
      public void onOpen() {
        // Send the first ping then send a ping every 5 seconds
        state = State.OPEN;
        reconnect = true;
        sendPing();
        pingTimerID = Platform.setPeriodic(pingInterval, new Handler<Void>() {
          @Override
          public void handle(Void ignore) {
            sendPing();
          }
        });
        String[] keys = handlerMap.keys();
        for (String key : keys) {
          assert handlerMap.getArray(key).length() > 0 : "Handlers registried on " + key
              + " shouldn't be empty";
          sendRegister(key);
        }
        deliverMessage(LOCAL_ON_OPEN, new DefaultMessage<Void>(false, null, LOCAL_ON_OPEN, null,
            null));
      }
    };

    reconnect();
  }

  @Override
  public void close() {
    state = State.CLOSING;
    reconnect = false;
    webSocket.close();
    registerHandler(LOCAL_ON_CLOSE, new Handler<Message>() {
      @Override
      public void handle(Message event) {
        clearHandlers();
      }
    });
  }

  public void login(String username, String password, final Handler<JsonObject> replyHandler) {
    JsonObject msg = Json.createObject().set("username", username).set("password", password);
    sendOrPub(true, "vertx.basicauthmanager.login", msg, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        if ("ok".equals(body.getString("status"))) {
          sessionID = body.getString("sessionID");
        }
        if (replyHandler != null) {
          scheduleHandle(body.remove("sessionID"), replyHandler);
        }
      }
    });
  }

  public void reconnect() {
    if (state == State.OPEN) {
      return;
    }
    if (webSocket != null) {
      webSocket.close();
    }

    state = State.CONNECTING;
    replyHandlers.clear();
    webSocket = Platform.net().createWebSocket(url, options);
    webSocket.setListen(webSocketHandler);
  }

  @Override
  // J2ObjC prevent us to override doRegisterHandler
  public HandlerRegistration registerHandler(final String address,
      final Handler<? extends Message> handler) {
    boolean first = super.doRegisterHandler(address, handler);
    if (first) {
      // First handler for this address so we should register the connection
      sendRegister(address);
    }
    return new HandlerRegistration() {
      @Override
      public void unregisterHandler() {
        doUnregisterHandler(address, handler);
      }
    };
  }

  @Override
  protected boolean doUnregisterHandler(String address, Handler<? extends Message> handler) {
    boolean last = super.doUnregisterHandler(address, handler);
    if (last && !super.isLocalFork(address)) {
      // No more handlers so we should unregister the connection
      JsonObject msg = Json.createObject().set("type", "unregister").set("address", address);
      send(msg.toJsonString());
    }
    return last;
  }

  @Override
  protected void sendOrPub(boolean send, String address, Object msg, Object replyHandler) {
    checkNotNull("address", address);
    if (super.isLocalFork(address)) {
      super.sendOrPub(send, address, msg, replyHandler);
      return;
    }
    if (state != State.OPEN) {
      throw new IllegalStateException("INVALID_STATE_ERR");
    }
    JsonObject envelope = Json.createObject().set("type", send ? "send" : "publish");
    envelope.set("address", address).set("body", msg);
    if (sessionID != null) {
      envelope.set("sessionID", sessionID);
    }
    if (replyHandler != null) {
      String replyAddress = makeUUID();
      envelope.set("replyAddress", replyAddress);
      replyHandlers.set(replyAddress, replyHandler);
    }
    send(envelope.toJsonString());
  }

  private void send(String msg) {
    if (state == State.OPEN) {
      webSocket.send(msg);
    } else {
      log.warning("WebSocket is in " + state + " state. Cannot send: " + msg);
    }
  }

  private void sendPing() {
    send(Json.createObject().set("type", "ping").toJsonString());
  }

  private void sendRegister(String address) {
    assert address != null : "address shouldn't be null";
    if (super.isLocalFork(address)) {
      return;
    }
    JsonObject msg = Json.createObject().set("type", "register").set("address", address);
    send(msg.toJsonString());
  }
}