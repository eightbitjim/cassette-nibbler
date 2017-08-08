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

package com.eightbitjim.cassettenibbler.Platforms.Oric.ByteExtraction;

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.ByteStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

import java.util.LinkedList;
import java.util.List;

public class OricByteReader implements PulseStreamConsumer, ByteStreamProvider {
    private List <ByteStreamConsumer> byteStreamConsumers = new LinkedList<>();
    private char currentPulse;
    private long currentTimeIndex;
    private OricByteFrame frame;
    private boolean silenceBeforeThisByte;
    private long erroneousPulsesBeforeThisByte;

    public OricByteReader(String channelName) {
        frame = new OricByteFrame(channelName);
    }

    @Override
    public void registerByteStreamConsumer(ByteStreamConsumer consumer) {
        if (!byteStreamConsumers.contains(consumer))
            byteStreamConsumers.add(consumer);
    }

    @Override
    public void deregisterByteStreamConsumer(ByteStreamConsumer consumer) {
        byteStreamConsumers.remove(consumer);
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.currentPulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;
        if (!PulseUtilities.isPulseAnnotation(pulseType))
           processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

    private void processPulse() {
        if (currentPulse == PulseStreamConsumer.SILENCE)
            silenceBeforeThisByte = true;

        if (currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT)
            erroneousPulsesBeforeThisByte++;

        int value = frame.processPulseAndReturnByteOrErrorCode(currentPulse);
        switch (value) {
            case OricByteFrame.END_OF_STREAM:
                break;

            case OricByteFrame.PARITY_ERROR:
                sendByteToConsumers(0); // TODO
                break;

            case OricByteFrame.MORE_PULSES_NEEDED:
                break;

            default:
                sendByteToConsumers(value);
                break;
        }
    }

    private void sendByteToConsumers(int value) {
        for (ByteStreamConsumer consumer : byteStreamConsumers)
            consumer.pushByte(value, currentTimeIndex, erroneousPulsesBeforeThisByte, silenceBeforeThisByte);

        silenceBeforeThisByte = false;
        erroneousPulsesBeforeThisByte = 0;
    }
}
