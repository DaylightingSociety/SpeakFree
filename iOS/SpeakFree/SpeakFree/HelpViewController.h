//
//  HelpViewController.h
//  SpeakFree
//
//  Created by Milo Trujillo on 3/1/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#ifndef HelpViewController_h
#define HelpViewController_h

#import <UIKit/UIKit.h>

@interface HelpViewController : UIViewController {
    
}

@property (weak, nonatomic) IBOutlet UITextView *aboutText;

- (IBAction)handleBack:(id)sender;

@end


#endif /* HelpViewController_h */
