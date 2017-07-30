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
import com.eightbitjim.cassettenibbler.Platforms.Commodore.ByteExtraction.CommodoreByteReader;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.PulseExtraction.Commodore64Vic20PulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;

public class C64 extends Platform {

    private static final String C64_DEFAULT_FILE_EXTENSION = "c64c128vic20pet";

    HighPass highPass = new HighPass(200);
    ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
    Commodore64Vic20PulseExtractor pulseExtractor = new Commodore64Vic20PulseExtractor(false);
    CommodoreFileExtractor fileExtractor = new CommodoreFileExtractor(C64_DEFAULT_FILE_EXTENSION);
    CommodoreByteReader byteReader = new CommodoreByteReader();

    public C64() {
        super();

        name = "commodore";
        description = "Commodore 64, 128, VIC20, PET";

        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);
        pulseExtractor.registerPulseStreamConsumer(byteReader);

        sampleInput = highPass;
        postFilterSampleInput = intervalExtractor;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;
        byteOutput = byteReader;

        registerTypes();
    }
}
