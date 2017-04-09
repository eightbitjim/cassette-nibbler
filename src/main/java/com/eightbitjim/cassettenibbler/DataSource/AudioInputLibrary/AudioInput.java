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

package com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary;

import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.SampleStreamProvider;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class AudioInput implements SampleStreamProvider {
    private List<SampleStreamConsumer> consumers = new LinkedList<>();
    private Sample sampleToPush;
    private AudioFile file;

    public AudioInput(String filename) throws IOException, UnsupportedAudioFileException {
        sampleToPush = new Sample();
        file = new AudioFile(filename);
    }

    public AudioInput(InputStream inputStream) throws IOException, UnsupportedAudioFileException {
        sampleToPush = new Sample();
        file = new AudioFile(inputStream);
    }

    public boolean processNextSampleAndReturnTrueIfFinished() {
        double normalizedValue = file.getNextSampleNormalisedValue();
        if (file.isEndOfFile()) {
            pushEndOfStream();
            return true;
        }

        pushSampleToConsumers(normalizedValue, file.getCurrentTimeIndex());
        return false;
    }

    private void pushEndOfStream() {
        pushSampleToConsumers(Sample.END_OF_STREAM, file.getCurrentTimeIndex());
    }

    public void processFile() {
        boolean finishedProcessing = false;
        while (!finishedProcessing) {
            finishedProcessing = processNextSampleAndReturnTrueIfFinished();
        }
    }

    private void pushSampleToConsumers(double normalizedValue, double timestampInSeconds) {
        sampleToPush.normalizedValue = normalizedValue;
        for (SampleStreamConsumer consumer : consumers)
            consumer.push(sampleToPush, timestampInSeconds);
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
