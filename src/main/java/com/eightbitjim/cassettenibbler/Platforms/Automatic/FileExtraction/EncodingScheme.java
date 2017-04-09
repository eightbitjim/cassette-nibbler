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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

import java.util.InvalidPropertiesFormatException;

public class EncodingScheme {

    private static final String separator = "-";
    private static final String mostSignificantBitFirstIndicator = "a";
    private static final String leastSignificantBitFirstIndicator = "b";

    PulseExtractiorParameters pulseExtractiorParameters;

    char [] zeroBit;
    char [] oneBit;
    int paddingBetweenBytes;
    boolean mostSignificantBitFirst;

    int maximumBitSize;

    public EncodingScheme(char [] zeroBit, char [] oneBit, int paddingBetweenBytes, boolean msbFirst) {
        this.zeroBit = zeroBit;
        this.oneBit = oneBit;
        this.paddingBetweenBytes = paddingBetweenBytes;
        this.mostSignificantBitFirst = msbFirst;
        pulseExtractiorParameters = new PulseExtractiorParameters();
        computeMaximumBitSize();
    }

    public EncodingScheme(String configurationString) throws InvalidPropertiesFormatException {
        readValuesFromConfiguration(configurationString);
        computeMaximumBitSize();
    }

    public void setPulseLengthsInNanoseconds(PulseExtractiorParameters parameters) {
        pulseExtractiorParameters = parameters.copyOf();
    }

    private void readValuesFromConfiguration(String config) throws InvalidPropertiesFormatException {
        if (config == null) {
            throw new InvalidPropertiesFormatException("Invalid properties format: null");
        }

        String[] values = config.split(separator);
        if (values.length < 4)
            throw new InvalidPropertiesFormatException(config);

        zeroBit = charArrayFrom(values[0]);
        oneBit = charArrayFrom(values[1]);

        try {
            paddingBetweenBytes = Integer.parseInt(values[2]);
            mostSignificantBitFirst = values[3].startsWith(mostSignificantBitFirstIndicator);
            pulseExtractiorParameters = new PulseExtractiorParameters();
            if (values.length > 4)
                getPulseParametersFromTaps(values, 4);
        } catch (Throwable t) {
            throw new InvalidPropertiesFormatException("Cannot parse configuration string (" + t.toString() + "): " + config);
        }
    }

    private void getPulseParametersFromTaps(String [] values, int startingIndex) throws InvalidPropertiesFormatException {
        if (values.length < startingIndex + 3)
            throw new InvalidPropertiesFormatException("Invalid properties. Cannot get pulse length values");

        try {
            for (int i = 0; i < 3; i++) {
                int numberOfTaps = Integer.parseInt(values[startingIndex + i]);
                switch (i) {
                    case 0:
                        pulseExtractiorParameters.shortPulseLength = pulseExtractiorParameters.secondsForTaps(numberOfTaps);
                        break;
                    case 1:
                        pulseExtractiorParameters.mediumPulseLength = pulseExtractiorParameters.secondsForTaps(numberOfTaps);
                        break;
                    case 2:
                        pulseExtractiorParameters.longPulseLength = pulseExtractiorParameters.secondsForTaps(numberOfTaps);
                        break;
                }
            }
        } catch (Throwable t) {
            throw new InvalidPropertiesFormatException("Invalid properties.");
        }
    }

    private char [] charArrayFrom(String s) {
        return s.toCharArray();
    }

    private void computeMaximumBitSize() {
        maximumBitSize = Math.max(zeroBit.length, oneBit.length);
    }

    public char [] getZeroBit() {
        return zeroBit;
    }

    public char [] getOneBit() {
        return oneBit;
    }

    public int getPaddingBetweenBytes() {
        return paddingBetweenBytes;
    }

    public boolean mostSignificantBitIsFirst() {
        return mostSignificantBitFirst;
    }

    public int getMaximumBitSize() {
        return maximumBitSize;
    }

    public int maximumByteFrameLength() {
        return paddingBetweenBytes + maximumBitSize * 8;
    }

    public String getDescriptor() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(zeroBit).append(separator);
        builder.append(oneBit).append(separator);
        builder.append(paddingBetweenBytes).append(separator);
        builder.append(mostSignificantBitFirst ? mostSignificantBitFirstIndicator : leastSignificantBitFirstIndicator);
        if (pulseExtractiorParameters != null) {
            builder.append(separator).append(pulseExtractiorParameters.wholeNumberOfTapsFor(pulseExtractiorParameters.shortPulseLength));
            builder.append(separator).append(pulseExtractiorParameters.wholeNumberOfTapsFor(pulseExtractiorParameters.mediumPulseLength));
            builder.append(separator).append(pulseExtractiorParameters.wholeNumberOfTapsFor(pulseExtractiorParameters.longPulseLength));
        }

        return builder.toString();
    }
}
