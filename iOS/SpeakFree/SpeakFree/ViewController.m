//
//  ViewController.m
//  SpeakFree
//
//  Created by Milo Trujillo on 3/1/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>
#import "ViewController.h"
#import "Sound.h"


@interface ViewController ()
@end

@implementation ViewController

NSArray* tipData;
Sound* sound;
Boolean isPlaying;
int currentTipIndex;

- (void)viewDidLoad {
    [super viewDidLoad];
    [self loadTips];
    [self changeTip]; // Load the first tip
    
    sound = [[Sound alloc] init];
    isPlaying = false;
    
    // Hook up tapping on a tip with changing the tip
    UITapGestureRecognizer* tipTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(changeTip)];
    [tipBox addGestureRecognizer:tipTap];
    
    // Set up the notification center so background threads from other views can safely make pop-up windows
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(displaySuccess:)
                                                 name:@"DisplaySuccess"
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(displayFailure:)
                                                 name:@"DisplayFailure"
                                               object:nil];
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
    // In our case this would be the XML data and the current audio sample,
    // but we'd need to reload both almost instantly, so not worth deleting.
}

// Loads our tips from an xml file, stores them in table
// We'll try to load them from a downloaded file, but fallback on the
// one included in the bundle if user hasn't downloaded anything.
- (void)loadTips
{
    NSString* tipFile = nil;
    
    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString* documentsDirectory = [paths objectAtIndex:0];
    NSString* downloadedTipsPath = [NSString stringWithFormat:@"%@/%@", documentsDirectory, @"tips.xml"];
    BOOL downloadedFileExists = [[NSFileManager defaultManager] fileExistsAtPath:downloadedTipsPath];
    
    if( downloadedFileExists )
        tipFile = downloadedTipsPath;
    else
        tipFile = [[NSBundle mainBundle] pathForResource:@"tips" ofType:@"xml"];
    
    NSLog(@"Loading from file '%@'",tipFile);
    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:tipFile];
    if( fileExists )
        tipData = [NSArray arrayWithContentsOfFile:tipFile];
    else
        NSLog(@"File '%@' does not exist!", tipFile);
}

// Selects a new tip, guarantees the tip will be different if there are 2+ tips
- (void)changeTip
{
    dispatch_async( dispatch_get_main_queue(), ^{
        // Fade out the tip
        [UIView animateWithDuration:0.5f animations:^{
            [tipBox setAlpha:0.0f];
        } completion:^(BOOL finished) {
            // Now that we've faded out, swap the tip and fade back in
            // The loop ensures that we never fade in the tip we just faded out
            int newTipIndex = -1;
            do{
                newTipIndex = arc4random_uniform((uint32_t)tipData.count);
            } while(newTipIndex == currentTipIndex && [tipData count] > 1);
            currentTipIndex = newTipIndex;
            [tipBox setText:tipData[newTipIndex]];
            [UIView animateWithDuration:0.5f animations:^{
                [tipBox setAlpha:1.0f];
            }];
        }];
    });
}

// Confirms with user, then kicks off a thread to handle downloading
- (IBAction)reloadTipsPressed:(id)sender
{
    UIAlertController* confirm = [UIAlertController alertControllerWithTitle:@"Confirm"
                                                                     message:@"Download new anti-surveillance tips?"
                                                              preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* cancel = [UIAlertAction actionWithTitle:@"No"
                                                     style:UIAlertActionStyleCancel
                                                   handler:^(UIAlertAction* action) {}];
    
    UIAlertAction* download = [UIAlertAction actionWithTitle:@"Yes"
                                                     style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction* action) {
                                                       dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                                                           [self downloadTips];
                                                       });
                                                   }];
    
    [confirm addAction:cancel];
    [confirm addAction:download];
    [self presentViewController:confirm animated:YES completion:nil];
}

