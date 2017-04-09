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

import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.SampleStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class Amplify implements SampleStreamConsumer, SampleStreamProvider {

    private double multiplier = 1.0;
    private List<SampleStreamConsumer> consumers;
    private Sample modifiedSample;

    public Amplify(double multiplier) {
        setMultiplier(multiplier);
        modifiedSample = new Sample();
        consumers = new LinkedList<>();
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        if (!sample.isEndOfStream())
            modifiedSample.normalizedValue = sample.normalizedValue * multiplier;
        else
            modifiedSample.normalizedValue = Sample.END_OF_STREAM;

        for (SampleStreamConsumer consumer : consumers)
            consumer.push(modifiedSample, currentTimeIndex);
    }

    @Override
    public void registerSampleStreamConsumer(SampleStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterSampleStreamConsumer(SampleStreamConsumer consumer) {
        consumers.remove(consumer);
    }
}
