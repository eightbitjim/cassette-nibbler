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

import com.eightbitjim.cassettenibbler.Platforms.Acorn.Formats.BBCBasicProgram;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.Formats.BBCBasicVariableData;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.General.Formats.BinaryToASCII;
import com.eightbitjim.cassettenibbler.Utilities.PrintableString;

import java.util.LinkedList;
import java.util.List;

public class BBCTapeFile extends GenericTapeFile {
    List<BBCFileBlock> blocks = new LinkedList<>();
    public enum Type { BASIC, VARIABLES, BYTES, UNKNOWN }

    public static final int VARIABLES_LOAD_ADDRESS = 65535;
    public static final int BASIC_EXECUTION_ADDRESS = 32803;
    public static final int VARIABLES_EXECUTION_ADDRESS = 65535;

    public static final int NO_LOAD_ADDRESS = -1;
    public static final int NO_EXECUTION_ADDRESS = -1;

    private Type type = Type.UNKNOWN;

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof BBCTapeFile))
            return false;

        BBCTapeFile other = (BBCTapeFile)o;
        if (!blocks.equals(other.blocks))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (blocks != null) {
            for (BBCFileBlock block : blocks)
                hash ^= block.hashCode();
        }

        return hash;
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();
        String nameFromBlocks;
        if (blocks.isEmpty())
            nameFromBlocks = "EMPTY";
        else
            nameFromBlocks = blocks.get(0).getFilename();

        if (nameFromBlocks == null || nameFromBlocks.length() == 0)
            nameFromBlocks = "UNNAMED";

        filenameBuilder.append(nameFromBlocks);
        filenameBuilder.append(".").append(getType().toString().toLowerCase());
        if (type == Type.BYTES) {
            filenameBuilder.append(".").append(getLoadAddress());
            filenameBuilder.append(".").append(getExecutionAddress());
        }

        return filenameBuilder.toString();
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getReadableData();
            case EMULATOR:
                // TODO -- implement this
                // Fall through
            default:
                return super.getDataBytesOfType(formatType);
        }
    }

    private byte [] getReadableData() {
        switch (getType()) {
            case BASIC:
                return new BBCBasicProgram(getRawData()).toString().getBytes();
            case VARIABLES:
                return new BBCBasicVariableData(getRawData()).toString().getBytes();
            case UNKNOWN:
            case BYTES:
            default:
                return BinaryToASCII.removeUnprintableCharactersFrombinaryCharacterArray(getRawData());
        }
    }

    public void addBlock(BBCFileBlock block) {
        blocks.add(block);
    }

    public void setName(String filename) {
        this.filename = filename;
    }

    public void setData(int [] data) {
        this.data = data;
    }

    public int getLoadAddress() {
        if (blocks.size() < 1)
            return NO_LOAD_ADDRESS;
        else
            return blocks.get(0).getLoadAddress();
    }

    public int getExecutionAddress() {
        if (blocks.size() < 1)
            return NO_EXECUTION_ADDRESS;
        else
            return blocks.get(0).getExecutionAddress();
    }

    public Type getType() {
        computeType();
        return type;
    }

    private void computeType() {
        if (getExecutionAddress() == BASIC_EXECUTION_ADDRESS)
            type = Type.BASIC;
        else if (getExecutionAddress() == VARIABLES_EXECUTION_ADDRESS)
            type = Type.VARIABLES;
        else
            type = Type.BYTES;
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "acorn.bin";
            case EMULATOR:
                return "acorn.emulator";
            case READABLE:
                return "acorn.txt";
        }
    }
}
