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
package com.goodow.realtime.objc;

import com.goodow.realtime.core.WebSocket;
import com.goodow.realtime.json.JsonObject;

/**
 * ObjCWebSocket implementation of {@link WebSocket}.
 */
final class ObjCWebSocket implements WebSocket {
  ObjCWebSocket(String url, JsonObject options) {
  }

  @Override
  public native void close();

  @Override
  public native void send(String data);

  @Override
  public native void setListen(WebSocketHandler handler);
}