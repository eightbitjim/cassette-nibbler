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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction;

import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommodorePulseTest implements PulseStreamConsumer {

    private Commodore64Vic20PulseExtractor pulseExtractor64;
    private ZeroCrossingIntervalExtractor intervalExtractor;
    private StringBuilder resultString;

    private String channelName = "channel";

    private static final String PATH_TO_TEST_FILES = "src/test/testFiles/";
    private static final String SHORT_PULSE_FILENAME = PATH_TO_TEST_FILES + "short.wav";

    @Before
    public void individualSetup() {
        pulseExtractor64 = new Commodore64Vic20PulseExtractor(false);
        intervalExtractor = new ZeroCrossingIntervalExtractor();
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor64);
        pulseExtractor64.registerPulseStreamConsumer(this);
        resultString = new StringBuilder();
    }

    @Test
    public void testCommodore64ShortFile() throws Throwable {
        String expectedResult = "smsmsmsmssmsmlmsmsmsmmssmsmsmsmsmlmsmsmmsmsmsmssmsmmslmsmsmsmsmsmsmsmsmmslmmssmsmmsmssmsmmsmslmsmmssmsmsmmssmsmmslmsmsmsmsmsmmssmsmsmlmmssmmsmsmssmmsmsmslmsmsmsmsmsmmssmmsmslmsmsmsmsmsmmssmsmsmlmmssmmsmsmssmmsmsmslmsmsmsmsmsmmssmmsmslmsmsmsmsmsmmssmsmsmlmmssmmsmsmssmmsmsmslmsmsmsmsmsmmssmsmsmlmsmmssmmssmsmmsmsmslmsmsmsmse";
        AudioInput reader = new AudioInput(SHORT_PULSE_FILENAME, channelName);
        try {
            reader.registerSampleStreamConsumer(intervalExtractor);
            pushStreamTrhoughSystem(reader);
            checkResultString(SHORT_PULSE_FILENAME, expectedResult);
        } finally {

        }
    }

    private void checkResultString(String message, String expectedResult) {
        String result = resultString.toString();
        assertTrue("Result of pulse extraction does not meet expected result for " + message, expectedResult.equals(result));
    }

    private void pushStreamTrhoughSystem(AudioInput reader) throws Throwable {
        reader.processFile();
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        resultString.append(pulseType);
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }
}
