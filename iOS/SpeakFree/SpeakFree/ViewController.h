//
//  ViewController.h
//  SpeakFree
//
//  Created by Milo Trujillo on 3/1/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ViewController : UIViewController {
    __weak IBOutlet UIImageView *statusImage;
    __weak IBOutlet UITextView *tipBox;
}

- (IBAction)soundPressed:(id)sender;
- (IBAction)reloadTipsPressed:(id)sender;

@end

