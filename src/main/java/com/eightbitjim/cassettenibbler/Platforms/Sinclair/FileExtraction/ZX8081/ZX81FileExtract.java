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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZX8081;

import com.eightbitjim.cassettenibbler.Platforms.Sinclair.Formats.ZX81Characters;
import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class ZX81FileExtract implements FileStreamProvider, ByteStreamConsumer {
    private static final int FILE_COMPLETE = 0;
    private static final int RECEIVING_BODY = 1;
    private static final int ERROR = 2;
    private static final int RECEIVING_FILENAME = 3;
    private static final int RECEIVING_FILE_VERSION = 4;
    private static final int WAITING_FOR_SILENCE = 5;
    private static final int ZX81_START_ADDRESS = 16393;
    private static final int EXPECTED_FILE_VERSION = 0;

    private List<Integer> fileData;
    private List<Integer> filenameBuffer;
    private Stack<TapeFile> filesToReturn;

    private int currentState;
    private int currentMemoryPosition;
    private long erroneousPulsesBeforeThisByte;
    private boolean silenceBeforeThisByte;
    private long currentTimeIndex;
    private int endAddressForFile;
    private static final int POSITION_OF_FILE_LENGTH_IN_FILE = 11;
    private int nextByte;
    private List<FileStreamConsumer> consumers;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private boolean errorDetectedSinceStartingThisFile;

    public ZX81FileExtract() {
        consumers = new LinkedList<>();
        fileData = new LinkedList<>();
        filenameBuffer = new LinkedList<>();
        currentMemoryPosition = ZX81_START_ADDRESS;
        filesToReturn = new Stack<>();
        reset();
    }

    private void reset() {
        if (currentState == RECEIVING_BODY)
            attemptToRecoverFile();

        currentState = RECEIVING_FILENAME;
        currentMemoryPosition = ZX81_START_ADDRESS;
        fileData.clear();
        filenameBuffer.clear();
        errorDetectedSinceStartingThisFile = false;
    }

    public void processByte() {
        if (silenceBeforeThisByte) {
            reset();
            logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "Silence detected. Resetting to start of file.");
        }

        processReceivedByteOrError();
        checkForEndOfFile();

        if (currentState == FILE_COMPLETE || currentState == ERROR)
            filesToReturn.add(constructTapeFileFromData());

        pushFileToConsumers();
    }

    private void processReceivedByteOrError() {
        if (currentState == WAITING_FOR_SILENCE)
            return;

        if (currentState == RECEIVING_FILENAME)
            addByteToFilename();
        else if (currentState == RECEIVING_FILE_VERSION)
            addByteToFileVersion();
        else
            addByteToFileData();
    }

    private void addByteToFilename() {
        if (erroneousPulsesBeforeThisByte > 0 && filenameBuffer.size() > 0)
            dealWithFramingErrorByte();
        else {
            checkForEndOfFilename();
            filenameBuffer.add(nextByte);
        }
    }

    private void checkForEndOfFilename() {
        if ((nextByte & 0x80) != 0) {
            nextByte -= 0x80;
            currentState = RECEIVING_FILE_VERSION;
        }
    }

    private void addByteToFileVersion() {
        currentMemoryPosition++;
        fileData.add(nextByte);
        if (nextByte != EXPECTED_FILE_VERSION) {
            logging.writeDataError(currentTimeIndex,"Invalid file version found: " + nextByte);
            giveUpOnFileAfterError();
        } else {
            currentState = RECEIVING_BODY;
        }
    }

    private void giveUpOnFileAfterError() {
        errorDetectedSinceStartingThisFile = true;
        attemptToRecoverFile();
        currentState = WAITING_FOR_SILENCE;
    }

    private void attemptToRecoverFile() {
        if (!options.getAttemptToRecoverCorruptedFiles())
            return;

        ZX81TapeFile recoveredFile = constructTapeFileFromData();
        if ((recoveredFile != null) && (recoveredFile.basicValesAreCorrect()))
            filesToReturn.push(recoveredFile);
        else
            logging.writeFileParsingInformation("Recovered file did not have expected key values, so discarding.");
    }

    private void checkForEndOfFile() {
        if (endAddressForFile > 0 && currentMemoryPosition == endAddressForFile - 1)
            currentState = FILE_COMPLETE;

        if (endAddressForFile > 0 && currentMemoryPosition > endAddressForFile - 1)
            currentState = ERROR;
    }

    private void dealWithFramingErrorByte() {
        errorDetectedSinceStartingThisFile = true;
        logging.writeDataError(currentTimeIndex, "Framing error, leading to byte: " + nextByte);
        if (/*!fileData.isEmpty() && */!options.getAllowIncorrectFrameChecksums())
            giveUpOnFileAfterError();
        else {
            checkForEndOfFilename();
            filenameBuffer.add(nextByte);
        }
    }

    private void addByteToFileData() {
        if (erroneousPulsesBeforeThisByte > 0)
            dealWithErroneousByte();
        else {
            fileData.add(nextByte);
            if (currentMemoryPosition == ZX81_START_ADDRESS + POSITION_OF_FILE_LENGTH_IN_FILE + 1)
                getFileLengthFromData();

            currentMemoryPosition++;
        }
    }

    private void dealWithErroneousByte() {
        errorDetectedSinceStartingThisFile = true;
        logging.writeDataError(currentTimeIndex,"Erroneous byte: " + nextByte);
        if (!fileData.isEmpty() && !options.getAttemptToRecoverCorruptedFiles())
            giveUpOnFileAfterError();
        else {
            fileData.add(nextByte);
            if (currentMemoryPosition == ZX81_START_ADDRESS + POSITION_OF_FILE_LENGTH_IN_FILE + 1)
                getFileLengthFromData();

            currentMemoryPosition++;
        }
    }

    private void getFileLengthFromData() {
        endAddressForFile = fileData.get(POSITION_OF_FILE_LENGTH_IN_FILE) + fileData.get(POSITION_OF_FILE_LENGTH_IN_FILE + 1) * 256;
        logging.writeFileParsingInformation("Found end address for file: " + endAddressForFile);
    }

    private ZX81TapeFile constructTapeFileFromData() {
        if (fileData.isEmpty())
            return null;

        if (currentState == ERROR && !options.getAttemptToRecoverCorruptedFiles())
            return null;

        String filename = constructFilename();
        ZX81TapeFile file = new ZX81TapeFile();
        file.setFilename(filename);
        file.setData(getFileData());
        if (errorDetectedSinceStartingThisFile) {
            file.hasAnError();
            errorDetectedSinceStartingThisFile = false;
        }

        return file;
    }

    private String constructFilename() {
        StringBuffer filename = new StringBuffer();
        if (filenameBuffer.isEmpty())
            filename.append("UNNAMED");
        else
            filename.append(getASCIIFromZX81Bytes());

        return filename.toString();
    }

    private String getFileType() {
        if (currentState == ERROR)
            return ".incomplete.p";
        else
            return ".p";
    }

    private String getASCIIFromZX81Bytes() {
        StringBuffer constructedString = new StringBuffer();
        for (Integer i : filenameBuffer)
            constructedString.append(ZX81Characters.printableStringForZXCode(i.intValue()));

        return constructedString.toString();
    }

    private int [] getFileData() {
        if (fileData.isEmpty())
            return null;

        int [] array = new int[fileData.size()];
        int count = 0;
        for (Integer i : fileData) {
            array[count] = i.intValue();
            count++;
        }

        return array;
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

    private void pushFileToConsumers() {
        for (TapeFile file : filesToReturn) {
            for (FileStreamConsumer consumer : consumers)
                if (file != null)
                    consumer.pushFile(file, currentTimeIndex);
        }

        if (filesToReturn.size() > 0)
            reset();

        filesToReturn.clear();
    }

    private void addEndOfStreamToFileStack() {
        filesToReturn.add(null);
    }

    @Override
    public void pushByte(int b, long currentTimeIndex, long erroneousPulsesBeforeThisByte, boolean silenceBeforeThisByte) {
        if (b == ByteStreamConsumer.END_OF_STREAM) {
            attemptToRecoverFile();
            addEndOfStreamToFileStack();
            pushFileToConsumers();
        } else {
            this.currentTimeIndex = currentTimeIndex;
            this.silenceBeforeThisByte = silenceBeforeThisByte;
            this.erroneousPulsesBeforeThisByte = erroneousPulsesBeforeThisByte;
            this.nextByte = b;
            processByte();
        }
    }
}

