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
package com.goodow.realtime.core;

import com.goodow.realtime.channel.State;
import com.goodow.realtime.json.JsonObject;

public interface WebSocket {
  /**
   * Listens for events on a {@link WebSocket}.
   */
  interface WebSocketHandler {

    /**
     * Called when the socket is closed. When the socket is closed, it cannot be reopened.
     */
    void onClose(JsonObject reason);

    /**
     * Called when an error occurs on the socket.
     */
    void onError(String error);

    /**
     * Called when the socket receives a message.
     */
    void onMessage(String message);

    /**
     * Called when the socket is ready to receive messages.
     */
    void onOpen();
  }

  /**
   * Close the socket. The socket cannot be used again after calling close; the server must create a
   * new socket.
   */
  void close();

  State getReadyState();

  void send(String data);

  void setListen(WebSocketHandler handler);
}