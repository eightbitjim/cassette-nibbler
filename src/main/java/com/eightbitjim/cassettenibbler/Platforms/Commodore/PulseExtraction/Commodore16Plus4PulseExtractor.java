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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction;

import com.eightbitjim.cassettenibbler.Transition;
import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class Commodore16Plus4PulseExtractor implements PulseStreamProvider, IntervalStreamConsumer {

    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final double shortPulseLength = 226757.0 * 2.0 * NANOSECOND;
    private static final double mediumPulseLength = 480000.0 * 2.0 * NANOSECOND;
    private static final double longPulseLength = (1111111.0 + 702947.0) * NANOSECOND;

    private static final double shortPulseMinimum = shortPulseLength - (mediumPulseLength - shortPulseLength) / 2;
    private static final double mediumPulseMinimum = shortPulseLength + (mediumPulseLength - shortPulseLength) / 2;
    private static final double longPulseMinimum = mediumPulseLength + (longPulseLength - mediumPulseLength) / 2;
    private static final double longPulseMaximum = longPulseLength + (longPulseLength - mediumPulseLength) / 2;

    private double intervalShiftMultiplier;
    private static final double percentageDifferenceNotToCountAsSamePulseLength = 10.0;
    private static final double toleranceForPulseCountingAsLeader = 1.6;

    private Transition interval;
    private double secondsSinceLastHighToLowTransition = 0.0;

    private double [] leaderBuffer;
    private static final int LEADER_BUFFER_LENGTH = 32;
    private int leaderBufferPointer;

    private long currentTimeIndex;
    private List<PulseStreamConsumer> consumers;

    public Commodore16Plus4PulseExtractor() {
        consumers = new LinkedList<>();
        interval = new Transition();
        currentTimeIndex = 0;
        leaderBuffer = new double[LEADER_BUFFER_LENGTH];
        leaderBufferPointer = 0;
        intervalShiftMultiplier = 1.0;
    }

    public void processInterval() {
        secondsSinceLastHighToLowTransition += interval.secondsSinceLastTransition;
        if (!interval.transitionedToHigh)
            return;

        registerWithLeaderBuffer();
        currentTimeIndex += secondsSinceLastHighToLowTransition / NANOSECOND;
        char pulseType = getPulseType();
        pushPulseToConsumers(pulseType);
        secondsSinceLastHighToLowTransition = 0.0;
    }

    private void registerWithLeaderBuffer() {
        leaderBuffer[leaderBufferPointer] = secondsSinceLastHighToLowTransition;
        leaderBufferPointer = (leaderBufferPointer + 1 ) % LEADER_BUFFER_LENGTH;
        adjustFreqncyIfValidLeaderBuffer();
    }

    private void adjustFreqncyIfValidLeaderBuffer() {
        double lastValue = leaderBuffer[0];
        double sum = lastValue;
        for (int i = 1; i < LEADER_BUFFER_LENGTH; i++) {
            double differenceFromLastPulseLength = Math.abs(leaderBuffer[i] - lastValue);
            lastValue = leaderBuffer[i];
            sum += lastValue;
            if (differenceFromLastPulseLength / lastValue * 100.0 > percentageDifferenceNotToCountAsSamePulseLength) {
                return;
            }
        }

        double average = sum / (double)LEADER_BUFFER_LENGTH;
        if ((average < shortPulseLength / toleranceForPulseCountingAsLeader
                || average > shortPulseLength * toleranceForPulseCountingAsLeader)) {
            return;
        }

        intervalShiftMultiplier =  shortPulseLength / average;
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
        this.interval = transition;
        this.currentTimeIndex = (long)(currentTimeIndex / NANOSECOND);
        processInterval();
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, currentTimeIndex);
    }
}
