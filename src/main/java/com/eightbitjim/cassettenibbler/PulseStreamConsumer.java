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

package com.eightbitjim.cassettenibbler;

import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

public interface PulseStreamConsumer {
    char SHORT_PULSE = 's';
    char MEDIUM_PULSE = 'm';
    char LONG_PULSE = 'l';
    char INVALID_PULSE_TOO_SHORT = '\'';
    char INVALID_PULSE_TOO_LONG = '*';
    char SILENCE = '?';
    char END_OF_STREAM = 'e';

    void pushPulse(char pulseType, long currentTimeIndex);

    void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters);
}
