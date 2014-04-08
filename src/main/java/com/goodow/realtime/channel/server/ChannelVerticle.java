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
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ChannelVerticle extends BusModBase {

  @Override
  public void start(final Future<Void> startedResult) {
    super.start();

    JsonObject cfg =
        config
            .copy()
            .putBoolean("bridge", getOptionalBooleanConfig("bridge", true))
            .putArray("inbound_permitted",
                getOptionalArrayConfig("inbound_permitted", new JsonArray().add(new JsonObject())))
            .putArray("outbound_permitted",
                getOptionalArrayConfig("outbound_permitted", new JsonArray().add(new JsonObject())));

    container.deployModule("io.vertx~mod-web-server~2.0.0-final", cfg,
        new AsyncResultHandler<String>() {
          @Override
          public void handle(AsyncResult<String> ar) {
            if (ar.succeeded()) {
              startedResult.setResult(null);
            } else {
              startedResult.setFailure(ar.cause());
            }
          }
        });
  }
}
