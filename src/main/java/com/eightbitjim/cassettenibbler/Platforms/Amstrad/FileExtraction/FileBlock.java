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

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileBlock {
    public enum Type { HEADER, DATA, UNKNOWN }

    private transient TapeExtractionLogging logging;
    private transient String channelName;

    List<FileSubsection> subSections;
    private transient FileSubsection currentSubsection;
    private Type blockType;
    private transient int expectedNumberOfSubsections;
    private boolean hasStructuralErrors;


    public FileBlock(int expectedNumberOfSubsections, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        logging.writeFileParsingInformation("New tape block, expecting " + expectedNumberOfSubsections + " subsections.");
        subSections = new LinkedList<>();
        this.expectedNumberOfSubsections = expectedNumberOfSubsections;
        blockType = Type.UNKNOWN;
    }

    public static FileBlock getDummyDataBlock(String channelName) {
        int dataSubsectionsInBlock = 0x8;
        int fullDataSubsectionSizeInBytes = 0x0100;

        FileBlock block = new FileBlock(dataSubsectionsInBlock, channelName);
        for (int i = 0; i < fullDataSubsectionSizeInBytes * dataSubsectionsInBlock; i++)
            block.addByte((byte)0);

        block.hasStructuralErrors = true;
        return block;
    }

    public boolean moreBytesNeeded() {
        return subSections.size() < expectedNumberOfSubsections;
    }

    public Type getBlockType() {
        return blockType;
    }

    public List<Byte> getData() {
        List <Byte> buffer = new ArrayList<>();
        for (FileSubsection subsection : subSections) {
            if (subsection instanceof DataSubsection)
                buffer.addAll(subsection.getDataAsList());
        }
        return buffer;
    }

    public int numberOfSubsections() {
        return subSections.size();
    }

    public int sizeOfDataPayloadInBytes() {
        if (numberOfSubsections() == 0)
            return 0;

        FileSubsection subsection = subSections.get(0);
        if (subsection instanceof HeaderSubsection)
            return (((HeaderSubsection) subsection)).getBlockLengthInBytes();
        else
            return 0;
    }

    public List <FileSubsection> getSubSections() {
        return subSections;
    }

    public boolean blockContainsErrors() {
        if (hasStructuralErrors)
            return true;

        for (FileSubsection subSection : subSections)
            if (!subSection.checksumIsValid())
                return true;

        return false;
    }

    public void addByte(byte value) {
        if (blockType == Type.UNKNOWN)
            getBlockTypeAndCreateSubsection(value);
        else {
            createSubsectionIfNecessary();
            addByteToSubsection(value);
        }
    }

    private void getBlockTypeAndCreateSubsection(byte blockTypeIndicatorByte) {
        currentSubsection = FileSubsection.getFileBlockOfType(blockTypeIndicatorByte);
        if (currentSubsection instanceof HeaderSubsection)
            blockType = Type.HEADER;
        else
            blockType = Type.DATA;

        if (blockType == Type.HEADER && expectedNumberOfSubsections != 1) {
            logging.writeFileParsingInformation("Looks like we weren't expecting a header block. Setting to one subsection.");
            expectedNumberOfSubsections = 1;
            hasStructuralErrors = true;
        }
    }

    private void createSubsectionIfNecessary() {
        if (currentSubsection == null)
            currentSubsection = new DataSubsection();   // It will always be a data subsection unless it is the first subsection,
                                                        // in which case it will have been created by getBlockTypeAndCreateSubsection
    }

    private void addByteToSubsection(byte value) {
        currentSubsection.addByte(value);
        completeSubsectionIfFull();
    }

    private void completeSubsectionIfFull() {
        if (currentSubsection.moreBytesNeeded())
            return;

        subSections.add(currentSubsection);
        currentSubsection = null;
    }

    @Override
    public int hashCode() {
        int hash = hasStructuralErrors ? 1 : 0;
        for (FileSubsection subsection : subSections)
            hash ^= subsection.hashCode();

        hash ^= blockType.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof FileBlock))
            return false;

        FileBlock other = (FileBlock) o;
        if ((subSections != null) && (!subSections.equals(other.subSections)))
            return false;

        if (hasStructuralErrors != other.hasStructuralErrors)
            return false;

        if (!blockType.equals(other.blockType))
            return false;

        return true;
    }
}
