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
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.util.LinkedList;
import java.util.List;

public class HighPass implements SampleStreamConsumer, SampleStreamProvider {

    private double cutoffFrequencyInHertz;
    private List<SampleStreamConsumer> consumers;
    private Sample modifiedSample;
    private double currentTimeIndex;
    private double expectedSampleTimeInSeconds;
    private boolean lastSampleTimeIsValid;
    private double lastSampleTimeInSeconds;

    private double RC;
    private double alpha;
    private double y1;
    private double x1;

    private TapeExtractionLogging logging;

    public HighPass(double cutoffFrequencyInHertz, String channelName) {
        consumers = new LinkedList<>();
        this.cutoffFrequencyInHertz = cutoffFrequencyInHertz;
        logging = TapeExtractionLogging.getInstance(channelName);
        reset();
    }

    private void reset() {
        modifiedSample = new Sample();
        x1 = 0.0;
        y1 = 0.0;
        RC = 1.0; // Dummy value
        alpha = 1.0; // Dummy value
        expectedSampleTimeInSeconds = 0.0; // Start with nonsense value
        lastSampleTimeIsValid = false;
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        if (sample.isEndOfStream()) {
            modifiedSample.normalizedValue = Sample.END_OF_STREAM;
            distributeSampleToConsumers();
            reset();
            return;
        }

        this.currentTimeIndex = currentTimeIndex;
        if (lastSampleTimeIsValid) {
            double timeElapsed = currentTimeIndex - lastSampleTimeInSeconds;
            double differenceFromSampleRate = Math.abs(timeElapsed - expectedSampleTimeInSeconds);
            if (differenceFromSampleRate > expectedSampleTimeInSeconds / 20.0) {
                expectedSampleTimeInSeconds = timeElapsed;
                registerSamplePeriod(timeElapsed);
                logging.writeFileParsingInformation("High pass filter: Sample rate detected as " + (int)(1.0 / timeElapsed) + "hz");
            }

            modifiedSample.setValue(filterSample(sample.normalizedValue));
        } else {
            modifiedSample.setValue(sample.normalizedValue);
            x1 = sample.normalizedValue;
        }

        y1 = modifiedSample.normalizedValue;
        distributeSampleToConsumers();

        lastSampleTimeInSeconds = currentTimeIndex;
        lastSampleTimeIsValid = true;
    }

    private void registerSamplePeriod(double period) {
        RC  = 1.0 / (2.0 * Math.PI * cutoffFrequencyInHertz);
        alpha = RC / (RC + period);
        logging.writeFileParsingInformation("High pass filter: RC = " + RC + " , alpha = " + alpha);
    }

    private double filterSample(double sampleValue) {
        double valueToReturn;
        valueToReturn = alpha * y1 + alpha * (sampleValue - x1);
        y1 = valueToReturn;
        x1 = sampleValue;

        return valueToReturn;
    }

    private void distributeSampleToConsumers() {
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
