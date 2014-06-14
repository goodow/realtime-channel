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
package com.goodow.realtime.objc;

import com.goodow.realtime.core.Diff;
import com.goodow.realtime.json.JsonArray;

import java.util.Comparator;

/*-[
 #import "JavaObjCDiff+Adapter.h"
 ]-*/
class ObjCDiff implements Diff {
  @Override
  public native void diff(String before, String after, ListTarget<String> target) /*-[
    [self diff:before after:after target:target];
  ]-*/;

  @Override
  public native void diff(JsonArray before, JsonArray after, ListTarget<JsonArray> target, Comparator<Object> comparator) /*-[
  ]-*/;
}