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

import com.google.gwt.core.client.JavaScriptObject;

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
public class GaeChannel extends ChannelDemuxer {

  private static final GaeChannel INSTANCE = new GaeChannel();

  private static final Logger log = Logger.getLogger(GaeChannel.class.getName());

  public static GaeChannel get() {
    return INSTANCE;
  }

  // @formatter:off
  private static native JsonObject nativeGetVars() /*-{
    return $wnd.__vars;
  }-*/;
  // @formatter:on

  // used by native code
  private JavaScriptObject socket;

  // There should only ever be one global instance, or strange things will happen.
  private GaeChannel() {
  }

  @Override
  public void connect(String token) {
    assert token != null : "Null token";
    if (!token.equals(currentToken)) {
      log.log(Level.INFO, "Connecting with token " + token);
      currentToken = token;
      connectNative(token);
    } else {
      log.log(Level.FINE, "Already using same token, ignoring " + token);
    }
  }

  // @formatter:off
  private native void connectNative(String token) /*-{
    var me = this;
  
    var socket = this.@com.goodow.realtime.channel.impl.GaeChannel::socket;
    if (socket != null) {
      socket.close();
    }
  
    channel = new $wnd.goog.appengine.Channel(token);
  
    socket = channel.open();
    this.@com.goodow.realtime.channel.impl.GaeChannel::socket = socket;
  
    socket.onopen = $entry(function() {
      me.
        @com.goodow.realtime.channel.impl.GaeChannel::onOpened()
        ();
    });
    socket.onmessage = $entry(function(msg) {
      me.
        @com.goodow.realtime.channel.impl.GaeChannel::publishMessage(Ljava/lang/String;)
        (msg.data);
    });
    socket.onerror = $entry(function(err) {
      me.
        @com.goodow.realtime.channel.impl.GaeChannel::onError(ILjava/lang/String;)
        (err.code, err.description);
    });
    socket.onclose = $entry(function() {
      me.
        @com.goodow.realtime.channel.impl.GaeChannel::close()
        ();
    });
  }-*/;
  // @formatter:on

  private void publishMessage(String msgString) {
    if (msgString == null) {
      log.log(Level.WARNING, "Null data on channel");
      return;
    }
    JsonObject msg = Json.parse(msgString);
    super.publishMessage(msg);
  }
}