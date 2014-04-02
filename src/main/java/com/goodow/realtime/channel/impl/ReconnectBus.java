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
package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.BusHook.BusHookProxy;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.util.FuzzingBackOffGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;

public class ReconnectBus extends WebSocketBus {
  private class QueuedMessage<T> {
    final boolean send;
    final String address;
    final Object msg;
    final Handler<Message<T>> replyHandler;

    QueuedMessage(boolean send, String address, Object msg, Handler<Message<T>> replyHandler) {
      this.send = send;
      this.address = address;
      this.msg = msg;
      this.replyHandler = replyHandler;
    }
  }

  public static final String AUTO_RECONNECT = "reconnect";
  private final FuzzingBackOffGenerator backOffGenerator;
  private BusHook hook;
  private boolean reconnect;
  private final JsonArray queuedMessages = Json.createArray(); // ArrayList<QueuedMessage>()

  public ReconnectBus(String url, JsonObject options) {
    super(url, options);
    backOffGenerator = new FuzzingBackOffGenerator(1 * 1000, 30 * 60 * 1000, 0.5);

    super.setHook(new BusHookProxy() {
      @SuppressWarnings({"unchecked", "rawtypes"})
      @Override
      public void handleOpened() {
        backOffGenerator.reset();

        String[] addresses = handlerMap.keys();
        for (String address : addresses) {
          assert handlerMap.getArray(address).length() > 0 : "Handlers registried on " + address
              + " shouldn't be empty";
          if (!ReconnectBus.this.isLocalFork(address)) {
            sendRegister(address);
          }
        }

        if (queuedMessages.length() != 0) {
          // Drain any messages that came in while the channel was not open.
          queuedMessages.forEach(new JsonArray.ListIterator<QueuedMessage>() {
            @Override
            public void call(int index, QueuedMessage value) {
              doSendOrPub(value.send, value.address, value.msg, value.replyHandler);
            }
          });
          queuedMessages.clear();
        }
        super.handleOpened();
      }

      @Override
      public void handlePostClose() {
        if (reconnect) {
          Platform.scheduler().scheduleDelay(backOffGenerator.next().targetDelay,
              new Handler<Void>() {
                @Override
                public void handle(Void event) {
                  if (reconnect) {
                    reconnect();
                  }
                }
              });
        }
        super.handlePostClose();
      }

      @Override
      public <T> boolean handleSendOrPub(boolean send, String address, Object msg,
          Handler<Message<T>> replyHandler) {
        boolean allow = super.handleSendOrPub(send, address, msg, replyHandler);
        if (!allow || state == State.OPEN || ReconnectBus.this.isLocalFork(address)) {
          return allow;
        }
        if (reconnect) {
          reconnect();
        }
        queuedMessages.push(new QueuedMessage<T>(send, address, msg, replyHandler));
        return false;
      }

      @Override
      protected BusHook delegate() {
        return hook;
      }
    });
  }

  public void reconnect() {
    if (state == State.OPEN) {
      return;
    }
    if (webSocket != null) {
      webSocket.close();
    }

    connect(url, getOptions());
  }

  @Override
  public SimpleBus setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }

  @Override
  public void setOptions(JsonObject options) {
    super.setOptions(options);
    reconnect =
        options == null || !options.has(AUTO_RECONNECT) ? true : options.getBoolean(AUTO_RECONNECT);
  }

  @Override
  protected void doClose() {
    reconnect = false;
    queuedMessages.clear();
    super.doClose();
  }
}
