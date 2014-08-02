//
//  GooglePlusConnect.h
//  GooglePlusConnect
//
//  Created by 沢谷 邦夫 on 2014/07/23.
//
//

#import <Cordova/CDV.h>
#import <GooglePlus/GPPSignIn.h>
#import <GoogleOpenSource/GTMOAuth2Authentication.h>

@interface GooglePlusConnect : CDVPlugin <GPPSignInDelegate>

@property (nonatomic, copy) NSString *callbackId;
@property (nonatomic, retain) GTMOAuth2Authentication* authrized;

- (void)login:(CDVInvokedUrlCommand*)command;

@end
