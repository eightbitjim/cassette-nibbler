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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.FileExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

public class ByteFrame {
    public static final int ERROR = -1;
    public static final int MORE_BITS_NEEDED = -2;

    private static final int ZERO_BIT = PulseStreamConsumer.MEDIUM_PULSE;
    private static final int ONE_BIT = PulseStreamConsumer.SHORT_PULSE;

    private static final int BITS_IN_FRAME = 8;

    private int content;
    private int currentBitNumber;
    private boolean error;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private long currentTimeIndex;
    private char currentPulse;

    private int pulsesSinceLastByte;
    private static final int maximumPulsesSinceByte = 20;

    public ByteFrame() {
        reset();
    }

    private void logError(String message) {
        logging.writeDataError(currentTimeIndex, message);
    }

    public void reset() {
        pulsesSinceLastByte = 0;
        content = 0;
        currentBitNumber = 0;
        error = false;
    }

    public int addPulse(char pulse, long currentTimeIndex) {
        currentPulse = pulse;
        this.currentTimeIndex = currentTimeIndex;

        int valueToReturn = ERROR;
        switch (pulse) {
            case ONE_BIT:
            case ZERO_BIT:
                valueToReturn = processBit(pulse);
                break;

            default:
                valueToReturn = ERROR;
                break;
        }

        pulsesSinceLastByte++;
        if (valueToReturn >= 0)
            pulsesSinceLastByte = 0;

        if (tooManyErrors()) {
            logging.writeFileParsingInformation("Too many pulses since last value byte received");
            valueToReturn = ERROR;
        }

        return valueToReturn;
    }

    public boolean tooManyErrors() {
        return pulsesSinceLastByte > maximumPulsesSinceByte;
    }

    private int processBit(int bit) {
         addBitToByte(bit);

        int valueToReturn = getReturnValue();
        return valueToReturn;
    }

    private int getReturnValue() {
        if (error) {
            return ERROR;
        }

        if (currentBitNumber < BITS_IN_FRAME)
            return MORE_BITS_NEEDED;

        int valueToReturn = content;
        logging.writeFileParsingInformation(": " + valueToReturn);
        reset();
        return valueToReturn;
    }

    private void resetByte() {
        currentBitNumber = 0;
    }

    private void addBitToByte(int bit) {
        if (bit == ONE_BIT)
            content |= (1 << (currentBitNumber));

        currentBitNumber++;
    }

    private void pushStringToPulseStream(String s) {
        logging.writeFileParsingInformation(s);
    }
}
