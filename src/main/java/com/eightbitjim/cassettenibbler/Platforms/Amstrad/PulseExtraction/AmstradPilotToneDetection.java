/*
 * Copyright (c) 2017. James Lean
 * This file is part of cassette-nibbler.
 *
 * cassette-nibbler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * cassette-nibbler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with cassette-nibbler.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.PulseExtraction;

public class AmstradPilotToneDetection {
    private static final int PILOT_TONE_T_STATES = 2301;
    private static final int maximumPulsesBeforeLosingPilotTone = 3;
    private int [] pilotToneBuffer;
    private static final int pilotToneBufferLength = 32;
    private int pilotToneBufferPointer;
    private int numberOfPulsesSincePilotToneValid;
    private double intervalShift;

    public AmstradPilotToneDetection() {
        pilotToneBuffer = new int[pilotToneBufferLength];
        pilotToneBufferPointer = 0;
        numberOfPulsesSincePilotToneValid = Integer.MAX_VALUE;
        intervalShift = 1.0;
    }

    public double registerWithPilotToneBufferAndReturnIntervalShift(int currentTransitionLengthInTstates) {
        pilotToneBuffer[pilotToneBufferPointer] = currentTransitionLengthInTstates;
        pilotToneBufferPointer = (pilotToneBufferPointer + 1 ) % pilotToneBufferLength;
        if (pilotToneBufferIsValid())
            intervalShift = getIntervalShiftMultiplier();

        return intervalShift;
    }

    private boolean pilotToneBufferIsValid() {
        for (int i = 0; i < pilotToneBufferLength - 1; i++) {
            if (!intervalsCountsAsPilotTone(pilotToneBuffer[i], pilotToneBuffer[i + 1])) {
                numberOfPulsesSincePilotToneValid++;
                return false;
            }
        }

        numberOfPulsesSincePilotToneValid = 0;
        return true;
    }

    public boolean pilotToneIsValid() {
        return numberOfPulsesSincePilotToneValid < maximumPulsesBeforeLosingPilotTone;
    }

    private double getIntervalShiftMultiplier() {
        double lastValue = pilotToneBuffer[0];
        double sum = lastValue;
        for (int i = 1; i < pilotToneBufferLength; i++) {
            sum += pilotToneBuffer[i];
        }

        double average = sum / (double)(pilotToneBufferLength);
        return (double)PILOT_TONE_T_STATES / average;
    }

    private boolean intervalsCountsAsPilotTone(int tstate1, int tstate2) {
        int toleranceInTStates = 300;
        if (Math.abs((tstate1 + tstate2) - PILOT_TONE_T_STATES * 2) < toleranceInTStates)
            return true;
        else
            return false;
    }
}
