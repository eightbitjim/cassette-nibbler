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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.LinkedList;
import java.util.Queue;

public class FileBuilder {
    private transient TapeFile currentFile;
    private transient TapeBlock currentBlock;
    private transient Queue<TapeFile> builtFiles;
    private transient TapeExtractionLogging logging;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeFile.FileType fileType;
    private transient String channelName;

    public FileBuilder(TapeFile.FileType fileType, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        this.fileType = fileType;
        builtFiles = new LinkedList<>();
    }

    public void addBlock(TapeBlock block) {
        currentBlock = block;
        completeAnyPreviousFiles();
        startNewFileIfNecessary();
        addBlockToThisFile();
        checkForCompletedFile();
    }

    public TapeFile getNextFile() {
        return builtFiles.poll();
    }

    public boolean moreFilesToReturn() {
        return builtFiles.peek() != null;
    }

    private void completeAnyPreviousFiles() {
        if (currentBlock == null)
            return;

        if (currentBlock.getType() == TapeBlock.BlockType.NAMEFILE && currentFile != null)
            dealWithIncompleteFile();
    }

    private void dealWithIncompleteFile() {
        // TODO: get file size and pad with blocks with zeros
        logging.writeFileParsingInformation("Storing incomplete file");
        completeCurrentFile();
    }

    private void startNewFileIfNecessary() {
        if (currentFile == null) {
            logging.writeFileParsingInformation("Starting new tape file");
            currentFile = new TapeFile(fileType);
            addDummyHeaderIfNecessary();
        }
    }

    private void addDummyHeaderIfNecessary() {
        if (currentBlock.getType() != TapeBlock.BlockType.NAMEFILE) {
            currentFile.hasAnError();
            logging.writeFileParsingInformation("Creating dummy header block");
            TapeBlock dummyHeader = TapeBlock.createDummyNamefile(channelName);
            currentFile.addBlock(dummyHeader);
        }
    }

    private void completeCurrentFile() {
        if (currentFile == null)
            return;

        logging.writeFileParsingInformation("Completing current file. Name: " + currentFile.getFilename() + " length " + currentFile.length());
        if (currentFile.containsErrors()) {
            logging.writeFileParsingInformation("File has at least one error.");
            if (!options.getAttemptToRecoverCorruptedFiles()) {
                logging.writeFileParsingInformation("Options set to not recover corrupted files. Discarding.");
                currentFile = null;
                return;
            } else
                logging.writeFileParsingInformation("Options are set to recover corrupted files.");
        }

        builtFiles.add(currentFile);
        currentFile = null;
    }

    private void addBlockToThisFile() {
        currentFile.addBlock(currentBlock);
    }

    private void checkForCompletedFile() {
        if (currentFile.containsErrors() && !options.getAttemptToRecoverCorruptedFiles()) {
            completeCurrentFile();
            return;
        }

        if (!currentFile.moreBlocksNeeded())
            completeCurrentFile();
    }

    public void finishAnyPartiallyCompleteFiles() {
        completeCurrentFile();
    }

    public void fileHasAnError() {
        if (currentFile != null)
            currentFile.hasAnError();
    }
}
