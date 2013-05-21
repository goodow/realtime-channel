/*
 * Copyright 2012 Goodow.com
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
package com.goodow.realtime.channel.operation;

import com.goodow.realtime.channel.RealtimeChannelDemuxer;
import com.goodow.realtime.channel.operation.GenericOperationChannel.ReceiveOpChannel;
import com.goodow.realtime.channel.rpc.Constants.Params;
import com.goodow.realtime.channel.rpc.DeltaService;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.util.ModelNative;
import com.goodow.realtime.util.Pair;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.util.Collections;
import elemental.util.MapFromIntTo;

/**
 * Implementation of a {@link ReceiveOpChannel} based on Google App Engine's channel API.
 * 
 * Converts a stream of possibly-missing, possibly-unordered, possibly-duplicated messages into a
 * stream of in-order, consecutive, no-dup messages.
 */
// TODO: Move the flaky layer into a separate class - possibly
// DeltaService, as its callback interface would be sufficient as
// it stands now.
public class ReceiveOpChannelImpl<O extends Operation<?>> implements ReceiveOpChannel<O> {
  private static final Logger log = Logger.getLogger(ReceiveOpChannelImpl.class.getName());
  /**
   * Delay catchup in case we receive operations in the meantime.
   */
  // TODO: Flags for these values, and fuzz.
  private static final int CATCHUP_DELAY_MILLIS = 3000;
  private boolean isCatchupTaskScheduled;
  private final Runnable catchupTask = new Runnable() {
    @Override
    public void run() {
      isCatchupTaskScheduled = false;
      maybeCatchup();
    }
  };

  private final DeltaService.Callback callback = new DeltaService.Callback() {
    @Override
    public void onConnectionError(Throwable e) {
      // TODO: Increase heartbeat interval
      log.log(Level.WARNING, "onConnectionError ", e);
    }

    @Override
    public void onFatalError(Throwable e) {
      log.log(Level.WARNING, "onFatalError ", e);
    }

    @Override
    public void onMessage(JsonObject msg) {
      ReceiveOpChannelImpl.this.onMessage(msg);
    }
  };

  private final RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();

  private final MapFromIntTo<Pair<String, O>> pending = Collections.mapFromIntTo();
  private final String id;
  private final DeltaService service;
  private ReceiveOpChannel.Listener<O> listener;
  private int currentRevision = 0;
  private int knownHeadRevision = 0;
  private int catchupRevision = 0;
  private final Transformer<O> transformer;

  private boolean corruptedByException = false;
  private boolean receiving = false;

  public ReceiveOpChannelImpl(String id, Rpc rpc, Transformer<O> transformer) {
    this.id = id;
    this.transformer = transformer;
    this.service = new DeltaService(rpc);
  }

  @Override
  public void connect(int revision, ReceiveOpChannel.Listener<O> listener) {
    assert this.listener == null;
    this.listener = listener;
    this.currentRevision = this.knownHeadRevision = revision;

    log.log(Level.FINE, "connect, rev=" + revision);
  }

  @Override
  public void disconnect() {
    demuxer.close(id);
  }

  @Override
  public void onKnownHeadRevision(int headVersion) {
    log.log(Level.FINE, "onKnownHeadRevision(" + headVersion + "), " + "old known="
        + knownHeadRevision + ", current=" + currentRevision);

    knownHeadRevision = Math.max(knownHeadRevision, headVersion);
    if (knownHeadRevision > currentRevision) {
      scheduleCatchup();
    }

    assert knownHeadRevision == currentRevision || isCatchupTaskScheduled;
  }

