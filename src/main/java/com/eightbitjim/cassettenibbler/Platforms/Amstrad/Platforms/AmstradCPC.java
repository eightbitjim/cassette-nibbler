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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Amstrad.FileExtraction.FileStateMachine;
import com.eightbitjim.cassettenibbler.Platforms.Amstrad.PulseExtraction.AmstradPulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;

public class AmstradCPC extends Platform {

    public AmstradCPC() {
        super();

        name = "amstrad";
        description = "Amstrad CPC 464";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        LowPass lowPass = new LowPass(4800, channelName);
        HighPass highPass = new HighPass(200, channelName);
        ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
        AmstradPulseExtractor pulseExtractor = new AmstradPulseExtractor(channelName);
        FileStateMachine fileExtractor = new FileStateMachine(channelName);

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = lowPass;
        intervalInput = pulseExtractor;
        postFilterSampleInput = intervalExtractor;
        pulseInput = fileExtractor;
        pulseOutput = pulseExtractor;
        intervalOutput = intervalExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
