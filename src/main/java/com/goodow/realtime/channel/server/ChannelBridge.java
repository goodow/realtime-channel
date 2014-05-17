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

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSServer;

public class ChannelBridge {
  private final Vertx vertx;
  private final JsonObject config;
  private EventBusBridgeHook hook;

  public ChannelBridge(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  public void bridge(final CountingCompletionHandler<Void> countDownLatch) {
    HttpServer server = vertx.createHttpServer();
    SockJSServer sjsServer = vertx.createSockJSServer(server).setHook(hook);
    JsonObject empty = new JsonObject();
    JsonArray all = new JsonArray().add(empty);
    JsonArray inboundPermitted = config.getArray("inbound_permitted", all);
    JsonArray outboundPermitted = config.getArray("outbound_permitted", all);

    sjsServer.bridge(config.getObject("sjs_config", new JsonObject()
        .putString("prefix", "/channel")), inboundPermitted, outboundPermitted, config.getObject(
        "bridge_config", empty));

    countDownLatch.incRequired();
    server.listen(config.getInteger("port", 1986), config.getString("host", "0.0.0.0"),
        new AsyncResultHandler<HttpServer>() {
          @Override
          public void handle(AsyncResult<HttpServer> ar) {
            if (!ar.succeeded()) {
              countDownLatch.failed(ar.cause());
            } else {
              countDownLatch.complete();
            }
          }
        });
  }

  public ChannelBridge setHook(EventBusBridgeHook hook) {
    this.hook = hook;
    return this;
  }
}
