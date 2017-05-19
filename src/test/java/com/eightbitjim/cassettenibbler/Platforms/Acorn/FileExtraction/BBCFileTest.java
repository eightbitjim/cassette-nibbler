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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction;

import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.PulseExtraction.AcornPulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.TapeFile;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BBCFileTest implements FileStreamConsumer {

    private BBCFileExtractor fileExtractor1200;
    private BBCFileExtractor fileExtractor300;
    private AcornPulseExtractor pulseExtractor;
    private ZeroCrossingIntervalExtractor intervalExtractor;
    private List<BBCTapeFile> results;
    private LowPass lowPass;
    private HighPass highPass;

    private static final String PATH_TO_TEST_FILES = "src/test/testFiles/";
    private static final String ONE_FILE_FILENAME = PATH_TO_TEST_FILES + "bbcOneFile1200.wav";
    private static final String TWO_FILE_FILENAME = PATH_TO_TEST_FILES + "bbcTwoFiles1200.wav";
    private static final String ONE_FILE_300_BAUD_FILENAME = PATH_TO_TEST_FILES + "bbcOneFile300.wav";
    private static final String VARIABLES_FILENAME = PATH_TO_TEST_FILES + "bbcVariables1200.wav";
    private static final String MEMORY_FILENAME = PATH_TO_TEST_FILES + "bbcMemory.wav";

    @Before
    public void individualSetup() {
        TapeExtractionOptions.getInstance().setLogging(TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS);

        lowPass = new LowPass(4800);
        highPass = new HighPass(200);
        fileExtractor1200 = new BBCFileExtractor(true);
        fileExtractor300 = new BBCFileExtractor(false);
        pulseExtractor = new AcornPulseExtractor();
        intervalExtractor = new ZeroCrossingIntervalExtractor();

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor1200);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor300);
        fileExtractor1200.registerFileStreamConsumer(this);
        fileExtractor300.registerFileStreamConsumer(this);
        results = new LinkedList<>();
    }

    @Test
    public void testBBC1200baudOneFile() throws Throwable {
        parseFile(ONE_FILE_FILENAME, 1);
        checkFileResult(0,550, "JUNIT1200.basic", 203395942, 6400);
    }

    @Test
    public void testTwoBBC1200baudFiles() throws Throwable {
        parseFile(TWO_FILE_FILENAME, 2);
        checkFileResult(0,550, "JUNIT1200.basic", 203395942, 6400);
        checkFileResult(1,872, "FILE2.basic", -65476424, 6400);
    }

    @Test
    public void testBBC300baudFile() throws Throwable {
        parseFile(ONE_FILE_300_BAUD_FILENAME, 1);
        checkFileResult(0,550, "JUNIT300.basic", 203395942, 6400);
    }

    @Test
    public void testBBC1200variablesFile() throws Throwable {
        parseFile(VARIABLES_FILENAME, 1);
        checkFileResult(0,2800, "VARIABLES.variables", 938873718, BBCTapeFile.VARIABLES_LOAD_ADDRESS);
    }

    @Test
    public void testBBCMemoryFile() throws Throwable {
        parseFile(MEMORY_FILENAME, 1);
        checkFileResult(0,512, "BYTES.bytes.6400.6400", 2146807500, 6400);
    }

    private void parseFile(String filename, int numberOfExpectedResults) throws Throwable {
        AudioInput reader = new AudioInput(filename);
        try {
            reader.registerSampleStreamConsumer(lowPass);
            pushStreamThroughSystem(reader);
            assertTrue("Wrong number of files after extracting " + filename + ". Got " + results.size(), results.size() == numberOfExpectedResults);
        } finally {

        }
    }

    private void checkFileResult(int fileNumber, int expectedFileLength, String expectedFilename, int expectedContentHash, int expectedLoadAddress) {
        assertTrue("File length incorrect: " + results.get(fileNumber).length(), results.get(fileNumber).length() == expectedFileLength);
        assertTrue("File name incorrect: " + results.get(fileNumber).getFilename(), results.get(fileNumber).getFilename().equals(expectedFilename));
        assertTrue("File content does not match expected hash: " + Arrays.hashCode(results.get(fileNumber).getDataBytesOfType(TapeFile.FormatType.EMULATOR)),
                Arrays.hashCode(results.get(fileNumber).getDataBytesOfType(TapeFile.FormatType.EMULATOR)) == expectedContentHash);
        assertTrue("Load address incorrect: " + results.get(fileNumber).getLoadAddress(),
                results.get(fileNumber).getLoadAddress() == expectedLoadAddress);
    }

    private void pushStreamThroughSystem(AudioInput reader) throws Throwable {
        reader.processFile();
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        if (file == null)
            return;

        assertTrue("Returned tape file is not Acorn format", file instanceof BBCTapeFile);
        results.add((BBCTapeFile) file);
    }
}
