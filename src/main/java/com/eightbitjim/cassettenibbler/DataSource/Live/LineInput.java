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

package com.eightbitjim.cassettenibbler.DataSource.Live;

import com.eightbitjim.cassettenibbler.DataSource.DataSourceNotAvailableException;
import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.SampleStreamProvider;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import javax.sound.sampled.*;
import java.util.LinkedList;

public class LineInput implements SampleStreamProvider {
    private AudioFormat lineInputFormat;
    private LinkedList<SampleStreamConsumer> consumers;
    private long statTime;
    private boolean active;
    private byte [] audioInputData;
    private TargetDataLine line;
    private Sample sample;

    private TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public LineInput() throws DataSourceNotAvailableException {
        consumers = new LinkedList<>();
        sample = new Sample();
        lineInputFormat = new AudioFormat(44100, 8, 1, false, false);
        getTargetDataLine();
        statTime = System.currentTimeMillis();
    }

    private double getCurrentTimeIndex() {
        long millisecondsElapsed = System.currentTimeMillis() - statTime;
        double timeElapsedInSeconds = (double)millisecondsElapsed / 1000.0;
        return timeElapsedInSeconds;
    }

    private void getTargetDataLine() throws DataSourceNotAvailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, lineInputFormat);
        if (!AudioSystem.isLineSupported(info))
            throw new DataSourceNotAvailableException("Audio input line is not supported");

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(lineInputFormat);
        } catch (LineUnavailableException ex) {
            throw new DataSourceNotAvailableException("Audio input line is not available");
        }
    }

    private void pushSample(Sample sample) {
        for (SampleStreamConsumer consumer : consumers)
            consumer.push(sample, getCurrentTimeIndex());
    }

    public void startAudioCapture() {
        audioInputData = new byte[line.getBufferSize() / 5];
        line.start();
        active = true;
    }

    public void stopAudioCapture() {
        line.stop();
        active = false;
    }

    public void processReceivedSamples() {
        if (!active) {
            logging.writeProgramOrEnvironmentError(0, "Attempted to read audio data when audio capture is not active.");
            return;
        }

        while (line.available() > 0) {
            int numBytesRead = line.read(audioInputData, 0, audioInputData.length);
            processReceivedData(numBytesRead);
        }
    }

    private void processReceivedData(int bytesInBuffer) {
        for (byte value : audioInputData) {
            processAudioByte(value);
        }
    }

    private void processAudioByte(byte value) {
        sample.normalizedValue = normalizedValueFromByte(value);
        pushSample(sample);
    }

    private double normalizedValueFromByte(byte value) {
        double normalizedValue = (double)value;
        value /= 128.0;
        return value;
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
