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

package com.eightbitjim.cassettenibbler.DataSink;

import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamConsumer;

public class VolumeLevel implements SampleStreamConsumer {
    private static final double DEFAULT_SAMPLE_WINDOW_SIZE_IN_SECONDS = 0.2;

    private double sampleWindowInSeconds;
    private double currentWindowStartedAtTimeIndex;
    private double maximumAmplitudeInWindow;
    private double maximumAmplitudeInLastWindow;
    private double currentTimeIndex;
    private Sample currentSample;

    public VolumeLevel() {
        sampleWindowInSeconds = DEFAULT_SAMPLE_WINDOW_SIZE_IN_SECONDS;
        maximumAmplitudeInLastWindow = 0.0;
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        this.currentTimeIndex = currentTimeIndex;
        this.currentSample = sample;

        if (newWindowIsNeeded())
            resetWindow();

        registerSampleWithWindow();
    }

    private boolean newWindowIsNeeded() {
         return currentTimeIndex > currentWindowStartedAtTimeIndex + sampleWindowInSeconds;
    }

    private void resetWindow() {
        currentWindowStartedAtTimeIndex = currentTimeIndex;
        maximumAmplitudeInLastWindow = maximumAmplitudeInWindow;
        maximumAmplitudeInWindow = 0;
    }

    private void registerSampleWithWindow() {
        double absoluteAmplitude = Math.abs(currentSample.normalizedValue);
        if (absoluteAmplitude > maximumAmplitudeInWindow)
            maximumAmplitudeInWindow = absoluteAmplitude;
    }

    public double getMaximumVolumeInWindow() {
        return maximumAmplitudeInLastWindow;
    }
}
