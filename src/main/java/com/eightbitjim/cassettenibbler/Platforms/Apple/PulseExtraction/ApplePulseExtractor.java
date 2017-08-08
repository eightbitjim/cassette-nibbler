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

package com.eightbitjim.cassettenibbler.Platforms.Apple.PulseExtraction;

import com.eightbitjim.cassettenibbler.*;

import java.util.LinkedList;
import java.util.List;

public class ApplePulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {
    enum State { WAITING_FOR_PILOT_TONE, RECEIVING_PILOT_TONE, RECEIVING_DATA }

    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final double MICROSECOND = 1.0 / 1000000.0;

    private static final int SYNC_PULSE_MICROSECONDS_1 = 200;
    private static final int SYNC_PULSE_MICROSECONDS_2 = 250;
    private static final int ZERO_PULSE_MICROSECONDS = 250;
    private static final int ONE_PULSE_MICROSECONDS = 500;

    private TapeExtractionLogging logging;
    private State state;
    private ApplePilotToneDetection pilotToneDetection;
    private List<PulseStreamConsumer> consumers;
    private double intervalShiftMultiplier;
    private int currentTransitionLengthInTstates;
    private int lastInterval;
    private boolean secondTStateInWave;
    private long currentTimeIndex;

    public ApplePulseExtractor(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        consumers = new LinkedList<>();
        pilotToneDetection = new ApplePilotToneDetection();
        currentTimeIndex = 0;
        intervalShiftMultiplier = 1.0;
        state = State.WAITING_FOR_PILOT_TONE;
        secondTStateInWave = true;
    }

    public void processInterval() {
        intervalShiftMultiplier = pilotToneDetection.registerWithPilotToneBufferAndReturnIntervalShift(currentTransitionLengthInTstates);
        adjustIntervalToMatchFrequencyShift();
        secondTStateInWave = !secondTStateInWave;
        switch (state) {
            case WAITING_FOR_PILOT_TONE:
                checkForPilotTone();
                break;

            case RECEIVING_PILOT_TONE:
                checkForEndOfPilotTone();
                break;

            case RECEIVING_DATA:
                processDataPulse();
                break;
        }

        lastInterval = currentTransitionLengthInTstates;
    }

    private void adjustIntervalToMatchFrequencyShift() {
        int intervalAfterShift = (int)(intervalShiftMultiplier * (double)currentTransitionLengthInTstates);
        currentTransitionLengthInTstates = intervalAfterShift;
    }

    private boolean countsAsSyncPulses(int pulse1, int pulse2) {
        int toleranceInTStates = 200;
        int totalLength = pulse1 + pulse2;
        int targetLength = SYNC_PULSE_MICROSECONDS_1 + SYNC_PULSE_MICROSECONDS_2;

        if (Math.abs(totalLength - targetLength) < toleranceInTStates)
            return true;
        else
            return false;
    }

    private void checkForPilotTone() {
        if (pilotToneDetection.pilotToneIsValid()) {
            state = State.RECEIVING_PILOT_TONE;
        }
    }

    private void checkForEndOfPilotTone() {
        if (countsAsSyncPulses(lastInterval, currentTransitionLengthInTstates)) {
            generatePulse(PulseStreamConsumer.SHORT_PULSE);
            state = State.RECEIVING_DATA;
            secondTStateInWave = true;
            return;
        }

        if (!pilotToneDetection.pilotToneIsValid()) {
                state = State.WAITING_FOR_PILOT_TONE;
                generatePulse(PulseStreamConsumer.SILENCE);
                return;
        }

        if (secondTStateInWave)
            generatePulse(PulseStreamConsumer.LONG_PULSE);
    }

    private void processDataPulse() {
        if (pilotToneDetection.pilotToneIsValid()) {
            state = State.RECEIVING_PILOT_TONE;
            return;
        }

        if (!secondTStateInWave)
            return;

        char pulseType = identifyPulsePairType(lastInterval, currentTransitionLengthInTstates);
        if (pulseType == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT)
            keepCurrentPulseAndAddToNext();
        else
            generatePulse(pulseType);
    }

    private void keepCurrentPulseAndAddToNext() {
        secondTStateInWave = false;
        currentTransitionLengthInTstates += lastInterval;
    }

    private void generatePulse(char pulseType) {
        pushPulseToConsumers(pulseType);
    }

    private char identifyPulsePairType(int pulse1, int pulse2) {
        int total = pulse1 + pulse2;
        int lowerTolerance = 400;
        int higherTolerance = 800;
        int midwayZeroToOne = ZERO_PULSE_MICROSECONDS + ONE_PULSE_MICROSECONDS;
        if (total < ZERO_PULSE_MICROSECONDS * 2 - lowerTolerance)
            return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        else
        if (total < midwayZeroToOne)
            return PulseStreamConsumer.SHORT_PULSE;
        else
        if (total < ONE_PULSE_MICROSECONDS * 2 + higherTolerance)
            return PulseStreamConsumer.MEDIUM_PULSE;
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
            currentTransitionLengthInTstates = (int)(transition.secondsSinceLastTransition / MICROSECOND);
            this.currentTimeIndex = (long) (currentTimeIndex / NANOSECOND);
            processInterval();
        }
    }

    private void pushEndOfStream() {
        pushPulseToConsumers(PulseStreamConsumer.END_OF_STREAM);
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, currentTimeIndex);
    }
}
