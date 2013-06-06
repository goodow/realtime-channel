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

import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.util.Pair;

import elemental.util.ArrayOf;
import elemental.util.Collections;

/**
 * Simple implementation of main concurrency control logic, independent of transport concerns.
 * <p>
 * For efficiency, client ops are also compacted before transforming and before sending.
 */
public class TransformQueue<O extends Operation<?>> {
  private final Transformer<O> transformer;
  private int expectedAckedClientOps = 0;
  private boolean newClientOpSinceTransform = false;
  private ArrayOf<O> serverOps = Collections.arrayOf();
  private int version = -1;
  private ArrayOf<O> queuedClientOps = Collections.arrayOf();
  private ArrayOf<O> unackedClientOps = Collections.arrayOf();

  public TransformQueue(Transformer<O> transformer) {
    this.transformer = transformer;
  }

  /**
   * @param resultingRevision
   * @return true if all unacked ops are now acked
   */
  public boolean ackClientOp(int resultingRevision) {
    checkRevision(resultingRevision);

    assert expectedAckedClientOps == 0 : "must call expectedAck, there are "
        + expectedAckedClientOps + " expectedAckedClientOps";
    assert !unackedClientOps.isEmpty() : this + ": unackedClientOps is empty; resultingRevision="
        + resultingRevision;

    this.version = resultingRevision;

    unackedClientOps.shift();

    return unackedClientOps.isEmpty();
  }

  public ArrayOf<O> ackOpsIfVersionMatches(int newRevision) {
    if (newRevision == version + unackedClientOps.length()) {
      ArrayOf<O> expectedAckingClientOps = unackedClientOps;
      expectedAckedClientOps += expectedAckingClientOps.length();
      unackedClientOps = Collections.arrayOf();
      version = newRevision;
      return expectedAckingClientOps;
    }

    return null;
  }

  public void clientOp(O clientOp) {
    assert !clientOp.isNoOp();
    if (!serverOps.isEmpty()) {
      ArrayOf<O> cOps = Collections.arrayOf();
      cOps.push(clientOp);
      Pair<ArrayOf<O>, ArrayOf<O>> pair = transformer.transform(serverOps, cOps);
      serverOps = pair.first;
      if (pair.second.isEmpty()) {
        return;
      }
      assert pair.second.length() == 1;
      clientOp = pair.second.get(0);
      assert !clientOp.isNoOp();
    }

    queuedClientOps.push(clientOp);
    newClientOpSinceTransform = true;
  }

  public boolean expectedAck(int resultingRevision) {
    if (expectedAckedClientOps == 0) {
      return false;
    }

    assert resultingRevision == version - expectedAckedClientOps + 1 : "bad rev "
        + resultingRevision + ", current rev " + version + ", expected remaining "
        + expectedAckedClientOps;

    expectedAckedClientOps--;

    return true;
  }

  public boolean hasQueuedClientOps() {
    return !queuedClientOps.isEmpty();
  }

  public boolean hasServerOp() {
    return !serverOps.isEmpty();
  }

  public boolean hasUnacknowledgedClientOps() {
    return !unackedClientOps.isEmpty();
  }

  public void init(int revision) {
    assert this.version == -1 : "Already at a revision (" + this.version + "), can't init at "
        + revision + ")";
    assert revision >= 0 : "Initial revision must be >= 0, not " + revision;
    this.version = revision;
  }

  public O peekServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.get(0);
  }

  /**
   * Pushes the queued client ops into the unacked ops, clearing the queued ops.
   * 
   * @return see {@link #unackedClientOps()}
   */
  public ArrayOf<O> pushQueuedOpsToUnacked() {
    assert unackedClientOps.isEmpty() : "Queue contains unacknowledged operations: "
        + unackedClientOps;

    unackedClientOps = transformer.compose(queuedClientOps);
    queuedClientOps = Collections.arrayOf();

    return unackedClientOps();
  }

  public O removeServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.shift();
  }

  public int revision() {
    return version;
  }

  public void serverOp(int resultingRevision, O serverOp) {
    assert !serverOp.isNoOp();
    checkRevision(resultingRevision);
    assert expectedAckedClientOps == 0 : "server op arrived @" + resultingRevision
        + " while expecting " + expectedAckedClientOps + " client ops";
    this.version = resultingRevision;

    ArrayOf<O> sOps = null;
    if (!unackedClientOps.isEmpty()) {
      sOps = Collections.arrayOf();
      sOps.push(serverOp);
      Pair<ArrayOf<O>, ArrayOf<O>> pair = transformer.transform(sOps, unackedClientOps);
      sOps = pair.first;
      unackedClientOps = pair.second;
      if (sOps.isEmpty()) {
        return;
      }
      assert sOps.length() == 1;
      assert !sOps.get(0).isNoOp();
    }

    if (!queuedClientOps.isEmpty() && newClientOpSinceTransform) {
      queuedClientOps = transformer.compose(queuedClientOps);
      newClientOpSinceTransform = false;
    }
    if (!queuedClientOps.isEmpty()) {
      if (sOps == null) {
        sOps = Collections.arrayOf();
        sOps.push(serverOp);
      }
      Pair<ArrayOf<O>, ArrayOf<O>> pair = transformer.transform(sOps, queuedClientOps);
      queuedClientOps = pair.second;
      if (pair.first.isEmpty()) {
        return;
      }
    }

    if (sOps != null) {
      assert sOps.length() == 1;
      serverOp = sOps.get(0);
      assert !serverOp.isNoOp();
    }
    serverOps.push(serverOp);
  }

  @Override
  public String toString() {
    return "TQ{ " + version + "\n  s:" + serverOps + "\n  exp: " + expectedAckedClientOps
        + "\n  u:" + unackedClientOps + "\n  q:" + queuedClientOps + "\n}";
  }

  /**
   * @return the current unacked client ops. Note: the behavior of this list after calling mutating
   *         methods on the transform queue is undefined. This method should be called each time
   *         immediately before use.
   */
  public ArrayOf<O> unackedClientOps() {
    return unackedClientOps;
  }

  public int unackedClientOpsCount() {
    return unackedClientOps.length();
  }

  private void checkRevision(int resultingRevision) {
    assert resultingRevision >= 1 : "New revision " + resultingRevision + " must be >= 1";
    assert this.version == resultingRevision - 1 : "Revision mismatch: at " + this.version
        + ", received " + resultingRevision;
  }
}