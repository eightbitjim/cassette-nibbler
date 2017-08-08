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

import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamProvider;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.util.LinkedList;
import java.util.List;

public class LowPass implements SampleStreamConsumer, SampleStreamProvider {
    private double sampleTimeInSeconds;
    private TapeExtractionLogging logging;
    private List<SampleStreamConsumer> consumers;

    private double RC;
    private double alpha;
    private double y1;
    private double x1;

    private boolean lastSampleTimeIsValid;
    private double lastSampleTimeInSeconds;
    private double cutoffInHertz;
    Sample modifiedSample;

    public LowPass(double cutoffInHertz, String channelName) {
        consumers = new LinkedList<>();
        this.cutoffInHertz = cutoffInHertz;
        logging = TapeExtractionLogging.getInstance(channelName);
        reset();
    }

    private void reset() {
        modifiedSample = new Sample();


        lastSampleTimeIsValid = false;
        sampleTimeInSeconds = 0.0; // Start with nonsense value
    }

    private void registerSamplePeriod(double period) {
        RC  = 1.0 / (2.0 * Math.PI * cutoffInHertz);
        alpha = period / (RC + period);
        logging.writeFileParsingInformation("Low pass filter: RC = " + RC + " , alpha = " + alpha);
    }

    double filterSample(double sample)
    {
        double result = y1 + alpha * (sample - y1);
        y1 = result;
        return result;
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        if (sample.isEndOfStream()) {
            distributeToConsumers(sample, currentTimeIndex);
            reset();
            return;
        }

        if (lastSampleTimeIsValid) {
            double timeElapsed = currentTimeIndex - lastSampleTimeInSeconds;
            double differenceFromSampleRate = Math.abs(timeElapsed - sampleTimeInSeconds);
            if (differenceFromSampleRate > sampleTimeInSeconds / 20.0) {
                sampleTimeInSeconds = timeElapsed;
                registerSamplePeriod(sampleTimeInSeconds);

                int sampleRate = (int)(1.0 / timeElapsed);
                logging.writeFileParsingInformation("Low pass filter: Sample rate detected as " + sampleRate + "hz");
            }

            modifiedSample.normalizedValue = filterSample(sample.normalizedValue);
        } else {
            modifiedSample.normalizedValue = sample.normalizedValue;
        }

        distributeToConsumers(modifiedSample, currentTimeIndex);

        lastSampleTimeInSeconds = currentTimeIndex;
        lastSampleTimeIsValid = true;
    }

    private void distributeToConsumers(Sample sample, double timeIndex) {
        for (SampleStreamConsumer consumer : consumers)
            consumer.push(sample, timeIndex);
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
