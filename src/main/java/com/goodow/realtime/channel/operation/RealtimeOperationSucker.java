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

import com.goodow.realtime.DocumentBridge;
import com.goodow.realtime.DocumentLoadedHandler;
import com.goodow.realtime.DocumentSaveStateChangedEvent;
import com.goodow.realtime.Error.ErrorHandler;
import com.goodow.realtime.ModelInitializerHandler;
import com.goodow.realtime.Realtime;
import com.goodow.realtime.channel.PollingChannel;
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

public class RealtimeOperationSucker implements
    GenericOperationChannel.Listener<RealtimeOperation<?>> {
  private static final Logger logger = Logger.getLogger(RealtimeOperationSucker.class.getName());

  private final GenericOperationChannel<RealtimeOperation<?>> channel;
  private static final RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();
  private final String id;
  private final String userId;
  private final Rpc rpc;
  private final RealtimeTransformer transformer;
  private final OperationSink<RealtimeOperation<?>> outputSink;
  private final ReceiveOpChannel<RealtimeOperation<?>> receiveChannel;
  private DocumentBridge bridge;

  public RealtimeOperationSucker(final String id, String userId) {
    this.id = id;
    this.userId = userId;
    this.rpc = demuxer.getRpc();
    transformer = new RealtimeTransformer();
    receiveChannel = new ReceiveOpChannelImpl<RealtimeOperation<?>>(id, rpc, transformer);
    TransformQueue<RealtimeOperation<?>> queue =
        new TransformQueue<RealtimeOperation<?>>(transformer);
    SaveService<RealtimeOperation<?>> submitService =
        new SaveService<RealtimeOperation<?>>(rpc, id, Realtime.getToken());
    channel =
        new GenericOperationChannel<RealtimeOperation<?>>(queue, receiveChannel, submitService,
            this);
    outputSink = new OperationSink<RealtimeOperation<?>>() {
      @Override
      public void consume(RealtimeOperation<?> op) {
        channel.send(op);
      }
    };
  }

  public void load(final DocumentLoadedHandler onLoaded,
      final ModelInitializerHandler opt_initializer, ErrorHandler opt_error) {
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
            opt_initializer.onInitializer(bridge.model);
          }
        }
        onLoaded.onLoaded(bridge.document);
        PollingChannel.get().connect(sessionId);
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
}