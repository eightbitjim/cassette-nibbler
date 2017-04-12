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

import com.eightbitjim.cassettenibbler.Platforms.General.Formats.BinaryToASCII;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TRS80TapeFile extends TapeFile {
    List<TRS80TapeBlock> blocks;
    private boolean atLeastOneError;

    public TRS80TapeFile() {
        blocks = new LinkedList<>();
        atLeastOneError = false;
    }

    public void hasAnError() {
        atLeastOneError = true;
    }

    public void addBlock(TRS80TapeBlock block) {
        blocks.add(block);
    }

    public boolean moreBlocksNeeded() {
        if (blocks.size() < 2)
            return true;

        switch (headerBlockDescribesFileOfType()) {
            case BASIC_PROGRAM:
            case MACHINE_CODE:
            case UNKNOWN:
                return false;
            case ASCII:
                return !blocks.get(blocks.size() - 1).lastBlockIfThisIsAnASCIIFile();
        }

        return false;
    }

    @Override
    public int length() {
        int size = 0;
        boolean firstBlock = true;
        for (TRS80TapeBlock block : blocks) {
            if (!firstBlock)
                size += block.getLength();

            firstBlock = false;
        }

        if (blocks.size() == 1)
            size = blocks.get(0).getLength();

        return size;
    }

    @Override
    public String getFilename() {
        StringBuilder filename = new StringBuilder();
        if (blocks.size() < 1)
            filename.append("unnamed");
        else
            filename.append(blocks.get(0).getFilename().trim());

        filename.append(".").append(headerBlockDescribesFileOfType().toString().toLowerCase());
        if (headerBlockDescribesFileOfType() == TRS80TapeBlock.BlockType.MACHINE_CODE)
            filename.append(".").append(blocks.get(0).getLoadAddressIfThisIsMachineCode());

        return filename.toString();
    }

    @Override
    public byte[] getDataBytesOfType(FormatType formatType) {
        byte [] data = null;
        switch (formatType) {
            case EMULATOR:
            case BINARY:
                data = getBinaryData();
                break;

            case READABLE:
                data = getASCIIData();
                break;
        }

        return data;
    }

    private byte [] getBinaryData() {
        List <Byte> data = new ArrayList<>();
        boolean firstBlock = true;
        if (blocks.size() == 1)
            firstBlock = false;

        for (TRS80TapeBlock block : blocks) {
            if (!firstBlock)
                data.addAll(block.getDataAsList());

            firstBlock = false;
        }

        return convertToByteArray(data);
    }

    private byte [] getASCIIData() {
        switch(headerBlockDescribesFileOfType()) {
            case ASCII:
                return getBinaryData();

            case BASIC_PROGRAM:
                return getASCIIFromBasicProgram();

            case HEADER:
            case MACHINE_CODE:
            case UNKNOWN:
            default:
                return convertBinaryDataToASCII();
        }
    }

    private byte [] getASCIIFromBasicProgram() {
        // TODO: implement MSX basic program decoding
        // For the time being, just convert binary
        return convertBinaryDataToASCII();
    }

    private byte [] convertBinaryDataToASCII() {
        return BinaryToASCII.convert(getBinaryData());
    }

    private byte [] convertToByteArray(List <Byte> list) {
        byte [] data = new byte[list.size()];
        int position = 0;
        for (Byte b : list) {
            data[position] = b;
            position++;
        }

        return data;
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "msx.bin";
            case EMULATOR:
                return "msx.emulator";
            case READABLE:
                return "msx.txt";
        }
    }

    @Override
    public String getExtension() {
        return null;
    }

    @Override
    public boolean containsErrors() {
        return atLeastOneError;
    }

    public boolean isOrphanHeader() {
        return headerBlockDescribesFileOfType() == TRS80TapeBlock.BlockType.UNKNOWN;
    }

    private TRS80TapeBlock.BlockType headerBlockDescribesFileOfType() {
        if (blocks.size() < 1)
            return TRS80TapeBlock.BlockType.UNKNOWN;
        else
            return blocks.get(0).getTypeOfNextBlockIfThisIsAHeader();
    }

    public TRS80TapeBlock.BlockType nextBlockType() {
        if (blocks.size() < 1)
            return TRS80TapeBlock.BlockType.HEADER;
        else
            return headerBlockDescribesFileOfType();
    }

    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append(getFilename());

        if (length() > 0)
            out.append(": Data length:" + length());
        else
            out.append(": NO DATA");

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof TRS80TapeFile))
            return false;

        TRS80TapeFile other = (TRS80TapeFile) o;
        if (getFilename() != null && !getFilename().equals(other.getFilename()))
            return false;

        byte [] data = getDataBytesOfType(FormatType.BINARY);
        if (data != null && !Arrays.equals(data, other.getDataBytesOfType(FormatType.BINARY)))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 0;

        byte [] data = getDataBytesOfType(FormatType.BINARY);
        if (data != null)
            code ^= data.hashCode();

        String filename = getFilename();
        if (filename != null)
            code ^= filename.hashCode();

        return code;
    }
}
