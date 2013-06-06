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

import com.goodow.realtime.channel.rpc.PollService;
import com.goodow.realtime.channel.util.ChannelNative;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.util.ArrayOfString;

public class PollChannel {
  private static final Logger log = Logger.getLogger(PollChannel.class.getName());
  private static final int HEARTBEAT_INTERVAL_MILLIS = 15 * 1000;
  private static final PollChannel INSTANCE = new PollChannel();

  public static PollChannel get() {
    return INSTANCE;
  }

  private final RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();
  private boolean isHeartbeatTaskCanceled = true;
  private final Runnable heartbeatTask = new Runnable() {
    @Override
    public void run() {
      if (isHeartbeatTaskCanceled) {
        return;
      }
      ArrayOfString ids = demuxer.getIds();
      JsonArray array = Json.createArray();
      if (ids.length() != 0) {
        for (int i = 0, len = ids.length(); i < len; i++) {
          JsonArray req = Json.createArray();
          String id = ids.get(i);
          req.set(0, id);
          req.set(1, demuxer.getRevision(id) + 1);
          array.set(i, req);
        }
        log.log(Level.FINE, "Heartbeat");
      }
      service.poll(array, sessionId);
      ChannelNative.get().scheduleFixedDelay(heartbeatTask, HEARTBEAT_INTERVAL_MILLIS);
    }
  };
  private final PollService service;
  private String sessionId;

  private PollChannel() {
    this.service = new PollService();
  }

  public void close() {
    isHeartbeatTaskCanceled = true;
  }

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
