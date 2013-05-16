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

import com.goodow.realtime.channel.rpc.Constants;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonObject;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

/**
 * De-multiplexes object channels a client is listening to.
 * 
 * Packets arrive with two keys, 'id' to identify the object, and 'm' containing the message
 * payload.
 */
public abstract class ChannelDemuxer {
  /**
   * Channel that listens for messages to a specific object
   */
  public interface Channel {
    void onMessage(JsonObject msg);
  }

  private static final Logger log = Logger.getLogger(ChannelDemuxer.class.getName());
  private final MapFromStringTo<Channel> channels = Collections.mapFromStringTo();
  protected String currentToken = null;

  public void close() {
    log.log(Level.FINE, "onClose ");
  }

  public abstract void connect(String token);

  public void deregisterChannel(String objectId) {
    assert channels.hasKey(objectId) : "Channel handler not registered for " + objectId;
    channels.remove(objectId);
  }

  public void publishMessage(JsonObject msg) {
    log.log(Level.FINE, "publishMessage data=" + msg.toJson());
    assert msg.hasKey(Constants.Params.ID) && msg.hasKey(Constants.Params.CHANGES) : "Bad data on channel (Missing fields) "
        + msg;
    String id = msg.getString(Constants.Params.ID);
    Channel channel = channels.get(id);
    if (channel == null) {
      log.log(Level.WARNING, "No channel registered for object with id " + id);
      return;
    }
    channel.onMessage(msg);
  }

  public void registerChannel(String objectId, Channel channel) {
    assert !channels.hasKey(objectId) : "Channel handler already registered for " + objectId;

    channels.put(objectId, channel);
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