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
package com.goodow.realtime.channel;

import com.goodow.realtime.DocumentBridge;
import com.goodow.realtime.channel.impl.GaeChannel;
import com.goodow.realtime.channel.rpc.Constants;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.channel.rpc.impl.AjaxRpc;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonObject;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

/**
 * De-multiplexes object channels a client is listening to.
 * 
 * Packets arrive with two keys, 'id' to identify the object, and 'm' containing the message
 * payload.
 */
public abstract class RealtimeChannelDemuxer {
  private static class Entry {
    final DocumentBridge snapshot;
    final ReceiveOpChannelImpl<?> channel;
    int count;

    Entry(DocumentBridge snapshot, ReceiveOpChannelImpl<?> channel) {
      this.snapshot = snapshot;
      this.channel = channel;
    }
  }

  private static final Logger log = Logger.getLogger(RealtimeChannelDemuxer.class.getName());

  private static final MapFromStringTo<Entry> entries = Collections.<Entry> mapFromStringTo();

  private static final Rpc rpc = new AjaxRpc("", null);

  public static final RealtimeChannelDemuxer get() {
    return GaeChannel.INSTANCE;
  }

  public void clear() {
    for (int i = 0, len = entries.keys().length(); i < len; i++) {
      close(entries.keys().get(i));
    }
  }

  public void close() {
    log.log(Level.FINE, "onClose ");
  }

  public void close(String id) {
    entries.remove(id);
  }

  public abstract void connect(String token);

  public ArrayOfString getIds() {
    return entries.keys();
  }

  public int getRevision(String id) {
    return entries.get(id).channel.currentRevision;
  }

  public Rpc getRpc() {
    return rpc;
  }

  public DocumentBridge getSnapshot(String id) {
    if (!entries.hasKey(id)) {
      return null;
    }
    return entries.get(id).snapshot;
  }

  public void publishMessage(JsonObject msg) {
    log.log(Level.FINE, "publishMessage data=" + msg.toJson());
    assert msg.hasKey(Constants.Params.ID) && msg.hasKey(Constants.Params.DELTAS) : "Bad data on channel (Missing fields) "
        + msg;
    String id = msg.getString(Constants.Params.ID);
    ReceiveOpChannelImpl<?> channel = entries.get(id).channel;
    if (channel == null) {
      log.log(Level.WARNING, "No channel registered for object with id " + id);
      return;
    }
    channel.onMessage(msg);
  }

  public void register(String id, DocumentBridge snapshot, ReceiveOpChannelImpl<?> channel) {
    assert !entries.hasKey(id) : "Channel handler already registered for " + id;
    entries.put(id, new Entry(snapshot, channel));
  }

  // called by native code
  protected void onError(int httpCode, String description) {
    log.log(Level.WARNING, "onError code=" + httpCode + " description=" + description);
  }

  // called by native code
  protected void onOpened() {
    log.log(Level.FINE, "onOpened ");
  }
}