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
import com.goodow.realtime.core.VoidHandler;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class WebSocketBusClient extends SimpleBus {
  private static final Logger log = Logger.getLogger(WebSocketBusClient.class.getName());
  private WebSocket webSocket = WebSocket.EMPTY;
  private String sessionID;
  private int pingTimerID = 0;
  private int pingInterval;

  public WebSocketBusClient(String url, JsonObject options) {
    webSocket = Platform.get().net().createWebSocket(url, options);
    if (options != null) {
      pingInterval = (int) options.getNumber("vertxbus_ping_interval");
    }
    if (pingInterval == 0) {
      pingInterval = 5 * 1000;
    }

    webSocket.setListen(new WebSocket.WebSocketHandler() {
      @Override
      public void onClose() {
        state = State.CLOSED;
        WebSocketBusClient.this.publish(Bus.LOCAL + Bus.LOCAL_ON_CLOSE, null);
      }

      @Override
      public void onError(String error) {
        log.warning(error);
        WebSocketBusClient.this.publish(Bus.LOCAL + Bus.LOCAL_ON_ERROR, null);
      }

      @Override
      public void onMessage(String msg) {
        JsonObject json = Json.parse(msg);
        final String replyAddress = json.getString("replyAddress");
        String address = json.getString("address");
        @SuppressWarnings({"unchecked"})
        DefaultMessage message =
            new DefaultMessage(false, WebSocketBusClient.this, address, replyAddress, json
                .get("body"));
        deliverMessage(address, message);
      }

      @Override
      public void onOpen() {
        // Send the first ping then send a ping every 5 seconds
        sendPing();
        pingTimerID = Platform.get().setPeriodic(5000, new VoidHandler() {
          @Override
          protected void handle() {
            sendPing();
          }
        });
        state = State.OPEN;
        WebSocketBusClient.this.publish(Bus.LOCAL + Bus.LOCAL_ON_OPEN, null);
      }
    });
  }

  @Override
  public void close() {
    checkOpen();
    if (pingTimerID > 0) {
      Platform.get().cancelTimer(pingTimerID);
    }
    state = State.CLOSING;
    webSocket.close();
    registerHandler(LOCAL_ON_CLOSE, new Handler<Message>() {
      @Override
      public void handle(Message event) {
        clearHandlers();
      }
    });
  }

  public void login(String username, String password, final Handler<JsonObject> replyHandler) {
    JsonObject msg = Json.createObject();
    msg.set("username", username);
    msg.set("password", password);
    sendOrPub(true, "vertx.basicauthmanager.login", msg, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        if ("ok".equals(body.getString("status"))) {
          sessionID = body.getString("sessionID");
        }
        if (replyHandler != null) {
          body.remove("sessionID");
          nativeHandle(body, replyHandler);
        }
      }
    });
  }

  @Override
  public Bus registerHandler(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    if (address.charAt(0) != Bus.LOCAL) {
      checkOpen();
    }
    JsonArray handlers = handlerMap.getArray(address);
    if (handlers == null) {
      handlers = Json.createArray();
      handlers.push(handler);
      handlerMap.set(address, handlers);
      if (address.charAt(0) != Bus.LOCAL) {
        // First handler for this address so we should register the connection
        JsonObject msg = Json.createObject();
        msg.set("type", "register");
        msg.set("address", address);
        webSocket.send(msg.toJsonString());
      }
    } else if (handlers.indexOf(handler) == -1) {
      handlers.push(handler);
    }
    return this;
  }

  @Override
  public Bus unregisterHandler(String address, Handler<? extends Message> handler) {
    checkNotNull("address", address);
    checkNotNull("handler", handler);
    if (address.charAt(0) != Bus.LOCAL) {
      checkOpen();
    }
    JsonArray handlers = handlerMap.get(address); // List<Handler<Message>>
    if (handlers != null) {
      int idx = handlers.indexOf(handler);
      if (idx != -1) {
        handlers.remove(idx);
      }
      if (handlers.length() == 0) {
        // No more local handlers so we should unregister the connection
        if (address.charAt(0) != Bus.LOCAL) {
          JsonObject msg = Json.createObject();
          msg.set("type", "unregister");
          msg.set("address", address);
          webSocket.send(msg.toJsonString());
        }
        handlerMap.remove(address);
      }
    }
    return this;
  }

  @Override
  protected void sendOrPub(boolean send, String address, JsonElement msg, Object replyHandler) {
    checkNotNull("address", address);
    if (address.charAt(0) == Bus.LOCAL) {
      sendOrPubLocal(send, address.substring(1), msg, replyHandler);
      return;
    }
    checkOpen();
    JsonObject envelope = Json.createObject();
    envelope.set("type", send ? "send" : "publish");
    envelope.set("address", address);
    envelope.set("body", msg);
    if (sessionID != null) {
      envelope.set("sessionID", sessionID);
    }
    if (replyHandler != null) {
      String replyAddress = makeUUID();
      envelope.set("replyAddress", replyAddress);
      replyHandlers.set(replyAddress, replyHandler);
    }
    webSocket.send(envelope.toJsonString());
  }

  private void checkOpen() {
    if (state != State.OPEN) {
      throw new IllegalStateException("INVALID_STATE_ERR");
    }
  }

  @SuppressWarnings({"unchecked"})
  private void sendOrPubLocal(boolean send, String address, JsonElement msg, Object replyHandler) {
    String replyAddress = null;
    if (replyHandler != null) {
      replyAddress = makeUUID();
      replyHandlers.set(replyAddress, replyHandler);
    }
    deliverMessage(address, new DefaultMessage(false, this, address, replyAddress == null ? null
        : (LOCAL + replyAddress), msg));
  }

  private void sendPing() {
    JsonObject msg = Json.createObject();
    msg.set("type", "ping");
    webSocket.send(msg.toJsonString());
  }
}