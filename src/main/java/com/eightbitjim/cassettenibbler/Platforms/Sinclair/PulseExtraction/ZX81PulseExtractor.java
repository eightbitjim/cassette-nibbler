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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.PulseExtraction;

import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;
import com.eightbitjim.cassettenibbler.Transition;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class ZX81PulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {

    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final double shortPulseLength = NANOSECOND * (181405.0 + 68027.0);
    private static final double longPulseLength = NANOSECOND * (1496598.0 + 90702.0);

    private static final double shortPulseMinimum = shortPulseLength - shortPulseLength / 2.0;
    private static final double shortPulseMaximum = shortPulseLength + shortPulseLength / 2.0;
    private static final double longPulseMinimum =  longPulseLength - longPulseLength / 3.0;
    private static final double longPulseMaximum = longPulseLength + longPulseLength / 3.0;

    private static final double silenceMinimumLengthInSeconds = 0.5;
    private double intervalShiftMultiplier;

    private Transition interval;
    private double secondsSinceLastHighToLowTransition = 0.0;

    private double [] leaderBuffer;
    private static final int LEADER_BUFFER_LENGTH = 32;
    private int leaderBufferPointer;
    private long currentTimeIndex;
    private boolean currentlySilent;

    private List<PulseStreamConsumer> consumers;

    public ZX81PulseExtractor() {
        interval = new Transition();
        currentTimeIndex = 0;
        consumers = new LinkedList<>();
        leaderBuffer = new double[LEADER_BUFFER_LENGTH];
        leaderBufferPointer = 0;
        intervalShiftMultiplier = 1.0;
        currentlySilent = true;
    }

    public void processInterval() {
        secondsSinceLastHighToLowTransition += interval.secondsSinceLastTransition;
        if (interval.transitionedToHigh)
            return;

        char pulseType = getPulseType();
        pushPulseToConsumers(pulseType);
        secondsSinceLastHighToLowTransition = 0.0;
    }

    public char getPulseType() {
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < shortPulseMinimum) return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < shortPulseMaximum) return PulseStreamConsumer.SHORT_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMinimum) return PulseStreamConsumer.SHORT_PULSE;// TODO invalid?
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMaximum) return PulseStreamConsumer.LONG_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier > silenceMinimumLengthInSeconds) return PulseStreamConsumer.SILENCE;
        return PulseStreamConsumer.INVALID_PULSE_TOO_LONG;
    }

    @Override
    public void registerPulseStreamConsumer(PulseStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterPulseStreamConsumer(PulseStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    @Override
    public void pushInterval(Transition transition, double currentTimeIndex) {
        this.interval = transition;
        this.currentTimeIndex = (long)(currentTimeIndex / NANOSECOND);
        processInterval();
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, currentTimeIndex);
    }
}
