/*
 * Copyright (C) 2017 James Lean.
 *
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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZXSpectrum;

import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.PETSCII;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.PulseExtraction.SpectrumPulseExtractor;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.TapeFile;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SpectrumFileTest implements FileStreamConsumer {

    private SpectrumFileStateMachine fileExtractor;
    private SpectrumPulseExtractor pulseExtractor;
    private ZeroCrossingIntervalExtractor intervalExtractor;
    private LowPass lowPassFilter;
    private List<SpectrumTapeFile> results;
    private TapeExtractionOptions tapeExtractionOptions = TapeExtractionOptions.getInstance();
    private String channelName = "channel";

    private static final String PATH_TO_TEST_FILES = "src/test/testFiles/";
    private static final String ONE_FILE_FILENAME = PATH_TO_TEST_FILES + "spectrumFileOne.wav";
    private static final String FIRST_HALF_OF_DATA_ONLY = PATH_TO_TEST_FILES + "spectrumIncompleteDataBlock.wav";
    private static final String TWO_FILE_FILENAME = PATH_TO_TEST_FILES + "spectrumTwoFiles.wav";
    private static final String ORPHAN_DATA_FILENAME = PATH_TO_TEST_FILES + "spectrumDataOnly.wav";
    private static final String ONE_FILE_FLOAT_WAV = PATH_TO_TEST_FILES + "spectrumOneFile32BitFloat.wav";
    private static final String ONE_FILE_FASTER_SAMPLE_RATE = PATH_TO_TEST_FILES + "spectrumOneFileFasterSampleRate.wav";
    private static final String HEADER_ONLY = PATH_TO_TEST_FILES + "spectrumHeaderOnly.wav";

    @Before
    public void individualSetup() {
        int lowPassFilterAmountInHertz = 2400;

        tapeExtractionOptions.setLogging(TapeExtractionOptions.LoggingMode.NONE, null);
        tapeExtractionOptions.setAttemptToRecoverCorruptedFiles(true);
        tapeExtractionOptions.setAllowIncorrectFileChecksums(true);

        fileExtractor = new SpectrumFileStateMachine(channelName);
        pulseExtractor = new SpectrumPulseExtractor(channelName);
        lowPassFilter = new LowPass(lowPassFilterAmountInHertz, channelName);
        intervalExtractor = new ZeroCrossingIntervalExtractor();
        lowPassFilter.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);
        fileExtractor.registerFileStreamConsumer(this);
        results = new LinkedList<>();
    }

    @Test
    public void testOneFile() throws Throwable {
        parseFile(ONE_FILE_FILENAME, 1);
        checkFileResult(0, 93, "test.program", 503730007);
    }

    @Test
    public void testOneFileFloatData() throws Throwable {
        parseFile(ONE_FILE_FLOAT_WAV, 1);
        checkFileResult(0, 93, "test.program", 503730007);
    }

    @Test
    public void testOneFileFloatDataFasterSampleRate() throws Throwable {
        parseFile(ONE_FILE_FASTER_SAMPLE_RATE, 1);
        checkFileResult(0, 93, "test.program", 503730007);
    }

    @Test
    public void testOneFileHeaderOnly() throws Throwable {
        parseFile(HEADER_ONLY, 0);
    }

    @Test
    public void testSpectrumFirstHalfOfDataOnly() throws Throwable {
        parseFile(FIRST_HALF_OF_DATA_ONLY, 1);
        checkFileResult(0, 77, "test.incomplete.program", -1481996460);
    }

    @Test
    public void testDataFileOnly() throws Throwable {
        parseFile(ORPHAN_DATA_FILENAME, 1);
        checkFileResult(0, 94, "headlessFile1.incomplete.unknown", -1564238850);
    }

    @Test
    public void testTwoSpectrumFiles() throws Throwable {
        parseFile(TWO_FILE_FILENAME, 2);
        checkFileResult(0, 93, "test.program", 503730007);
        checkFileResult(1, 191, "bigger.program", -2137267722);
    }

    private void parseFile(String filename, int numberOfExpectedResults) throws Throwable {
        AudioInput reader = new AudioInput(filename, channelName);
        try {
            reader.registerSampleStreamConsumer(lowPassFilter);
            pushStreamTrhoughSystem(reader);
            assertTrue("Wrong number of files after extracting " + filename + ". Got " + results.size(), results.size() == numberOfExpectedResults);
        } finally {

        }
    }

    private void checkFileResult(int fileNumber, int expectedFileLength, String expectedFilename, int expectedContentHash) {
        assertTrue("File length incorrect: " + results.get(fileNumber).length(), results.get(fileNumber).length() == expectedFileLength);
        assertTrue("File name incorrect: " + results.get(fileNumber).getFilename(), results.get(fileNumber).getFilename().equals(expectedFilename));
        assertTrue("File content does not match expected hash: " + Arrays.hashCode(results.get(fileNumber).getDataBytesOfType(TapeFile.FormatType.EMULATOR)),
                Arrays.hashCode(results.get(fileNumber).getDataBytesOfType(TapeFile.FormatType.BINARY)) == expectedContentHash);
    }

    String convertPetsciiByteArrayToString(byte [] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b: data) {
            builder.append(PETSCII.printableCharacterForPetsciiCode(b));
        }

        return builder.toString();
    }

    private void pushStreamTrhoughSystem(AudioInput reader) throws Throwable {
        reader.processFile();
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        assertTrue("Returned tape file is not Spectrum format", file instanceof SpectrumTapeFile);
        results.add((SpectrumTapeFile) file);
    }
}
