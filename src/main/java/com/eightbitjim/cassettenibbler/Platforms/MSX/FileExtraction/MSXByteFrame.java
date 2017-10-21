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

package com.eightbitjim.cassettenibbler.Platforms.MSX.FileExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

public class MSXByteFrame {
    public static final int ERROR = -1;
    public static final int MORE_BITS_NEEDED = -2;

    private static final int ZERO_BIT = PulseStreamConsumer.MEDIUM_PULSE;
    private static final int ONE_BIT = PulseStreamConsumer.SHORT_PULSE;

    private static final int BITS_IN_FRAME = 11;
    private static final int START_BIT_POSITION = 0;
    private static final int STOP_BIT_1_POSTION = 9;
    private static final int STOP_BIT_2_POSTION = 10;

    private int content;
    private int currentBitNumber;
    private boolean error;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging;
    private long currentTimeIndex;
    private char currentPulse;
    private int shortCyclesInARow;
    private int longCyclesInARow;

    private int pulsesSinceLastByte;
    private static final int maximumPulsesSinceByte = 20;

    public MSXByteFrame(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        reset();
    }

    public void setPulseStreamConsumer() {
    }

    private void log(String message) {
        logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, message);
    }

    private void logError(String message) {
        logging.writeDataError(currentTimeIndex, message);
    }

    public void reset() {
        pulsesSinceLastByte = 0;
        content = 0;
        currentBitNumber = 0;
        error = false;
        shortCyclesInARow = 0;
        longCyclesInARow = 0;
    }

    public int addPulse(char pulse, long currentTimeIndex) {
        currentPulse = pulse;
        this.currentTimeIndex = currentTimeIndex;

        int bit = addPulseToBit();
        int valueToReturn = ERROR;
        switch (bit) {
            case ONE_BIT:
            case ZERO_BIT:
                valueToReturn = processBit(bit);
                break;

            case MORE_BITS_NEEDED:
                valueToReturn = MORE_BITS_NEEDED;
                break;

            case ERROR:
                valueToReturn = ERROR;
                break;

            default:
                valueToReturn = MORE_BITS_NEEDED;
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
        switch (currentBitNumber) {
            case START_BIT_POSITION:
                checkForStartBit(bit);
                break;
            case STOP_BIT_1_POSTION:
                checkForStopBit(bit);
                break;

            case STOP_BIT_2_POSTION:
                checkForStopBit(bit);
                pushStringToPulseStream(" " + content);
                break;
            default:
                addBitToByte(bit);
                break;
        }

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
        reset();
        return valueToReturn;
    }

    private void checkForStartBit(int bit) {
        if (bit != ZERO_BIT) {
            resetByte();
            error = true;
            pushStringToPulseStream(" SKIP SHORT PULSE\n");
            return;
        }

        currentBitNumber++;
    }

    private void resetByte() {
        currentBitNumber = 0;
    }

    private void checkForStopBit(int bit) {
        if (bit != ONE_BIT) {
            error = true;
            logError("Invalid stop bit in byte frame: " + currentPulse);
        }

        currentBitNumber++;
    }

    private void addBitToByte(int bit) {
        if (bit == ONE_BIT)
            content |= (1 << (currentBitNumber - 1));

        currentBitNumber++;
    }

    private int addPulseToBit() {
        int shortPulsesInAOne = 2;
        int longPulsesInAZero = 1;

        switch (currentPulse) {
            case PulseStreamConsumer.SHORT_PULSE:
                if (longCyclesInARow > 0)
                    recordErroneousNumberOfLongCycles(longCyclesInARow);

                shortCyclesInARow++;
                longCyclesInARow = 0;
                break;
            case PulseStreamConsumer.MEDIUM_PULSE:
                if (shortCyclesInARow > 0)
                    recordErroneousNumberOfShortCycles(shortCyclesInARow);

                longCyclesInARow++;
                shortCyclesInARow = 0;
                break;
            default:
                return ERROR;
        }

        if (shortPulsesInAOne == shortCyclesInARow) {
            shortCyclesInARow = 0;
            return ONE_BIT;
        }

        if (longPulsesInAZero == longCyclesInARow) {
            longCyclesInARow = 0;
            return ZERO_BIT;
        }

        return MORE_BITS_NEEDED;
    }

    private void recordErroneousNumberOfLongCycles(int numberOfcycles) {
        logError("Erroneous number of long cycles. Got " + numberOfcycles + ".");
    }

    private void recordErroneousNumberOfShortCycles(int numberOfcycles) {
        logError("Erroneous number of short cycles. Got " + numberOfcycles + ".");
    }

    private void pushStringToPulseStream(String s) {
        logging.writeFileParsingInformation(s);
    }
}
