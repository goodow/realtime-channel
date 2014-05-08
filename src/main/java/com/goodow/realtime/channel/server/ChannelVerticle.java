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

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;

public class ChannelVerticle extends BusModBase {

  @Override
  public void start(final Future<Void> startedResult) {
    super.start();

    HttpServer server = vertx.createHttpServer();
    SockJSServer sjsServer = vertx.createSockJSServer(server);
    JsonArray inboundPermitted =
        getOptionalArrayConfig("inbound_permitted", new JsonArray().add(new JsonObject()));
    JsonArray outboundPermitted =
        getOptionalArrayConfig("outbound_permitted", new JsonArray().add(new JsonObject()));

    sjsServer.bridge(getOptionalObjectConfig("sjs_config", new JsonObject().putString("prefix",
        "/eventbus")), inboundPermitted, outboundPermitted, getOptionalObjectConfig(
        "bridge_config", new JsonObject()));

    server.listen(getOptionalIntConfig("port", 8080), getOptionalStringConfig("host", "0.0.0.0"),
        new AsyncResultHandler<HttpServer>() {
          @Override
          public void handle(AsyncResult<HttpServer> ar) {
            if (!ar.succeeded()) {
              startedResult.setFailure(ar.cause());
            } else {
              startedResult.setResult(null);
            }
          }
        });
  }
}
