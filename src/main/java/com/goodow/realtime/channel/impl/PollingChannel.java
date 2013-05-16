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
package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.ChannelDemuxer;
import com.goodow.realtime.channel.RealtimeActiveMap;
import com.goodow.realtime.channel.rpc.Constants.Params;
import com.goodow.realtime.channel.rpc.DeltaService;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.util.ArrayOfString;

public class PollingChannel extends ChannelDemuxer {
  private static final Logger log = Logger.getLogger(PollingChannel.class.getName());
  private static final int HEARTBEAT_INTERVAL_MILLIS = 15 * 1000;
  private static final PollingChannel INSTANCE = new PollingChannel(RealtimeActiveMap.get());

  public static ChannelDemuxer get() {
    return INSTANCE;
  }

  private boolean isHeartbeatTaskCanceled = true;
  private final RepeatingCommand heartbeatTask = new RepeatingCommand() {
    @Override
    public boolean execute() {
      if (isHeartbeatTaskCanceled) {
        return false;
      }
      ArrayOfString keys = registry.getIds();
      if (keys.length() == 0) {
        return true;
      }
      JsonArray array = Json.createArray();
      for (int i = 0, len = keys.length(); i < len; i++) {
        JsonObject obj = Json.createObject();
        String key = keys.get(i);
        obj.put(Params.ID, key);
        obj.put(Params.START_REVISION, registry.getRevision(key));
        array.set(i, obj);
      }
      log.log(Level.FINE, "Heartbeat");
      service.fetchHistories(array);
      return true;
    }
  };
  private final RealtimeActiveMap registry;
  private final DeltaService service;

  private PollingChannel(RealtimeActiveMap registry) {
    this.registry = registry;
    this.service = new DeltaService(registry.getRpc());
  }

  @Override
  public void close() {
    isHeartbeatTaskCanceled = true;
    super.close();
  }

  @Override
  public void connect(String token) {
    if (!isHeartbeatTaskCanceled) {
      return;
    }
    // Send the first heartbeat immediately, to quickly catch up any initial missing
    // ops, which might happen if the object is currently active.
    isHeartbeatTaskCanceled = false;
    heartbeatTask.execute();
    Scheduler.get().scheduleFixedDelay(heartbeatTask, HEARTBEAT_INTERVAL_MILLIS);
  }
}
