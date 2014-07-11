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
package com.goodow.realtime.channel.server.impl;

import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;

class VertxMessage<T> implements Message<T> {
  private final VertxBus bus;
  private final org.vertx.java.core.eventbus.Message<T> delegate;

  public VertxMessage(VertxBus bus, org.vertx.java.core.eventbus.Message<T> delegate) {
    this.bus = bus;
    this.delegate = delegate;
  }

  @Override
  public String topic() {
    return delegate.address();
  }

  @SuppressWarnings("unchecked")
  @Override
  public T body() {
    return (T) VertxBus.unwrapMsg(delegate.body());
  }

  @Override
  public void fail(int failureCode, String msg) {
    delegate.fail(failureCode, msg);
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public void reply(Object msg) {
    reply(msg, null);
  }

  @SuppressWarnings("hiding")
  @Override
  public <T> void reply(Object msg, final Handler<Message<T>> replyHandler) {
    BusHook hook = bus.getHook();
    if (hook == null || hook.handleSendOrPub(true, replyTopic(), msg, replyHandler)) {
      org.vertx.java.core.Handler<org.vertx.java.core.eventbus.Message<T>> handler =
          replyHandler == null ? null
              : new org.vertx.java.core.Handler<org.vertx.java.core.eventbus.Message<T>>() {
                @Override
                public void handle(org.vertx.java.core.eventbus.Message<T> message) {
                  VertxMessage<T> event = new VertxMessage<T>(bus, message);
                  BusHook hook = bus.getHook();
                  if (hook == null || hook.handleReceiveMessage(event)) {
                    replyHandler.handle(event);
                  }
                }
              };
      delegate.reply(VertxBus.wrapMsg(msg), handler);
    }
  }

  @Override
  public String replyTopic() {
    return delegate.replyAddress();
  }
}