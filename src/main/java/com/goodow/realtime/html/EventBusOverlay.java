package com.goodow.realtime.html;

import com.goodow.realtime.channel.EventBus;
import com.goodow.realtime.channel.EventHandler;
import com.goodow.realtime.core.WebSocket.State;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportClosure;
import org.timepedia.exporter.client.ExportConstructor;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("good.channel")
@Export
public abstract class EventBusOverlay implements ExportOverlay<EventBus> {
  @ExportPackage("good.channel._ExportOverlay_")
  @ExportClosure
  public interface __EventHandlerOverlay__ extends ExportOverlay<EventHandler<Object>> {
    void handler(Object message, EventHandler<Object> reply);
  }

  @ExportConstructor
  public static EventBus constructor() {
    return new EventBus();
  }

  @ExportConstructor
  public static EventBus constructor(String url) {
    return new EventBus(url, null);
  }

  @ExportConstructor
  public static EventBus constructor(String url, JsonObject options) {
    return new EventBus(url, options);
  }

  private EventBusOverlay() {
  }

  public abstract void close();

  public abstract State getReadyState();

  public abstract void login(String username, String password,
      final EventHandler<JsonObject> replyHandler);

  public abstract void publish(String address, JsonElement message);

  public abstract <T extends JsonElement> void registerHandler(String address,
      EventHandler<T> handler);

  public abstract <T extends JsonElement> void send(String address, JsonElement message,
      EventHandler<T> replyHandler);

  public abstract <T extends JsonElement> void unregisterHandler(String address,
      EventHandler<T> handler);
}
