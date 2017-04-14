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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.PulseExtraction;

import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;
import com.eightbitjim.cassettenibbler.Transition;

import java.util.LinkedList;
import java.util.List;

public class TRS80PulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {
    private static final double NANOSECOND = 1.0 / 1000000000.0;

    private double shortPulseLength;
    private double mediumPulseLength;
    private double longPulseLength;

    private double shortPulseMinimum;
    private double mediumPulseMinimum;
    private double longPulseMinimum;
    private double longPulseMaximum;

    private double intervalShiftMultiplier;
    private static final double percentageDifferenceNotToCountAsSamePulseLength = 10.0;
    private static final double toleranceForPulseCountingAsLeader = 0.2;

    private Transition interval;
    private double secondsSinceLastHighToLowTransition = 0.0;

    private double [] leaderBuffer;
    private static final int LEADER_BUFFER_LENGTH = 32;
    private int leaderBufferPointer;
    private long currentTimeIndex;

    private List<PulseStreamConsumer> consumers;

    public TRS80PulseExtractor() {
        interval = new Transition();
        consumers = new LinkedList<>();
        setUpPulseLengths();
        currentTimeIndex = 0;
        leaderBuffer = new double[LEADER_BUFFER_LENGTH];
        leaderBufferPointer = 0;
        intervalShiftMultiplier = 1.0;
    }

    private void setUpPulseLengths() {
        shortPulseLength = 1.0 / 2400.0;
        mediumPulseLength = shortPulseLength * 2.0;
        longPulseLength = mediumPulseLength * 4.0;

        shortPulseMinimum = shortPulseLength / 3.0;
        mediumPulseMinimum = shortPulseLength + shortPulseLength / 2.0;
        longPulseMinimum = longPulseLength - shortPulseLength / 2.0;
        longPulseMaximum = longPulseLength + (longPulseLength * 2.0);
    }

    public void processInterval() {
        secondsSinceLastHighToLowTransition += interval.secondsSinceLastTransition;
        if (interval.transitionedToHigh)
            return;

        char pulseType = getPulseType();
        recordPulseLength(pulseType);
        pushPulseToConsumers(pulseType);

        secondsSinceLastHighToLowTransition = 0.0;
    }

    private void recordPulseLength(char pulseType) {
    }

    public char getPulseType() {
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < shortPulseMinimum) return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < mediumPulseMinimum) return PulseStreamConsumer.SHORT_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMinimum) return PulseStreamConsumer.MEDIUM_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMaximum) return PulseStreamConsumer.LONG_PULSE;
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
        if (transition.isEndOfStream()) {
            pushEndOfStream();
        } else {
            this.interval = transition;
            this.currentTimeIndex = (long) (currentTimeIndex / NANOSECOND);
            processInterval();
        }
    }

    private void pushEndOfStream() {
        char pulseType = PulseStreamConsumer.END_OF_STREAM;
        pushPulseToConsumers(pulseType);
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, currentTimeIndex);
    }
}
