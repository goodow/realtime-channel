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
  
  [bus registerHandler:@"someaddress" handler:^(id<GDCMessage> message) {
    NSMutableDictionary *body = [message body];
    XCTAssertTrue([@"send1" isEqualToString:[body objectForKey:@"text"]]);
    
    NSDictionary *msg = @{@"text": @"reply1"};
    [message reply:msg];
  }];
  
  [bus send:@"someaddress" message:@{@"text": @"send1"} replyHandler:^(id<GDCMessage> message) {
    NSMutableDictionary *body = [message body];
    XCTAssertTrue([@"reply1" isEqualToString:[body objectForKey:@"text"]]);
  }];
}

@end
