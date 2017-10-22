//
//  SubmitViewController.m
//  SpeakFree
//
//  Created by Milo Trujillo on 5/17/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SubmitViewController.h"

@interface SubmitViewController ()

@end



@implementation SubmitViewController

- (void) viewDidLoad {
    [super viewDidLoad];
    [self.tipText setDelegate:self];
}

- (BOOL)textFieldShouldReturn:(UITextField*)textField
{
    [textField resignFirstResponder];
    return YES;
}

- (IBAction)trash:(id)sender
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void) submitTip:(NSString*) tip
{
    // URL encode whatever's in the text box, and put it in POST-format
    NSCharacterSet* URLEncoding = [[NSCharacterSet characterSetWithCharactersInString:@" \"#%/:<>?@[\\]^`{|}&="] invertedSet];
    NSString* escapedInput = [tip stringByAddingPercentEncodingWithAllowedCharacters:URLEncoding];
    NSString* post = [NSString stringWithFormat:@"tip=%@", escapedInput];
    
    // Convert it in to the format for POST
    NSData* postData = [post dataUsingEncoding:NSASCIIStringEncoding allowLossyConversion:YES];
    NSString* postLength = [NSString stringWithFormat:@"%lu", [postData length]];
    
    // Prime the request
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] init];
    [request setURL:[NSURL URLWithString:@"https://speakfree.daylightingsociety.org/submitTip"]];
    [request setHTTPMethod:@"POST"];
    [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:postData];
    
    // Send it!
    //NSURLConnection* conn = [[NSURLConnection alloc] initWithRequest:request delegate:self];
    
    NSHTTPURLResponse* httpResponse;
    
    NSData* response = [NSURLConnection sendSynchronousRequest:request
                          returningResponse:(NSURLResponse**)&httpResponse
                                      error:nil];
    
    NSString* responseString = [[NSString alloc] initWithData:response encoding:NSUTF8StringEncoding];
    if( [httpResponse statusCode] == 200 )

        [[NSNotificationCenter defaultCenter] postNotificationName:@"DisplaySuccess" object:nil userInfo:responseString];
    else
        [[NSNotificationCenter defaultCenter] postNotificationName:@"DisplayFailure" object:nil userInfo:responseString];
}

- (IBAction)submit:(id)sender
{
    // Can't submit an empty tip
    // TODO: Consider making a pop-up explaining why we're not doing anything?
    NSString* tipString = [self.tipText text];
    if( [tipString length] == 0 )
        return;

    dispatch_async( dispatch_get_main_queue(), ^{
        [self submitTip:tipString];
    });
    
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
