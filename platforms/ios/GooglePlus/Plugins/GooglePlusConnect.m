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

@implementation GooglePlusConnect

@synthesize callbackId;

static NSString* const kClientIdKey = @"GooglePlusClientID";

- (void)login:(CDVInvokedUrlCommand *)command
{
    GPPSignIn* signIn = [GPPSignIn sharedInstance];
    signIn.clientID = [[[NSBundle mainBundle] infoDictionary] objectForKey:kClientIdKey];
    signIn.scopes = [NSArray arrayWithObjects:kGTLAuthScopePlusLogin, nil];
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.delegate = self;
    
    self.callbackId = command.callbackId;
    [signIn trySilentAuthentication];
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
        NSString* email = auth.userEmail;
        NSString* token = auth.accessToken;
        NSLog(@"Received auth object %@ (%@): %@", auth, email, token);
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:email];
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
