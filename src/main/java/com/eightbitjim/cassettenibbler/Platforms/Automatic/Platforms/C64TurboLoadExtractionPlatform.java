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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.Platforms;

import com.eightbitjim.cassettenibbler.Platform;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction.EncodingScheme;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction.EncodingSchemeFileExtractor;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction.FileCombinerWithThreashold;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction.SinglePulseEncodingSchemeLibrary;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.SequenceRecognition.C64RecognitionLibrary;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.PulseExtraction.AutomaticPulseExtractor;
import com.eightbitjim.cassettenibbler.Platforms.General.Demodulation.ZeroCrossingIntervalExtractor;

import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;

public class C64TurboLoadExtractionPlatform extends Platform {
    ZeroCrossingIntervalExtractor intervalExtractor = new ZeroCrossingIntervalExtractor();
    AutomaticPulseExtractor pulseExtractor = new AutomaticPulseExtractor(2);
    FileCombinerWithThreashold fileExtractor = new FileCombinerWithThreashold(new C64RecognitionLibrary());
    List<EncodingScheme> schemeList;

    String platformSettings;

    public C64TurboLoadExtractionPlatform() {
        super();

        name = "c64turboload";
        description = "Analysis of pulses to automatically determine loading format, based on the assumption that it is a C64 turbo loader";
    }

    @Override
    public boolean hasHighProcessingOverhead() {
        return true;
    }

    @Override
    public void setConfigurationString(String platformSettings) {
        this.platformSettings = platformSettings;
        getEncodingSchemesToTry();

        EncodingSchemeFileExtractor analysisFileExtractor;
        if (schemeList == null)
            return;

        for (EncodingScheme scheme : schemeList) {
            for (int skipPulses = 0; skipPulses < scheme.maximumByteFrameLength(); skipPulses++) {
                analysisFileExtractor = new EncodingSchemeFileExtractor(scheme, skipPulses);
                analysisFileExtractor.registerFileStreamConsumer(fileExtractor);
                pulseInput = analysisFileExtractor; // TODO currently this only goes to one
                pulseExtractor.registerPulseStreamConsumer(analysisFileExtractor);
            }
        }

        intervalExtractor.registerIntervalStreamConsumer(pulseExtractor);

        sampleInput = intervalExtractor;
        intervalInput = pulseExtractor;

        intervalOutput = intervalExtractor;
        pulseOutput = pulseExtractor;
        fileOutput = fileExtractor;

        registerTypes();
    }

    private void getEncodingSchemesToTry() {
        if (platformSettings == null || platformSettings.length() == 0)
            useLibraryOfSchemes();
        else
            useSchemeSpecifiedInPlatformSettings();
    }

    private void useLibraryOfSchemes() {
        SinglePulseEncodingSchemeLibrary library = new SinglePulseEncodingSchemeLibrary();
        schemeList = library.getSchemes();
    }

    private void useSchemeSpecifiedInPlatformSettings() {
        try {
            EncodingScheme schemeToUse = new EncodingScheme(platformSettings);
            schemeList = new LinkedList<>();
            schemeList.add(schemeToUse);
        } catch (InvalidPropertiesFormatException e) {
            System.err.println("Invalid encoding scheme specified: " + e.toString());
        }
    }
}
