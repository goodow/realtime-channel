//
//  source: /Users/larry/workspace/realtime/realtime-channel/src/main/java/com/goodow/realtime/objc/ObjCWebSocket.java
//
//  Created by Larry Tin.
//

#include "com/goodow/realtime/core/WebSocket.h"
#include "com/goodow/realtime/json/JsonObject.h"
#include "com/goodow/realtime/objc/ObjCWebSocket.h"

#import "SocketRocket/SRWebSocket.h"

@interface ComGoodowRealtimeObjcObjCWebSocket() <SRWebSocketDelegate> {
  SRWebSocket *_webSocket;
  id<ComGoodowRealtimeCoreWebSocket_WebSocketHandler> _handler;
}
@end

@implementation ComGoodowRealtimeObjcObjCWebSocket

- (id)initWithNSString:(NSString *)url
      withGDJsonObject:(id<GDJsonObject>)options {
  _webSocket.delegate = nil;
  [_webSocket close];
  
  _webSocket = [[SRWebSocket alloc] initWithURLRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:url]]];
  _webSocket.delegate = self;
  [_webSocket open];
  
  return [super init];
}

-(void)close {
  _webSocket.delegate = nil;
  [_webSocket close];
  _webSocket = nil;
}

-(void)sendWithNSString:(NSString *)data {
  [_webSocket send:data];
}

-(void)setListenWithComGoodowRealtimeCoreWebSocket_WebSocketHandler:(id<ComGoodowRealtimeCoreWebSocket_WebSocketHandler>)handler {
  _handler = handler;
}

#pragma mark - SRWebSocketDelegate
-(void)webSocketDidOpen:(SRWebSocket *)webSocket {
  NSLog(@"Websocket Connected");
  [_handler onOpen];
}
-(void)webSocket:(SRWebSocket *)webSocket didFailWithError:(NSError *)error {
  NSLog(@":( Websocket Failed With Error %@", error);
  [_handler onErrorWithNSString:[error description]];
  _webSocket = nil;
}
-(void)webSocket:(SRWebSocket *)webSocket didReceiveMessage:(id)message {
  NSString *messageString = nil;
  if([message isKindOfClass:[NSString class]]) {
    messageString = message;
  } else {
    messageString = [[NSString alloc] initWithData:message encoding:NSUTF8StringEncoding];
  }
  [_handler onMessageWithNSString:messageString];
}
-(void)webSocket:(SRWebSocket *)webSocket didCloseWithCode:(NSInteger)code reason:(NSString *)reason wasClean:(BOOL)wasClean {
  NSLog(@"WebSocket closed");
  [_handler onClose];
  webSocket = nil;
}

@end