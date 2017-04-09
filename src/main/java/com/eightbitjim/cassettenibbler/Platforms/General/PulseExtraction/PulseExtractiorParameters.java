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

package com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction;

public class PulseExtractiorParameters {
    public static final double TAP = 8.0/985248.0;
    public double shortPulseLength = TAP * (double)0x30;
    public double mediumPulseLength = TAP * (double)0x42;
    public double longPulseLength = TAP * (double)0x56;

    public boolean useLeaderBufferToAdjustPulseFrequency = false;
    public double leaderPulseLength = shortPulseLength;
    public int LEADER_BUFFER_LENGTH = 32;
    public boolean bottomHalfOfPulseExtractionOnly = false;

    public double percentageDifferenceNotToCountAsSamePulseLength = 10.0;
    public double toleranceForPulseCountingAsLeader = 1.6;

    public PulseExtractiorParameters copyOf() {
        PulseExtractiorParameters copy = new PulseExtractiorParameters();
        copy.shortPulseLength = shortPulseLength;
        copy.mediumPulseLength = mediumPulseLength;
        copy.longPulseLength = longPulseLength;
        copy.leaderPulseLength = leaderPulseLength;
        copy.LEADER_BUFFER_LENGTH = LEADER_BUFFER_LENGTH;
        copy.bottomHalfOfPulseExtractionOnly = bottomHalfOfPulseExtractionOnly;
        copy.percentageDifferenceNotToCountAsSamePulseLength = percentageDifferenceNotToCountAsSamePulseLength;
        copy.toleranceForPulseCountingAsLeader = toleranceForPulseCountingAsLeader;
        copy.useLeaderBufferToAdjustPulseFrequency = useLeaderBufferToAdjustPulseFrequency;
        return copy;
    }

    public int wholeNumberOfTapsFor(double seconds) {
        return (int)(seconds / TAP);
    }

    public double secondsForTaps(int taps) {
        return ((double)taps) * TAP;
    }
}