  public void onMessage(JsonObject msg) {
    JsonArray deltas = msg.getArray(Params.DELTAS);
    for (int i = 0, len = deltas.length(); i < len; i++) {
      JsonArray delta = deltas.getArray(i);
      log.log(Level.INFO, "Store message: " + delta.toJson());
      O op;
      try {
        op = transformer.createOperation(delta.get(0), delta.getString(1), delta.getString(3));
      } catch (JsonException e) {
        listener.onError(e);
        return;
      }
      receiveUnorderedData((int) delta.getNumber(2), delta.getString(3), op);
    }
    if (msg.hasKey(Params.REVISION)) {
      // The head revision might be greater than expected if some
      // history items were missed, so let's give the listener
      // as much information as possible.
      onKnownHeadRevision((int) msg.getNumber(Params.REVISION));
    }
    // In case the result is batched, we'll keep fetching.
    if (msg.hasKey(Params.HAS_MORE)) {
      log.log(Level.INFO, "fetch history returned incomplete result, retrying for the rest");
      service.fetchHistory(id, currentRevision + 1, callback);
    }
  }

  public int revision() {
    return currentRevision;
  }

  private void maybeCatchup() {
    // Check we're still out of date, and not already catching up.
    if (knownHeadRevision > currentRevision && knownHeadRevision > catchupRevision) {
      log.log(Level.FINE, "Catching up to " + knownHeadRevision);
      catchupRevision = knownHeadRevision;
      service.fetchHistory(id, currentRevision + 1, callback);
    } else {
      log.log(Level.FINE, "No need to catchup");
    }
  }

  private void receiveUnorderedData(int resultingRevision, String sessionId, O op) {
    assert !corruptedByException : "receiveUnorderedData called while corrupted";
    assert !receiving : "receiveUnorderedData called re-entrantly";
    receiving = true;

    try {
      unguardedReceiveUnorderedData(resultingRevision, sessionId, op);
    } catch (RuntimeException e) {
      corruptedByException = true;
      log.log(Level.WARNING, "Op channel is now corrupted", e);
      throw e;
    }

    receiving = false;
  }

  private void scheduleCatchup() {
    log.log(Level.FINE, "scheduleCatchup()");
    // Check, to avoid resetting the delay.
    if (!isCatchupTaskScheduled) {
      isCatchupTaskScheduled = true;
      ModelNative.get().scheduleFixedDelay(catchupTask, CATCHUP_DELAY_MILLIS);
    }
  }

  private void unguardedReceiveUnorderedData(int resultingRevision, String sessionId, O op) {
    knownHeadRevision = Math.max(knownHeadRevision, resultingRevision);

    if (resultingRevision <= currentRevision) {
      log.log(Level.FINE, "Old dup at revision " + resultingRevision + ", current is now "
          + currentRevision);
      return;
    }

    Pair<String, O> existing = pending.get(resultingRevision);
    if (existing != null) {
      // Should not have pending data at a revision we could have pushed out.
      assert resultingRevision > currentRevision + 1 : "should not have pending data";

      // Sanity check
      if (!existing.first.equals(sessionId)) {
        listener.onError(new Exception("Duplicates did not match at resultingRevision "
            + resultingRevision + ": " + existing + " vs " + "(" + sessionId + "," + op.toString()
            + ")"));
      }
      log.log(Level.FINE, "Dup message: " + "(" + sessionId + "," + op.toString() + ")");
      return;
    }

    if (resultingRevision > currentRevision + 1) {
      pending.put(resultingRevision, Pair.of(sessionId, op));
      log.log(Level.FINE, "Missed message, currentRevision=" + currentRevision
          + " message revision=" + resultingRevision);
      scheduleCatchup();
      return;
    }

    assert resultingRevision == currentRevision + 1 : "other cases should have been caught";

    while (true) {
      log.log(Level.FINE, "Ordered op @" + resultingRevision + " sid=" + sessionId + ", payload="
          + op.toString());
      listener.onMessage(currentRevision + 1, sessionId, op);
      currentRevision++;

      int next = currentRevision + 1;
      Pair<String, O> pair = pending.get(next);
      if (pair != null) {
        sessionId = pair.first;
        op = pair.second;
        pending.remove(next);
      } else {
        break;
      }
    }

    assert !pending.hasKey(currentRevision + 1);
  }
}