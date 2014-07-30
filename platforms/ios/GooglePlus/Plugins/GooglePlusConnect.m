//
//  GooglePlusConnect.m
//  GooglePlusConnect
//
//  Created by 沢谷 邦夫 on 2014/07/23.
//
//

#import "GooglePlusConnect.h"
#import <GoogleOpenSource/GTLPlusConstants.h>
#import <GoogleOpenSource/GTMOAuth2Authentication.h>
#import <GooglePlus/GPPURLHandler.h>

@implementation GooglePlusConnect

@synthesize callbackId;

static NSString* const kClientIdKey = @"GooglePlusClientID";

- (void)handleOpenURL:(NSNotification *)notification
{
    
    NSURL* url = [notification object];
    
    NSLog(@"Handling URL:%@", url);
    
    if ([url isKindOfClass:[NSURL class]]) {
        NSString* srcApp = @"com.apple.mobilesafari";
        id annotation = NULL;
        NSLog(@"Invoking GPPURLHandler with URL:%@ sourceApplication:%@ annotation:%@", url, srcApp, annotation);
        [GPPURLHandler handleURL:url sourceApplication:srcApp annotation:annotation];
    }
}

- (void)login:(CDVInvokedUrlCommand *)command
{
    NSLog(@"GooglePlusConnect.login invoked with %@", command);
    GPPSignIn* signIn = [GPPSignIn sharedInstance];
    signIn.clientID = [[[NSBundle mainBundle] infoDictionary] objectForKey:kClientIdKey];
    signIn.scopes = [NSArray arrayWithObjects:kGTLAuthScopePlusLogin, nil];
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.delegate = self;
    
    self.callbackId = command.callbackId;
    NSLog(@"Start auth (callback:%@)", command.callbackId);
    signIn.attemptSSO = YES;
    [signIn authenticate];
    NSLog(@"authorizing...");
}

- (void)disconnect:(CDVInvokedUrlCommand *)command
{
    GPPSignIn* signIn = [GPPSignIn sharedInstance];
    
    self.callbackId = command.callbackId;
    [signIn disconnect];
}

- (void)finishedWithAuth:(GTMOAuth2Authentication *)auth error:(NSError *)error
{
    CDVPluginResult* result = nil;
    if (error) {
        NSLog(@"Received error: %@", error);
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:auth.userEmail, @"accountName", auth.accessToken, @"accessToken", nil];
        NSLog(@"Received auth object %@: %@", auth, dict);
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
    }
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

- (void)didDisconnectWithError:(NSError *)error
{
    CDVPluginResult* result = nil;
    if (error) {
        NSLog(@"Received error: %@", error);
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        NSLog(@"Disconnected");
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

@end
