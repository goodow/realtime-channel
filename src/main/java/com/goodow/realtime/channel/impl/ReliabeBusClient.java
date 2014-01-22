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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts a stream of possibly-missing, possibly-unordered, possibly-duplicated messages into a
 * stream of in-order, consecutive, no-dup messages.
 */
public class ReliabeBusClient implements Bus {
  private static final Logger log = Logger.getLogger(ReliabeBusClient.class.getName());
  private final JsonObject pendings;
  private final JsonObject currentRevisions;
  private final JsonObject knownHeadRevisions;
  private final SimpleBus wrapped;
  private BusHook hook;

  public ReliabeBusClient(SimpleBus wrapped) {
    this.wrapped = wrapped;
    pendings = Json.createObject();
    currentRevisions = Json.createObject();
    knownHeadRevisions = Json.createObject();

    wrapped.setHook(new BusHook() {
      @SuppressWarnings("rawtypes")
      @Override
      public boolean handlePreRegister(String address, Handler<? extends Message> handler) {
        pendings.set(address, Json.createObject());
        return hook == null ? true : hook.handlePreRegister(address, handler);
      }

      @Override
      public boolean handleReceiveMessage(Message<?> message) {
        if (hook != null && !hook.handleReceiveMessage(message)) {
          return false;
        }
        return onMessage(message);
      }

      @Override
      public <T> boolean handleSendOrPub(boolean send, String address, Object msg,
          Handler<Message<T>> replyHandler) {
        return hook == null ? true : hook.handleSendOrPub(send, address, msg, replyHandler);
      }

      @Override
      public boolean handleUnregister(String address) {
        pendings.remove(address);
        currentRevisions.remove(address);
        knownHeadRevisions.remove(address);
        return hook == null ? true : hook.handleUnregister(address);
      }
    });
  }

  @Override
  public void close() {
    wrapped.close();
  }

  @Override
  public State getReadyState() {
    return wrapped.getReadyState();
  }

  @Override
  public Bus publish(String address, Object msg) {
    return wrapped.publish(address, msg);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public HandlerRegistration registerHandler(String address, Handler<? extends Message> handler) {
    return wrapped.registerHandler(address, handler);
  }

  @Override
  public <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler) {
    return wrapped.send(address, msg, replyHandler);
  }

  @Override
  public ReliabeBusClient setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }

  protected boolean onMessage(Message<?> message) {
    String address = message.address();
    Object body = message.body();
    if (!(body instanceof JsonObject) || !((JsonObject) body).has("_id")) {
      return true;
    }
    double number = ((JsonObject) body).getNumber("_id");
    boolean isExist = pendings.has(address);
    if (!isExist) {
      currentRevisions.set(address, number);
      knownHeadRevisions.set(address, number);
      return true;
    }

    double currentRevision = currentRevisions.getNumber(address);
    if (number <= currentRevision) {
      log.log(Level.CONFIG, "Old dup at revision " + number + ", current is now ", currentRevision);
      return false;
    }
    JsonObject pending = pendings.getObject(address);
    JsonObject existing = pending.getObject("" + number);
    if (existing != null) {
      // Should not have pending data at a revision we could have pushed out.
      assert number > currentRevision + 1 : "should not have pending data";
      log.log(Level.CONFIG, "Dup message: " + message);
      return false;
    }

    double knownHeadRevision = Math.max(knownHeadRevisions.getNumber(address), number);
    knownHeadRevisions.set(address, knownHeadRevision);

    if (number > currentRevision + 1) {
      pending.set("" + number, message);
      log.log(Level.CONFIG, "Missed message, currentRevision=" + currentRevision
          + " message revision=" + number);
      scheduleCatchup(address, currentRevision + 1);
      return false;
    }

    assert number == currentRevision + 1 : "other cases should have been caught";
    String next;
    while (true) {
      wrapped.doDeliverMessage(message);
      currentRevisions.set(address, ++currentRevision);
      next = currentRevision + 1 + "";
      message = pending.get(next);
      if (message != null) {
        pending.remove(next);
      } else {
        break;
      }
    }
    assert !pending.has(next);
    return false;
  }

  private void scheduleCatchup(String address, double d) {
    // TODO Auto-generated method stub

  }
}
