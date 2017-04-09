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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.util.LinkedList;
import java.util.Queue;

public class CommodoreFileBuilder {
    Queue<CommodoreTapeFile> filesToReturn = new LinkedList<>();
    CommodoreTapeFile currentFile;
    CommodoreFileBlock previousBlock = null;
    CommodoreFileBlock currentBlock = null;

    private static final int DEFAULT_START_ADDRESS = 2049;

    TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private String defaultFileExtension;

    public CommodoreFileBuilder(String defaultFileExtension) {
        this.defaultFileExtension = defaultFileExtension;
        currentFile = new CommodoreTapeFile(defaultFileExtension);
    }

    public void addBlock(CommodoreFileBlock block) {
        currentBlock = block;

        if (block.isARepeat()) {
            combineWithAnyPreviousBlockAndAddToFile();
        } else {
            deadlWithNonRepeatedBlock();
        }
    }

    private void combineWithAnyPreviousBlockAndAddToFile() {
        combineWithPreviousBlock();

        logging.writeFileParsingInformation("Adding block to file");
        addBlockToFile(currentBlock);
    }

    private void combineWithPreviousBlock() {
        if (previousBlock == null) {
            logging.writeFileParsingInformation("This repeated block was never received in its original form. Adding to file.");
            return;
        }

        logging.writeFileParsingInformation("Combining this repeated block with the previous one");
        currentBlock.merge(previousBlock);
        previousBlock = null;
    }

    private void addBlockToFile(CommodoreFileBlock blockToAdd) {
        pushOutFileIfThisBlockWouldNotBeSuitableToAdd(blockToAdd);
        addHeadersIfThisIsAnOrphanBlock();
        currentFile.addBlock(blockToAdd);
        pushOutFileIfObviouslyComplete();
    }

    private void addHeadersIfThisIsAnOrphanBlock() {
        if (currentFile.getType() != CommodoreTapeFile.Type.UNKNOWN)
            return;

        switch (currentBlock.getFileType()) {
            case CommodoreFileBlock.DATA:
                addDummyProgramHeader(currentBlock.getBlockLength());
                break;
            case CommodoreFileBlock.SEQENTIAL_FILE_DATA:
                addDummySequentialFileHeader();
                break;
        }
    }

    private void pushOutFileIfThisBlockWouldNotBeSuitableToAdd(CommodoreFileBlock block) {
        CommodoreTapeFile.Type fileType = currentFile.getType();

        switch(fileType) {
            case SEQ:
                if (block.getFileType() == CommodoreFileBlock.SEQENTIAL_FILE_DATA) {
                    logging.writeFileParsingInformation("Block appears to be sequential file data. Will add to current file.");
                    return;
                } else {
                    logging.writeFileParsingInformation("Block is not sequential file data. Therefore previous file must be complete.");
                }
                break;

            case PRG:
            case NRP:
                return;

            case UNKNOWN:
                logging.writeFileParsingInformation("First block in a new file");
                return;
        }

        pushCurrentFileToOutputQueue();
    }

    private void pushCurrentFileToOutputQueue() {
        logging.writeFileParsingInformation("Pushing file of type " + currentFile.getType() + " to output queue.");
        filesToReturn.add(currentFile);
        currentFile = new CommodoreTapeFile(defaultFileExtension);
    }

    private void pushOutFileIfObviouslyComplete() {
        if (!currentBlock.isARepeat())
            return;

        switch (currentBlock.getFileType()) {
            case CommodoreFileBlock.PROGRAM_HEADER:
            case CommodoreFileBlock.NON_RELOCATABLE_PROGRAM_HEADER:
            case CommodoreFileBlock.SEQENTIAL_FILE_HEADER:
            case CommodoreFileBlock.SEQENTIAL_FILE_DATA:
                return;
        }

        switch (currentFile.getType()) {
            case PRG:
            case NRP:
            case ORPHAN_DATA_BLOCK:
            case UNKNOWN:
                logging.writeFileParsingInformation("BLOCK IS A REPEAT IN A FILE TYPE THAT SHOULD ONLY HAVE ONE DATA BLOCK. SO CONCLUDING IT IS THE END OF THE FILE.");
                pushCurrentFileToOutputQueue();
                break;
        }
    }

    private void deadlWithNonRepeatedBlock() {
        if (previousBlock != null) {
            logging.writeFileParsingInformation("Previously read block was never repeated. Adding to the file first.");
            currentFile.addBlock(previousBlock);
        }

        pushOutFileIfThisBlockWouldNotBeSuitableToAdd(currentBlock);

        logging.writeFileParsingInformation("Remembering this block in case subsequently repeated");
        previousBlock = currentBlock;
    }

    public void completeAnyCurrentFiles() {
        if (previousBlock != null) {
            if (currentFile.getType() == CommodoreTapeFile.Type.UNKNOWN)
                addDummyProgramHeader(previousBlock.getBlockLength());

            currentFile.addBlock(previousBlock);
        }

        previousBlock = null;

        if (currentFile.getType() != CommodoreTapeFile.Type.UNKNOWN)
            pushCurrentFileToOutputQueue();

        currentFile = new CommodoreTapeFile(defaultFileExtension);
    }

    public CommodoreTapeFile getFile() {
        if (filesToReturn.isEmpty())
            return null;
        else
            return filesToReturn.remove();
    }

    private void addDummyProgramHeader(int dataLength) {
        logging.writeFileParsingInformation("PREPARING DUMMY PROGRAM FILE HEADER AND ADDING TO FILE");

        CommodoreFileBlock block = new CommodoreFileBlock();
        block.dataPointer = CommodoreFileBlock.HEADER_DATA_LENGTH;
        block.setFilenameBuffer(block.makeFilenameBuffer("HEADLESS_FILE"));
        block.setStartAddress(DEFAULT_START_ADDRESS);
        block.setEndAddressPlusOne(DEFAULT_START_ADDRESS + dataLength);
        block.setType(CommodoreFileBlock.PROGRAM_HEADER);
        currentFile.addBlock(block);

    }

    private void addDummySequentialFileHeader() {
        logging.writeFileParsingInformation("PREPARING DUMMY SEQUENTIAL FILE HEADER AND ADDING TO FILE");
        CommodoreFileBlock block = new CommodoreFileBlock();
        block.prepareDummySequentialFileHeader(0);
        block.setFilenameBuffer(block.makeFilenameBuffer("HEADLESS_SEQ_FILE"));
        currentFile.addBlock(block);
    }
}
