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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.Platforms;

import com.eightbitjim.cassettenibbler.DataSink.Directory;
import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.HighPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.LowPass;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.SampleStreamSplitter;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.PulseExtraction.ZX81PulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.Amplify;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.ByteExtraction.ZX81ByteReader;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZX8081.ZX81FileExtract;

public class ZX81 extends Platform {

    Directory fileCombiner = new Directory();
    SampleStreamSplitter splitter = new SampleStreamSplitter();

    @Override
    public boolean hasHighProcessingOverhead() {
        return true;
    }

    public ZX81() {
        super();

        name = "zx81";
        description = "Sinclair ZX81, 1K and 16K";

        double minVolume = -30.0;
        double maxVolume = 60.0;
        double volumeStep = 1.0;

        for (double volume = minVolume; volume < maxVolume; volume += volumeStep) {
            Amplify amplifier = new Amplify(volume);
            ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
            ZX81PulseExtractor pulseExtractor = new ZX81PulseExtractor();
            ZX81ByteReader byteExtractor = new ZX81ByteReader();
            ZX81FileExtract fileExtractor = new ZX81FileExtract();

            splitter.registerSampleStreamConsumer(amplifier);
            amplifier.registerSampleStreamConsumer(intervalExtractor);
            amplifier.setMultiplier(volume);

            intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);
            intervalExtractor.setThreasholdLine(-0.5);

            pulseExtractor.registerPulseStreamConsumer(byteExtractor);
            byteExtractor.registerByteStreamConsumer(fileExtractor);
            fileExtractor.registerFileStreamConsumer(fileCombiner);
        }

        sampleInput = splitter;
        postFilterSampleInput = splitter;
        intervalInput = null;
        pulseInput = null;

        intervalOutput = null;
        pulseOutput = null;
        fileOutput = fileCombiner;

        registerTypes();
    }
}
