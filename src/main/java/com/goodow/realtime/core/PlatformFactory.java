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
package com.goodow.realtime.core;

/**
 * Generic platform interface. New platforms are defined as implementations of this interface.
 */
public interface PlatformFactory {

  /**
   * Cancel the timer with the specified {@code id}. Returns {@code} true if the timer was
   * successfully cancelled, or {@code false} if the timer does not exist.
   */
  boolean cancelTimer(int id);

  Net net();

  /**
   * A deferred command is executed after the event loop returns.
   */
  void scheduleDeferred(VoidHandler handler);

  /**
   * Set a periodic timer to fire every {@code delayMs} milliseconds, at which point {@code handler}
   * will be called with the id of the timer.
   * 
   * @return the unique ID of the timer
   */
  int setPeriodic(int delayMs, VoidHandler handler);

  Platform.Type type();
}
