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

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.ByteStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CommodoreByteReader implements PulseStreamConsumer, ByteStreamProvider {

    private CommodoreTapeStateMachine stateMachine;
    private List<ByteStreamConsumer> consumers;
    private char pulse;
    private long currentTimeIndex;
    private long lastTimeIndex;
    private boolean firstPulse;

    public CommodoreByteReader(String channelName) {
        firstPulse = true;
        consumers = new LinkedList<>();
        stateMachine = new CommodoreTapeStateMachine(channelName);
    }

    public void close() throws IOException {

    }

    public long erroneousPulsesSinceLastByte() {
        return stateMachine.getPulsesSinceLastFrame();
    }

    private void processPulse() {
        stateMachine.addPulse(pulse, currentTimeIndex - lastTimeIndex);
        switch (stateMachine.getState()) {
            case CommodoreTapeStateMachine.FINISHED:
                pushByteToConsumers(ByteStreamConsumer.END_OF_STREAM);
                break;
            case CommodoreTapeStateMachine.FRAME_RECEIVED:
                pushByteToConsumers(stateMachine.getByteFromFrame());
                break;
        }
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
            consumer.pushByte(byteToReturn, currentTimeIndex, erroneousPulsesSinceLastByte(), false);
    }
}
