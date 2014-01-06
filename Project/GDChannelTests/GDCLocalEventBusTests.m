//
//  GDCLocalEventBusTests.m
//  GDChannel
//
//  Created by Larry Tin.
//  Copyright (c) 2013年 Goodow. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "GDChannel.h"

@interface GDCLocalEventBusTests : XCTestCase

@end

@implementation GDCLocalEventBusTests

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
  id<GDCBus> bus = [[GDCSimpleBus alloc] init];
  __block BOOL testComplete = NO;
  
  [bus registerHandler:@"someaddress" handler:^(id<GDCMessage> message) {
    XCTAssertTrue(self == [message body]);
    
    NSDictionary *msg = @{@"text": @"reply"};
    [message reply:msg];
  }];
  
  [bus send:@"someaddress" message:self replyHandler:^(id<GDCMessage> message) {
    NSMutableDictionary *body = [message body];
    XCTAssertTrue([@"reply" isEqualToString:[body objectForKey:@"text"]]);
    
    testComplete = YES;
  }];
  
  // Begin a run loop terminated when the testComplete it set to true
  while (!testComplete && [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.01]]);
}

@end
