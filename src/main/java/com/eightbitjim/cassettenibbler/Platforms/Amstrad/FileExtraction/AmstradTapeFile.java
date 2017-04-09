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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.Amstrad.Formats.AmstradBasic;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.Formats.SpectrumBasicProgram;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.Utilities.PrintableString;

import java.util.LinkedList;
import java.util.List;

public class AmstradTapeFile extends TapeFile {

    private List<FileBlock> blocks = new LinkedList<>();
    private transient HeaderSubsection lastHeaderSubsection;
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    private static final int BLOCK_SUBSECTION_LENGTH = 0x100;
    private static final int SUBSECTIONS_IN_HEADER = 0x1;
    private static final int MAXIMUM_NUMBER_OF_BYTES_IN_DATA_BLOCK = 0x0800;

    public void addBlock(FileBlock block) {
        checkBlockIsExpectedType(block);

        if (block.getBlockType() == FileBlock.Type.HEADER) {
            lastHeaderSubsection = (HeaderSubsection) block.getSubSections().get(0);
            ensureNextBlockIsEvenNumber();
        }

        blocks.add(block);
    }

    private void checkBlockIsExpectedType(FileBlock block) {
        if (evenNumberOfBlocks())
            expectingHeader(block);
        else
            expectingData(block);
    }

    private void expectingHeader(FileBlock block) {
        if (block.getBlockType() == FileBlock.Type.HEADER)
            return;

        logging.writeFileParsingInformation("Expected header block but found data block");
        dealWithDataBlockWhenHeaderExpected(block);
    }

    private void expectingData(FileBlock block) {
        if (block.getBlockType() == FileBlock.Type.DATA)
            return;

        logging.writeFileParsingInformation("Expected data block but found header block");
        dealWithHeaderBlockWhenDataExpected(block);
    }

    private void dealWithDataBlockWhenHeaderExpected(FileBlock block) {
        addDummyHeaderBlockForDataBlock(block);
    }

    private void dealWithHeaderBlockWhenDataExpected(FileBlock block) {
        addDummyDataBlock();
    }

    private void ensureNextBlockIsEvenNumber() {
        if (!evenNumberOfBlocks())
            addDummyDataBlock();
    }

    private HeaderSubsection.FileType getFileType() {
        if (blocks.size() == 0)
            return HeaderSubsection.FileType.BINARY;

        FileBlock firstFileBlock = blocks.get(0);
        List <FileSubsection> subsections = firstFileBlock.getSubSections();
        if (subsections.size() == 0 || !(subsections.get(0) instanceof HeaderSubsection))
            return HeaderSubsection.FileType.BINARY;

        HeaderSubsection header = (HeaderSubsection)subsections.get(0);
        return header.getFileType();
    }

    private void addDummyDataBlock() {
        logging.writeFileParsingInformation("Adding dummy data block so header block can be parsed");
        blocks.add(FileBlock.getDummyDataBlock());
    }

    private void addDummyHeaderBlockForDataBlock(FileBlock block) {
        logging.writeFileParsingInformation("Adding dummy header block so data can be included (note may be truncated)");
        if (lastHeaderSubsection == null) {
            logging.writeFileParsingInformation("No previous header. Creating new dummy header not yet implemented. Adding data block twice to try to regain sync.");
            blocks.add(block);
            return;
        }

        blocks.add(blocks.get(blocks.size() - 1));
        return;
    }

    public int getNumberOfSubsectionsInNextBlock() {
        if ((lastHeaderSubsection == null) || evenNumberOfBlocks())
            return SUBSECTIONS_IN_HEADER;

     return numberOfSubsectionsForByteLength(lastHeaderSubsection.getBlockLengthInBytes());
    }

    private boolean evenNumberOfBlocks() {
        return blocks.size() % 2 == 0;
    }

    public boolean moreBytesNeeded() {
        if (lastHeaderSubsection == null)
            return true;

        if (!evenNumberOfBlocks())
            return true;

        return !lastHeaderSubsection.isLastBLock();
    }

    private int numberOfSubsectionsForByteLength(int numberOfBytes) {
        int result = numberOfBytes / BLOCK_SUBSECTION_LENGTH;
        if (numberOfBytes % BLOCK_SUBSECTION_LENGTH != 0)
            result++;

        return result;
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public int length() {
        return getRawData().length;
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();
        String filename = getFilenameFromHeader();
        if ((filename == null) || (filename.length() == 0))
            filenameBuilder.append("unnamed");
        else
            filenameBuilder.append(filename);

        return filenameBuilder.toString();
    }

    @Override
    public String getExtension() {
        return "";
    }

    @Override
    public boolean containsErrors() {
        if (blocks.size() == 0)
            return false;

        for (FileBlock block : blocks)
            if (block.blockContainsErrors())
                return true;

        return false;
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getASCII();
            case EMULATOR:
                return getEmulatorFileData();
            case BINARY:
            default:
                return getRawData();
        }
    }

    private String getFilenameFromHeader() {
        if (lastHeaderSubsection == null)
            return "unnamed";
        else
            return lastHeaderSubsection.getFilename();
    }

    private byte [] getASCII() {
        HeaderSubsection.FileType type = getFileType();
        switch (type) {
            case ASCII:
            case BASIC:
                return getRawData();

            case ENCRYPTED_BASIC:
                return getBasicProgram();

            case BINARY:
            default:
                return printableStringFromRawData();
        }
    }

    private byte [] getBasicProgram() {
        String basicListing = new AmstradBasic(getRawData()).toString();
        return basicListing.getBytes();
    }

    private byte [] printableStringFromRawData() {
        StringBuilder inputString = new StringBuilder();
        byte [] rawData = getRawData();
        for (byte b : rawData)
            inputString.append(Byte.toUnsignedInt(b));

        String printableString = PrintableString.convertToPrintable(inputString.toString());
        return printableString.getBytes();
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        StringBuilder builder = new StringBuilder();
        switch (getFileType()) {
            case BINARY:
                builder.append("binary");
                break;
            case ENCRYPTED_BASIC:
            case BASIC:
                builder.append("basic");
                break;
            case ASCII:
                builder.append("ascii");
                break;
            default:
                builder.append("unknown");
                break;
        }

        builder.append(".");
        switch (formatType) {
            case BINARY:
            default:
                builder.append("amstrad.bin");
                break;
            case EMULATOR:
                builder.append("amstrad.emulator");
                break;
            case READABLE:
                builder.append("amstrad.txt");
                break;
        }

        return builder.toString();
    }

    private byte [] getEmulatorFileData() {
        // TODO
        return getRawData();
    }

    private byte [] getRawData() {
        int dataSize = countDataBytes();

        byte [] dataBuffer = new byte[dataSize];
        int position = 0;
        for (FileBlock block : blocks) {
            List<Byte> blockData = block.getData();
            for (Byte value : blockData) {
                if (position < dataBuffer.length)
                    dataBuffer[position++] = value;
            }
        }

        return dataBuffer;
    }

    private int countDataBytes() {
        int dataLength = 0;
        for (FileBlock block : blocks)
            dataLength += block.sizeOfDataPayloadInBytes();

        return dataLength;
    }

    @Override
    public int hashCode() {
        int hash = getFilename().hashCode();
        for (FileBlock block : blocks)
            hash ^= blocks.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof AmstradTapeFile))
            return false;

        AmstradTapeFile other = (AmstradTapeFile) o;
        if ((blocks != null) && (!blocks.equals(other.blocks)))
            return false;

        return true;
    }
}
