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

package com.eightbitjim.cassettenibbler.Platforms.General.Filters;

import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.Sample;

public class TimeCounter implements SampleStreamConsumer {
    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final long NANOSECONDS_IN_A_SECOND = 1000000000L;

    long counter = 0L;
    long lastCounterValue = 0L;

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        long currentCounter = (long)(currentTimeIndex / NANOSECOND);
        if (currentCounter < lastCounterValue)
            lastCounterValue = currentCounter;

        counter += (currentCounter - lastCounterValue);
        lastCounterValue = currentCounter;
    }

    public long getTimeInNanoSeconds() {
        return counter;
    }

    @Override
    public String toString() {
        long seconds = getTimeInNanoSeconds() / NANOSECONDS_IN_A_SECOND;
        if (seconds < 60)
            return ""  + seconds + " second" + (seconds == 1 ? "" : "s");

        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes < 60)
            return "" + minutes + " minute" + (minutes == 1 ? " " : "s ") +
                    seconds + " second" + (seconds == 1 ? "" : "s");

        long hours = minutes / 60;
        minutes = minutes % 60;
        return "" + hours + " hour" + (hours == 1 ? " " : "s ") +
                minutes + " minute" + (minutes == 1 ? " " : "s ") +
                seconds + " second" + (seconds == 1 ? "" : "s");
    }
}