// Downloads new tips xml from server, saves to disk, announces success or failure to user
// On success, will also trigger a reload of tips
- (void)downloadTips
{
    NSString* downloadString = @"https://speakfree.daylightingsociety.org/getTips/iOS";
    NSURL* downloadUrl = [NSURL URLWithString:downloadString];
    NSData* data = [NSData dataWithContentsOfURL:downloadUrl];
    if( data )
    {
        NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString* documentsDirectory = [paths objectAtIndex:0];
        NSString* filePath = [NSString stringWithFormat:@"%@/%@", documentsDirectory, @"tips.xml"];
        [data writeToFile:filePath atomically:YES];
        
        // Reload the tips from the new data
        [self loadTips];
        
        // Announce our success!
        UIAlertController* success = [UIAlertController alertControllerWithTitle:@"Success"
                                                                       message:[NSString stringWithFormat:@"%lu anti-surveillance tips downloaded.", [tipData count]]
                                                                preferredStyle:UIAlertControllerStyleAlert];
        
        UIAlertAction* okay = [UIAlertAction actionWithTitle:@"Okay"
                                                       style:UIAlertActionStyleDefault
                                                     handler:^(UIAlertAction* action) {}];
        
        [success addAction:okay];
        [self presentViewController:success animated:YES completion:nil];

    } else {
        // Admit defeat.
        UIAlertController* error = [UIAlertController alertControllerWithTitle:@"Error"
                                                                         message:@"Unable to download new anti-surveillance tips, please try again later."
                                                                  preferredStyle:UIAlertControllerStyleAlert];
        
        UIAlertAction* okay = [UIAlertAction actionWithTitle:@"Okay"
                                                           style:UIAlertActionStyleDefault
                                                         handler:^(UIAlertAction* action) {}];
        
        [error addAction:okay];
        [self presentViewController:error animated:YES completion:nil];
    }
}

- (void)setActiveImage
{
    [UIView transitionWithView:self->statusImage
                      duration:0.1f
                       options:UIViewAnimationOptionTransitionCrossDissolve
                    animations:^{
                        [statusImage setImage:[UIImage imageNamed:@"active.png"]];
                    } completion:nil];
}

- (void)setInactiveImage
{
    [UIView transitionWithView:self->statusImage
                      duration:0.1f
                       options:UIViewAnimationOptionTransitionCrossDissolve
                    animations:^{
                        [statusImage setImage:[UIImage imageNamed:@"inactive.png"]];
                    } completion:nil];
}

- (IBAction)soundPressed:(id)sender
{
    UIBarButtonItem *button = (UIBarButtonItem*)sender;
    if( isPlaying == false )
    {
        NSLog(@"Sound pressed (starting to play)");
        isPlaying = true;
        [sound start];
        // Prevent the phone from sleeping while we're jamming microphones
        [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
        [button setImage:[UIImage imageNamed:@"pause.png"]];
        [self setActiveImage];

    } else {
        NSLog(@"Sound pressed (stopping playback)");
        isPlaying = false;
        [sound stop];
        // App is idle, we can let the phone sleep again
        [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
        [button setImage:[UIImage imageNamed:@"play.png"]];
        [self setInactiveImage];
    }
}

- (void)displaySuccess:(NSNotification*)note
{
    UIAlertController* success = [UIAlertController alertControllerWithTitle:@"Success"
                                                                     message:[NSString stringWithFormat:@"%@", [note userInfo]]
                                                              preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* okay = [UIAlertAction actionWithTitle:@"Dismiss"
                                                   style:UIAlertActionStyleDefault
                                                 handler:^(UIAlertAction* action) {}];
    
    [success addAction:okay];
    [self presentViewController:success animated:YES completion:nil];
}

- (void)displayFailure:(NSNotification*)note
{
    UIAlertController* success = [UIAlertController alertControllerWithTitle:@"Failure"
                                                                     message:[NSString stringWithFormat:@"%@", [note userInfo]]
                                                              preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* okay = [UIAlertAction actionWithTitle:@"Dismiss"
                                                   style:UIAlertActionStyleDefault
                                                 handler:^(UIAlertAction* action) {}];
    
    [success addAction:okay];
    [self presentViewController:success animated:YES completion:nil];
}

@end
