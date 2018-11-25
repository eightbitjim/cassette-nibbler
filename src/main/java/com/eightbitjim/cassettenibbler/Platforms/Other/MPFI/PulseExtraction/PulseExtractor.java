/*
 * Copyright (c) 2018. James Lean
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

package com.eightbitjim.cassettenibbler.Platforms.Other.MPFI.PulseExtraction;

import com.eightbitjim.cassettenibbler.*;

import java.util.LinkedList;
import java.util.List;

public class PulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {
    enum State {WAITING_FOR_LEAD_SYNC, RECEIVING_LEAD_SYNC, RECEIVING_HEADER, RECEIVING_MID_SYNC, RECEIVING_DATA }

    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final double MICROSECOND = 1.0 / 1000000.0;

    private static final int SHORT_PULSE_MICROSECONDS = 250;
    private static final int LONG_PULSE_MICROSECONDS = 500;

    private TapeExtractionLogging logging;
    private State state;
    private SyncDetection leadSyncDetection;
    private SyncDetection midOrTailSyncDetection;
    private List<PulseStreamConsumer> consumers;
    private double intervalShiftMultiplier;
    private int currentTransitionLengthInMicroseconds;
    private int lastInterval;
    private boolean secondTStateInWave;
    private long currentTimeIndex;

    public PulseExtractor(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        consumers = new LinkedList<>();
        leadSyncDetection = new SyncDetection(SyncDetection.Type.LEAD);
        midOrTailSyncDetection = new SyncDetection(SyncDetection.Type.MID);
        currentTimeIndex = 0;
        intervalShiftMultiplier = 1.0;
        state = State.WAITING_FOR_LEAD_SYNC;
        secondTStateInWave = true;
    }

    public void processInterval() {
        intervalShiftMultiplier = leadSyncDetection.registerWithSyncToneBufferAndReturnIntervalShift(currentTransitionLengthInMicroseconds);
        midOrTailSyncDetection.registerWithSyncToneBufferAndReturnIntervalShift(currentTransitionLengthInMicroseconds);

        adjustIntervalToMatchFrequencyShift();
        checkForLeadSync(); // Always check for lead sync as it signals the start of a new file

        secondTStateInWave = !secondTStateInWave;
        switch (state) {
            case WAITING_FOR_LEAD_SYNC:
                break;

            case RECEIVING_LEAD_SYNC:
                checkIfLeadSyncFinished();
                break;

            case RECEIVING_HEADER:
                processDataPulse();
                break;

            case RECEIVING_MID_SYNC:
                break;

            case RECEIVING_DATA:
                processDataPulse();
                break;
        }

        lastInterval = currentTransitionLengthInMicroseconds;
    }

    private void adjustIntervalToMatchFrequencyShift() {
        int intervalAfterShift = (int)(intervalShiftMultiplier * (double) currentTransitionLengthInMicroseconds);
        currentTransitionLengthInMicroseconds = intervalAfterShift;
    }

    private void checkForLeadSync() {
        if (leadSyncDetection.isValid() && state != State.RECEIVING_LEAD_SYNC) {
            state = State.RECEIVING_LEAD_SYNC;
            logging.writeFileParsingInformation("LEAD SYNC DETECTED");
        }
    }

    private void checkIfLeadSyncFinished() {
        if (!leadSyncDetection.isValid()) {
            state = State.RECEIVING_HEADER;
            logging.writeFileParsingInformation("LEAD SYNC ENDED");
            processDataPulse();
        }
    }

    private void processDataPulse() {
        if (leadSyncDetection.isValid()) {
            state = State.RECEIVING_LEAD_SYNC;
            return;
        }

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
        int lowerTolerance = 400;
        int higherTolerance = 800;
        int midwayZeroToOne = SHORT_PULSE_MICROSECONDS + LONG_PULSE_MICROSECONDS;
        if (total < SHORT_PULSE_MICROSECONDS * 2 - lowerTolerance)
            return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        else
        if (total < midwayZeroToOne)
            return PulseStreamConsumer.SHORT_PULSE;
        else
        if (total < LONG_PULSE_MICROSECONDS * 2 + higherTolerance)
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
            currentTransitionLengthInMicroseconds = (int)(transition.secondsSinceLastTransition / MICROSECOND);
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
