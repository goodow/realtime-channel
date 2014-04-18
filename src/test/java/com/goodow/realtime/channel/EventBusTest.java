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
import com.goodow.realtime.channel.impl.SimpleBus;
import com.goodow.realtime.channel.server.VertxPlatform;
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

public class EventBusTest extends TestVerticle {

  private static final Logger log = Logger.getLogger(EventBusTest.class.getName());
  private Bus bus;

  @Override
  public void start() {
    initialize();
    VertxPlatform.register(vertx);

    JsonObject config = Json.createObject().set("port", 8080).set("static_files", false);
    // Deploy the module - the System property `vertx.modulename` will contain the name of the
    // module so you don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"),
        new org.vertx.java.core.json.JsonObject(config.toJsonString()),
        new AsyncResultHandler<String>() {
          @Override
          public void handle(AsyncResult<String> asyncResult) {
            // Deployment is asynchronous and this this handler will be called when it's complete
            // (or failed)
            assertTrue(asyncResult.succeeded());
            assertNotNull("deploymentID should not be null", asyncResult.result());

            // If deployed correctly then start the tests!
            bus =
                new ReconnectBus("ws://localhost:8080/eventbus/websocket", Json.createObject().set(
                    SimpleBus.MODE_MIX, true));

            startTests();
          }
        });
  }

  @Test
  public void test() {
    bus.registerHandler(Bus.LOCAL_ON_OPEN, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        handlerEventBusOpened(bus);
      }
    });
    bus.registerHandler(Bus.LOCAL_ON_CLOSE, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.info("EventBus closed");
      }
    });
    bus.registerHandler(Bus.LOCAL_ON_ERROR, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.log(Level.SEVERE, "EventBus Error");
      }
    });

    bus.registerHandler("someaddress", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("send1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply1");
        message.reply(o1, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            VertxAssert.assertEquals("reply2", message.body().getString("text"));
            // VertxAssert.assertNull(message.replyAddress());

            bus.close();
            VertxAssert.testComplete();
          }
        });
      }
    });
  }

  private void handlerEventBusOpened(final Bus bus) {
    JsonObject o1 = Json.createObject().set("text", "send1");
    bus.send("someaddress", o1, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("reply1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply2");
        message.reply(o1);
      }
    });
  }

}
