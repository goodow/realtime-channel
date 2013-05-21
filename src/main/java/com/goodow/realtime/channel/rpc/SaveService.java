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

import com.goodow.realtime.channel.operation.GenericOperationChannel.SendOpService;
import com.goodow.realtime.channel.rpc.Constants.Params;
import com.goodow.realtime.operation.Operation;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.MapFromStringToString;

/**
 * Low-level service that submits a delta to a wave. Does not handle being called while another
 * delta is still in flight, that is a job for the channel layer above.
 */
public class SaveService<O extends Operation<?>> implements SendOpService<O> {
  private static final Logger log = Logger.getLogger(SaveService.class.getName());
  private final Rpc rpc;
  private final String id;
  private final String token;

  public SaveService(Rpc rpc, String id, String token) {
    this.rpc = rpc;
    this.id = id;
    this.token = token;
  }

  @Override
  public void callbackNotNeeded(SendOpService.Callback callback) {
    // nothing
  }

  @Override
  public void requestRevision(String sessionId, final SendOpService.Callback callback) {
    MapFromStringToString params = Collections.mapFromStringToString();
    params.put(Params.ID, id);
    params.put(Params.ACCESS_TOKEN, token);
    rpc.get(Constants.Services.REVISION, params, new Rpc.RpcCallback() {
      @Override
      public void onConnectionError(Throwable e) {
        callback.onConnectionError(e);
      }

      @Override
      public void onFatalError(Throwable e) {
        callback.onFatalError(e);
      }

      @Override
      public void onSuccess(String data) {
        log.log(Level.FINE, data);
        JsonObject connectResponse = RpcUtil.evalPrefixed(data);
        callback.onSuccess((int) connectResponse.getNumber(Constants.Params.REVISION));
      }
    });
  }

  @Override
  public void submitOperations(String sessionId, int revision, ArrayOf<O> operations,
      final SendOpService.Callback callback) {
    MapFromStringToString params = Collections.mapFromStringToString();
    params.put(Params.ID, id);
    params.put(Params.ACCESS_TOKEN, token);
    params.put(Params.SESSION_ID, sessionId);
    JsonObject formData = Json.createObject();
    formData.put(Params.REVISION, revision);
    formData.put(Params.CHANGES, serialize(operations));
    rpc.post(Constants.Services.SAVE, params, formData.toJson(), new Rpc.RpcCallback() {
      @Override
      public void onConnectionError(Throwable e) {
        callback.onConnectionError(e);
      }

      @Override
      public void onFatalError(Throwable e) {
        callback.onFatalError(e);
      }

      @Override
      public void onSuccess(String data) {
        JsonObject json = RpcUtil.evalPrefixed(data);
        callback.onSuccess((int) json.getNumber(Constants.Params.REVISION));
      }
    });
  }

  protected JsonValue serialize(ArrayOf<O> ops) {
    return Json.instance().parse("[" + ops.join() + "]");
  }
}
