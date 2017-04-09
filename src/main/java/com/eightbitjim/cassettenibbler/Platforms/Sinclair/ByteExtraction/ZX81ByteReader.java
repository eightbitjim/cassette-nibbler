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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.ByteExtraction;

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.ByteStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

import java.util.LinkedList;
import java.util.List;

public class ZX81ByteReader implements ByteStreamProvider, PulseStreamConsumer {

    private boolean longPulseFound;
    int numberOfShortPulsesSinceLongPulse;
    int numberOfInvalidPulsesSinceLastByte;

    private List<ByteStreamConsumer> consumers;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private static final int INVALID = -1;
    private static final int END_OF_STREAM = -2;

    private long timeIndexOfLastLongPulse;
    private int bitsReadInByte;
    private int buildingByte;
    private boolean silenceBeforeThisByte;
    char pulse;
    long currentTimeIndex;

    private static final long nanosecondsBetweenLongPulsesForZeroBit = 2380000L;
    private static final long nanosecondsBetweenLongPulsesForOneBit = 3700000L;
    private static final long leewayInmatchingBitLengths = 300000L;

    public ZX81ByteReader() {
        longPulseFound = false;
        consumers = new LinkedList<>();
        numberOfShortPulsesSinceLongPulse = 0;
        numberOfInvalidPulsesSinceLastByte = 0;
        bitsReadInByte = 0;
        buildingByte = 0;
        timeIndexOfLastLongPulse = 0;
        silenceBeforeThisByte = false;
    }

    private void resetByte() {
        buildingByte = 0;
        bitsReadInByte = 0;
        numberOfInvalidPulsesSinceLastByte = 0;
        silenceBeforeThisByte = false;
    }

    private void addBitToByte(int bit) {
        switch (bit) {
            case END_OF_STREAM:
                break;
            case INVALID:
                logging.writeDataError(currentTimeIndex, "Invalid pulse length detected");
                buildingByte = 0;
                bitsReadInByte = 0;
                numberOfInvalidPulsesSinceLastByte++;
                break;
            case 0:
                buildingByte <<= 1;
                bitsReadInByte++;
                break;
            case 1:
                buildingByte <<= 1;
                buildingByte |= 1;
                bitsReadInByte++;
                break;
        }

        if (bitsReadInByte == 8) {
            pushByteToConsumers(buildingByte);
            resetByte();
        }
    }

    private void processPulse() {
        int gotBit;
        if (pulse == PulseStreamConsumer.END_OF_STREAM) {
            pushEndOfStream();
            return;
        }

        switch (pulse) {
            case PulseStreamConsumer.INVALID_PULSE_TOO_LONG:
            case PulseStreamConsumer.INVALID_PULSE_TOO_SHORT:
            case PulseStreamConsumer.MEDIUM_PULSE:
                longPulseFound = false;
                numberOfShortPulsesSinceLongPulse = 0;
                numberOfInvalidPulsesSinceLastByte++;
                break;

            case PulseStreamConsumer.SHORT_PULSE:
                numberOfShortPulsesSinceLongPulse++;
                break;

            case PulseStreamConsumer.SILENCE:
                silenceBeforeThisByte = true;
                // Fall through deliberately

            case PulseStreamConsumer.LONG_PULSE:
                if (longPulseFound) {
                    if (numberOfShortPulsesSinceLongPulse == 3)
                        gotBit = 0;
                    else
                    if (numberOfShortPulsesSinceLongPulse == 8)
                        gotBit = 1;
                    else
                        gotBit = attamptToIdentifyBitThroughDistanceBetweenLongPulses();

                    longPulseFound = true;
                    numberOfShortPulsesSinceLongPulse = 0;
                    addBitToByte(gotBit);
                } else {
                    longPulseFound = true;
                    numberOfShortPulsesSinceLongPulse = 0;
                }

                timeIndexOfLastLongPulse = currentTimeIndex;
                break;
        }
    }

    private int attamptToIdentifyBitThroughDistanceBetweenLongPulses() {
        long nanosecondsBetweenLongPulses = currentTimeIndex - timeIndexOfLastLongPulse;
        if (nanosecondsBetweenLongPulses > nanosecondsBetweenLongPulsesForZeroBit - leewayInmatchingBitLengths &&
                nanosecondsBetweenLongPulses < nanosecondsBetweenLongPulsesForZeroBit + leewayInmatchingBitLengths) {
            logging.writeDataError(currentTimeIndex, "Invalid number of short pulses between long pulses (" + numberOfShortPulsesSinceLongPulse + "). Matched as 0 bit through tinings between long pulses.");
            return 0;
        }

        if (nanosecondsBetweenLongPulses > nanosecondsBetweenLongPulsesForOneBit - leewayInmatchingBitLengths &&
                nanosecondsBetweenLongPulses < nanosecondsBetweenLongPulsesForOneBit + leewayInmatchingBitLengths) {
            logging.writeDataError(currentTimeIndex, "Invalid number of short pulses between long pulses (" + numberOfShortPulsesSinceLongPulse + "). Matched as 1 bit through tinings between long pulses.");
            return 1;
        }

        logging.writeDataError(currentTimeIndex, "Invalid number of short pulses between long pulses (" + numberOfShortPulsesSinceLongPulse + "). Cannot match to a 1 or a 0. Time between long pulses: " + nanosecondsBetweenLongPulses + "ns");
        return INVALID;
    }

    private long erroneousPulsesSinceLastByte() {
        return numberOfInvalidPulsesSinceLastByte;
    }
    private boolean silenceBeforePreviousByte() {
        return silenceBeforeThisByte;
    }

    @Override
    public void registerByteStreamConsumer(ByteStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterByteStreamConsumer(ByteStreamConsumer consumer) {
        consumers.remove(consumer);
    }


    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.pulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;
        if (!PulseUtilities.isPulseAnnotation(pulseType))
            processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {
    }

    private void pushEndOfStream() {
        pushByteToConsumers(ByteStreamConsumer.END_OF_STREAM);
    }

    private void pushByteToConsumers(int byteToReturn) {
        for (ByteStreamConsumer consumer : consumers)
            consumer.pushByte(byteToReturn, currentTimeIndex, erroneousPulsesSinceLastByte(), silenceBeforePreviousByte());
    }
}
