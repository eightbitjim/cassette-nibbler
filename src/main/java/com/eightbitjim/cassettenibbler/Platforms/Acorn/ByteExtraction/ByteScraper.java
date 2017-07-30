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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.ByteExtraction;

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.ByteStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction.AcornByte;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

import java.util.LinkedList;
import java.util.List;

public class ByteScraper implements ByteStreamProvider, PulseStreamConsumer{
    private AcornByte acornByte;
    private List<ByteStreamConsumer> consumers;
    private char pulse;
    private long currentTimeIndex;
    private long lastTimeIndex;
    private boolean firstPulse;
    private long errorsSinceLastByte;

    public ByteScraper(AcornByte.Baud baudRate) {
        errorsSinceLastByte = 0;
        firstPulse = true;
        acornByte = new AcornByte(baudRate);
        consumers = new LinkedList<>();
    }

    private void processPulse() {
        resetByteIfTimeElapsed();
        int result = acornByte.addPulse(pulse, currentTimeIndex);
        switch (result) {
            case AcornByte.ERROR:
                acornByte.reset();
                errorsSinceLastByte++;
                break; // Ignore erroneous bytes
            case AcornByte.MORE_BITS_NEEDED:
                break; // Nothing to do yet
            default:
                acornByte.reset();
                pushByteToConsumers(result);
                errorsSinceLastByte = 0;
                break;
        }
    }

    private void resetByteIfTimeElapsed() {
        final long CUTOFF_TIME_IN_NS = 100000000L;
        if (currentTimeIndex - lastTimeIndex > CUTOFF_TIME_IN_NS)
            acornByte.reset();
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
        if (firstPulse) {
            this.lastTimeIndex = currentTimeIndex;
            firstPulse = false;
        }

        if (!PulseUtilities.isPulseAnnotation(pulseType))
            processPulse();

        this.lastTimeIndex = currentTimeIndex;
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

    private void pushByteToConsumers(int byteToReturn) {
        for (ByteStreamConsumer consumer : consumers)
            consumer.pushByte(byteToReturn, currentTimeIndex, errorsSinceLastByte, false);
    }
}
