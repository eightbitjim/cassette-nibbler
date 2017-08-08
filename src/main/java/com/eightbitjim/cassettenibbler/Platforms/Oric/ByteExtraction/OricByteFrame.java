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
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
public class OricByteFrame {
    public static final int END_OF_STREAM = -1;
    public static final int MORE_PULSES_NEEDED = -2;
    public static final int PARITY_ERROR = -3;

    private static final char START_PULSE_VALUE = PulseStreamConsumer.LONG_PULSE;
    private static final char ZERO_PULSE_VALUE = PulseStreamConsumer.LONG_PULSE;
    private static final char ONE_PULSE_VALUE = PulseStreamConsumer.MEDIUM_PULSE;

    private char [] frameBuffer;
    private int frameBufferPointer;

    private static final int STATE_WAITING_FOR_LEADER = 0;
    private static final int RECEIVING_LEADER = 1;
    private static final int RECEIVING_DATA = 2;

    private int state;
    private int dataPulsesReceived;
    private static final int dataPulsesInAFrame = 10;
    private static final int FRAME_BUFFER_LENGTH = dataPulsesInAFrame;

    private int leaderPulsesInARow;
    private static final int numberOfPulsesToCountAsValidLeader = 2;
    private int valueToReturn;
    private char currentPulse;

    private transient TapeExtractionLogging logging;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    public OricByteFrame(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        reset();
    }

    public void reset() {
        frameBuffer = new char[FRAME_BUFFER_LENGTH];
        frameBufferPointer = 0;
        switchStateTo(STATE_WAITING_FOR_LEADER);
    }

    public int processPulseAndReturnByteOrErrorCode(char pulse) {
        currentPulse = pulse;
        if (PulseUtilities.isPulseAnnotation(pulse))
            return MORE_PULSES_NEEDED;

        if (pulse == ByteStreamConsumer.END_OF_STREAM)
            return END_OF_STREAM;

        pushPulseToStream(pulse);

        valueToReturn = MORE_PULSES_NEEDED;
        switch (state) {
            case STATE_WAITING_FOR_LEADER:
                addPulseToLeader();
                break;

            case RECEIVING_LEADER:
                checkLeader();
                break;

            case RECEIVING_DATA:
                addPulseToData();
                break;
        }

        return valueToReturn;
    }

    private void switchStateTo(int state) {
        this.state = state;
        switch (state) {
            case STATE_WAITING_FOR_LEADER:
                leaderPulsesInARow = 0;
                break;
            case RECEIVING_LEADER:
                break;
            case RECEIVING_DATA:
                dataPulsesReceived = 0;
                break;
        }
    }

    private void addPulseToLeader() {
        switch (currentPulse) {
            case PulseStreamConsumer.MEDIUM_PULSE:
                leaderPulsesInARow++;
                if (leaderPulsesInARow >= numberOfPulsesToCountAsValidLeader)
                    switchStateTo(RECEIVING_LEADER);
                break;

            default:
                leaderPulsesInARow = 0;
                break;
        }
    }

    private void checkLeader() {
        switch (currentPulse) {
            case PulseStreamConsumer.MEDIUM_PULSE:
                leaderPulsesInARow++;
                break;
            default:
                switchStateTo(RECEIVING_DATA);
                addPulseToData();
                break;
        }
    }

    private void addPulseToData() {
        switch (currentPulse) {
            case PulseStreamConsumer.INVALID_PULSE_TOO_LONG:
            case PulseStreamConsumer.SILENCE:
                errorInFrame();
                break;

            case PulseStreamConsumer.INVALID_PULSE_TOO_SHORT:
            case PulseStreamConsumer.SHORT_PULSE:
                pushStringToPulseStream("TOO SHORT PULSE. IGNORING.");
                break;

            case PulseStreamConsumer.LONG_PULSE:
            case PulseStreamConsumer.MEDIUM_PULSE:
                addToFrameBuffer(currentPulse);
                valueToReturn = checkForValidByte();
                break;

            default:
                break;
        }
    }

    private void errorInFrame() {
        pushStringToPulseStream("ERROR IN FRAME\n");
        switchStateTo(STATE_WAITING_FOR_LEADER);
        valueToReturn = PARITY_ERROR;
    }

    private void addToFrameBuffer(char pulse) {
        dataPulsesReceived++;
        frameBuffer[frameBufferPointer] = pulse;
        frameBufferPointer = (frameBufferPointer + 1) % FRAME_BUFFER_LENGTH;
    }

    private int checkForValidByte() {
        if (dataPulsesReceived < dataPulsesInAFrame)
            return MORE_PULSES_NEEDED;

        if (!startBitPresent()) {
            return MORE_PULSES_NEEDED;
        }

        if (!parityCorrect()) {
            pushStringToPulseStream(": PARITY INCORRECT\n");
            if (!options.getAllowIncorrectFrameChecksums()) {
                dataPulsesReceived = 0;
                return PARITY_ERROR;
            } else {
                pushStringToPulseStream(" ALLOWING AS OPTIONS SET TO ALLOW INVALID FRAMES");
            }
        }

        int value = byteValue();
        pushStringToPulseStream(": " + Integer.toHexString(value));
        switchStateTo(STATE_WAITING_FOR_LEADER);
        return value;
    }

    private boolean startBitPresent() {
        return pulseAtPosition(0) == START_PULSE_VALUE;
    }

    private boolean parityCorrect() {
        int parityCounter = 0;
        for (int position = 1; position < 1 + 8; position++)
            parityCounter += pulseAtPosition(position) == ONE_PULSE_VALUE ? 1 : 0;

        char expectedParityValue = (parityCounter % 2 == 0) ? ONE_PULSE_VALUE : ZERO_PULSE_VALUE;
        boolean valid;
        valid =  pulseAtPosition(9) == expectedParityValue;
        return valid;
    }

    private int byteValue() {

        int value = 0;
        for (int i = 0; i < 8; i++) {
            if (pulseAtPosition(1 + i) == ONE_PULSE_VALUE)
                value |= (1 << i);

        }

        return value;
    }

    private char pulseAtPosition(int position) {
        position = (position + frameBufferPointer) % FRAME_BUFFER_LENGTH;
        return frameBuffer[position];
    }

    private void pushPulseToStream(char pulse) {
        logging.writePulse(pulse);
    }

    private void pushStringToPulseStream(String s) {
        logging.writeFileParsingInformation(s);
    }
}
