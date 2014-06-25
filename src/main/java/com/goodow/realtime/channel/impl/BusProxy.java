package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;

public abstract class BusProxy implements Bus {
  protected final Bus delegate;
  protected BusHook hook;

  public BusProxy(Bus delegate) {
    this.delegate = delegate;
  }

  @Override
  public void close() {
    delegate.close();
  }

  public Bus getDelegate() {
    return delegate;
  }

  @Override
  public State getReadyState() {
    return delegate.getReadyState();
  }

  @Override
  public String getSessionId() {
    return delegate.getSessionId();
  }

  @Override
  public Bus publish(String topic, Object msg) {
    return delegate.publish(topic, msg);
  }

  @Override
  public Bus publishLocal(String topic, Object msg) {
    return delegate.publishLocal(topic, msg);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Registration subscribe(String topic, Handler<? extends Message> handler) {
    return delegate.subscribe(topic, handler);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Registration subscribeLocal(String topic, Handler<? extends Message> handler) {
    return delegate.subscribeLocal(topic, handler);
  }

  @Override
  public <T> Bus send(String topic, Object msg, Handler<Message<T>> replyHandler) {
    return delegate.send(topic, msg, replyHandler);
  }

  @Override
  public <T> Bus sendLocal(String topic, Object msg, Handler<Message<T>> replyHandler) {
    return delegate.sendLocal(topic, msg, replyHandler);
  }

  @Override
  public Bus setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }
}