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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.ByteExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

public class CommodoreByteFrame {
    public static final int FRAME_COMPLETE_WITH_ERROR = -1;
    public static final int FRAME_INCOMPLETE = -2;
    public static final int INVALID_FRAME_START = -3;
    public static final int FRAME_COMPLETE = 0;
    private static final int PULSES_IN_FRAME = 20;

    private static final int ONE_BIT = 1;
    private static final int ZERO_BIT = 0;
    private static final int INVALID_BIT = -1;

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    char [] pulses;
    long [] pulseLengthInNanoSeconds;
    long currentFrameLengthInNanoseconds;
    long [] frameLengthInNanoseconds;
    int frameLengthPointer;
    long framesReceived;
    long averageFrameLength;

    private int overrideByteToReturn;
    private static final int SIZE_OF_FRAME_LENGTH_BUFFER = 8;
    private char currentPulse;
    private long currentPulseLength;
    private boolean scanningForNextFrameStart;

    int numberOfPulses;

    public CommodoreByteFrame() {
        pulses = new char[PULSES_IN_FRAME];
        pulseLengthInNanoSeconds = new long[PULSES_IN_FRAME];
        frameLengthInNanoseconds = new long[SIZE_OF_FRAME_LENGTH_BUFFER];
        currentFrameLengthInNanoseconds = 0;
        frameLengthPointer = 0;
        framesReceived = 0;
        averageFrameLength = 0;
        overrideByteToReturn = -1;

        for (int i = 0; i < PULSES_IN_FRAME; i++) {
            pulses[i] = PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
            pulseLengthInNanoSeconds[i] = 0;
        }

        numberOfPulses = 0;
    }

    public void resetFrameLengthMeasurements() {
        framesReceived = 0;
        averageFrameLength = 0;
    }

    public void reset() {
        numberOfPulses = 0;
        currentFrameLengthInNanoseconds = 0;
        overrideByteToReturn = -1;
        scanningForNextFrameStart = false;
    }

    public void scanForNextFrameStart() {
        scanningForNextFrameStart = true;
        numberOfPulses = 0;
        logging.writeFileParsingInformation("Waiting for next frame start based on timing");
    }

    public int addPulseAndReturnStatus(char pulse, long lengthOfThisPulse) {
        if (numberOfPulses == PULSES_IN_FRAME)
            reset();

        overrideByteToReturn = -1;
        currentPulse = pulse;
        currentPulseLength = lengthOfThisPulse;
        recordPulseLengthForAverage(lengthOfThisPulse);

        if (scanningForNextFrameStart)
            checkIfFrameStart();

        checkFrameLengthIsWithinLimit();

        pulses[numberOfPulses] = pulse;
        pulseLengthInNanoSeconds[numberOfPulses] = lengthOfThisPulse;
        numberOfPulses++;

        if (dataMarkerIsInvalid()) return INVALID_FRAME_START;
        if (numberOfPulses == PULSES_IN_FRAME) {
            if (checkbitIsInvalid()) return FRAME_COMPLETE_WITH_ERROR;
            else {
                logging.writeFileParsingInformation(" (" + getByteToleratingError() + ")");
                recordFrameLength();
                return FRAME_COMPLETE;
            }
        }

        if (overrideByteToReturn >= 0)
            return FRAME_COMPLETE_WITH_ERROR;
        else
            return FRAME_INCOMPLETE;
    }

    private void checkIfFrameStart() {
        if (currentFrameLengthInNanoseconds <= averageFrameLength)
            return;

        switch (currentPulse) {
            case PulseStreamConsumer.INVALID_PULSE_TOO_SHORT:
            case PulseStreamConsumer.SHORT_PULSE:
            case PulseStreamConsumer.MEDIUM_PULSE:
                return;
        }

        reset();
        scanningForNextFrameStart = false;
        recordPulseLengthForAverage(currentPulseLength);
        logging.writeFileParsingInformation("Found potential start of next frame. Pulse is: " + currentPulse);
    }

    private void recordPulseLengthForAverage(long length) {
        currentFrameLengthInNanoseconds += length;
    }

