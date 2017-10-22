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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction.Commodore16Plus4PulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;

public class C16 extends Platform {
    private static final String C16PLUS4_DEFAULT_FILE_EXTENSION = "c16plus4";

    public C16() {
        super();

        name = "commodore16+4";
        description = "ROM Loader for Commodore 16, Commodore +4";
    }

    @Override
    public void initialise(String channelName) {
        channelName = name + channelName;

        HighPass highPass = new HighPass(200, channelName);
        ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
        Commodore16Plus4PulseExtractor pulseExtractor = new Commodore16Plus4PulseExtractor();
        CommodoreFileExtractor fileExtractor = new CommodoreFileExtractor(C16PLUS4_DEFAULT_FILE_EXTENSION, channelName);

        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = highPass;
        postFilterSampleInput = intervalExtractor;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
