package com.goodow.realtime.html;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.channel.impl.DefaultMessage;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("good.channel")
@SuppressWarnings("rawtypes")
@Export("WebSocketBusClient")
public abstract class BusOverlay implements ExportOverlay<HtmlWebSocketBusClient> {
  @ExportPackage("good.channel._ExportOverlay_")
  @Export
  public interface DefaultMessageOverlay extends ExportOverlay<DefaultMessage> {
    // @ExportInstanceMethod
    // public static void reply(DefaultMessage instance, JsonElement message) {
    // instance.reply(message);
    // }

    public abstract String address();

    public abstract Object body();

    public abstract void fail(int failureCode, String message);

    // public abstract void reply();

    // public abstract void reply(JsonElement message);

    public abstract <T> void reply(JsonElement message, Handler<Message<T>> replyHandler);

    public abstract String replyAddress();
  }

  // @ExportPackage("good.channel._ExportOverlay_")
  // @ExportClosure
  // public interface __HandlerOverlay__ extends ExportOverlay<Handler> {
  // void handle(Object message);
  // }

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

  public abstract Bus publish(String address, JsonElement message);

  public abstract Bus registerHandler(String address, Handler<? extends Message> handler);

  public abstract <T> Bus send(String address, JsonElement message, Handler<Message<T>> replyHandler);

  public abstract Bus unregisterHandler(String address, Handler<? extends Message> handler);
}
