/*
 * Copyright 2012 Goodow.com
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
package com.goodow.realtime.channel.rpc;

import elemental.json.JsonException;
import elemental.util.MapFromStringToString;

/**
 * Interface for an RPC mechanism.
 */
public interface Rpc {

  public enum ConnectionState {
    /** Normal. */
    CONNECTED,
    /** Cannot connect to server, or too many "retryable" failures. */
    OFFLINE,
    /** Authentication problem - user needs to log back in. */
    LOGGED_OUT,
    /** A refresh is required, at the user's discretion. */
    SOFT_RELOAD,
    /**
     * A refresh is required, and no further rpcs should be sent. The server could send a signal to
     * the client to put it into this state, e.g. if we are fixing a critical DOS bug or if we are
     * making an incompatible change to the client-server protocol.
     */
    HARD_RELOAD;

    /**
     * Returns if the state transition is permitted.
     * 
     * States can only monotonically increase after reaching SOFT_RELOAD.
     * 
     * E.g. once we're broken in some way that is not likely to be recoverable, and we must reload,
     * we shouldn't leave that state due to some unrelated RPC happening to succeed.
     * 
     */
    public boolean canTransitionTo(ConnectionState newState) {
      assert newState != null : "Null newState";
      boolean onlyMonotonic = this.ordinal() >= ConnectionState.SOFT_RELOAD.ordinal();
      return !onlyMonotonic || newState.ordinal() > this.ordinal();
    }
  }

  public interface ConnectionStateListener {
    void connectionStateChanged(ConnectionState newConnectionState);
  }

  enum Method {
    GET, POST;
  }

  public interface RpcCallback {

    /**
     * Called when some problem occurred in the RPC layer, and the request may be retryable.
     * 
     * Examples include server 500 errors, and connection time outs.
     */
    void onConnectionError(Throwable e);

    /**
     * Called for fatal errors where the request would not be retryable.
     * 
     * Examples include 40x status codes and garbled response data.
     */
    void onFatalError(Throwable e);

    /**
     * Called on success with respect to the RPC layer.
     * 
     * The implementor may throw {@link JsonException} if the data is in fact invalid with respect
     * to the application logic. This will then be treated as a fatal error.
     */
    void onSuccess(String data);
  }

  public interface RpcHandle {
    /**
     * Drops the rpc request this is a handle for. Neither its success or failure methods will be
     * called after this. It is always valid to call this method.
     */
    void drop();

    /**
     * @return true if the request is still pending. false if either the success or failure methods
     *         have been called (or are being called), or if the request was dropped (by calling
     *         {@link #drop()}).
     */
    boolean isPending();
  }

  /**
   * Make a get request to the backend.
   * 
   * @param serviceName RPC service name (e.g. "fetch")
   * @param params parameter key value pairs. For convenience, null values are accepted, and that
   *          parameter is just ignored.
   * @param rpcCallback called back on success or failure.
   * @return a handle to the request, or null if sending failed
   */
  RpcHandle get(String serviceName, MapFromStringToString params, final Rpc.RpcCallback rpcCallback);

  /**
   * Modifies the state if the transition is permitted.
   */
  void maybeSetConnectionState(ConnectionState state);

  /**
   * Make a post request to the backend.
   * 
   * @param serviceName RPC service name (e.g. "fetch")
   * @param params parameter key value pairs. For convenience, null values are accepted, and that
   *          parameter is just ignored.
   * @param rpcCallback called back on success or failure.
   * @return a handle to the request, or null if sending failed
   */
  RpcHandle post(String serviceName, MapFromStringToString params, String formData,
      final Rpc.RpcCallback rpcCallback);
}