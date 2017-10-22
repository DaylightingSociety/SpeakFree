//
//  SubmitViewController.h
//  SpeakFree
//
//  Created by Milo Trujillo on 5/17/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#ifndef SubmitViewController_h
#define SubmitViewController_h

#import <UIKit/UIKit.h>

@interface SubmitViewController : UIViewController <UITextFieldDelegate>
{
    
}

@property (weak, nonatomic) IBOutlet UITextField* tipText;

- (IBAction)trash:(id)sender;
- (IBAction)submit:(id)sender;

@end

#endif /* SubmitViewController_h */
