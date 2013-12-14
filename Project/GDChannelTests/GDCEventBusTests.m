//
//  GDCEventBusTests.m
//  GDChannel
//
//  Created by Larry Tin.
//  Copyright (c) 2013年 Goodow. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "GDChannel.h"

@interface GDCEventBusTests : XCTestCase

@end

@implementation GDCEventBusTests {
  BOOL testComplete;
}

- (void)setUp
{
  [super setUp];
  // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown
{
  // Put teardown code here. This method is called after the invocation of each test method in the class.
  [super tearDown];
}

- (void)testExample
{
  id<GDCBus> bus = [GDCWebSocketBusClient create:@"ws://data.goodow.com:8080/eventbus/websocket" options:nil];
  
  [bus registerHandler:[GDCBus LOCAL_ON_OPEN] handler:^(id<GDCMessage> message) {
    [self handlerEventBusOpened:bus];
  }];
  [bus registerHandler:[GDCBus LOCAL_ON_CLOSE] handler:^(id<GDCMessage> message) {
    NSLog(@"%@", @"EventBus closed");
  }];
  
  NSRunLoop *loop = [NSRunLoop currentRunLoop];
  // Begin a run loop terminated when the testComplete it set to true
  while (!testComplete && [loop runMode:NSDefaultRunLoopMode beforeDate:[NSDate distantFuture]]) {
  }
}

-(void)handlerEventBusOpened:(id<GDCBus>) bus {
  [bus registerHandler:@"someaddress" handler:^(id<GDCMessage> message) {
    NSMutableDictionary *body = [message body];
    XCTAssertTrue([@"send1" isEqualToString:[body objectForKey:@"text"]]);
    
    NSDictionary *msg = @{@"text": @"reply1"};
    [message reply:msg replyHandler:^(id<GDCMessage> message) {
      NSMutableDictionary *body = [message body];
      XCTAssertTrue([@"reply2" isEqualToString:[body objectForKey:@"text"]]);
      
      testComplete = YES;
    }];
  }];
  
  [bus send:@"someaddress" message:@{@"text": @"send1"} replyHandler:^(id<GDCMessage> message) {
    NSMutableDictionary *body = [message body];
    XCTAssertTrue([@"reply1" isEqualToString:[body objectForKey:@"text"]]);
    
    [message reply:@{@"text": @"reply2"}];
  }];
}

@end
