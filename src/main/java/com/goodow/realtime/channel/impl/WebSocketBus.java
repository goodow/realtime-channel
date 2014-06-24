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
  public static final String SESSION = "_session";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String PING_INTERVAL = "vertxbus_ping_interval";
  public static final String TOPIC_CHANNEL = "realtime/channel";

  protected static final String BODY = "body";
  protected static final String TOPIC = "address";
  protected static final String REPLY_TOPIC = "replyAddress";
  protected static final String TYPE = "type";

  private final WebSocket.WebSocketHandler webSocketHandler;
  String serverUri;
  WebSocket webSocket;
  private int pingInterval;
  private int pingTimerID = -1;
  private String sessionId;
  private String username;
  private String password;
  final JsonObject handlerCount = Json.createObject();

  public WebSocketBus(String serverUri, JsonObject options) {
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
        MessageImpl message =
            new MessageImpl(false, false, WebSocketBus.this, json.getString(TOPIC), json
                .getString(REPLY_TOPIC), json.get(BODY));
        internalHandleReceiveMessage(false, message);
      }

      @Override
      public void onOpen() {
        sendConnect();
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

    connect(serverUri, options);
  }

  public void connect(String serverUri, JsonObject options) {
    this.serverUri = serverUri;
    pingInterval =
        options == null || !options.has(PING_INTERVAL) ? 5 * 1000 : (int) options
            .getNumber(PING_INTERVAL);
    sessionId = options == null || !options.has(SESSION) ? idGenerator.next(23) : options.getString(
        SESSION);
    username = options == null || !options.has(USERNAME) ? null : options.getString(USERNAME);
    password = options == null || !options.has(PASSWORD) ? null : options.getString(PASSWORD);

    webSocket = Platform.net().createWebSocket(serverUri, options);
    webSocket.setListen(webSocketHandler);
  }

  @Override
  public State getReadyState() {
    return webSocket.getReadyState();
  }

  @Override
  public String getSessionId() {
    return sessionId;
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
  protected boolean doRegisterHandler(boolean local, String topic,
      Handler<? extends Message> handler) {
    boolean registered = super.doRegisterHandler(local, topic, handler);
    if (local || !registered || (hook != null && !hook.handlePreRegister(topic, handler))) {
      return false;
    }
    if (handlerCount.has(topic)) {
      handlerCount.set(topic, handlerCount.getNumber(topic) + 1);
      return false;
    }
    handlerCount.set(topic, 1);
    sendRegister(topic);
    return true;
  }

  @Override
  protected <T> void doSendOrPub(boolean local, boolean send, String topic, Object msg,
      Handler<Message<T>> replyHandler) {
    checkNotNull(TOPIC, topic);
    if (local) {
      super.doSendOrPub(local, send, topic, msg, replyHandler);
      return;
    }
    JsonObject envelope = Json.createObject().set(TYPE, send ? "send" : "publish");
    envelope.set(TOPIC, topic).set(BODY, msg);
    if (replyHandler != null) {
      String replyTopic = makeUUID();
      envelope.set(REPLY_TOPIC, replyTopic);
      replyHandlers.set(replyTopic, replyHandler);
    }
    send(envelope);
  }

  @Override
  protected boolean doUnregisterHandler(boolean local, String topic,
      Handler<? extends Message> handler) {
    boolean unregistered = super.doUnregisterHandler(local, topic, handler);
    if (local || !unregistered || (hook != null && !hook.handleUnregister(topic))) {
      return false;
    }
    handlerCount.set(topic, handlerCount.getNumber(topic) - 1);
    if (handlerCount.getNumber(topic) == 0) {
      handlerCount.remove(topic);
      sendUnregister(topic);
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

  protected void sendConnect() {
    JsonObject msg = Json.createObject().set(SESSION, sessionId);
    if(username != null) {
      msg.set(USERNAME, username);
      if(password != null) {
        msg.set(PASSWORD, password);
      }
    }
    send(TOPIC_CHANNEL + "/_CONNECT", msg, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        if (0 != message.body().getNumber("code")) {
          throw new RuntimeException(message.body().toJsonString());
        }
      }
    });
  }

  protected void sendPing() {
    send(Json.createObject().set(TYPE, "ping"));
  }

  /*
   * First handler for this topic so we should register the connection
   */
  protected void sendRegister(String topic) {
    assert topic != null : "topic shouldn't be null";
    JsonObject msg = Json.createObject().set(TYPE, "register").set(TOPIC, topic);
    send(msg);
  }

  /*
   * No more handlers so we should unregister the connection
   */
  protected void sendUnregister(String topic) {
    JsonObject msg = Json.createObject().set(TYPE, "unregister").set(TOPIC, topic);
    send(msg);
  }
}