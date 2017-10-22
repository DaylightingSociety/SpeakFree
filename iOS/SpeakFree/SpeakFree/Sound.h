//
//  Sound.h
//  SpeakFree
//
//  Created by Milo Trujillo on 3/8/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//

#ifndef Sound_h
#define Sound_h

#include <AudioToolbox/AudioToolbox.h>

@interface Sound : NSObject {

}

- (Sound*)init;

- (void)start;
- (void)stop;

- (void)deinit;

@end


#endif /* Sound_h */
