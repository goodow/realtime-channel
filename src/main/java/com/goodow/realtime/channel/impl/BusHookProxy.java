package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;

public abstract class BusHookProxy implements BusHook {
  @Override
  public void handleOpened() {
    if (delegate() != null) {
      delegate().handleOpened();
    }
  }

  @Override
  public void handlePostClose() {
    if (delegate() != null) {
      delegate().handlePostClose();
    }
  }

  @Override
  public boolean handlePreClose() {
    return delegate() == null ? true : delegate().handlePreClose();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean handlePreSubscribe(String topic, Handler<? extends Message> handler) {
    return delegate() == null ? true : delegate().handlePreSubscribe(topic, handler);
  }

  @Override
  public boolean handleReceiveMessage(Message<?> message) {
    return delegate() == null ? true : delegate().handleReceiveMessage(message);
  }

  @Override
  public <T> boolean handleSendOrPub(boolean send, String topic, Object msg,
      Handler<Message<T>> replyHandler) {
    return delegate() == null ? true : delegate().handleSendOrPub(send, topic, msg,
        replyHandler);
  }

  @Override
  public boolean handleUnsubscribe(String topic) {
    return delegate() == null ? true : delegate().handleUnsubscribe(topic);
  }

  protected abstract BusHook delegate();
}