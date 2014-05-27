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
package com.goodow.realtime.channel.server.impl;

import com.goodow.realtime.core.Diff;
import com.goodow.realtime.json.JsonArray;
import name.fraser.neil.plaintext.diff_match_patch;

import java.util.Comparator;
import java.util.LinkedList;

public class JavaDiff implements Diff {
  private final diff_match_patch dmp = new diff_match_patch();

  @Override
  public void diff(String before, String after, ListTarget<String> target) {
    LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(before, after);
    dmp.diff_cleanupSemantic(diffs);
    int cursor = 0;
    for (diff_match_patch.Diff diff : diffs) {
      String text = diff.text;
      int len = text.length();
      switch (diff.operation) {
        case EQUAL:
          cursor += len;
          break;
        case INSERT:
          target.insert(cursor, text);
          cursor += len;
          break;
        case DELETE:
          target.remove(cursor, len);
          break;
        default:
          throw new RuntimeException("Shouldn't reach here!");
      }
    }
  }

  @Override
  public void diff(JsonArray before, JsonArray after, ListTarget<JsonArray> target, Comparator<Object> comparator) {

  }
}