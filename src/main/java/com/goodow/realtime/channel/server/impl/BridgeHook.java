/*
 * Copyright 2014 Goodow.com
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
package com.goodow.realtime.channel.server.impl;

import com.goodow.realtime.channel.impl.WebSocketBus;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BridgeHook implements EventBusBridgeHook {
  public static final String TOPIC = "topic";
  public static final String SESSION_WATCH_ADDR =
      WebSocketBus.ADDR + "/" + WebSocketBus.SESSION + "/_watch";
  private final EventBus eb;
  private final Map<String, String> connections = new HashMap<String, String>();
  private final SharedData sharedData;

  public static String getSessionsKey(String address) {
    return WebSocketBus.ADDR + "/" + address + "/" + WebSocketBus.SESSION;
  }

  public BridgeHook(Vertx vertx) {
    this.eb = vertx.eventBus();
    this.sharedData = vertx.sharedData();
  }

  @Override
  public void handlePostRegister(SockJSSocket sock, String address) {
    Set<String> sessionIds = sharedData.getSet(getSessionsKey(address));
    String sessionId = connections.get(sock.writeHandlerID());
    sessionIds.add(sessionId);
    publishPresence(address, sessionId, true);
  }

  @Override
  public boolean handlePreRegister(SockJSSocket sock, String address) {
    return true;
  }

  @Override
  public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg,
      final String address) {
    if (address.equals(WebSocketBus.ADDR + "/_CONNECT")) {
      connections.put(sock.writeHandlerID(), msg.getObject("body").getString(WebSocketBus.SESSION));
    } else if (msg.getValue("body") instanceof JsonObject) {
      JsonObject body = msg.getObject("body");
      if (!body.containsField(WebSocketBus.SESSION)) {
        body.putString(WebSocketBus.SESSION, connections.get(sock.writeHandlerID()));
      }
    }
    return true;
  }

  @Override
  public void handleSocketClosed(SockJSSocket sock) {
    connections.remove(sock.writeHandlerID());
  }

  @Override
  public boolean handleSocketCreated(SockJSSocket sock) {
    return true;
  }

  @Override
  public boolean handleUnregister(SockJSSocket sock, String address) {
    Set<String> sessionIds = sharedData.getSet(getSessionsKey(address));
    String sessionId = connections.get(sock.writeHandlerID());
    sessionIds.remove(sessionId);
    publishPresence(address, sessionId, false);
    return true;
  }

  @Override
  public boolean handleAuthorise(JsonObject message, String sessionID,
                                 Handler<AsyncResult<Boolean>> handler) {
    return false;
  }

  private void publishPresence(final String address, String sessionId, final boolean isJoined) {
    JsonObject msg = new JsonObject().putString(WebSocketBus.SESSION, sessionId).putBoolean(
        "isJoined", isJoined).putString(TOPIC, address);
    eb.publish(SESSION_WATCH_ADDR, msg);
  }
}