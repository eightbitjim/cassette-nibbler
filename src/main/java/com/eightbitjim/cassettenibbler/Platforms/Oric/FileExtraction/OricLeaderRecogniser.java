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

package com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

public class OricLeaderRecogniser {

    private final char [] compareWith = {
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.MEDIUM_PULSE,
            PulseStreamConsumer.MEDIUM_PULSE,
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.MEDIUM_PULSE,
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.LONG_PULSE,
            PulseStreamConsumer.LONG_PULSE
    };

    private char [] buffer = new char[compareWith.length];
    private int bufferPointer = 0;

    public void reset() {
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = 0;
    }

    public void addPulse(char pulse) {
        buffer[bufferPointer] = pulse;
        bufferPointer = (bufferPointer + 1) % buffer.length;
    }

    public boolean isLeaderValid() {
        for (int i = 0; i < compareWith.length; i++) {
            int positionInBuffer = (bufferPointer + i) % buffer.length;
            if (compareWith[i] != buffer[positionInBuffer])
                return false;
        }

        return true;
    }
}
