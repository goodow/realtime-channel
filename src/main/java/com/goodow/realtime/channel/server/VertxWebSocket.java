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
package com.goodow.realtime.channel.server;

import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.Json;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

class VertxWebSocket implements WebSocket {
  private static final Logger log = Logger.getLogger(VertxWebSocket.class.getName());

  private org.vertx.java.core.http.WebSocket socket;
  private WebSocketHandler eventHandler;

  VertxWebSocket(Vertx vertx, String uri) {
    URI serverUri = null;
    try {
      serverUri = new URI(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    HttpClient client =
        vertx.createHttpClient().setHost(serverUri.getHost()).setPort(serverUri.getPort());

    client.connectWebsocket(serverUri.getPath(), new Handler<org.vertx.java.core.http.WebSocket>() {
      @Override
      public void handle(org.vertx.java.core.http.WebSocket ws) {
        socket = ws;
        log.info("Websocket Connected");

        socket.closeHandler(new Handler<Void>() {
          @Override
          public void handle(Void event) {
            log.info("WebSocket closed");
            if (eventHandler == null) {
              return;
            }
            eventHandler.onClose(Json.createObject());
            socket = null;
          }
        });

        socket.dataHandler(new Handler<Buffer>() {
          @Override
          public void handle(Buffer buffer) {
            log.finest("Websocket received: " + buffer);
            if (eventHandler == null) {
              return;
            }
            eventHandler.onMessage(buffer.toString());
          }
        });

        socket.exceptionHandler(new Handler<Throwable>() {
          @Override
          public void handle(Throwable e) {
            log.log(Level.SEVERE, "Websocket Failed With Exception", e);
            if (eventHandler == null) {
              return;
            }
            String message = e.getMessage();
            eventHandler.onError(message == null ? e.getClass().getSimpleName() : message);
          }
        });

        if (eventHandler != null) {
          eventHandler.onOpen();
        }
      }
    });
  }

  @Override
  public void close() {
    if (socket != null) {
      socket.close();
    }
  }

  @Override
  public void send(String data) {
    if (socket == null) {
      log.warning("WebSocket is closed");
      return;
    }
    try {
      log.finest("Websocket send: " + data);
      socket.writeTextFrame(data);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setListen(WebSocketHandler handler) {
    this.eventHandler = handler;
  }
}