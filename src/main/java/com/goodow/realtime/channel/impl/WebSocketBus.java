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
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

@SuppressWarnings("rawtypes")
public class WebSocketBus extends SimpleBus {
  public static final String PING_INTERVAL = "vertxbus_ping_interval";
  public static final String AUTH_ADDRESS = "auth_address";

  protected static final String BODY = "body";
  protected static final String ADDRESS = "address";
  protected static final String REPLY_ADDRESS = "replyAddress";
  protected static final String TYPE = "type";

  private final WebSocket.WebSocketHandler webSocketHandler;
  String url;
  WebSocket webSocket;
  private int pingInterval;
  private String authAddress;
  private int pingTimerID = -1;
  private String accessToken;
  private String sid;
  final JsonObject handlerCount = Json.createObject();

  public WebSocketBus(String url, JsonObject options) {
    webSocketHandler = new WebSocket.WebSocketHandler() {
      @Override
      public void onClose(JsonObject reason) {
        Platform.scheduler().cancelTimer(pingTimerID);
        publishLocal(ON_CLOSE, reason);
        if (hook != null) {
          hook.handlePostClose();
        }
      }

      @Override
      public void onError(String error) {
        publishLocal(ON_ERROR, Json.createObject().set("message", error));
      }

      @Override
      public void onMessage(String msg) {
        JsonObject json = Json.<JsonObject> parse(msg);
        @SuppressWarnings({"unchecked"})
        DefaultMessage message =
            new DefaultMessage(false, false, WebSocketBus.this, json.getString(ADDRESS), json
                .getString(REPLY_ADDRESS), json.get(BODY));
        internalHandleReceiveMessage(false, message);
      }

      @Override
      public void onOpen() {
        // Send the first ping then send a ping every 5 seconds
        sendPing();
        pingTimerID = Platform.scheduler().schedulePeriodic(pingInterval, new Handler<Void>() {
          @Override
          public void handle(Void ignore) {
            sendPing();
          }
        });
        if (hook != null) {
          hook.handleOpened();
        }
        publishLocal(ON_OPEN, null);
      }
    };

    connect(url, options);
  }

  public void connect(String url, JsonObject options) {
    this.url = url;
    pingInterval =
        options == null || !options.has(PING_INTERVAL) ? 5 * 1000 : (int) options
            .getNumber(PING_INTERVAL);
    authAddress =
        options == null || !options.has(AUTH_ADDRESS) ? "realtime.auth" : options
            .getString(AUTH_ADDRESS);

    webSocket = Platform.net().createWebSocket(url, options);
    webSocket.setListen(webSocketHandler);
  }

  @Override
  public State getReadyState() {
    return webSocket.getReadyState();
  }

  public void login(String userId, String token, final Handler<JsonObject> replyHandler) {
    JsonObject msg = Json.createObject().set("userId", userId).set("token", token);
    send(authAddress + ".login", msg, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        if ("ok".equals(body.getString("status"))) {
          accessToken = body.getString("access_token");
          sid = body.getString("sid");
        }
        if (replyHandler != null) {
          body.remove("access_token");
          scheduleHandle(authAddress + ".login", replyHandler, body);
        }
      }
    });
  }

  @Override
  protected void doClose() {
    webSocket.close();
    registerLocalHandler(Bus.ON_CLOSE, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        clearHandlers();
        handlerCount.clear();
      }
    });
  }

  @Override
  protected boolean doRegisterHandler(boolean local, String address,
      Handler<? extends Message> handler) {
    boolean registered = super.doRegisterHandler(local, address, handler);
    if (local || !registered || (hook != null && !hook.handlePreRegister(address, handler))) {
      return false;
    }
    if (handlerCount.has(address)) {
      handlerCount.set(address, handlerCount.getNumber(address) + 1);
      return false;
    }
    handlerCount.set(address, 1);
    sendRegister(address);
    return true;
  }

  @Override
  protected <T> void doSendOrPub(boolean local, boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    checkNotNull(ADDRESS, address);
    if (local) {
      super.doSendOrPub(local, send, address, msg, replyHandler);
      return;
    }
    JsonObject envelope = Json.createObject().set(TYPE, send ? "send" : "publish");
    envelope.set(ADDRESS, address).set(BODY, msg);
    if (accessToken != null) {
      envelope.set("sessionID", accessToken);
    }
    if (sid != null) {
      envelope.set("sid", accessToken);
    }
    if (replyHandler != null) {
      String replyAddress = makeUUID();
      envelope.set(REPLY_ADDRESS, replyAddress);
      replyHandlers.set(replyAddress, replyHandler);
    }
    send(envelope);
  }

  @Override
  protected boolean doUnregisterHandler(boolean local, String address,
      Handler<? extends Message> handler) {
    boolean unregistered = super.doUnregisterHandler(local, address, handler);
    if (local || !unregistered || (hook != null && !hook.handleUnregister(address))) {
      return false;
    }
    handlerCount.set(address, handlerCount.getNumber(address) - 1);
    if (handlerCount.getNumber(address) == 0) {
      handlerCount.remove(address);
      sendUnregister(address);
      return true;
    }
    return false;
  }

  protected void send(JsonObject msg) {
    if (getReadyState() != State.OPEN) {
      throw new IllegalStateException("INVALID_STATE_ERR");
    }
    webSocket.send(msg.toJsonString());
  }

  protected void sendPing() {
    send(Json.createObject().set(TYPE, "ping"));
  }

  /*
   * First handler for this address so we should register the connection
   */
  protected void sendRegister(String address) {
    assert address != null : "address shouldn't be null";
    JsonObject msg = Json.createObject().set(TYPE, "register").set(ADDRESS, address);
    send(msg);
  }

  /*
   * No more handlers so we should unregister the connection
   */
  protected void sendUnregister(String address) {
    JsonObject msg = Json.createObject().set(TYPE, "unregister").set(ADDRESS, address);
    send(msg);
  }
}