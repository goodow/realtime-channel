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

import com.goodow.realtime.channel.impl.WebSocketBusClient;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.JsonObject;

public class HtmlWebSocketBusClient extends WebSocketBusClient {

  // @formatter:off 
  private static native <T> void handle(T message, Object handler) /*-{
    handler(@org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(message));
  }-*/;
  // @formatter:on

  public HtmlWebSocketBusClient(String url, JsonObject options) {
    super(url, options);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> void nativeHandle(T message, Object handler) {
    if (handler instanceof Handler) {
      ((Handler<T>) handler).handle(message);
    } else {
      handle(message, handler);
    }
  }
}