    private void checkFrameLengthIsWithinLimit() {
        if (averageFrameLength == 0)
            return;

        if (currentFrameLengthInNanoseconds > averageFrameLength + averageFrameLength / 10) {
            logging.writeDataError(0, "Frame not complete but taken too long. Next pulse " + currentPulse);

            startANewFrameHere();
        }
    }

    private void startANewFrameHere() {
        overrideByteToReturn = getByteToleratingError();
        reset();
        currentFrameLengthInNanoseconds = currentPulseLength;
    }

    private void recordFrameLength() {
        frameLengthInNanoseconds[frameLengthPointer] = currentFrameLengthInNanoseconds;
        frameLengthPointer = (frameLengthPointer + 1) % SIZE_OF_FRAME_LENGTH_BUFFER;

        framesReceived++;
        if (framesReceived > SIZE_OF_FRAME_LENGTH_BUFFER)
            computeAverageFrameLength();
    }

    private void computeAverageFrameLength() {
        averageFrameLength = 0;
        for (long frameLength : frameLengthInNanoseconds) {
            averageFrameLength += frameLength;
        }

        averageFrameLength /= frameLengthInNanoseconds.length;
    }

    public int getByte() {
        if (numberOfPulses < PULSES_IN_FRAME) return FRAME_INCOMPLETE;
        if (checkbitIsInvalid()) return FRAME_COMPLETE_WITH_ERROR;

        int byteToReturn = getByteFromFrame();
        return byteToReturn;
    }

    public int getByteToleratingError() {
        if (overrideByteToReturn >= 0) {
            int toReturn = overrideByteToReturn;
            return toReturn;
        }

        if (numberOfPulses < PULSES_IN_FRAME) return FRAME_INCOMPLETE;

        int byteToReturn = getByteFromFrame();
        return byteToReturn;
    }

    private boolean dataMarkerIsInvalid() {
        // Should be LONG, MEDIUM, but will tolerate LONG, SHORTER
        if (numberOfPulses < 2) return false;

        if (pulseLengthInNanoSeconds[1] >= pulseLengthInNanoSeconds[0]) {
            logging.writeDataError(0, "INVALID FRAME START. PULSE LENGTHS " + pulseLengthInNanoSeconds[0] + ", " + pulseLengthInNanoSeconds[1]);
            return true;
        }

        return false;
    }

    private boolean checkbitIsInvalid() {
        if (numberOfPulses < PULSES_IN_FRAME) return false;

        int byteValue = getByteFromFrame();
        if (byteValue == INVALID_FRAME_START)
            return false;

        int checkPulsesValue = getBitFromPulsesAt(18);
        if (checkPulsesValue == INVALID_BIT)
            return false;

        boolean checkbitValue = checkPulsesValue == ONE_BIT;
        boolean calculatedValue = true;
        for (int i = 0; i < 8; i++) {
            boolean currentBit = getBitFromPulsesAt(2 + i * 2) == ONE_BIT;
            calculatedValue ^= currentBit;
        }

        return calculatedValue != checkbitValue;
    }

    private int getByteFromFrame() {
        if (overrideByteToReturn >= 0) {
            int toReturn = overrideByteToReturn;
            overrideByteToReturn = 0;
            return toReturn;
        }

        int byteValue = 0;
        for (int bit = 0; bit < 8; bit++) {
            int bitValue = getBitFromPulsesAt(2 + bit * 2);
            if (bitValue == INVALID_BIT)
                return INVALID_FRAME_START;

            if (bitValue == ONE_BIT)
                byteValue |= (1 << bit);
        }

        return byteValue;
    }

    private int getBitFromPulsesAt(int position) {
        if (pulses[position] == PulseStreamConsumer.SHORT_PULSE && pulses[position + 1] == PulseStreamConsumer.MEDIUM_PULSE)
            return ZERO_BIT;

        if (pulses[position] == PulseStreamConsumer.MEDIUM_PULSE && pulses[position + 1] == PulseStreamConsumer.SHORT_PULSE)
            return ONE_BIT;

        return estimateBitValueAtPosition(position);
    }

    private int estimateBitValueAtPosition(int position) {
        if (pulseLengthInNanoSeconds[position] < pulseLengthInNanoSeconds[position + 1])
            return ZERO_BIT;
        else
            return ONE_BIT;
    }
}
