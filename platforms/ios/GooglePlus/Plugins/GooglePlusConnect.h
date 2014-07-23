//
//  GooglePlusConnect.h
//  GooglePlusConnect
//
//  Created by 沢谷 邦夫 on 2014/07/23.
//
//

#import <Cordova/CDV.h>
#import <GooglePlus/GPPSignIn.h>

@interface GooglePlusConnect : CDVPlugin <GPPSignInDelegate>

@property (nonatomic, copy) NSString *callbackId;

- (void)login:(CDVInvokedUrlCommand*)command;

@end
