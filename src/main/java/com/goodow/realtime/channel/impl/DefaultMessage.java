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
package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;

class DefaultMessage<U> implements Message<U> {

  protected U body;
  protected Bus bus;
  protected String address;
  protected String replyAddress;
  protected boolean send; // Is it a send or a publish?

  public DefaultMessage(boolean send, Bus bus, String address, String replyAddress, U body) {
    this.send = send;
    this.bus = bus;
    this.address = address;
    this.replyAddress = replyAddress;
    this.body = body;
  }

  @Override
  public String address() {
    return address;
  }

  @Override
  public U body() {
    return body;
  }

  @Override
  public void fail(int failureCode, String msg) {
    // sendReply(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, failureCode, message), null);
  }

  @Override
  public void reply(Object msg) {
    sendReply(msg, null);
  }

  @Override
  public <T> void reply(Object msg, Handler<Message<T>> replyHandler) {
    sendReply(msg, replyHandler);
  }

  @Override
  public String replyAddress() {
    return replyAddress;
  }

  @Override
  public String toString() {
    return body == null ? null : body.toString();
  }

  private <T> void sendReply(Object msg, Handler<Message<T>> replyHandler) {
    if (bus != null && replyAddress != null) {
      // Send back reply
      bus.send(replyAddress, msg, replyHandler);
    }
  }
}
