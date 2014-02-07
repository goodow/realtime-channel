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
import com.goodow.realtime.channel.BusHook.BusHookProxy;
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
  public static final String SEQUENCE_NUMBER = "_seq";
  private static final Logger log = Logger.getLogger(ReliabeBusClient.class.getName());
  private final JsonObject pendings;
  private final JsonObject currentSequences;
  private final JsonObject knownHeadSequences;
  private final SimpleBus delegate;
  private BusHook hook;

  public ReliabeBusClient(SimpleBus delegate) {
    this.delegate = delegate;
    pendings = Json.createObject();
    currentSequences = Json.createObject();
    knownHeadSequences = Json.createObject();

    delegate.setHook(new BusHookProxy() {
      @SuppressWarnings("rawtypes")
      @Override
      public boolean handlePreRegister(String address, Handler<? extends Message> handler) {
        pendings.set(address, Json.createObject());
        return super.handlePreRegister(address, handler);
      }

      @Override
      public boolean handleReceiveMessage(Message<?> message) {
        if (hook != null && !hook.handleReceiveMessage(message)) {
          return false;
        }
        return onReceiveMessage(message);
      }

      @Override
      public boolean handleUnregister(String address) {
        pendings.remove(address);
        currentSequences.remove(address);
        knownHeadSequences.remove(address);
        return super.handleUnregister(address);
      }

      @Override
      protected BusHook delegate() {
        return hook;
      }
    });
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public State getReadyState() {
    return delegate.getReadyState();
  }

  @Override
  public Bus publish(String address, Object msg) {
    return delegate.publish(address, msg);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public HandlerRegistration registerHandler(String address, Handler<? extends Message> handler) {
    return delegate.registerHandler(address, handler);
  }

  @Override
  public <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler) {
    return delegate.send(address, msg, replyHandler);
  }

  @Override
  public ReliabeBusClient setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }

  protected boolean onReceiveMessage(Message<?> message) {
    String address = message.address();
    Object body = message.body();
    if (!(body instanceof JsonObject) || !((JsonObject) body).has(SEQUENCE_NUMBER)) {
      return true;
    }
    double sequence = ((JsonObject) body).getNumber(SEQUENCE_NUMBER);
    boolean isExist = pendings.has(address);
    if (!isExist) {
      currentSequences.set(address, sequence);
      knownHeadSequences.set(address, sequence);
      return true;
    }

    double currentSequence = currentSequences.getNumber(address);
    if (sequence <= currentSequence) {
      log.log(Level.CONFIG, "Old dup at sequence " + sequence + ", current is now ",
          currentSequence);
      return false;
    }
    JsonObject pending = pendings.getObject(address);
    JsonObject existing = pending.getObject("" + sequence);
    if (existing != null) {
      // Should not have pending data at a sequence we could have pushed out.
      assert sequence > currentSequence + 1 : "should not have pending data";
      log.log(Level.CONFIG, "Dup message: " + message);
      return false;
    }

    double knownHeadSequence = Math.max(knownHeadSequences.getNumber(address), sequence);
    knownHeadSequences.set(address, knownHeadSequence);

    if (sequence > currentSequence + 1) {
      pending.set("" + sequence, message);
      log.log(Level.CONFIG, "Missed message, current sequence=" + currentSequence
          + " message sequence=" + sequence);
      scheduleCatchup(address, currentSequence + 1);
      return false;
    }

    assert sequence == currentSequence + 1 : "other cases should have been caught";
    String next;
    while (true) {
      delegate.doReceiveMessage(message);
      currentSequences.set(address, ++currentSequence);
      next = currentSequence + 1 + "";
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
