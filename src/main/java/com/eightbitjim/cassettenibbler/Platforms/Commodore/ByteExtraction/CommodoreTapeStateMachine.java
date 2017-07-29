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

class CommodoreTapeStateMachine {
    public static final int WAITING_FOR_LEADER = 0;
    public static final int LEADER_FOUND = 1;
    public static final int RECEIVING_FRAME = 2;
    public static final int FRAME_RECEIVED = 3;
    public static final int FRAME_ERROR = 4;
    public static final int FINISHED = 5;

    private int state;
    private char [] pulseBuffer;
    private long [] pulseLengthBuffer;

    private static final int SIZE_OF_PULSE_BUFFER = 64;
    private int bufferPointer;
    private long pulsesSinceLastFrame;

    private CommodoreByteFrame frame;
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public CommodoreTapeStateMachine() {
        state = FRAME_ERROR;
        pulseBuffer = new char[SIZE_OF_PULSE_BUFFER];
        pulseLengthBuffer = new long[SIZE_OF_PULSE_BUFFER];
        bufferPointer = SIZE_OF_PULSE_BUFFER - 1;
        pulsesSinceLastFrame = 0;
        frame = new CommodoreByteFrame();
    }

    public void addPulse(char pulseType, long pulseLengthInNanoSeconds) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            state = FINISHED;
            return;
        }

        logging.writePulse(pulseType);
        bufferPointer = (bufferPointer + 1) % SIZE_OF_PULSE_BUFFER;
        pulseBuffer[bufferPointer] = pulseType;
        pulseLengthBuffer[bufferPointer] = pulseLengthInNanoSeconds;

        frame.resetFrameLengthMeasurements(); // Don't allow averages if byte scraping

        switch (state) {
            case WAITING_FOR_LEADER:
                checkForValidLeaderExceptLastTwoPulsesWhichMightBeANewDataMarker();
                break;
            case LEADER_FOUND:
                checkForValidLeaderExceptLastTwoPulsesWhichMightBeANewDataMarker();
                checkForFrameStart();
                break;
            case FRAME_RECEIVED:
                startNewFrame();
                addPulseToFrame();
                break;
            case RECEIVING_FRAME:
                addPulseToFrame();
                return;
            case FRAME_ERROR:
                addPulseToErrorRecovery();
                return;
        }

        if (state != RECEIVING_FRAME && state != FRAME_RECEIVED)
            pulsesSinceLastFrame++;
        else
            pulsesSinceLastFrame = 0;
    }

    public int getState() {
        return state;
    }

    private void checkForValidLeaderExceptLastTwoPulsesWhichMightBeANewDataMarker() {
        for (int i = 0; i < SIZE_OF_PULSE_BUFFER - 2; i++) {
            int positionToCheck = bufferPointer - 2 - i;
            if (positionToCheck < 0)
                positionToCheck += SIZE_OF_PULSE_BUFFER;
            
            if (pulseBuffer[positionToCheck] != PulseStreamConsumer.SHORT_PULSE) {
                state = WAITING_FOR_LEADER;
                return;
            }
        }

        state = LEADER_FOUND;
    }

    private void checkForFrameStart() {
        if (currentPulse() == PulseStreamConsumer.MEDIUM_PULSE && previousPulse() == PulseStreamConsumer.LONG_PULSE) {
            state = RECEIVING_FRAME;
            frame.reset();
            frame.addPulseAndReturnStatus(previousPulse(), previousPulseLength());
            frame.addPulseAndReturnStatus(currentPulse(), currentPulseLength());
        }
    }

    private void addPulseToFrame() {
        int result = frame.addPulseAndReturnStatus(currentPulse(), currentPulseLength());
        switch (result) {
            case CommodoreByteFrame.FRAME_COMPLETE_WITH_ERROR:
                state = FRAME_ERROR;
                logging.writeDataError(0, "CHECKBIT ERROR IN FRAME");
                break;
            case CommodoreByteFrame.INVALID_FRAME_START:
                state = FRAME_ERROR;
                logging.writeDataError(0, "UNEXPECTED PULSE IN FRAME");
                break;
            case CommodoreByteFrame.FRAME_COMPLETE:
                state = FRAME_RECEIVED;
                break;
            case CommodoreByteFrame.FRAME_INCOMPLETE:
                state = RECEIVING_FRAME;
                break;
        }
    }

    public int getByteFromFrame() {
        if (state != FRAME_RECEIVED)
            return -1;

        return frame.getByte();
    }

    private void startNewFrame() {
        frame.reset();
    }

    private char currentPulse() {
        return pulseBuffer[bufferPointer];
    }
    private long currentPulseLength() {
        return pulseLengthBuffer[bufferPointer];
    }

    private char previousPulse() {
        int position = bufferPointer - 1;
        if (position < 0)
            position = SIZE_OF_PULSE_BUFFER - 1;

        return pulseBuffer[position];
    }

    private long previousPulseLength() {
        int position = bufferPointer - 1;
        if (position < 0)
            position = SIZE_OF_PULSE_BUFFER - 1;

        return pulseLengthBuffer[position];
    }

    private void addPulseToErrorRecovery() {
        checkForFrameStart();
    }

    public long getPulsesSinceLastFrame() {
        return pulsesSinceLastFrame;
    }
}
