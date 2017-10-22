//
//  Sound.m
//  SpeakFree
//
//  Created by Milo Trujillo on 3/8/17.
//  Copyright © 2017 Daylighting Society. All rights reserved.
//


/*
 
 Lots of thanks to admsyn and StackOverflow: https://stackoverflow.com/a/14478420
 Thanks also to Matt Gallagher: https://www.cocoawithlove.com/2010/10/ios-tone-generator-introduction-to.html
 
 */

#import <Foundation/Foundation.h>
#import "Sound.h"

#define NUMBER_WAVES 10

@interface Sound () {
    AudioUnit outputUnit;
    struct soundState
    {
        double frequency[NUMBER_WAVES];
        double phase[NUMBER_WAVES];
    } noiseState;
}
@end

@implementation Sound

-(Sound*)init
{
    AudioComponentDescription outputUnitDescription = {
        .componentType         = kAudioUnitType_Output,
        .componentSubType      = kAudioUnitSubType_RemoteIO,
        .componentManufacturer = kAudioUnitManufacturer_Apple,
        .componentFlags        = 0,
        .componentFlagsMask    = 0
    };
    
    // Get the iPhone speaker / headphone jack
    AudioComponent outputComponent = AudioComponentFindNext(NULL, &outputUnitDescription);
    NSAssert(outputComponent, @"Can't find an audio output!");
    
    // Create an audio unit referencing the speaker / headphones
    AudioComponentInstanceNew(outputComponent, &outputUnit);
    AudioUnitInitialize(outputUnit);
    
    // Floating point samples @ 44100 Hz in mono
    AudioStreamBasicDescription streamDescription = {
        .mSampleRate       = 44100,
        .mFormatID         = kAudioFormatLinearPCM,
        .mFormatFlags      = kAudioFormatFlagsNativeFloatPacked,
        .mChannelsPerFrame = 1,
        .mFramesPerPacket  = 1,
        .mBitsPerChannel   = sizeof(Float32) * 8,
        .mBytesPerPacket   = sizeof(Float32),
        .mBytesPerFrame    = sizeof(Float32)
    };
    
    // Set the above sample rate and single channel attributes
    // for the output unit we've chosen
    AudioUnitSetProperty(outputUnit,
                         kAudioUnitProperty_StreamFormat,
                         kAudioUnitScope_Input,
                         0,
                         &streamDescription,
                         sizeof(streamDescription));
    
    // Set up a callback so the speakers will pull a sample from
    // out static noise generator
    AURenderCallbackStruct callbackInfo = {
        .inputProc       = NoiseGeneratorCallback,
        .inputProcRefCon = &noiseState
    };
    
    AudioUnitSetProperty(outputUnit,
                         kAudioUnitProperty_SetRenderCallback,
                         kAudioUnitScope_Global,
                         0,
                         &callbackInfo,
                         sizeof(callbackInfo));
    
    NSLog(@"Initialized sound system.");
    
    return self;
}

-(void)start
{
    for( int i = 0; i < NUMBER_WAVES; i++ )
    {
        noiseState.frequency[i] = getRandomFrequency();
        noiseState.phase[i] = getRandomPhase();
    }
    AudioOutputUnitStart(outputUnit);
}

-(void)stop
{
    AudioOutputUnitStop(outputUnit);
}

-(void) deinit
{
    AudioUnitUninitialize(outputUnit);
    AudioComponentInstanceDispose(outputUnit);
}

// Called frequently to make tiny sound buffers
// This is in a background thread that will be killed if we take longer
// than a tiny interval
OSStatus NoiseGeneratorCallback(void* inRefCon,
                                AudioUnitRenderActionFlags* ioActionFlags,
                                const AudioTimeStamp* inTimeStamp,
                                UInt32 inBusNumber,
                                UInt32 inNumberFrames,
                                AudioBufferList* ioData)
{
    struct soundState* state = ((struct soundState*)inRefCon);
    Float32* outputBuffer = (Float32 *)ioData->mBuffers[0].mData;
    
    for(int i = 0; i < inNumberFrames; i++) {
        // Layer the waves on top of eachother to get a noise from -1 to 1
        Float32 maxOutput = 0.0;
        for( int i = 0; i < NUMBER_WAVES; i++ )
        {
            Float32 output = sin(state->phase[i]);
            double phaseStep = ((state->frequency[i] / 44100.) * M_PI * 2.);
            state->phase[i] += phaseStep;
            if( fabsf(output) > fabsf(maxOutput) )
                maxOutput = output;
        }
        outputBuffer[i] = maxOutput;
    }
    
    // Does nothing right now, but if we were stereo or higher (multi-channel)
    // it would copy data from the first channel to all the others
    for(int i = 1; i < ioData->mNumberBuffers; i++) {
        memcpy(ioData->mBuffers[i].mData, outputBuffer, ioData->mBuffers[i].mDataByteSize);
    }

    int randWave = random() % NUMBER_WAVES;
    state->phase[randWave] = getRandomPhase();
    state->frequency[randWave] = getRandomFrequency();
    
    return noErr;
}

// We want to start our wave at a random point
// otherwise sine() will always start at zero
double getRandomPhase()
{
    return (((double)rand() / RAND_MAX) * 2 * M_PI);
}

// Returns a random frequency in the range of human voice
// From a quick gander at wikipedia, this is 300 - 3400 Hz
double getRandomFrequency()
{
    double frequency = (((double)rand() / RAND_MAX) * 3100) + 300;
    //NSLog(@"Chose frequency: %fhz", frequency);
    return frequency;
}


@end

