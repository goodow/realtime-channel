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
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class WebSocketBus extends SimpleBus {
  public static final String PING_INTERVAL = "vertxbus_ping_interval";
  protected static final String BODY = "body";
  protected static final String ADDRESS = "address";
  protected static final String REPLY_ADDRESS = "replyAddress";
  protected static final String TYPE = "type";

  private static final Logger log = Logger.getLogger(WebSocketBus.class.getName());
  protected WebSocket webSocket;
  protected String url;
  private final WebSocket.WebSocketHandler webSocketHandler;
  private int pingInterval;
  private String sessionID;
  private int pingTimerID = -1;

  public WebSocketBus(String url, JsonObject options) {
    state = State.CONNECTING;

    webSocketHandler = new WebSocket.WebSocketHandler() {
      @Override
      public void onClose(JsonObject reason) {
        state = State.CLOSED;
        Platform.scheduler().cancelTimer(pingTimerID);
        doReceiveMessage(new DefaultMessage<JsonObject>(false, null, LOCAL_ON_CLOSE, null, reason));
        if (hook != null) {
          hook.handlePostClose();
        }
      }

      @Override
      public void onError(String error) {
        doReceiveMessage(new DefaultMessage<JsonObject>(false, null, LOCAL_ON_ERROR, null, Json
            .createObject().set("message", error)));
      }

      @Override
      public void onMessage(String msg) {
        JsonObject json = Json.<JsonObject> parse(msg);
        @SuppressWarnings({"unchecked"})
        DefaultMessage message =
            new DefaultMessage(false, WebSocketBus.this, json.getString(ADDRESS), json
                .getString(REPLY_ADDRESS), json.get(BODY));
        internalHandleReceiveMessage(message);
      }

      @Override
      public void onOpen() {
        state = State.OPEN;
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
        doReceiveMessage(new DefaultMessage<Void>(false, null, LOCAL_ON_OPEN, null, null));
      }
    };

    connect(url, options);
  }

  public void connect(String url, JsonObject options) {
    this.url = url;
    setOptions(options);
    pingInterval =
        options == null || !options.has(PING_INTERVAL) ? 5 * 1000 : (int) options
            .getNumber(PING_INTERVAL);

    state = State.CONNECTING;
    webSocket = Platform.net().createWebSocket(url, options);
    webSocket.setListen(webSocketHandler);
  }

  public void login(String username, String password, final Handler<JsonObject> replyHandler) {
    JsonObject msg = Json.createObject().set("username", username).set("password", password);
    final String addr = "vertx.basicauthmanager.login";
    doSendOrPub(true, addr, msg, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        if ("ok".equals(body.getString("status"))) {
          sessionID = body.getString("sessionID");
        }
        if (replyHandler != null) {
          scheduleHandle(addr, replyHandler, body.remove("sessionID"));
        }
      }
    });
  }

  @Override
  protected void doClose() {
    state = State.CLOSING;
    webSocket.close();
    registerHandler(LOCAL_ON_CLOSE, new Handler<Message>() {
      @Override
      public void handle(Message event) {
        clearHandlers();
      }
    });
  }

  @Override
  protected boolean doRegisterHandler(String address, Handler<? extends Message> handler) {
    boolean first = super.doRegisterHandler(address, handler);
    if (first && !isLocalFork(address)
        && (hook == null || hook.handlePreRegister(address, handler))) {
      sendRegister(address);
    }
    return first;
  }

  @Override
  protected <T> void doSendOrPub(boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler) {
    checkNotNull(ADDRESS, address);
    if (isLocalFork(address)) {
      super.doSendOrPub(send, address, msg, replyHandler);
      return;
    }
    if (state != State.OPEN) {
      throw new IllegalStateException("INVALID_STATE_ERR");
    }
    JsonObject envelope = Json.createObject().set(TYPE, send ? "send" : "publish");
    envelope.set(ADDRESS, address).set(BODY, msg);
    if (sessionID != null) {
      envelope.set("sessionID", sessionID);
    }
    if (replyHandler != null) {
      String replyAddress = makeUUID();
      envelope.set(REPLY_ADDRESS, replyAddress);
      replyHandlers.set(replyAddress, replyHandler);
    }
    send(envelope.toJsonString());
  }

  @Override
  protected boolean doUnregisterHandler(String address, Handler<? extends Message> handler) {
    boolean last = super.doUnregisterHandler(address, handler);
    if (last && !isLocalFork(address) && (hook == null || hook.handleUnregister(address))) {
      sendUnregister(address);
    }
    return last;
  }

  protected void send(String msg) {
    if (state == State.OPEN) {
      webSocket.send(msg);
    } else {
      log.warning("WebSocket is in " + state + " state. Cannot send: " + msg);
    }
  }

  protected void sendPing() {
    send(Json.createObject().set(TYPE, "ping").toJsonString());
  }

  /*
   * First handler for this address so we should register the connection
   */
  protected void sendRegister(String address) {
    assert address != null : "address shouldn't be null";
    JsonObject msg = Json.createObject().set(TYPE, "register").set(ADDRESS, address);
    send(msg.toJsonString());
  }

  /*
   * No more handlers so we should unregister the connection
   */
  protected void sendUnregister(String address) {
    JsonObject msg = Json.createObject().set(TYPE, "unregister").set(ADDRESS, address);
    send(msg.toJsonString());
  }
}