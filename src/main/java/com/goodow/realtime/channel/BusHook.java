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
package com.goodow.realtime.channel;

import com.goodow.realtime.core.Handler;

/**
 * A hook that you can use to receive various events on the Bus.
 */
public interface BusHook {
  /**
   * Called when the bus is opened
   */
  void handleOpened();

  /**
   * Called when the bus is closed
   */
  void handlePostClose();

  /**
   * Called before close the bus
   * 
   * @return true to close the bus, false to reject it
   */
  boolean handlePreClose();

  /**
   * Called before register a handler
   * 
   * @param address The address
   * @param handler The handler
   * @return true to let the registration occur, false otherwise
   */
  @SuppressWarnings("rawtypes")
  boolean handlePreRegister(String address, Handler<? extends Message> handler);

  /**
   * Called when a message is received
   * 
   * @param message The message
   * @return true To allow the message to deliver, false otherwise
   */
  boolean handleReceiveMessage(Message<?> message);

  /**
   * Called when sending or publishing on the bus
   * 
   * @param send if true it's a send else it's a publish
   * @param address The address the message is being sent/published to
   * @param msg The message
   * @param replyHandler Reply handler will be called when any reply from the recipient is received
   * @return true To allow the send/publish to occur, false otherwise
   */
  <T> boolean handleSendOrPub(boolean send, String address, Object msg,
      Handler<Message<T>> replyHandler);

  /**
   * Called when unregistering a handler
   * 
   * @param address The address
   * @return true to let the unregistration occur, false otherwise
   */
  boolean handleUnregister(String address);
}
