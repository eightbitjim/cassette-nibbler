/*
 * Copyright (c) 2018. James Lean
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

package com.eightbitjim.cassettenibbler.Platforms.Other.MPFI.PulseExtraction;

public class SyncDetection {
    private static final int LEAD_SYNC_HALF_CYCLE_TIME_IN_MICROSECONDS = 500;
    private static final int MID_SYNC_HALF_CYCLE_TIME_IN_MICROSECONDS = 250;

    private static final int maximumPulsesBeforeLosingSyncTone = 6;
    private int [] syncToneBuffer;
    private static final int syncToneBufferLength = 32;
    private int syncToneBufferPointer;
    private int numberOfPulsesSinceSyncToneValid;
    private double intervalShift;
    private int syncToneTargetTime;

    public enum Type { LEAD, MID }

    public SyncDetection(Type syncType) {
        if (syncType == Type.LEAD)
            syncToneTargetTime = LEAD_SYNC_HALF_CYCLE_TIME_IN_MICROSECONDS;
        else
            syncToneTargetTime = MID_SYNC_HALF_CYCLE_TIME_IN_MICROSECONDS;

        syncToneBuffer = new int[syncToneBufferLength];
        syncToneBufferPointer = 0;
        numberOfPulsesSinceSyncToneValid = Integer.MAX_VALUE;
        intervalShift = 1.0;
    }

    public double registerWithSyncToneBufferAndReturnIntervalShift(int currentTransitionLengthInTstates) {
        syncToneBuffer[syncToneBufferPointer] = currentTransitionLengthInTstates;
        syncToneBufferPointer = (syncToneBufferPointer + 1 ) % syncToneBufferLength;
        if (syncToneBufferIsValid())
            intervalShift = getIntervalShiftMultiplier();

        return intervalShift;
    }

    private boolean syncToneBufferIsValid() {
        for (int i = 0; i < syncToneBufferLength - 1; i++) {
            if (!intervalsCountsAsSyncTone(syncToneBuffer[i], syncToneBuffer[i + 1])) {
                numberOfPulsesSinceSyncToneValid++;
                return false;
            }
        }

        numberOfPulsesSinceSyncToneValid = 0;
        return true;
    }

    public boolean isValid() {
        return numberOfPulsesSinceSyncToneValid < maximumPulsesBeforeLosingSyncTone;
    }

    private double getIntervalShiftMultiplier() {
        double lastValue = syncToneBuffer[0];
        double sum = lastValue;
        for (int i = 1; i < syncToneBufferLength; i++) {
            sum += syncToneBuffer[i];
        }

        double average = sum / (double)(syncToneBufferLength);
        return (double) syncToneTargetTime / average;
    }

    private boolean intervalsCountsAsSyncTone(int tstate1, int tstate2) {
        int toleranceInMicroseconds = 100;
        if (Math.abs((tstate1 + tstate2) - syncToneTargetTime * 2) < toleranceInMicroseconds)
            return true;
        else
            return false;
    }
}
