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
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioFileOutput implements SampleStreamConsumer {
    private File out;
    private ByteArrayOutputStream outputStream;
    TapeExtractionLogging logging;

    public AudioFileOutput(String filename, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        out = new File(filename);
        outputStream = new ByteArrayOutputStream();
    }

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        if (sample.isEndOfStream())
            sendStreamToFile();
        else
            addSampleToStream(sample);
    }

    private void addSampleToStream(Sample sample) {
        int sampleValue = (int)(sample.normalizedValue * 32768.0);
        byte lowByte, highByte;
        lowByte = (byte)sampleValue;
        highByte = (byte)(sampleValue >>> 8);

        addToStream(lowByte);
        addToStream(highByte);
    }

    private void addToStream(byte b) {
        outputStream.write(b);
    }

    private void sendStreamToFile() {
        boolean bigEndian = false;
        boolean signed = true;
        int bits = 16;
        int channels = 1;
        float sampleRate = 44100.0f;

        AudioFormat format;
        format = new AudioFormat(sampleRate, bits, channels, signed, bigEndian);
        byte [] intermediateArray = outputStream.toByteArray();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(intermediateArray);
        AudioInputStream audioInputStream;
        audioInputStream = new AudioInputStream(inputStream, format, intermediateArray.length / 2);
        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
            audioInputStream.close();
        } catch (IOException e) {
            logging.writeProgramOrEnvironmentError(0, e.toString());
        }
    }
}
