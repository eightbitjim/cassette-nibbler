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

package com.eightbitjim.cassettenibbler.Platforms.General.Demodulation;

import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.Transition;
import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;
import com.eightbitjim.cassettenibbler.IntervalStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class ZeroCrossingIntervalExtractor implements IntervalStreamProvider, SampleStreamConsumer {

    private boolean lastQuantizedState = false;
    private boolean lastQuantizedStateValid = false;

    public static final int END_OF_STREAM = -1;

    private double currentTimeIndex;
    private double timeIndexOfLastTransition;

    private Sample sample;
    private List<IntervalStreamConsumer> consumers;
    private double threasholdLine;
    private Transition transition;

    public ZeroCrossingIntervalExtractor() {
        threasholdLine = 0.0;
        currentTimeIndex = 0;
        timeIndexOfLastTransition = currentTimeIndex;
        consumers = new LinkedList<>();
        transition = new Transition();
        sample = new Sample();
    }

    public void setThreasholdLine(double value) {
        threasholdLine = value;
    }

    private boolean sampleValueIsHigh() {
        return sample.normalizedValue > threasholdLine;
    }

    private void processSample() {
        boolean quantizedState = sampleValueIsHigh();
        if (lastQuantizedStateValid &&
                (lastQuantizedState != quantizedState)) {
            lastQuantizedState = quantizedState;
            foundTransition();
            return;
        }

        lastQuantizedStateValid = true;
        lastQuantizedState = quantizedState;
    }

    private void foundTransition() {
        transition.secondsSinceLastTransition = currentTimeIndex - timeIndexOfLastTransition;
        transition.transitionedToHigh = sampleValueIsHigh();
        timeIndexOfLastTransition = currentTimeIndex;
        pushIntervalToConsumers(transition);
    }

    private void pushEndOfStream() {
        pushPartiallyCompleteInterval();

        Transition transition = new Transition();
        transition.secondsSinceLastTransition = Transition.END_OF_STREAM;
        pushIntervalToConsumers(transition);
    }

    private void pushPartiallyCompleteInterval() {
        foundTransition();
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        if (sample.isEndOfStream())
            pushEndOfStream();
        else {
            this.currentTimeIndex = currentTimeIndex;
            this.sample = sample;
            processSample();
        }
    }

    private void pushIntervalToConsumers(Transition transition) {
        for (IntervalStreamConsumer consumer : consumers)
            consumer.pushInterval(transition, currentTimeIndex);
    }

    @Override
    public void registerIntervalStreamConsumer(IntervalStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterIntervalStreamConsumer(IntervalStreamConsumer consumer) {
        consumers.remove(consumer);
    }
}
