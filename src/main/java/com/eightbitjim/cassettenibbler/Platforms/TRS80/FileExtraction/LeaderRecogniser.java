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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.FileExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

public class LeaderRecogniser {
    private char [] compareWith;
    private char [] buffer;
    private int bufferPointer = 0;

    private static final int BYTE_LENGTH = 8;
    private static final char ONE_PULSE = PulseStreamConsumer.SHORT_PULSE;
    private static final char ZERO_PULSE = PulseStreamConsumer.MEDIUM_PULSE;

    private static final int [] matchSequence = { 0x55, 0x3c }; // TRS80 leader plus sync

    public LeaderRecogniser() {
        createComparisonBuffer();
        createInputBuffer();
    }

    private void createComparisonBuffer() {
        compareWith = new char [matchSequence.length * BYTE_LENGTH];
        int position = 0;
        while (position < compareWith.length) {
            int currentByteValue = matchSequence[position / 8];
            int bitPositionWithinByte = /*7 - */(position % 8);
            compareWith[position] = (currentByteValue & (1 << bitPositionWithinByte)) == 0 ?
                    ZERO_PULSE : ONE_PULSE;

            position++;
        }
    }

    private void createInputBuffer() {
        buffer = new char[compareWith.length];
    }

    public void reset() {
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = 0;
    }

    public void addPulse(char pulse) {
        buffer[bufferPointer] = pulse;
        bufferPointer = (bufferPointer + 1) % buffer.length;
    }

    public boolean leaderIsValid() {
        for (int i = 0; i < compareWith.length; i++) {
            int positionInBuffer = (bufferPointer + i) % buffer.length;
            if (compareWith[i] != buffer[positionInBuffer])
                return false;
        }

        return true;
    }
}
