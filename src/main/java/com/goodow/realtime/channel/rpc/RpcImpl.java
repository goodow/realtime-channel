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

import com.goodow.realtime.channel.http.HttpRequest;
import com.goodow.realtime.channel.http.HttpRequestCallback;
import com.goodow.realtime.channel.http.HttpResponse;
import com.goodow.realtime.channel.util.ChannelNative;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonException;
import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromIntTo;
import elemental.util.MapFromStringToString;

/**
 * Ajax HTTP-Request based implementation of the Rpc interface.
 */
public class RpcImpl implements Rpc {
  private final class Handle implements RpcHandle {
    private final int id;

    Handle(int id) {
      this.id = id;
    }

    @Override
    public void drop() {
      dropRequest(this);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Handle) {
        return id == ((Handle) other).id;
      }
      return false;
    }

    public int getId() {
      return id;
    }

    @Override
    public int hashCode() {
      return id * 43;
    }

    @Override
    public boolean isPending() {
      return handles.hasKey(id);
    }
  }

  private enum Result {
    OK, PERMANENT_FAILURE, RETRYABLE_FAILURE
  }

  private static final int MAX_CONSECUTIVE_FAILURES = 20;

  static void dropRequest(Handle handle) {
    handles.remove(handle.getId());
  }

  /** Root prefix to prepend to service urls */
  private final String rpcRoot;
  private final ConnectionStateListener listener;

  private ConnectionState connectionState = ConnectionState.CONNECTED;
  private int consecutiveFailures = 0;

  /** Log stream to use */
  private static final Logger log = Logger.getLogger(RpcImpl.class.getName());

  /** Incrementing id counter */
  private static int nextRequestId;

  private static MapFromIntTo<Handle> handles = Collections.mapFromIntTo();

  /**
   * Drops all pending AJAX requests
   */
  public static void dropAll() {
    ArrayOfInt keys = handles.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      handles.remove(keys.get(i));
    }
  }

  /**
   * @param rpcRoot root prefix to prepend to service urls
   */
  public RpcImpl(final String rpcRoot, ConnectionStateListener listener) {
    this.rpcRoot = rpcRoot;
    this.listener = listener;
  }

  @Override
  public RpcHandle get(String serviceName, MapFromStringToString params, RpcCallback rpcCallback) {
    return makeRequest(Method.GET, serviceName, params, null, rpcCallback);
  }

  /**
   * {@inheritDoc}
   * 
   * If a change occurs, notifies the listener.
   */
  @Override
  public void maybeSetConnectionState(ConnectionState newState) {
    if (newState != connectionState && connectionState.canTransitionTo(newState)) {
      connectionState = newState;
      if (listener != null) {
        listener.connectionStateChanged(connectionState);
      }
    }
  }

  @Override
  public RpcHandle post(String serviceName, MapFromStringToString params, String formData,
      RpcCallback rpcCallback) {
    return makeRequest(Method.POST, serviceName, params, formData, rpcCallback);
  }

  private StringBuilder addParams(final StringBuilder b, MapFromStringToString params) {
    ArrayOfString keys = params.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      String value = params.get(key);
      if (value != null) {
        b.append(key + "=" + ChannelNative.get().escapeUriQuery(value) + "&");
      }
    }
    return b;
  }

  private RpcHandle makeRequest(Method method, String serviceName, MapFromStringToString params,
      String requestData, final Rpc.RpcCallback rpcCallback) {
    final int requestId = nextRequestId;
    nextRequestId++;

    // See the javadoc for HARD_RELOAD.
    if (connectionState == ConnectionState.HARD_RELOAD) {
      return new Handle(requestId);
    }

    StringBuilder urlBuilder = new StringBuilder(rpcRoot + "/" + serviceName + "?");
    addParams(urlBuilder, params);
    final String url = urlBuilder.toString();

    HttpRequest r = ChannelNative.get().getHttpTransport().buildRequest(method.name(), url);
    // if (method == Method.POST) {
    // r.setContentType("application/x-www-form-urlencoded");
    // r.addHeader("X-Same-Domain", "true");
    // }

    // log.log(Level.INFO, "RPC Request, id=" + requestId + " method=" + method.name() + " urlSize="
    // + url.length() + " bodySize=" + (requestData == null ? 0 : requestData.length()));

    class RpcRequestCallback implements HttpRequestCallback {
      final int id;
      final RpcCallback callback;
      final String url_;

      RpcRequestCallback(int id, RpcCallback callback, String url_) {
        this.id = id;
        this.callback = callback;
        this.url_ = url_;
      }

      @Override
      public void onFailure(Throwable exception) {
        if (!handles.hasKey(id)) {
          log.log(Level.INFO, "RPC FailureDrop, id=" + id + " " + exception.getMessage());
          return;
        }
        removeHandle();
        error(exception);
      }

      @Override
      public void onResponse(HttpResponse response) {
        RpcHandle handle = handles.get(id);
        if (handle == null) {
          // It's been dropped
          log.log(Level.INFO, "RPC SuccessDrop, id=" + id);
          return;
        }

        // Clear it now, before callbacks
        removeHandle();

        int statusCode = response.getStatusCode();
        String data = response.getContent();

        Result result;
        if (statusCode < 100) {
          result = Result.RETRYABLE_FAILURE;
          maybeSetConnectionState(ConnectionState.OFFLINE);
        } else if (statusCode == 200) {
          result = Result.OK;
          maybeSetConnectionState(ConnectionState.CONNECTED);
          consecutiveFailures = 0;
        } else if (statusCode >= 500) {
          result = Result.RETRYABLE_FAILURE;
          consecutiveFailures++;
          if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
            maybeSetConnectionState(ConnectionState.OFFLINE);
          } else {
            maybeSetConnectionState(ConnectionState.CONNECTED);
          }
        } else {
          result = Result.PERMANENT_FAILURE;
          maybeSetConnectionState(ConnectionState.SOFT_RELOAD);
        }

        switch (result) {
          case OK:
            // log.log(Level.INFO, "RPC Success, id=" + id);
            try {
              callback.onSuccess(data);
            } catch (JsonException e) {
              // Semi-HACK: Treat parse errors as login problems
              // due to loading a login or authorization page. (It's unlikely
              // we'd otherwise get a parse error from a 200 OK result).
              // The simpler solution of detecting redirects is not possible
              // with XmlHttpRequest, the web is unfortunately broken.

              // TODO Possible alternatives:
              // either change our server side to not require
              // login through web.xml but to check if UserService says currentUser==null (or
              // whatever it does if not logged in) and return a well-defined "not logged in"
              // response instead, or to prefix all responses from the server with a fixed string
              // (like we do with "OK" elsewhere) and assume not logged in if that prefix is
              // missing. We could strip off that prefix here and make it transparent to the
              // callbacks.

              maybeSetConnectionState(ConnectionState.LOGGED_OUT);

              error(new Exception("RPC failed due to message exception, treating as auth failure"
                  + ", status code: " + statusCode + ", data: " + data));
            }
            break;
          case RETRYABLE_FAILURE:
            error(new Exception("RPC failed, status code: " + statusCode + ", data: " + data));
            break;
          case PERMANENT_FAILURE:
            fatal(new Exception("RPC bad request, status code: " + statusCode + ", data: " + data));
            break;
          default:
            throw new AssertionError("Unknown result " + result);
        }
      }

      private void error(Throwable e) {
        log.log(Level.WARNING, "RPC Failure, id=" + id + " " + e.getMessage() + " Request url:"
            + url_, e);
        callback.onConnectionError(e);
      }

      private void fatal(Throwable e) {
        log.log(Level.WARNING, "RPC Bad Request, id=" + id + " " + e.getMessage() + " Request url:"
            + url_, e);
        callback.onFatalError(e);
      }

      private void removeHandle() {
        handles.remove(id);
      }
    }

    RpcRequestCallback innerCallback = new RpcRequestCallback(requestId, rpcCallback, url);

    // TODO: store the Request object somewhere so we can e.g. cancel it
    Handle handle = new Handle(requestId);
    handles.put(handle.getId(), handle);
    r.executeAsync(innerCallback, requestData);
    return handle;
  }
}