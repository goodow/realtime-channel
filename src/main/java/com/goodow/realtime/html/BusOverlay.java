package com.goodow.realtime.html;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.impl.DefaultMessage;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.JsonObject;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("good.channel")
@Export("WebSocketBusClient")
abstract class BusOverlay implements ExportOverlay<HtmlWebSocketBusClient> {
  // @ExportPackage("good.channel._ExportOverlay_")
  // @ExportClosure
  // public interface __HandlerOverlay__ extends ExportOverlay<Handler> {
  // void handle(Object event);
  // }

  @ExportPackage("good.channel")
  @Export
  @SuppressWarnings("rawtypes")
  interface DefaultMessageOverlay extends ExportOverlay<DefaultMessage> {
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
      this.g.@com.goodow.realtime.html.HtmlWebSocketBusClient::send(Ljava/lang/String;Ljava/lang/Object;Lcom/goodow/realtime/core/Handler;)
          (address, msg, replyHandler);
    };
    
    _ = $wnd.good.channel.DefaultMessage.prototype;
    _.reply = function(msg, replyHandler) {
      this.g.@com.goodow.realtime.channel.impl.DefaultMessage::reply(Ljava/lang/Object;Lcom/goodow/realtime/core/Handler;)
          (msg, replyHandler);
    };
  }-*/;
  // @formatter:off

  @ExportConstructor
  public static HtmlWebSocketBusClient constructor(String url) {
    return new HtmlWebSocketBusClient(url, null);
  }

  @ExportConstructor
  public static HtmlWebSocketBusClient constructor(String url, JsonObject options) {
    return new HtmlWebSocketBusClient(url, options);
  }

  private BusOverlay() {
  }

  public abstract void close();

  public abstract State getReadyState();

  public abstract void login(String username, String password, Handler<JsonObject> replyHandler);

  public abstract Bus publish(String address, Object msg);

  public abstract void reconnect();

  // public abstract <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler);

  @SuppressWarnings("rawtypes")
  public abstract Bus registerHandler(String address, Handler<? extends Message> handler);

  @SuppressWarnings("rawtypes")
  public abstract Bus unregisterHandler(String address, Handler<? extends Message> handler);
}
