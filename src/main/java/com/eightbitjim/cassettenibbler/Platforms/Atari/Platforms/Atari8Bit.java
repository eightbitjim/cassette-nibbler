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

package com.eightbitjim.cassettenibbler.Platforms.Atari.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Apple.FileExtraction.AppleFileStateMachine;
import com.eightbitjim.cassettenibbler.Platforms.Apple.PulseExtraction.ApplePulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Atari.FileExtraction.AtariFileStateMachine;
import com.eightbitjim.cassettenibbler.Platforms.Atari.PulseExtraction.AtariPulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;

public class Atari8Bit extends Platform {

    public Atari8Bit() {
        super();

        name = "atari";
        description = "Atari 400/800/XL";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        LowPass lowPass = new LowPass(6000, channelName);
        HighPass highPass = new HighPass(3800, channelName);
        ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
        AtariPulseExtractor pulseExtractor = new AtariPulseExtractor();
        AtariFileStateMachine fileExtractor = new AtariFileStateMachine(channelName);

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
        intervalInput = pulseExtractor;

        registerTypes();
    }
}
