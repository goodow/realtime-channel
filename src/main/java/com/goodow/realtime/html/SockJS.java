/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.html;

import com.goodow.realtime.channel.State;
import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.JsonObject;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * SockJS implementation of {@link WebSocket}.
 */
final class SockJS extends JavaScriptObject implements WebSocket {

  // @formatter:off
  public static native WebSocket create(String url, Object _reserved, JsonObject options) /*-{
    return new $wnd.SockJS(url, _reserved, options);
  }-*/;  

  protected SockJS() {
  }

  @Override
  public native void close() /*-{
    this.close();
  }-*/;
 
  @Override
  public State getReadyState() {
    return State.values[nativeGetReadyState()];
  }

  @Override
  public native void send(String data) /*-{
    this.send(data);
  }-*/;

  @Override
  public native void setListen(WebSocketHandler handler) /*-{
    if (!handler) {
      this.onopen = null;
      this.onclose = null;
      this.onmessage = null;
      this.onerror = null;
      return;
    }
    
    this.onopen = function(e) {
      handler.@com.goodow.realtime.core.WebSocket.WebSocketHandler::onOpen()();
    };
    this.onclose = function(e) {
      handler.@com.goodow.realtime.core.WebSocket.WebSocketHandler::onClose(Lcom/goodow/realtime/json/JsonObject;)(e);
    };
    this.onmessage = function(e) {
      handler.@com.goodow.realtime.core.WebSocket.WebSocketHandler::onMessage(Ljava/lang/String;)(e.data);
    };
    this.onerror = function(e) {
      handler.@com.goodow.realtime.core.WebSocket.WebSocketHandler::onError(Ljava/lang/String;)(e);
    };
  }-*/;
  
  private native int nativeGetReadyState() /*-{
    return this.readyState;
  }-*/;
  // @formatter:on

}