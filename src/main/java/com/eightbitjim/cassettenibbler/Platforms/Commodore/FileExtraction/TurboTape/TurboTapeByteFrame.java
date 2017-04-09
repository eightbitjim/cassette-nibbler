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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.TurboTape;

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

public class TurboTapeByteFrame {
    public static final int END_OF_STREAM = -1;
    public static final int MORE_PULSES_NEEDED = -2;
    public static final int INVALID_PULSE = -3;

    private static final char ZERO_PULSE_VALUE = PulseStreamConsumer.MEDIUM_PULSE;
    private static final char ONE_PULSE_VALUE = PulseStreamConsumer.LONG_PULSE;

    private char [] frameBuffer;
    private int frameBufferPointer;

    private int dataPulsesReceived;
    private static final int dataPulsesInAFrame = 8;
    private static final int FRAME_BUFFER_LENGTH = dataPulsesInAFrame;

    private int valueToReturn;
    private char currentPulse;

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    public TurboTapeByteFrame() {
        reset();
    }

    public void reset() {
        frameBuffer = new char[FRAME_BUFFER_LENGTH];
        frameBufferPointer = 0;
        dataPulsesReceived = 0;
    }

    public int processPulseAndReturnByteOrErrorCode(char pulse) {
        currentPulse = pulse;
        if (PulseUtilities.isPulseAnnotation(pulse))
            return MORE_PULSES_NEEDED;

        if (pulse == ByteStreamConsumer.END_OF_STREAM)
            return END_OF_STREAM;

        pushPulseToStream(pulse);

        valueToReturn = MORE_PULSES_NEEDED;
        addPulseToData();

        return valueToReturn;
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
        valueToReturn = INVALID_PULSE;
    }

    private void addToFrameBuffer(char pulse) {
        dataPulsesReceived++;
        frameBuffer[frameBufferPointer] = pulse;
        frameBufferPointer = (frameBufferPointer + 1) % FRAME_BUFFER_LENGTH;
    }

    private int checkForValidByte() {
        if (dataPulsesReceived < dataPulsesInAFrame)
            return MORE_PULSES_NEEDED;

        int value = byteValue();
        pushStringToPulseStream(": " + Integer.toHexString(value));
        return value;
    }

    private int byteValue() {

        int value = 0;
        for (int i = 0; i < 8; i++) {
            if (pulseAtPosition(i) == ONE_PULSE_VALUE)
                value |= (1 << (7 - i));
        }

        dataPulsesReceived = 0;
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
