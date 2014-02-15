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
import com.goodow.realtime.channel.impl.SimpleBus.BusProxy;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts a stream of possibly-missing, possibly-unordered, possibly-duplicated messages into a
 * stream of in-order, consecutive, no-dup messages.
 */
public class ReliabeBusClient extends BusProxy {
  public static final String SEQUENCE_NUMBER = "_seq";
  public static final String ACKNOWLEDGE_DELAY_MILLIS = "acknowledgeDelayMillis";
  private static final Logger log = Logger.getLogger(ReliabeBusClient.class.getName());
  /**
   * Delay acknowledgment in case we receive operations in the meantime.
   */
  private final int acknowledgeDelayMillis;
  private final JsonObject pendings;
  private final JsonObject currentSequences;
  private final JsonObject knownHeadSequences;
  private final JsonObject acknowledgeScheduled;
  private final JsonObject acknowledgeNumbers;

  public ReliabeBusClient(SimpleBus delegate) {
    super(delegate);
    JsonObject options = delegate.getOptions();
    acknowledgeDelayMillis =
        (options == null || !options.has(ACKNOWLEDGE_DELAY_MILLIS)) ? 3 * 1000 : (int) options
            .getNumber(ACKNOWLEDGE_DELAY_MILLIS);
    pendings = Json.createObject();
    currentSequences = Json.createObject();
    knownHeadSequences = Json.createObject();
    acknowledgeScheduled = Json.createObject();
    acknowledgeNumbers = Json.createObject();

    delegate.setHook(new BusHookProxy() {
      @Override
      public void handlePostClose() {
        pendings.clear();
        currentSequences.clear();
        knownHeadSequences.clear();
        acknowledgeScheduled.clear();
        acknowledgeNumbers.clear();
      }

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
        acknowledgeScheduled.remove(address);
        acknowledgeNumbers.remove(address);
        return super.handleUnregister(address);
      }

      @Override
      protected BusHook delegate() {
        return hook;
      }
    });
  }

  public void synchronizeSequenceNumber(String address, double initialSequenceNumber) {
    currentSequences.set(address, initialSequenceNumber);
    knownHeadSequences.set(address, !knownHeadSequences.has(address) ? initialSequenceNumber : Math
        .max(initialSequenceNumber, knownHeadSequences.getNumber(address)));
    // Send the first acknowledgment immediately, to quickly catch up any initial missing messages,
    // which might happen if the address is currently active.
    delegate.send(address + ".ack", initialSequenceNumber + 1, null);
  }

  protected boolean onReceiveMessage(Message<?> message) {
    String address = message.address();
    Object body = message.body();
    if (!(body instanceof JsonObject) || !((JsonObject) body).has(SEQUENCE_NUMBER)) {
      return true;
    }
    double sequence = ((JsonObject) body).getNumber(SEQUENCE_NUMBER);
    if (!currentSequences.has(address)) {
      currentSequences.set(address, sequence);
      knownHeadSequences.set(address, sequence);
      return true;
    }

    double currentSequence = currentSequences.getNumber(address);
    if (sequence <= currentSequence) {
      log.log(Level.CONFIG, "Old dup at sequence " + sequence + ", current is now "
          + currentSequence);
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

    knownHeadSequences.set(address, Math.max(knownHeadSequences.getNumber(address), sequence));

    if (sequence > currentSequence + 1) {
      pending.set("" + sequence, message);
      log.log(Level.CONFIG, "Missed message, current sequence=" + currentSequence
          + " incoming sequence=" + sequence);
      scheduleAcknowledgment(address);
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

  /**
   * Acknowledgment Number is the next sequence number that the receiver is expecting
   */
  private void scheduleAcknowledgment(final String address) {
    if (!acknowledgeScheduled.has(address)) {
      acknowledgeScheduled.set(address, true);
      Platform.scheduler().scheduleDelay(acknowledgeDelayMillis, new Handler<Void>() {
        @Override
        public void handle(Void event) {
          if (acknowledgeScheduled.has(address)) {
            acknowledgeScheduled.remove(address);
            // Check we're still out of date, and not already catching up.
            double knownHeadSequence = knownHeadSequences.getNumber(address);
            double currentSequence = currentSequences.getNumber(address);
            if (knownHeadSequence > currentSequence
                && (!acknowledgeNumbers.has(address) || knownHeadSequence > acknowledgeNumbers
                    .getNumber(address))) {
              acknowledgeNumbers.set(address, knownHeadSequence);
              log.log(Level.CONFIG, "Catching up to " + knownHeadSequence);
              delegate.send(address + ".ack", currentSequence + 1, null);
            } else {
              log.log(Level.FINE, "No need to catchup");
            }
          }
        }
      });
    }
  }
}
