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

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

public class AudioFile {
    private int bytesPerFrame;
    private long totalFramesReceived;
    private double currentTimeIndex;
    private double sampleLengthInSeconds;
    private double currentNormalisedSampleValue;
    private boolean endOfFile;
    private byte [] audioBytes;
    private InputStream inputStream;

    private AudioInputStream audioInputStream;
    private TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public AudioFile(String filename) throws IOException, UnsupportedAudioFileException {
        totalFramesReceived = 0;
        openFileFromFilename(filename);
        initialise();
    }

    public AudioFile(InputStream inputStream) throws IOException, UnsupportedAudioFileException {
        totalFramesReceived = 0;
        this.inputStream = inputStream;
        initialise();
    }

    private void openFileFromFilename(String filename) throws FileNotFoundException {
        inputStream = new FileInputStream(filename);
    }

    private void initialise() throws IOException, UnsupportedAudioFileException {
        ensureMarkResetIsSupported();
        currentTimeIndex = 0.0;
        audioInputStream = AudioSystem.getAudioInputStream(inputStream);
        convertAudioInputStreamToPcmSigned();

        sampleLengthInSeconds = (double)(1.0f / audioInputStream.getFormat().getSampleRate());
        bytesPerFrame = audioInputStream.getFormat().getFrameSize();
        if (bytesPerFrame == AudioSystem.NOT_SPECIFIED)
            bytesPerFrame = 1;

        audioBytes = new byte[bytesPerFrame];
        recordFileDataInLoggingOutput();
    }

    private void ensureMarkResetIsSupported() {
        if (!inputStream.markSupported())
            inputStream = new BufferedInputStream(inputStream);
    }

    private void convertAudioInputStreamToPcmSigned() {
        AudioFormat format = audioInputStream.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            audioInputStream = AudioSystem.getAudioInputStream(
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            format.getSampleRate(),
                            16,
                            1,
                            2,
                            format.getSampleRate(),
                            false), audioInputStream);
        }
    }

    private void recordFileDataInLoggingOutput() {
        logging.writeFileParsingInformation("New audio input stream");
        logging.writeFileParsingInformation("Sample rate " + (1.0 / sampleLengthInSeconds) + "hz");
    }

    public double getNextSampleNormalisedValue() {
        try {
            int numBytesRead = 0;
            int numFramesRead = 0;

            numBytesRead = audioInputStream.read(audioBytes);
            numFramesRead = numBytesRead / bytesPerFrame;
            if (numFramesRead < 1)
                recordEndOfFile();
            else
                processSample();
        } catch (Exception ex) {
            logging.writeFileParsingInformation("Exception while reading audio file: " + ex.toString());
            recordEndOfFile();
        }

        return currentNormalisedSampleValue;
    }

    private void recordEndOfFile() {
        endOfFile = true;
        currentNormalisedSampleValue = 0.0;
        logging.writeFileParsingInformation("End of audio input stream reached");
    }

    private void processSample() {
        int sampleValue = convertBytesToSignedInteger(audioBytes[0], audioBytes[1]);
        double normalisedSampleValue = (double)sampleValue / (double)0x8000;
        totalFramesReceived++;
        currentNormalisedSampleValue = normalisedSampleValue;
        currentTimeIndex = (double)totalFramesReceived * sampleLengthInSeconds;
    }

    private int convertBytesToSignedInteger(byte low, byte high) {
        int unsignedValue = Byte.toUnsignedInt(low) + Byte.toUnsignedInt(high) * 0x100;
        int signedValue;
        if ((unsignedValue & 0x8000) == 0)
            signedValue = unsignedValue;
        else
            signedValue = unsignedValue - 0x10000;

        return signedValue;
    }

    public double getCurrentTimeIndex() {
        return currentTimeIndex;
    }

    public boolean isEndOfFile() {
        return endOfFile;
    }
}
