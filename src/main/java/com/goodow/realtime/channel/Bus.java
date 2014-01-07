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
package com.goodow.realtime.channel;

import com.goodow.realtime.core.Handler;

public interface Bus {
  String LOCAL = "@";
  String LOCAL_ON_OPEN = LOCAL + "bus.onOpen";
  String LOCAL_ON_CLOSE = LOCAL + "bus.onClose";
  String LOCAL_ON_ERROR = LOCAL + "bus.onError";

  /**
   * Close the EventBus and release all resources.
   */
  void close();

  State getReadyState();

  /**
   * Publish a message
   * 
   * @param address The address to publish it to
   * @param msg The message
   */
  Bus publish(String address, Object msg);

  /**
   * Registers a handler against the specified address
   * 
   * @param address The address to register it at
   * @param handler The handler
   */
  @SuppressWarnings("rawtypes")
  Bus registerHandler(String address, Handler<? extends Message> handler);

  /**
   * Send a message
   * 
   * @param address The address to send it to
   * @param msg The message
   * @param replyHandler Reply handler will be called when any reply from the recipient is received
   */
  <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler);

  /**
   * Unregisters a handler given the address and the handler
   * 
   * @param address The address the handler was registered at
   * @param handler The handler
   */
  @SuppressWarnings("rawtypes")
  Bus unregisterHandler(String address, Handler<? extends Message> handler);
}
