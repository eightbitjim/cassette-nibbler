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

package com.eightbitjim.cassettenibbler.Platforms.MSX.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;
import com.eightbitjim.cassettenibbler.Platforms.MSX.FileExtraction.MSXFileStateMachine;
import com.eightbitjim.cassettenibbler.Platforms.MSX.PulseExtraction.MSXPulseExtractor;

public class MSX2400 extends Platform {

    public MSX2400() {
        super();

        name = "msx2400";
        description = "MSX compatible computer 2400 baud";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        LowPass lowPass = new LowPass(4800, channelName);
        HighPass highPass = new HighPass(200, channelName);
        ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
        MSXPulseExtractor pulseExtractor = new MSXPulseExtractor(MSXPulseExtractor.Baud.BAUD_2400);
        MSXFileStateMachine fileExtractor = new MSXFileStateMachine(channelName);

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = lowPass;
        postFilterSampleInput = intervalExtractor;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
