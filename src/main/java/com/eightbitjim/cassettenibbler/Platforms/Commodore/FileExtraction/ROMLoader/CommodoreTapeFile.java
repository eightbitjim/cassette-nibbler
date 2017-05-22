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

import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.CommodoreBasicProgram;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.PETSCII;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CommodoreTapeFile extends TapeFile {
    private List<CommodoreFileBlock> blocks = new LinkedList<>();
    public enum Type {PRG, NRP, SEQ, UNKNOWN, ORPHAN_DATA_BLOCK, ORPHAN_SEQ_BLOCK}

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private static final int C64_PROGRAM_START_ADDRESS = 2049;
    private static final String C64_FILE_EXTENSION = "c64";
    private static final int C128_PROGRAM_START_ADDRESS = 7169;
    private static final String C128_FILE_EXTENSION = "c128";
    private static final int VIC20_PROGRAM_START_ADDRESS = 0x4097;
    private static final int VIC20_PROGRAM_START_ADDRESS2 = 0x4609;
    private static final String VIC20_FILE_EXTENSION = "vic20";

    public static final int NO_LOAD_ADDRESS = -1;
    private boolean errorInFile;

    public CommodoreTapeFile(String defaultExtension) { }

    public void addBlock(CommodoreFileBlock block) {
        blocks.add(block);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (CommodoreFileBlock block : blocks)
            hash ^= block.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof CommodoreTapeFile))
            return false;

        CommodoreTapeFile other = (CommodoreTapeFile) o;
        if ((blocks != null) && (!blocks.equals(other.blocks)))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public int length() {
        int [] fileData = getDataAsInts();
        if (fileData == null)
            return 0;
        else
            return fileData.length;
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();
        String nameFromBlocks;
        if (blocks.isEmpty())
            nameFromBlocks = "empty";
        else
            nameFromBlocks = blocks.get(0).getFilenameString();

        if (nameFromBlocks == null || nameFromBlocks.length() == 0)
            nameFromBlocks = "unnamed";

        filenameBuilder.append(nameFromBlocks);
        filenameBuilder.append(".").append(getType().toString().toLowerCase());
        if (getType() == Type.NRP)
            filenameBuilder.append(".").append(getLoadAddress());

        filenameBuilder.append(".").append(getExtension());
        return filenameBuilder.toString();
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getASCIIFromFile();
            case EMULATOR:
                return getPRGFromFile();
            case BINARY:
            default:
                return getDataAsBytesWithNoLoadAddress();
        }
    }

    protected byte[] getDataAsBytes() {
        if (blocks.isEmpty())
            return null;

        byte [] dataBytes = new byte[length()];
        int [] originalData = getDataAsInts();
        for (int i = 0; i < length(); i++) {
            dataBytes[i] = (byte)originalData[i];
        }

        return dataBytes;
    }

    protected byte [] getDataAsBytesWithNoLoadAddress() {
        int [] originalData = getDataAsIntsWithNoLoadAddress();
        if (originalData == null)
            return null;

        byte [] data = new byte[originalData.length];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte)originalData[i];

        return data;
    }

    protected int [] getDataAsIntsWithNoLoadAddress() {
        switch (getType()) {
            case UNKNOWN:
                return null;
            case NRP:
            case PRG:
                return getDataWithNoLoadAddress();
            case SEQ:
                return getSequentialFileData();
            case ORPHAN_DATA_BLOCK:
            case ORPHAN_SEQ_BLOCK:
                return getOrphanBlockContents();
            default:
                return null;
        }
    }

    protected byte [] getASCIIFromFile() {
        switch (getType()) {
            case PRG:
            case NRP:
                return getASCIIFromProgram();

            case SEQ:
                return getASCIIFromSequentialFile();

            case ORPHAN_DATA_BLOCK:
            case ORPHAN_SEQ_BLOCK:
            case UNKNOWN:
            default:
                return getDataAsBytes();
        }
    }

    protected byte [] getPRGFromFile() {
        if (blocks.isEmpty())
            return null;

        return getDataAsBytes();
    }

    protected byte [] getASCIIFromProgram() {
        return new CommodoreBasicProgram(getDataAsBytes()).toString().getBytes();
    }

    protected byte [] getASCIIFromSequentialFile() {
        return PETSCII.printableStringFromPetsciiBytes(getDataAsBytes()).getBytes();
    }

    @Override
    public String getExtension() {
        int loadAddress = getLoadAddress();
        switch (loadAddress) {
            case C64_PROGRAM_START_ADDRESS:
                return C64_FILE_EXTENSION;
            case C128_PROGRAM_START_ADDRESS:
                return C128_FILE_EXTENSION;
            case VIC20_PROGRAM_START_ADDRESS:
            case VIC20_PROGRAM_START_ADDRESS2:
                return VIC20_FILE_EXTENSION;
            default:
            case NO_LOAD_ADDRESS:
                return "ROMloader";
        }
    }

    @Override
    public boolean containsErrors() {
        return errorInFile;
    }

    public void hasError() {
        errorInFile = true;
    }

    public Type getType() {
        if (blocks.isEmpty())
            return Type.UNKNOWN;

        CommodoreFileBlock firstBlock = blocks.get(0);
        switch (firstBlock.getFileType()) {
            case CommodoreFileBlock.DATA:
                return Type.ORPHAN_DATA_BLOCK;
            case CommodoreFileBlock.NON_RELOCATABLE_PROGRAM_HEADER:
                return Type.NRP;
            case CommodoreFileBlock.PROGRAM_HEADER:
                return Type.PRG;
            case CommodoreFileBlock.SEQENTIAL_FILE_HEADER:
                return Type.SEQ;
            case CommodoreFileBlock.SEQENTIAL_FILE_DATA:
                return Type.ORPHAN_SEQ_BLOCK;
            default:
                return Type.UNKNOWN;
        }
    }

    private int [] getDataAsInts() {
        switch (getType()) {
            case UNKNOWN:
                return null;
            case NRP:
            case PRG:
                return getDataWithLoadAddress();
            case SEQ:
                return getSequentialFileData();
            case ORPHAN_DATA_BLOCK:
            case ORPHAN_SEQ_BLOCK:
                return getOrphanBlockContents();
            default:
                return null;
        }
    }

    private int [] getSequentialFileData() {
        int numberOfDataBlocks = blocks.size() - 1;
        List <Integer> dataConstruction = new LinkedList<>();

        for (int i = 1; i <= numberOfDataBlocks; i++) {
            CommodoreFileBlock block = blocks.get(i);
            int [] blockData = block.getData();
            for (int j = 1; j < block.getBlockLength() - 1; j++) {
                if (i == numberOfDataBlocks && blockData[j] == 0)
                    break;

                dataConstruction.add(blockData[j]);
            }
        }

        int [] dataToReturn = new int[dataConstruction.size()];
        ListIterator iter = dataConstruction.listIterator();
        for (int i = 0; i < dataToReturn.length; i++)
            dataToReturn[i] = (int)iter.next();

        return dataToReturn;
    }

    private int [] getOrphanBlockContents() {
        CommodoreFileBlock data = blocks.get(0);
        if (data == null)
            return null;
        else
            return data.getData();
    }

    private int [] getDataWithLoadAddress() {
        if (blocks.size() < 2)
            return new int[0];

        CommodoreFileBlock header = blocks.get(0);
        CommodoreFileBlock data = blocks.get(1);
        if (data == null)
            return null;

        int [] originalData = data.getData();
        int [] fileData = new int[data.getBlockLength() + 2];
        fileData[0] = header.getStartAddress() % 256;
        fileData[1] = header.getStartAddress() / 256;

        for (int i = 0; i < data.getBlockLength(); i++)
            fileData[i + 2] = originalData[i];

        return fileData;
    }

    private int [] getDataWithNoLoadAddress() {
        if (blocks.size() < 2)
            return new int[0];

        CommodoreFileBlock header = blocks.get(0);
        CommodoreFileBlock data = blocks.get(1);
        if (data == null)
            return null;

        int [] originalData = data.getData();
        int [] fileData = new int[data.getBlockLength()];

        for (int i = 0; i < data.getBlockLength(); i++)
            fileData[i] = originalData[i];

        return fileData;
    }

    public int getLoadAddress() {
        if (blocks.size() < 1)
            return 0;

        CommodoreFileBlock header = blocks.get(0);
        switch (getType()) {
            case PRG:
            case NRP:
                return header.getStartAddress();
            default:
                return NO_LOAD_ADDRESS;
        }
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "commodore.bin";
            case EMULATOR:
                return "commodore.PRG";
            case READABLE:
                return "commodore.txt";
        }
    }
}
