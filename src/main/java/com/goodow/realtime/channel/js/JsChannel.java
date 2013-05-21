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
package com.goodow.realtime.channel.js;

import com.goodow.realtime.channel.Channel;
import com.goodow.realtime.channel.Socket;
import com.goodow.realtime.channel.SocketListener;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * GWT implementation of {@link Channel}.
 */
public class JsChannel extends JavaScriptObject implements Channel {

  protected JsChannel() {
  }

  @Override
  // @formatter:off
  public final native Socket open(SocketListener listener) /*-{
    var socket = this.open();
    
    socket.onopen = function(event) {
      listener.@com.goodow.realtime.channel.SocketListener::onOpen(*)();
    };
    
    socket.onmessage = function(event) {
      listener.@com.goodow.realtime.channel.SocketListener::onMessage(*)(event.data);
    };
    
    socket.onerror = function(error) {
      listener.@com.goodow.realtime.channel.SocketListener::onError(*)(error);
    };
    
    socket.onclose = function() {
      listener.@com.goodow.realtime.channel.SocketListener::onClose(*)();
    };
    
    return socket;
  }-*/;
  // @formatter:on
}
