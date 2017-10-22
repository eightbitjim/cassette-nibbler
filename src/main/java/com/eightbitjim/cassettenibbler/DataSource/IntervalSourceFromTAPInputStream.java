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

package com.eightbitjim.cassettenibbler.DataSource;

import com.eightbitjim.cassettenibbler.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class IntervalSourceFromTAPInputStream implements IntervalStreamProvider {

    enum State { READING_HEADER, WAITING_FOR_NEXT_INTERVAL, MULTI_BYTE_1, MULTI_BYTE_2, MULTI_BYTE_3 }
    private State state;

    private int byteNumberWithinFile;
    private List<IntervalStreamConsumer> consumers = new LinkedList<>();
    private InputStream inputStream;

    private double currentTimeIndex = 0.0;
    private Transition transition;
    private int value;
    private TAPHeader header;
    private int multiByteValue;

    private static final double CLOCK_CYCLES_PER_SECOND_PAL = 985248.0;
    private static final double CLOCK_CYCLES_PER_SECOND_NTSC = 1022730.0;
    private static final int MAX_VERSION_SUPPORTED = 1;
    public static final int END_OF_STREAM = -1;
    public static final int SUCCESS = 0;

    public IntervalSourceFromTAPInputStream(InputStream stream) {
        inputStream = stream;
        reset();
    }

    @Override
    public void registerIntervalStreamConsumer(IntervalStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterIntervalStreamConsumer(IntervalStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    public int getNextIntervalAndPushToConsumers() throws IOException {
        if (inputStream.available() < 1) {
            pushEndOfStream();
            return END_OF_STREAM;
        }

        value = inputStream.read();
        switch (state) {
            case READING_HEADER:
                processHeaderByte();
                break;
            case MULTI_BYTE_1:
                processMultiByteValue(0);
                break;
            case MULTI_BYTE_2:
                processMultiByteValue(1);
                break;
            case MULTI_BYTE_3:
                processMultiByteValue(2);
                break;
            case WAITING_FOR_NEXT_INTERVAL:
                processIntervalByte();
                break;
        }

        countBytesRead();
        return SUCCESS;
    }

    public void processStreamToEnd() throws IOException {
        int status = SUCCESS;
        while (status == SUCCESS) {
            status = getNextIntervalAndPushToConsumers();
        }
    }

    private void countBytesRead() {
        byteNumberWithinFile++;
        if (state != State.READING_HEADER && byteNumberWithinFile >= header.getFileSize())
            reset();
    }

    private void reset() {
        state = State.READING_HEADER;
        byteNumberWithinFile = 0;
        header = new TAPHeader();
        transition = new Transition();
    }

    private void pushEndOfStream() {
        transition.secondsSinceLastTransition = Transition.END_OF_STREAM;
        pushIntervalToConsumers();
    }

    private void pushIntervalToConsumers() {
        for (IntervalStreamConsumer consumer : consumers)
            consumer.pushInterval(transition, currentTimeIndex);
    }

    private void processHeaderByte() {
        header.addByte(value);
        if (!header.moreBytesNeeded())
            checkHeaderContents();
    }

    private void checkHeaderContents() {
        if (byteNumberWithinFile < TAPHeader.HEADER_SIZE - 1)
            return;

        if (header.getFileVersion() > MAX_VERSION_SUPPORTED) {
            reset();
        } else if (header.getFileSize() < 1) {
            reset();
        } else {
            state = State.WAITING_FOR_NEXT_INTERVAL;
        }
    }

    private void processMultiByteValue(int index) {
        switch (index) {
            case 0:
                multiByteValue = value;
                state = State.MULTI_BYTE_2;
                break;
            case 1:
                multiByteValue |= (value << 8);
                state = State.MULTI_BYTE_3;
                break;
            case 2:
                multiByteValue |= (value << 16);
                pushIntervalsForCycleLength((double)multiByteValue / CLOCK_CYCLES_PER_SECOND_PAL);
                state = State.WAITING_FOR_NEXT_INTERVAL;
                break;
        }
    }

    private void processIntervalByte() {
        if (value == 0)
            processZeroMarker();
        else {
            double cycleLength = (double)value * 8.0 / CLOCK_CYCLES_PER_SECOND_PAL;
            pushIntervalsForCycleLength(cycleLength);
        }
    }

    private void pushIntervalsForCycleLength(double cycleLengthInSeconds) {
        double intervalLength = cycleLengthInSeconds / 2.0;

        // Emit two intervals of half the cycle length as the Commodore hardware on which the TAP file format is
        // based only triggered on a rising pulse edge.
        transition.secondsSinceLastTransition = intervalLength;
        transition.transitionedToHigh = false;
        currentTimeIndex += intervalLength;
        pushIntervalToConsumers();

        transition.secondsSinceLastTransition = intervalLength;
        transition.transitionedToHigh = true;
        currentTimeIndex += intervalLength;
        pushIntervalToConsumers();
    }

    private void processZeroMarker() {
        switch (header.getFileVersion()) {
            case 0:
                pushLongCycleLength();
                break;
            case 1:
                // A three byte cycle length will follow
                state = State.MULTI_BYTE_1;
                break;
        }
    }

    private void pushLongCycleLength() {
        // Push a cycle length that is longer than the TAP file can represent, but we don't know how long the
        // original cycle was. So will assume it's the maximim cycle length * 2.
        double cycleLength = (double)(255 * 8 * 2) / CLOCK_CYCLES_PER_SECOND_PAL;
        pushIntervalsForCycleLength(cycleLength);
    }
}

class TAPHeader {
    public static final int HEADER_SIZE = 20;
    public TAPHeader() {
        headerValues = new int [HEADER_SIZE];
        bytesRead = 0;
    }

    private int [] headerValues;
    private int bytesRead;

    public void addByte(int value) {
        headerValues[bytesRead] = value;
        bytesRead++;
    }

    public boolean moreBytesNeeded() {
        return bytesRead < HEADER_SIZE;
    }

    public int getFileVersion() {
        return headerValues[12];
    }

    public long getFileSize() {
        return headerValues[16] + headerValues[17] * 0x100 + headerValues[18] * 0x10000 + headerValues[19] * 0x1000000;
    }
}
