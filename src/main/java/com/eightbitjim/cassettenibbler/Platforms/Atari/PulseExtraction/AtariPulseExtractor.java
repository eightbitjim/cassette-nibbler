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

package com.eightbitjim.cassettenibbler.Platforms.Atari.PulseExtraction;

import com.eightbitjim.cassettenibbler.*;

import java.util.LinkedList;
import java.util.List;

public class AtariPulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {
    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final double MICROSECOND = 1.0 / 1000000.0;

    private static final int ZERO_PULSE_MICROSECONDS = 125;
    private static final int ONE_PULSE_MICROSECONDS = 94;

    private static final double PULSES_IN_A_MARK = 8.6666;
    private static final double PULSES_IN_A_SPACE = 6.6666;

    private static final char SPACE_PULSE = PulseStreamConsumer.MEDIUM_PULSE;
    private static final char MARK_PULSE = PulseStreamConsumer.SHORT_PULSE;

    private TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private List<PulseStreamConsumer> consumers;
    private int currentTransitionLengthInMicroseconds;
    private int lastInterval;
    private boolean secondTStateInWave;
    private long currentTimeIndex;
    private long lastTimeIndex;
    private char lastPulse;
    private int pulseCount;

    public AtariPulseExtractor() {
        consumers = new LinkedList<>();
        currentTimeIndex = 0;
        secondTStateInWave = true;
        lastTimeIndex = 0;
    }

    public void processInterval() {
        secondTStateInWave = !secondTStateInWave;
        processDataPulse();

        lastInterval = currentTransitionLengthInMicroseconds;
    }

    private void processDataPulse() {
        if (!secondTStateInWave)
            return;

        char pulseType = identifyPulsePairType(lastInterval, currentTransitionLengthInMicroseconds);
        if (pulseType == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT)
            keepCurrentPulseAndAddToNext();
        else
            generatePulse(pulseType);
    }

    private void keepCurrentPulseAndAddToNext() {
        secondTStateInWave = false;
        currentTransitionLengthInMicroseconds += lastInterval;
    }

    private void generatePulse(char pulseType) {
        pushPulseToConsumers(pulseType);
    }

    private char identifyPulsePairType(int pulse1, int pulse2) {
        int total = pulse1 + pulse2;
        int lowerTolerance = 40;
        int higherTolerance = 80;
        int midwayZeroToOne = ZERO_PULSE_MICROSECONDS + ONE_PULSE_MICROSECONDS;
        if (total < ONE_PULSE_MICROSECONDS * 2 - lowerTolerance)
            return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        else
        if (total < midwayZeroToOne)
            return SPACE_PULSE;
        else
        if (total < ZERO_PULSE_MICROSECONDS * 2 + higherTolerance)
            return MARK_PULSE;
        else
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
            currentTransitionLengthInMicroseconds = (int)(transition.secondsSinceLastTransition / MICROSECOND);
            this.currentTimeIndex = (long) (currentTimeIndex / NANOSECOND);
            processInterval();
        }
    }

    private void pushEndOfStream() {
        pushPulseToConsumers(PulseStreamConsumer.END_OF_STREAM);
    }

    private void pushPulseToConsumers(char pulseType) {
        pulseCount++;
        if (pulseType != lastPulse) {
            double bitCount = pulseCount / (pulseType == MARK_PULSE ? PULSES_IN_A_MARK : PULSES_IN_A_SPACE);
            int bitCountInteger = (int)Math.floor(bitCount + 0.5);
            pulseCount = 0;

            for (PulseStreamConsumer consumer : consumers) {
                for (int i = 0; i < bitCountInteger; i++)
                    consumer.pushPulse(lastPulse, currentTimeIndex);
            }
        }

        lastPulse = pulseType;
    }
}
