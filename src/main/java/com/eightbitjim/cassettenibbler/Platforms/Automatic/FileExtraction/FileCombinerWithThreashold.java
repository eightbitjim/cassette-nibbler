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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction;

import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.SequenceRecognition.RecognitionLibrary;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.SequenceRecognition.Recogniser;

import java.util.LinkedList;
import java.util.List;

public class FileCombinerWithThreashold implements FileStreamConsumer, FileStreamProvider {
    private transient TapeExtractionLogging logging;

    private double asciiThreashold = 0.15;
    private int scoreThreashold = 150;
    private int minimumFileLengthInBytes = 64;

    private RecognitionLibrary recognitionLibrary;
    List<FileStreamConsumer> consumers = new LinkedList<>();

    public FileCombinerWithThreashold(RecognitionLibrary recognitionLibrary, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.recognitionLibrary = recognitionLibrary;
    }

    @Override
    public void registerFileStreamConsumer(FileStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterFileStreamConsumer(FileStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {

        if(file == null || file.getDataBytesOfType(TapeFile.FormatType.BINARY) == null)
            return;

        if (file.getDataBytesOfType(TapeFile.FormatType.BINARY).length < minimumFileLengthInBytes)
            return;

        double bestASCIIproportion = 0.0;

        Recogniser bestRecogniserMatch = workOutBestRecogniser(file);
        int recogniserValue = 0;
        if (bestRecogniserMatch != null)
            recogniserValue = bestRecogniserMatch.getMatchWeightAgainst(file.getDataBytesOfType(TapeFile.FormatType.BINARY));

        int score = qualityOfMatchPercent(bestASCIIproportion, recogniserValue);
        if (score > scoreThreashold) {
            file.setAdditionalInformation("score" + score);
            for (FileStreamConsumer consumer : consumers)
                consumer.pushFile(file, currentTimeIndex);

            System.err.print(file.getFilename() + ": ");
            for (byte b: file.getDataBytesOfType(TapeFile.FormatType.READABLE))
                System.err.print((char)b);

            System.err.println();
        }
    }

    private Recogniser workOutBestRecogniser(TapeFile file) {
        Recogniser bestRecogniser = recognitionLibrary.getClosestMatch(file.getDataBytesOfType(TapeFile.FormatType.BINARY));
        return bestRecogniser;
    }

    private int qualityOfMatchPercent(double ascii, int recogniser) {
        int quality = Math.max(0, recogniser * 3);
        if (ascii > asciiThreashold)
            quality += ((ascii - asciiThreashold) * 40.0);

        return quality;
    }

}
