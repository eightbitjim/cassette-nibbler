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

    private long counter;
    private long lastCounterValue;
    private long systemTimeAtStartInMillis;

    public TimeCounter() {
        counter = 0L;
        lastCounterValue = 0L;
        systemTimeAtStartInMillis = System.currentTimeMillis();
    }

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
        long timeNow = System.currentTimeMillis();
        long timeTakenInSeconds = (timeNow - systemTimeAtStartInMillis) / 1000L;
        long amountOfDataProcessedInSeconds = getTimeInNanoSeconds() / NANOSECONDS_IN_A_SECOND;
        double speedFactor = amountOfDataProcessedInSeconds / timeTakenInSeconds;

        long hours = hoursIn(amountOfDataProcessedInSeconds);
        long minutes = minutesIn(amountOfDataProcessedInSeconds) % 60;
        long seconds = amountOfDataProcessedInSeconds % 60;

        StringBuilder builder = new StringBuilder();
        builder.append("Processed ");

        if (hours > 0)
            builder.append(hours).append(" hour").append(hours == 1 ? " " : "s ");

        if (minutes > 0)
            builder.append(minutes).append(" minute").append(hours == 1 ? " " : "s ");

        builder.append(seconds).append(" second").append(hours == 1 ? " " : "s ");
        builder.append("(" + speedFactor + "x realtime)");
        return builder.toString();
    }

    private long hoursIn(long numberOfSeconds) {
        return minutesIn(numberOfSeconds) / 60;
    }

    private long minutesIn(long numberOfSeconds) {
        return numberOfSeconds / 60;
    }


}
