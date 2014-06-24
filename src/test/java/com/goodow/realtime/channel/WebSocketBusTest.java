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
package com.goodow.realtime.channel;

import com.goodow.realtime.channel.impl.ReconnectBus;
import com.goodow.realtime.channel.server.impl.VertxPlatform;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertTrue;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSocketBusTest extends TestVerticle {

  private static final Logger log = Logger.getLogger(WebSocketBusTest.class.getName());
  private Bus bus;

  @Override
  public void start() {
    initialize();
    VertxPlatform.register(vertx);

    // Deploy the module - the System property `vertx.modulename` will contain the name of the
    // module so you don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"),
        new AsyncResultHandler<String>() {
          @Override
          public void handle(AsyncResult<String> asyncResult) {
            assertTrue(asyncResult.succeeded());
            assertNotNull("deploymentID should not be null", asyncResult.result());

            bus = new ReconnectBus("ws://localhost:1986/channel/websocket", null);
            startTests();
          }
        });
  }

  @Test
  public void test() {
    bus.registerLocalHandler(Bus.ON_OPEN, new MessageHandler<Void>() {
      @Override
      public void handle(Message<Void> message) {
        log.info("EventBus opened");
      }
    });
    bus.registerLocalHandler(Bus.ON_CLOSE, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.info("EventBus closed");
      }
    });
    bus.registerLocalHandler(Bus.ON_ERROR, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.log(Level.SEVERE, "EventBus Error");
      }
    });

    JsonObject o1 = Json.createObject().set("text", "send1");
    bus.send("some/topic", o1, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("reply1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply2");
        message.reply(o1, null);
      }
    });

    bus.registerHandler("some/topic", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("send1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply1");
        message.reply(o1, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            VertxAssert.assertEquals("reply2", message.body().getString("text"));
            VertxAssert.assertNull(message.replyTopic());

            bus.close();
            VertxAssert.testComplete();
          }
        });
      }
    });
  }
}