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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.PulseExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.FrequencyAnalysis.Analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class AutomaticPulseExtractor implements IntervalStreamConsumer, PulseStreamProvider {

    private PulseExtractiorParameters parameters;

    public double shortPulseMinimum;
    public double mediumPulseMinimum;
    public double longPulseMinimum;
    public double longPulseMaximum;

    private Analysis intervalAnalaysis;
    private Queue<Transition> intervalBuffer;
    private int queueLength = 200000;
    private List<PulseStreamConsumer> consumers;

    private double NANOSECOND = 1.0 / 1000000000.0;
    private long intervalInNanoseconds = 25000;
    private long maxNanoseconds =        2000000;
    private long numberOfIntervals = maxNanoseconds / intervalInNanoseconds;
    private double secondsSinceLastHighToLowTransition;
    private double intervalShiftMultiplier = 1.0;
    private boolean pulsesLocked = false;
    private int numberOfPulseTypes;
    private long interval;

    private TapeExtractionLogging logging;

    public AutomaticPulseExtractor(int numberOfPulseTypes, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.numberOfPulseTypes = numberOfPulseTypes;
        if (this.numberOfPulseTypes < 2)
            this.numberOfPulseTypes = 2;

        if (this.numberOfPulseTypes > 3)
            this.numberOfPulseTypes = 3;

        intervalBuffer = new ArrayBlockingQueue<Transition>(queueLength);
        consumers = new LinkedList<>();
        intervalAnalaysis = new Analysis("interval analysys", (int)numberOfIntervals);
        parameters = new PulseExtractiorParameters();
        interval = 0;
        pulsesLocked = false;
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
        transition = makeNewTransitionForQueue(transition);
        addTransitionToAnalysis(transition);

        if (!pulsesLocked) {
            if (intervalBuffer.offer(transition) == false ||
                    transition.isEndOfStream()) {
                processQueueContents();
            }
        } else {
            processInterval(transition);
        }
    }

    private Transition makeNewTransitionForQueue(Transition original) {
        Transition copy = new Transition();
        copy.secondsSinceLastTransition = original.secondsSinceLastTransition;
        copy.transitionedToHigh = original.transitionedToHigh;
        return copy;
    }

    private void processQueueContents() {
        workOutPulseParameters();
        intervalAnalaysis.reset();

        secondsSinceLastHighToLowTransition = 0.0;
        processQueuedPulses();
    }

    private void addTransitionToAnalysis(Transition transition) {
        interval += (long)(transition.secondsSinceLastTransition / NANOSECOND);
        if (transition.transitionedToHigh) {
            addIntervalToAnalysis(interval);
            interval = 0;
        }
    }

    private void processQueuedPulses() {
        while (!intervalBuffer.isEmpty()) {
            Transition transition = intervalBuffer.poll();
            processInterval(transition);

            if (!pulsesLocked) {
                addCurrentQueueToAnalysis();
                break;
            }
        }
    }

    private void addCurrentQueueToAnalysis() {
        for (Transition transition : intervalBuffer) {
            addTransitionToAnalysis(transition);
        }
    }

    private void processInterval(Transition transition) {
        secondsSinceLastHighToLowTransition += transition.secondsSinceLastTransition;
        if (transition.transitionedToHigh) {
            pushPulseForIntervalLength();
            secondsSinceLastHighToLowTransition = 0;
        }
    }

    private void pushPulseForIntervalLength() {
        char pulseType = getPulseType();
        pushPulseToConsumers(pulseType);
        if (pulseType == PulseStreamConsumer.INVALID_PULSE_TOO_LONG)
            lostPulseLock();
    }

    private void lostPulseLock() {
        pulsesLocked = false;
        intervalAnalaysis.reset();
        logging.writeFileParsingInformation("Lost pulse lock");
    }

    private void workOutPulseParameters() {
        intervalAnalaysis.groupNearbyValues();
        List <Integer> topValueList = intervalAnalaysis.getTopNValueIndices(numberOfPulseTypes);

        int count = 0;
        for (Integer length : topValueList) {
            switch (count) {
                case 2:
                    parameters.longPulseLength = workOutPulseLengthInSeconds(length * intervalInNanoseconds);
                    break;
                case 1:
                    parameters.mediumPulseLength = workOutPulseLengthInSeconds(length * intervalInNanoseconds);
                    break;
                case 0:
                    parameters.shortPulseLength = workOutPulseLengthInSeconds(length  * intervalInNanoseconds);
                    break;
            }
            count++;
        }

        if (topValueList.size() < 3)
            parameters.longPulseLength = parameters.mediumPulseLength * 3.0;

        adjustPulseTimingsToMatchSettings();
        pushPulseTimingValuesToConsumers();
        pulsesLocked = true;
        logging.writeFileParsingInformation("Locked onto pulse lengths:" + parameters.longPulseLength + ", "
                + parameters.mediumPulseLength + ", " + parameters.shortPulseLength);
    }

    private double workOutPulseLengthInSeconds(long intervalInNanoseconds) {
        double result = (double)intervalInNanoseconds * NANOSECOND;
        return result;
    }

    private void addIntervalToAnalysis(long interval) {
        interval /= intervalInNanoseconds;
        intervalAnalaysis.addOccurrence((int)interval);
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

    public char getPulseType() {
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < shortPulseMinimum) return PulseStreamConsumer.INVALID_PULSE_TOO_SHORT;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < mediumPulseMinimum) return PulseStreamConsumer.SHORT_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMinimum) return PulseStreamConsumer.MEDIUM_PULSE;
        if (secondsSinceLastHighToLowTransition * intervalShiftMultiplier < longPulseMaximum) return PulseStreamConsumer.LONG_PULSE;
        return PulseStreamConsumer.INVALID_PULSE_TOO_LONG;
    }

    private void pushPulseToConsumers(char pulseType) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulseType, 0); // TODO time index
    }

    private void pushPulseTimingValuesToConsumers() {
        for (PulseStreamConsumer consumer : consumers)
            consumer.notifyChangeOfPulseLengths(parameters);
    }
}
