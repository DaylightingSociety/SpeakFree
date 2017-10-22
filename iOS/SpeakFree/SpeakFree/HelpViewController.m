//
//  HelpViewController.m
//  SpeakFree
//
//  Created by Milo Trujillo on 3/1/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "HelpViewController.h"

@interface HelpViewController ()



@end



@implementation HelpViewController

// Initial setup code goes here
- (void)viewDidLoad {
    [super viewDidLoad];
    
    // Increase the margins on the about text box a bit
    self.aboutText.textContainerInset = UIEdgeInsetsMake(0, 20, 0, 20);
}

// Returns to the main view without restarting it
- (IBAction)handleBack:(id)sender
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
