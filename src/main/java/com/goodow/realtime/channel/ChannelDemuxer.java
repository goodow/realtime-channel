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

import com.goodow.realtime.channel.constant.Constants;
import com.goodow.realtime.channel.constant.Constants.Params;
import com.goodow.realtime.channel.constant.Platform;
import com.goodow.realtime.channel.operation.OperationSucker;
import com.goodow.realtime.channel.operation.ReceiveOpChannelImpl;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.channel.rpc.RpcImpl;
import com.goodow.realtime.channel.util.ChannelNative;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.id.IdGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * De-multiplexes object channels a client is listening to.
 * 
 * Packets arrive with two keys, 'id' to identify the object, and 'm' containing the message
 * payload.
 */
public class ChannelDemuxer implements SocketListener {
  private static class Entry {
    final OperationSucker.Listener snapshot;
    final ReceiveOpChannelImpl<?> channel;

    Entry(OperationSucker.Listener snapshot, ReceiveOpChannelImpl<?> channel) {
      this.snapshot = snapshot;
      this.channel = channel;
    }
  }

  private static final Logger log = Logger.getLogger(ChannelDemuxer.class.getName());
  private static final ChannelDemuxer INSTANCE = new ChannelDemuxer();
  private static final Map<String, Entry> entries = new HashMap<String, Entry>();
  private static final Rpc rpc = new RpcImpl("", null);
  private static String sessionId;

  public static final ChannelDemuxer get() {
    return INSTANCE;
  }

  public static final String getSessionId() {
    if (sessionId != null) {
      return sessionId;
    }
    IdGenerator idGenerator = new IdGenerator();
    String userAgent = ChannelNative.get().getDefaultUserAgent();
    String sid = idGenerator.next(Constants.SESSION_LENGTH - 1);
    sessionId = Platform.fromUserAgent(userAgent).prefix() + sid;
    return sessionId;
  }

  private String accessToken;
  private String channelToken = null;
  private Socket socket;

  private ChannelDemuxer() {
  }

  public void clear() {
    entries.clear();
  }

  public void close() {
    log.log(Level.FINE, "onClose ");
  }

  public void close(String id) {
    entries.remove(id);
  }

  public void connect(String token) {
    assert token != null : "Null token";
    if (!token.equals(this.channelToken)) {
      log.log(Level.INFO, "Connecting with token " + token);
      this.channelToken = token;
      if (socket != null) {
        socket.close();
      }
      Channel channel = ChannelNative.get().createChannel(token);
      socket = channel.open(this);
    }
  }

  public String getAccessToken() {
    return accessToken;
  }

  public Set<String> getIds() {
    return entries.keySet();
  }

  public int getRevision(String id) {
    return entries.get(id).channel.revision();
  }

  public Rpc getRpc() {
    return rpc;
  }

  public OperationSink<?> getSnapshot(String id) {
    if (!entries.containsKey(id)) {
      return null;
    }
    return entries.get(id).snapshot;
  }

  @Override
  public void onClose() {
    log.log(Level.FINE, "onClose ");
  }

  @Override
  public void onError(ChannelError error) {
    log.log(Level.WARNING, "onError code=" + error.getCode() + " description="
        + error.getDescription());
  }

  @Override
  public void onMessage(String message) {
    if (message == null) {
      log.log(Level.WARNING, "Null data on channel");
      return;
    }
    JsonObject msg = Json.parse(message);
    publishMessage(msg);
  }

  @Override
  public void onOpen() {
    log.log(Level.FINE, "onOpened ");
  }

  public void publishMessage(JsonObject msg) {
    log.log(Level.FINE, "publishMessage data=" + msg.toJson());
    assert msg.hasKey(Params.ID) && msg.hasKey(Params.DELTAS) : "Bad data on channel (Missing fields) "
        + msg;
    String id = msg.getString(Params.ID);
    Entry entry = entries.get(id);
    if (entry == null) {
      log.log(Level.WARNING, "No channel registered for object with id " + id);
      return;
    }
    if (msg.hasKey(Params.IS_JOINED)) {
      entry.snapshot.onCollaboratorChanged(msg.getBoolean(Params.IS_JOINED), msg);
    } else {
      entry.channel.onMessage(msg);
    }
  }

  public void register(String id, OperationSucker.Listener snapshot, ReceiveOpChannelImpl<?> channel) {
    assert !entries.containsKey(id) : "Channel handler already registered for " + id;
    entries.put(id, new Entry(snapshot, channel));
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
}