//
//  InfoViewController.h
//  SpeakFree
//
//  Created by Milo Trujillo on 3/1/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#ifndef InfoViewController_h
#define InfoViewController_h

#import <UIKit/UIKit.h>

@interface InfoViewController : UIViewController {
    
}

@property (weak, nonatomic) IBOutlet UITextView *aboutText;

- (IBAction)handleBack:(id)sender;

@end


#endif /* InfoViewController_h */
