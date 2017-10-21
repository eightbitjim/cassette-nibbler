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

package com.eightbitjim.cassettenibbler.Platforms.Oric.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.Amplify;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;
import com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction.OricAtmosFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction.OricOneFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Oric.PulseExtraction.OricPulseExtractor;

public class OricAtmos extends Platform {
    LowPass lowPass;
    HighPass highPass;
    Amplify inverter;
    ZeroCrossingIntervalExtractor intervalExtractor;
    OricPulseExtractor pulseExtractor;
    OricAtmosFileExtractor fileExtractor;

    public OricAtmos() {
        super();

        name = "oricatmos";
        description = "Tangerine Computer Systems Oric Atmos";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        lowPass = new LowPass(4800, channelName);
        highPass = new HighPass(200, channelName);
        inverter = new Amplify(-1.0);

        intervalExtractor = new ZeroCrossingIntervalExtractor();
        pulseExtractor = new OricPulseExtractor();
        fileExtractor = new OricAtmosFileExtractor(channelName);

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(inverter);
        inverter.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = lowPass;
        postFilterSampleInput = inverter;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
