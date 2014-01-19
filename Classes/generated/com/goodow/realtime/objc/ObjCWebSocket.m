// Copyright 2014 Goodow.com. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
//  source: /Users/larry/workspace/realtime/realtime-channel/src/main/java/com/goodow/realtime/objc/ObjCWebSocket.java
//
//  Created by Larry Tin.
//

#import "com/goodow/realtime/objc/ObjCWebSocket.h"
#import "SocketRocket/SRWebSocket.h"
#import "GDChannel.h"

@interface ComGoodowRealtimeObjcObjCWebSocket() <SRWebSocketDelegate> {
  SRWebSocket *_webSocket;
  id<ComGoodowRealtimeCoreWebSocket_WebSocketHandler> _handler;
}
@end

@implementation ComGoodowRealtimeObjcObjCWebSocket

- (id)initWithNSString:(NSString *)url
      withGDJsonObject:(id<GDJsonObject>)options {
  [_webSocket close];
  
  _webSocket = [[SRWebSocket alloc] initWithURLRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:url]]];
  _webSocket.delegate = self;
  [_webSocket open];
  
  return [super init];
}

- (void)close {
  [_webSocket close];
}

- (void)sendWithNSString:(NSString *)data {
  NSLog(@"Websocket send \"%@\"", data);
  [_webSocket send:data];
}

- (void)setListenWithComGoodowRealtimeCoreWebSocket_WebSocketHandler:(id<ComGoodowRealtimeCoreWebSocket_WebSocketHandler>)handler {
  _handler = handler;
}

#pragma mark - SRWebSocketDelegate
- (void)webSocketDidOpen:(SRWebSocket *)webSocket {
  NSLog(@"Websocket Connected");
  [_handler onOpen];
}
- (void)webSocket:(SRWebSocket *)webSocket didFailWithError:(NSError *)error {
  NSLog(@":( Websocket Failed With Error %@", error);
  [_handler onErrorWithNSString:[error description]];
  
  _handler = nil;
  _webSocket.delegate = nil;
  _webSocket = nil;
}
- (void)webSocket:(SRWebSocket *)webSocket didReceiveMessage:(id)message {
  NSString *messageString = nil;
  if([message isKindOfClass:[NSString class]]) {
    messageString = message;
  } else {
    messageString = [[NSString alloc] initWithData:message encoding:NSUTF8StringEncoding];
  }
  NSLog(@"Websocket Received \"%@\"", messageString);
  [_handler onMessageWithNSString:messageString];
}
- (void)webSocket:(SRWebSocket *)webSocket didCloseWithCode:(NSInteger)code reason:(NSString *)reason wasClean:(BOOL)wasClean {
  NSLog(@"WebSocket closed");
  [_handler onCloseWithGDJsonObject:@{@"code":[NSNumber numberWithInteger:code], @"reason":reason, @"wasClean":[NSNumber numberWithBool:wasClean]}];
  
  _handler = nil;
  _webSocket.delegate = nil;
  _webSocket = nil;
}

@end