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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction;

import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.PETSCII;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction.Commodore64Vic20PulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.TapeFile;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CommodoreFileTest implements FileStreamConsumer {

    private CommodoreFileExtractor fileExtractor;
    private Commodore64Vic20PulseExtractor pulseExtractor64;
    private ZeroCrossingIntervalExtractor intervalExtractor;
    private List<CommodoreTapeFile> results;

    private String channelName = "channel";

    private static final String PATH_TO_TEST_FILES = "src/test/testFiles/";
    private static final String ONE_FILE_FILENAME = PATH_TO_TEST_FILES + "c64file.wav";
    private static final String ORPHAN_DATA_FILENAME = PATH_TO_TEST_FILES + "c64orphandatablock.wav";
    private static final String HEADER_ONLY = PATH_TO_TEST_FILES + "c64headeronly.wav";
    private static final String MISSING_SECOND_HALF_DATA_FILENAME = PATH_TO_TEST_FILES + "c64headerandfirsthalfofdata.wav";
    private static final String FIRST_HALF_OF_DATA_ONLY = PATH_TO_TEST_FILES + "c64firsthalfofdatablockonly.wav";
    private static final String SECOND_HALF_OF_DATA_ONLY = PATH_TO_TEST_FILES + "c64repeateddatablockonly.wav";
    private static final String TWO_FILE_FILENAME = PATH_TO_TEST_FILES + "twoc64files.wav";
    private static final String ONE_128_FILE_FILENAME = PATH_TO_TEST_FILES + "c128file.wav";
    private static final String TWO_128_SEQ_FILE_FILENAME = PATH_TO_TEST_FILES + "c128seqfileandwriter.wav";
    private static final String FILE_EXTENSION = "default";

    @Before
    public void individualSetup() {
        TapeExtractionOptions.getInstance().setLogging(TapeExtractionOptions.LoggingMode.NONE, null);
        fileExtractor = new CommodoreFileExtractor(FILE_EXTENSION, channelName);
        pulseExtractor64 = new Commodore64Vic20PulseExtractor(false);
        intervalExtractor = new ZeroCrossingIntervalExtractor();
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor64);
        pulseExtractor64.registerPulseStreamConsumer(fileExtractor);
        fileExtractor.registerFileStreamConsumer(this);
        results = new LinkedList<>();
    }

    @Test
    public void testC64File() throws Throwable {
        parseFile(ONE_FILE_FILENAME, 1);
        checkFileResult(0,122, "test1.prg.c64", 271631688, 2049);
    }

    @Test
    public void testC64orphanData() throws Throwable {
        parseFile(ORPHAN_DATA_FILENAME, 1);
        checkFileResult(0,122, "headless_file.prg.c64", 271631688, 2049);
    }

    @Test
    public void testC64headerOnly() throws Throwable {
        parseFile(HEADER_ONLY, 1);
        checkFileResult(0,0, "test1.prg.c64", 1, 2049);
    }

    @Test
    public void testC64firstHalfOfDataOnly() throws Throwable {
        parseFile(FIRST_HALF_OF_DATA_ONLY, 1);
        checkFileResult(0,122, "headless_file.prg.c64", 271631688, 2049);
    }

    @Test
    public void testC64onlyRepeatedDataBlock() throws Throwable {
        parseFile(SECOND_HALF_OF_DATA_ONLY, 1);
        checkFileResult(0,122, "headless_file.prg.c64", 271631688, 2049);
    }

    @Test
    public void testC64missingSecondHalfOfData() throws Throwable {
        parseFile(MISSING_SECOND_HALF_DATA_FILENAME, 1);
        checkFileResult(0,122, "test1.prg.c64", 271631688, 2049);
    }

    @Test
    public void testTwoC64Files() throws Throwable {
        parseFile(TWO_FILE_FILENAME, 2);
        checkFileResult(0,122, "test1.prg.c64", 271631688, 2049);
        checkFileResult(1,46, "another program.prg.c64", 1983920052, 2049);
    }

    @Test
    public void testC128File() throws Throwable {
        parseFile(ONE_128_FILE_FILENAME, 1);
        checkFileResult(0,73, "c128basic.prg.c128", 839433314, 7169);
    }

    @Test
    public void testC128SequentialFile() throws Throwable {
        parseFile(TWO_128_SEQ_FILE_FILENAME, 2);
        checkFileResult(0,421, "seq test 128.seq.ROMloader", 698139554, CommodoreTapeFile.NO_LOAD_ADDRESS);
        checkFileResult(1,117, "unnamed.prg.c128", -1048168624, 7169);
    }

    private void parseFile(String filename, int numberOfExpectedResults) throws Throwable {
        AudioInput reader = new AudioInput(filename, channelName);
        try {
            reader.registerSampleStreamConsumer(intervalExtractor);
            pushStreamTrhoughSystem(reader);
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
        assertTrue("Returned tape file is not commodore format", file instanceof CommodoreTapeFile);
        results.add((CommodoreTapeFile)file);
    }
}
