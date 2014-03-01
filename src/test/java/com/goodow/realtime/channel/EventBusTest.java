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
import com.goodow.realtime.channel.util.IdGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistrations;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import org.junit.Test;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBusTest extends TestVerticle {

  private static final String ADDR = "java.someaddress." + new IdGenerator().next(5);
  private static final Logger log = Logger.getLogger(EventBusTest.class.getName());
  private Bus bus;
  private final HandlerRegistrations handlerRegs = new HandlerRegistrations();

  @Override
  public void start() {
    initialize();

    VertxPlatform.register(vertx);
    bus =
        new ReconnectBus("ws://data.goodow.com:8080/eventbus/websocket", Json.createObject().set(
            SimpleBus.MODE_MIX, true));

    startTests();
  }

  @Test
  public void test() {
    handlerRegs.add(bus.registerHandler(Bus.LOCAL_ON_OPEN, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        handlerEventBusOpened(bus);
      }
    }));
    handlerRegs.add(bus.registerHandler(Bus.LOCAL_ON_CLOSE, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.info("EventBus closed");
      }
    }));
    handlerRegs.add(bus.registerHandler(Bus.LOCAL_ON_ERROR, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        log.log(Level.SEVERE, "EventBus Error");
      }
    }));

    handlerRegs.add(bus.registerHandler(ADDR, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("send1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply1");
        message.reply(o1, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            VertxAssert.assertEquals("reply2", message.body().getString("text"));
            VertxAssert.assertNull(message.replyAddress());

            bus.close();
            handlerRegs.unregisterHandler();
            VertxAssert.testComplete();
          }
        });
      }
    }));
  }

  private void handlerEventBusOpened(final Bus bus) {
    JsonObject o1 = Json.createObject().set("text", "send1");
    bus.send(ADDR, o1, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        VertxAssert.assertEquals("reply1", message.body().getString("text"));

        JsonObject o1 = Json.createObject().set("text", "reply2");
        message.reply(o1);
      }
    });
  }

}
