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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FileBuilder {
    private List<Integer> data;
    private BBCTapeFile currentFile;
    private int [] filename;
    private int previousBlockNumber;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public FileBuilder() {

        filename = new int[0];
        previousBlockNumber = -1;
        currentFile = new BBCTapeFile();
    }

    public void reset() {
        data = new LinkedList<>();
        filename = new int[0];
        currentFile = new BBCTapeFile();
        previousBlockNumber = -1;
    }

    public void currentFileHasAnError() {
        if (currentFile != null)
            currentFile.isInError();
    }

    private void log(String message) {
        logging.writeFileParsingInformation(message);
    }

    private void logError(String message) {
        logging.writeDataError(0, message);
    }

    public boolean isNameSameAs(int [] name) {
        return Arrays.equals(name, filename);
    }

    public void setName(int [] name) {
        filename = Arrays.copyOf(name, name.length);
    }

    public int [] getName() {
        return filename;
    }

    public int getSize() {
        return data.size();
    }

    public int [] getData() {
        if (data == null)
            return new int[0];

        int [] buffer = new int[data.size()];
        int count = 0;
        for (Integer i : data)
            buffer[count++] = i;

        return buffer;
    }

    public boolean addBytesAndReturnTrueIfSuccessful(BBCFileBlock block) {
        currentFile.addBlock(block);
        if (block.hasAnErorr())
            currentFile.isInError();

        int blockNumber = block.getBlockNumber();
        if (blockNumber != previousBlockNumber + 1) {
            logError("Current block number is " + blockNumber + ", whereas previous was " + previousBlockNumber + ".");
            if (!options.getAttemptToRecoverCorruptedFiles())
                return false;
       }

        int numberOfBytes = block.getNumberOfDataBytesReceived();
        if (numberOfBytes > 0) {
            int[] addThis = block.getData();
            for (int i = 0; i < numberOfBytes; i++)
                data.add(addThis[i]);
        }

        previousBlockNumber = blockNumber;
        return true;
    }

    public void padWithZeroBytes(int numberOfBytes) {
        for (int i = 0; i < numberOfBytes; i++)
            data.add(0);
    }

    public String getFilename() {
        StringBuilder builder = new StringBuilder();
        if (filename == null || filename.length == 0)
            return "UNNAMED";

        for (int i = 0; i < filename.length; i++) {
            builder.append((char) filename[i]);
        }

        return builder.toString();
    }

    public BBCTapeFile getFile() {
        currentFile.setName(getFilename());
        currentFile.setData(getData());
        return currentFile;
    }

}
