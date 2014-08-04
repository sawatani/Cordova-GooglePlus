//
//  GooglePlusConnect.m
//  GooglePlusConnect
//
//  Created by 沢谷 邦夫 on 2014/07/23.
//
//

#import "GooglePlusConnect.h"
#import <GoogleOpenSource/GTLPlusConstants.h>
#import <GooglePlus/GPPURLHandler.h>

@implementation GooglePlusConnect

@synthesize callbackId;
@synthesize authrized;

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
    id arg = [command argumentAtIndex:0];
    NSLog(@"GooglePlusConnect.login invoked with argument:%@(arguments are ignored)", arg);
    
    self.callbackId = command.callbackId;
    NSLog(@"Start auth (callback:%@)", command.callbackId);
    
    if (self.authrized) {
        int value = [self.authrized.expirationDate timeIntervalSinceNow];
        NSLog(@"previous authorized expiration left: %d seconds", value);
        if (value < 600) {
            NSLog(@"Resetting previous authorized: %@", self.authrized);
            [self.authrized reset];
        }
    }
    
    GPPSignIn* signIn = [GPPSignIn sharedInstance];
    signIn.clientID = [[[NSBundle mainBundle] infoDictionary] objectForKey:kClientIdKey];
    signIn.actions = [NSArray arrayWithObjects:@"https://schemas.google.com/AddActivity", nil];
    NSLog(@"SignIn with actions: %@", signIn.actions);
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.scopes = [NSArray arrayWithObjects:kGTLAuthScopePlusLogin, nil];
    signIn.delegate = self;
    
    signIn.attemptSSO = YES;
    [signIn authenticate];
    NSLog(@"authorizing...");
}

- (void)disconnect:(CDVInvokedUrlCommand *)command
{
    id arg = [command argumentAtIndex:0];
    NSLog(@"GooglePlusConnect.disconnect invoked with argument:%@(arguments are ignored)", arg);
    GPPSignIn* signIn = [GPPSignIn sharedInstance];
    
    self.callbackId = command.callbackId;
    [signIn disconnect];
}

- (void)finishedWithAuth:(GTMOAuth2Authentication *)auth error:(NSError *)error
{
    if (error) {
        NSLog(@"Received error: %@", error);
        [self callback:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR]];
    } else {
        NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:auth.accessToken, @"accessToken", auth.userEmail, @"accountName", nil];
        NSLog(@"Received auth object %@: %@", auth, dict);
        self.authrized = auth;
        [self callback:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict]];
    }
}

- (void)didDisconnectWithError:(NSError *)error
{
    if (error) {
        NSLog(@"Received error: %@", error);
        [self callback:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR]];
    } else {
        NSLog(@"Disconnected");
        [self callback:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK]];
    }
}

- (void)callback:(CDVPluginResult *)result
{
    if (self.callbackId) {
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
        self.callbackId = nil;
    } else {
        NSLog(@"Could not callback: %@", result);
    }
}

@end
