/*
 * Copyright 2014 Goodow.com
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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.impl.WebSocketBusClient;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.json.JsonObject;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("good.channel")
@Export
abstract class ChannelOverlay implements ExportOverlay<WebSocketBusClient> {
  // @ExportPackage("good.channel._ExportOverlay_")
  // @ExportClosure
  // public interface __HandlerOverlay__ extends ExportOverlay<Handler> {
  // void handle(Object event);
  // }

  @ExportPackage("good.channel")
  @Export
  interface HandlerRegistrationOverlay extends ExportOverlay<HandlerRegistration> {
    void unregisterHandler();
  }
  @ExportPackage("good.channel")
  @Export
  @SuppressWarnings("rawtypes")
  interface MessageOverlay extends ExportOverlay<Message> {
    public abstract String address();

    public abstract Object body();

    public abstract void fail(int failureCode, String msg);

    // public abstract <T> void reply(Object msg, Handler<Message<T>> replyHandler);

    public abstract String replyAddress();
  }

  /**
   * Customized JS code to execute after good.channel has been exported.
   */
  @ExportAfterCreateMethod
  // @formatter:off
  public static native void afterCreate() /*-{
    var _ = $wnd.good.channel.WebSocketBusClient.prototype;
    _.send = function(address, msg, replyHandler) {
      this.g.@com.goodow.realtime.channel.impl.WebSocketBusClient::send(Ljava/lang/String;Ljava/lang/Object;Lcom/goodow/realtime/core/Handler;)
          (address, msg, replyHandler);
    };
    
    _ = $wnd.good.channel.Message.prototype;
    _.reply = function(msg, replyHandler) {
      this.g.@com.goodow.realtime.channel.impl.DefaultMessage::reply(Ljava/lang/Object;Lcom/goodow/realtime/core/Handler;)
          (msg, replyHandler);
    };
  }-*/;
  // @formatter:off

  @ExportConstructor
  public static WebSocketBusClient constructor(String url) {
    return new WebSocketBusClient(url, null);
  }

  @ExportConstructor
  public static WebSocketBusClient constructor(String url, JsonObject options) {
    return new WebSocketBusClient(url, options);
  }

  private ChannelOverlay() {
  }

  public abstract void close();

  public abstract State getReadyState();

  public abstract void login(String username, String password, Handler<JsonObject> replyHandler);

  public abstract Bus publish(String address, Object msg);

  public abstract void reconnect();

  // public abstract <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler);

  @SuppressWarnings("rawtypes")
  public abstract HandlerRegistration registerHandler(String address, Handler<? extends Message> handler);
}
