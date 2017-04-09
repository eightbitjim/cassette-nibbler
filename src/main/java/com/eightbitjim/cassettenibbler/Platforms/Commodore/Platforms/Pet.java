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
import com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction.Commodore64Vic20PulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;

public class Pet extends Platform {
    private static final String C64_DEFAULT_FILE_EXTENSION = "c64c128vic20pet";

    ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
    Commodore64Vic20PulseExtractor pulseExtractor = new Commodore64Vic20PulseExtractor(false);
    CommodoreFileExtractor fileExtractor = new CommodoreFileExtractor(C64_DEFAULT_FILE_EXTENSION);

    public Pet() {
        super();

        name = "pet";
        description = "Commodore PET";

        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);

        sampleInput = intervalExtractor;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }
}
