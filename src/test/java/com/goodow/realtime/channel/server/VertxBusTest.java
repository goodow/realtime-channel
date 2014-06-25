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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.MessageHandler;
import com.goodow.realtime.channel.server.impl.VertxBus;
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

public class VertxBusTest extends TestVerticle {

  private Bus bus;

  @Override
  public void start() {
    initialize();

    // Deploy the module - the System property `vertx.modulename` will contain the name of the
    // module so you don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"),
        new AsyncResultHandler<String>() {
          @Override
          public void handle(AsyncResult<String> asyncResult) {
            assertTrue(asyncResult.succeeded());
            assertNotNull("deploymentID should not be null", asyncResult.result());

            bus = new VertxBus(vertx.eventBus());
            startTests();
          }
        });
  }

  @Test
  public void test() {
    bus.subscribe("somea/topic", new MessageHandler<JsonObject>() {
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

    JsonObject o1 = Json.createObject().set("text", "send1");
    bus.send("somea/topic", o1, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("reply1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply2");
        message.reply(o1, null);
      }
    });
  }
}