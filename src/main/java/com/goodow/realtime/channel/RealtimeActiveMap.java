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
import com.goodow.realtime.channel.impl.PollingChannel;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.channel.rpc.impl.AjaxRpc;

import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

public class RealtimeActiveMap {
  private static class Entry {
    final DocumentBridge snapshot;
    final GenericOperationChannel<?> channel;
    int count;

    Entry(DocumentBridge snapshot, GenericOperationChannel<?> channel) {
      this.snapshot = snapshot;
      this.channel = channel;
    }
  }

  private static final RealtimeActiveMap INSTANCE = new RealtimeActiveMap();

  public static final RealtimeActiveMap get() {
    return INSTANCE;
  }

  private final Rpc rpc = new AjaxRpc("", null);

  private final MapFromStringTo<Entry> entries = Collections.<Entry> mapFromStringTo();;

  private RealtimeActiveMap() {
  }

  public void clear() {
    for (int i = 0, len = entries.keys().length(); i < len; i++) {
      unregister(entries.keys().get(i));
    }
  }

  public ArrayOfString getIds() {
    return entries.keys();
  }

  public int getRevision(String id) {
    return entries.get(id).channel.revision();
  }

  public Rpc getRpc() {
    return rpc;
  }

  public DocumentBridge load(String id) {
    if (!entries.hasKey(id)) {
      return null;
    }
    add(id, 1);
    return entries.get(id).snapshot;
  }

  public void register(String id, DocumentBridge snapshot, GenericOperationChannel<?> channel) {
    add(id, 1);
    assert !entries.hasKey(id);
    entries.put(id, new Entry(snapshot, channel));
    PollingChannel.get().connect(null);
  }

  public void unregister(String id) {
    add(id, -1);
  }

  private void add(String id, int num) {
    int result;
    if (entries.hasKey(id)) {
      result = entries.get(id).count + num;
    } else {
      result = num;
    }
    assert result > 0;
    if (result == 0) {
      entries.remove(id);
    } else {
      entries.get(id).count = result;
    }
  }
}