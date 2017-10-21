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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.PulseExtraction;

import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import org.junit.Before;

import static org.junit.Assert.assertTrue;

public class SpectrumPulseTest implements PulseStreamConsumer {

    private SpectrumPulseExtractor pulseExtractor;
    private ZeroCrossingIntervalExtractor intervalExtractor;
    private StringBuilder resultString;

    private String channelName = "channel";

    private static final String PATH_TO_TEST_FILES = "src/test/testFiles/";
    private static final String SHORT_PULSE_FILENAME = PATH_TO_TEST_FILES + "spectrumPulses.wav";

    @Before
    public void individualSetup() {
        pulseExtractor = new SpectrumPulseExtractor(channelName);
        intervalExtractor = new ZeroCrossingIntervalExtractor();
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(this);
        resultString = new StringBuilder();
    }

    private void checkResultString(String message, String expectedResult) {
        String result = resultString.toString();
        assertTrue("Result of pulse extraction does not meet expected result for " + message + ". Got " + result + " expected " + expectedResult, expectedResult.equals(result));
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
