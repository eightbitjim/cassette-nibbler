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

import com.eightbitjim.cassettenibbler.Transition;
import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class GeneralPulseExtraction implements PulseStreamProvider, IntervalStreamConsumer {

    private Transition interval;
    private double secondsSinceLastHighToLowTransition = 0.0;
    public static final double NANOSECOND = 1.0 / 1000000000.0;

    private double [] leaderBuffer;
    private int leaderBufferPointer;

    private long currentTimeIndex;
    private List<PulseStreamConsumer> consumers;
    private PulseExtractiorParameters parameters;
    private double intervalShiftMultiplier;

    public double shortPulseMinimum;
    public double mediumPulseMinimum;
    public double longPulseMinimum;
    public double longPulseMaximum;

    public GeneralPulseExtraction(PulseExtractiorParameters parameters) {
        this.parameters = parameters;

        consumers = new LinkedList<>();
        interval = new Transition();
        currentTimeIndex = 0;
        leaderBuffer = new double[parameters.LEADER_BUFFER_LENGTH];
        leaderBufferPointer = 0;
        intervalShiftMultiplier = 1.0;

        adjustPulseTimingsToMatchSettings();
    }

    public void adjustPulseTimingsToMatchSettings() {
        if (parameters.bottomHalfOfPulseExtractionOnly) {
            parameters.shortPulseLength /= 2.0;
            parameters.mediumPulseLength /= 2.0;
            parameters.longPulseLength /= 2.0;
        }

        shortPulseMinimum = parameters.shortPulseLength / 2.0;
        mediumPulseMinimum = parameters.shortPulseLength + (parameters.mediumPulseLength - parameters.shortPulseLength) / 2.0;
        longPulseMinimum = parameters.longPulseLength - (parameters.longPulseLength - parameters.mediumPulseLength) / 2.0;
        longPulseMaximum = parameters.longPulseLength + parameters.longPulseLength / 2.0;
    }

    public void processInterval() {
        if (settingsAllowThisIntervalToBeRecorded())
            secondsSinceLastHighToLowTransition += interval.secondsSinceLastTransition;

        if (!interval.transitionedToHigh)
            return;

        if (parameters.useLeaderBufferToAdjustPulseFrequency)
            registerWithLeaderBuffer();

        currentTimeIndex += secondsSinceLastHighToLowTransition / NANOSECOND;

        char pulseType = getPulseType();
        pushPulseToConsumers(pulseType);
        secondsSinceLastHighToLowTransition = 0.0;
    }

    private boolean settingsAllowThisIntervalToBeRecorded() {
        if (!parameters.bottomHalfOfPulseExtractionOnly)
            return true;

        if (interval.transitionedToHigh)
            return true;

        return false;
    }

    private void registerWithLeaderBuffer() {
        leaderBuffer[leaderBufferPointer] = secondsSinceLastHighToLowTransition;
        leaderBufferPointer = (leaderBufferPointer + 1 ) % leaderBuffer.length;
        adjustFreqncyIfValidLeaderBuffer();
    }

    private void adjustFreqncyIfValidLeaderBuffer() {
        double lastValue = leaderBuffer[0];
        double sum = lastValue;
        for (int i = 1; i < leaderBuffer.length; i++) {
            double differenceFromLastPulseLength = Math.abs(leaderBuffer[i] - lastValue);
            lastValue = leaderBuffer[i];
            sum += lastValue;
            if (differenceFromLastPulseLength / lastValue * 100.0 > parameters.percentageDifferenceNotToCountAsSamePulseLength) {
                return;
            }
        }

        double average = sum / (double)leaderBuffer.length;
        if ((average < parameters.shortPulseLength / parameters.toleranceForPulseCountingAsLeader
                || average > parameters.shortPulseLength * parameters.toleranceForPulseCountingAsLeader)) {
            return;
        }

        intervalShiftMultiplier =  parameters.shortPulseLength / average;
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
        if (transition.isEndOfStream())
            pushPulseToConsumers(PulseStreamConsumer.END_OF_STREAM);
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, currentTimeIndex);
    }
}
