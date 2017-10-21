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

package com.eightbitjim.cassettenibbler.Platforms.Apple.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Apple.FileExtraction.AppleFileStateMachine;
import com.eightbitjim.cassettenibbler.Platforms.Apple.PulseExtraction.ApplePulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;

public class AppleII extends Platform {

    LowPass lowPass;
    HighPass highPass;
    ZeroCrossingIntervalExtractor intervalExtractor;
    ApplePulseExtractor pulseExtractor;
    AppleFileStateMachine fileExtractor;

    public AppleII() {
        super();

        name = "apple2";
        description = "Apple II computer";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        lowPass = new LowPass(4800, channelName);
        highPass = new HighPass(200, channelName);
        intervalExtractor = new ZeroCrossingIntervalExtractor();
        pulseExtractor = new ApplePulseExtractor(channelName);
        fileExtractor = new AppleFileStateMachine(channelName);

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = lowPass;
        postFilterSampleInput = intervalExtractor;
        pulseInput = fileExtractor;
        pulseOutput = pulseExtractor;
        intervalOutput = intervalExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
