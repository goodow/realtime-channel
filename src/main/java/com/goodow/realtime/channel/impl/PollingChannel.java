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

import com.goodow.realtime.channel.RealtimeChannelDemuxer;
import com.goodow.realtime.channel.rpc.PollingService;
import com.goodow.realtime.util.NativeInterface;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.util.ArrayOfString;

public class PollingChannel extends RealtimeChannelDemuxer {
  private static final Logger log = Logger.getLogger(PollingChannel.class.getName());
  private static final int HEARTBEAT_INTERVAL_MILLIS = 15 * 1000;
  public static final PollingChannel INSTANCE = new PollingChannel();

  private boolean isHeartbeatTaskCanceled = true;
  private final Runnable heartbeatTask = new Runnable() {
    @Override
    public void run() {
      if (isHeartbeatTaskCanceled) {
        return;
      }
      ArrayOfString ids = getIds();
      JsonArray array = Json.createArray();
      if (ids.length() != 0) {
        for (int i = 0, len = ids.length(); i < len; i++) {
          JsonArray req = Json.createArray();
          String id = ids.get(i);
          req.set(0, id);
          req.set(1, getRevision(id) + 1);
          array.set(i, req);
        }
        log.log(Level.FINE, "Heartbeat");
      }
      service.poll(array, sessionId);
      NativeInterface.get().scheduleFixedDelay(heartbeatTask, HEARTBEAT_INTERVAL_MILLIS);
    }
  };
  private final PollingService service;
  private String sessionId;

  private PollingChannel() {
    this.service = new PollingService();
  }

  @Override
  public void close() {
    isHeartbeatTaskCanceled = true;
    super.close();
  }

  @Override
  public void connect(String sessionId) {
    this.sessionId = sessionId;
    if (!isHeartbeatTaskCanceled) {
      return;
    }
    // Send the first heartbeat immediately, to quickly catch up any initial missing
    // ops, which might happen if the object is currently active.
    isHeartbeatTaskCanceled = false;
    heartbeatTask.run();
  }
}
