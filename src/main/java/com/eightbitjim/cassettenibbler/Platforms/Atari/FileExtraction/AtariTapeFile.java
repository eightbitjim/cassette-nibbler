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

package com.eightbitjim.cassettenibbler.Platforms.Atari.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.Atari.Formats.AtariBasicProgram;
import com.eightbitjim.cassettenibbler.Platforms.General.Formats.BinaryToASCII;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AtariTapeFile extends TapeFile {
    List<AtariTapeBlock> blocks;
    private boolean atLeastOneError;

    public AtariTapeFile() {
        blocks = new LinkedList<>();
        atLeastOneError = false;
    }

    public void hasAnError() {
        atLeastOneError = true;
    }

    public void addBlock(AtariTapeBlock block) {
        blocks.add(block);
    }

    private AtariTapeBlock getLastBlockReceived() {
        if (blocks.size() == 0)
            return null;

        return blocks.get(blocks.size() - 1);
    }

    public boolean moreBlocksNeeded() {
        if (blocks.size() < 2)
            return true;

        switch (getLastBlockReceived().getType()) {
            default:
            case EOF:
            case UNKNOWN:
                return false;

            case FULL_DATA_RECORD:
            case LAST_DATA_RECORD:
                return true;
        }
    }

    @Override
    public int length() {
        int size = 0;
        for (AtariTapeBlock block : blocks)
            size += block.getLength();

        return size;
    }

    @Override
    public String getFilename() {
        return "unnamed";
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
        for (AtariTapeBlock block : blocks) {
            data.addAll(block.getDataAsList());
        }

        return convertToByteArray(data);
    }

    private byte [] getASCIIData() {
        byte [] data = getBasicProgramListingIfValid();
        if (data == null)
            data = convertBinaryToASCII();

        return data;
    }

    private byte [] getBasicProgramListingIfValid() {
        AtariBasicProgram basicProgram = new AtariBasicProgram(getBinaryData());
        if (basicProgram.isValid())
            return basicProgram.toString().getBytes();
        else
            return null;
    }

    private byte [] convertBinaryToASCII() {
        return BinaryToASCII.removeUnprintableCharactersFrombinaryCharacterArray(getBinaryData());
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
                return "atari.bin";
            case EMULATOR:
                return "atari.BAS";
            case READABLE:
                return "atari.txt";
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

        if (!(o instanceof AtariTapeFile))
            return false;

        AtariTapeFile other = (AtariTapeFile) o;
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
            code ^= Arrays.hashCode(data);

        String filename = getFilename();
        if (filename != null)
            code ^= filename.hashCode();

        return code;
    }
}
