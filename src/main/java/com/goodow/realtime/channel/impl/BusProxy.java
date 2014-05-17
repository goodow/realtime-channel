package com.goodow.realtime.channel.impl;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.BusHook;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.State;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;

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
  public Bus publish(String address, Object msg) {
    return delegate.publish(address, msg);
  }

  @Override
  public Bus publishLocal(String address, Object msg) {
    return delegate.publishLocal(address, msg);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public HandlerRegistration registerHandler(String address, Handler<? extends Message> handler) {
    return delegate.registerHandler(address, handler);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public HandlerRegistration registerLocalHandler(String address, Handler<? extends Message> handler) {
    return delegate.registerLocalHandler(address, handler);
  }

  @Override
  public <T> Bus send(String address, Object msg, Handler<Message<T>> replyHandler) {
    return delegate.send(address, msg, replyHandler);
  }

  @Override
  public <T> Bus sendLocal(String address, Object msg, Handler<Message<T>> replyHandler) {
    return delegate.sendLocal(address, msg, replyHandler);
  }

  @Override
  public Bus setHook(BusHook hook) {
    this.hook = hook;
    return this;
  }
}