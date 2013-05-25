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
package com.goodow.realtime.channel.operation;

import com.goodow.realtime.Document;
import com.goodow.realtime.DocumentBridge;
import com.goodow.realtime.DocumentLoadedHandler;
import com.goodow.realtime.DocumentSaveStateChangedEvent;
import com.goodow.realtime.Error;
import com.goodow.realtime.Error.ErrorHandler;
import com.goodow.realtime.Model;
import com.goodow.realtime.ModelInitializerHandler;
import com.goodow.realtime.Realtime;
import com.goodow.realtime.channel.PollChannel;
import com.goodow.realtime.channel.RealtimeChannelDemuxer;
import com.goodow.realtime.channel.operation.GenericOperationChannel.ReceiveOpChannel;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.channel.rpc.SaveService;
import com.goodow.realtime.channel.rpc.SnapshotService;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.RealtimeTransformer;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonValue;

/*-[
 #import "GDRRealtime+OCNI.h"
 #import "GDRError+OCNI.h"
 ]-*/
public class RealtimeOperationSucker implements
    GenericOperationChannel.Listener<RealtimeOperation<?>> {
  private static final Logger logger = Logger.getLogger(RealtimeOperationSucker.class.getName());

  private static final RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();
  private final String id;
  private final String userId;
  private GenericOperationChannel<RealtimeOperation<?>> channel;
  private Rpc rpc;
  private RealtimeTransformer transformer;
  private OperationSink<RealtimeOperation<?>> outputSink;
  private ReceiveOpChannel<RealtimeOperation<?>> receiveChannel;
  private DocumentBridge bridge;

  public RealtimeOperationSucker(final String id, String userId) {
    this.id = id;
    this.userId = userId;
  }

  public void load(final DocumentLoadedHandler onLoaded,
      final ModelInitializerHandler opt_initializer, ErrorHandler opt_error) {
    DocumentBridge snapshot = demuxer.getSnapshot(id);
    if (snapshot != null) {
      loadDoucument(onLoaded, snapshot.document);
      return;
    }
    init();
    SnapshotService snapshotService = new SnapshotService(rpc);
    snapshotService.load(id, Realtime.getToken(), true, new SnapshotService.Callback() {
      @Override
      public void onSuccess(JsonValue snapshot, String sessionId, int revision) {
        bridge = transformer.createSnapshot(snapshot);
        demuxer.register(id, bridge, (ReceiveOpChannelImpl<?>) receiveChannel);
        bridge.userId = userId;
        bridge.sessionId = sessionId;
        bridge.outputSink = outputSink;
        channel.connect(revision, sessionId);
        if (revision == 1) {
          bridge.createRoot();
          if (opt_initializer != null) {
            initializeModel(opt_initializer, bridge.model);
          }
        }
        loadDoucument(onLoaded, bridge.document);
        PollChannel.get().connect(sessionId);
      }
    });
  }

  @Override
  public void onAck(RealtimeOperation<?> serverHistoryOp, boolean clean) {
  }

  @Override
  public void onError(Throwable e) {
    logger.log(Level.WARNING, "Unable to save", e);
  }

  @Override
  public void onRemoteOp(RealtimeOperation<?> serverHistoryOp) {
    while (channel.peek() != null) {
      bridge.consume(channel.receive());
    }
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    DocumentSaveStateChangedEvent event =
        new DocumentSaveStateChangedEvent(bridge.document, isSaving, isPending);
    bridge.fireDocumentSaveStateChangedEvent(event);
  }

  // @formatter:off
  private native void __ocniHandleError__(Object opt_error, Error error) /*-[
    GDRErrorBlock block = (GDRErrorBlock) opt_error;
    return block(error);
  ]-*/ /*-{
  }-*/;

  private native void __ocniInitializeModel__(Object opt_initializer, Model model) /*-[
    GDRModelInitializerBlock block = (GDRModelInitializerBlock) opt_initializer;
    return block(model);
  ]-*/ /*-{
  }-*/;
  
  private native void __ocniLoadDoucument__(Object onLoaded, Document document) /*-[
    GDRDocumentLoadedBlock block = (GDRDocumentLoadedBlock) onLoaded;
    return block(document);
  ]-*/ /*-{
  }-*/;
  // @formatter:on

  @SuppressWarnings("cast")
  private void handlerError(ErrorHandler opt_error, Error error) {
    if (opt_error instanceof ErrorHandler) {
      opt_error.handleError(error);
    } else {
      __ocniHandleError__(opt_error, error);
    }
  }

  private void init() {
    this.rpc = demuxer.getRpc();
    transformer = new RealtimeTransformer();
    receiveChannel = new ReceiveOpChannelImpl<RealtimeOperation<?>>(id, rpc, transformer);
    TransformQueue<RealtimeOperation<?>> queue =
        new TransformQueue<RealtimeOperation<?>>(transformer);
    SaveService<RealtimeOperation<?>> saveService =
        new SaveService<RealtimeOperation<?>>(rpc, id, Realtime.getToken());
    channel =
        new GenericOperationChannel<RealtimeOperation<?>>(queue, receiveChannel, saveService, this);
    outputSink = new OperationSink<RealtimeOperation<?>>() {
      @Override
      public void consume(RealtimeOperation<?> op) {
        channel.send(op);
      }
    };
  }

  @SuppressWarnings("cast")
  private void initializeModel(ModelInitializerHandler opt_initializer, Model model) {
    if (opt_initializer instanceof ModelInitializerHandler) {
      opt_initializer.onInitializer(model);
    } else {
      __ocniInitializeModel__(opt_initializer, model);
    }
  }

  @SuppressWarnings("cast")
  private void loadDoucument(final DocumentLoadedHandler onLoaded, Document document) {
    if (onLoaded instanceof DocumentLoadedHandler) {
      onLoaded.onLoaded(document);
    } else {
      __ocniLoadDoucument__(onLoaded, document);
    }
  }
}