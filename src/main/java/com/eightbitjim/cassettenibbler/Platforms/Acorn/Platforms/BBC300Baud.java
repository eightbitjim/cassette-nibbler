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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.ByteExtraction.ByteScraper;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction.AcornByte;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction.BBCFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.PulseExtraction.AcornPulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;

public class BBC300Baud extends Platform {

    LowPass lowPass = new LowPass(4800);
    HighPass highPass = new HighPass(200);
    ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
    AcornPulseExtractor pulseExtractor = new AcornPulseExtractor();
    BBCFileExtractor fileExtractor = new BBCFileExtractor(false);
    ByteScraper byteScraper = new ByteScraper(AcornByte.Baud.BAUD_300);

    public BBC300Baud() {
        super();

        name = "acorn300";
        description = "Acorn BBC Micro A, B and Master Series, Acorn Electron at 300 baud (slow)";

        lowPass.registerSampleStreamConsumer(highPass);
        highPass.registerSampleStreamConsumer(intervalExtractor);
        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
        pulseExtractor.registerPulseStreamConsumer(fileExtractor);
        pulseExtractor.registerPulseStreamConsumer(byteScraper);

        sampleInput = lowPass;
        postFilterSampleInput = intervalExtractor;
        intervalInput = pulseExtractor;
        pulseInput = fileExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;
        byteOutput = byteScraper;

        registerTypes();
    }
}
