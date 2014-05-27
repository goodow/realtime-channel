/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.html;

import com.goodow.realtime.core.Diff;
import com.goodow.realtime.json.JsonArray;

import java.util.Comparator;

final class HtmlDiff implements Diff {

  protected HtmlDiff() {
  }

  @Override
  // @formatter:off
  public native void diff(String before, String after, ListTarget<String> target) /*-{
    var dmp = new $wnd.diff_match_patch();
    var diffs = dmp.diff_main(before, after);
    dmp.diff_cleanupSemantic(diffs);
    var cursor = 0;
    for (var i in diffs) {
      var text = diffs[i][1], len = text.length;
      switch (diffs[i][0]) {
        case 0:
          cursor += len;
          break;
        case 1:
          target.@com.goodow.realtime.core.Diff.ListTarget::insert(ILjava/lang/Object;)(cursor, text);
          cursor += len;
          break;
        case -1:
          target.@com.goodow.realtime.core.Diff.ListTarget::remove(II)(cursor, len);
          break;
        default:
          throw @java.lang.RuntimeException::new(Ljava/lang/String;)("Shouldn't reach here!");
      }
    }
  }-*/;
  // @formatter:on


  @Override
  public void diff(JsonArray before, JsonArray after, ListTarget<JsonArray> target, Comparator<Object> comparator) {

  }
}