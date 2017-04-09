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

package com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

public class PulseUtilities {
    public static boolean isPulseAnnotation(char pulse) {
        switch (pulse) {
            case PulseStreamConsumer.END_OF_STREAM:
            case PulseStreamConsumer.INVALID_PULSE_TOO_LONG:
            case PulseStreamConsumer.INVALID_PULSE_TOO_SHORT:
            case PulseStreamConsumer.LONG_PULSE:
            case PulseStreamConsumer.MEDIUM_PULSE:
            case PulseStreamConsumer.SHORT_PULSE:
            case PulseStreamConsumer.SILENCE:
                return false;
            default:
                return true;
        }
    }
}
