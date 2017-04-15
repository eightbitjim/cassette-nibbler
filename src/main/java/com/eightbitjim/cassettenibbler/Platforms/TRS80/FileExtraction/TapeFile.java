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
import com.eightbitjim.cassettenibbler.Platforms.TRS80.Formats.BasicProgram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TapeFile extends com.eightbitjim.cassettenibbler.TapeFile {
    List<TapeBlock> blocks;
    private boolean atLeastOneError;
    public enum FileType { DRAGON32, TRS80 }
    private FileType fileType;

    public TapeFile(FileType fileType) {
        blocks = new LinkedList<>();
        atLeastOneError = false;
        this.fileType = fileType;
    }

    public void hasAnError() {
        atLeastOneError = true;
    }

    public void addBlock(TapeBlock block) {
        blocks.add(block);
    }

    public boolean moreBlocksNeeded() {
        if (blocks.size() < 2)
            return true;

        return blocks.get(blocks.size() - 1).getType() != TapeBlock.BlockType.END_OF_FILE;
    }

    @Override
    public int length() {
        int size = 0;
        boolean firstBlock = true;
        for (TapeBlock block : blocks) {
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
        if (headerBlockDescribesFileOfType() == TapeBlock.FileDataType.MACHINE_CODE)
            filename.append(".").append(blocks.get(0).getLoadAddressIfThisIsMachineCode());

        return filename.toString();
    }

    @Override
    public byte[] getDataBytesOfType(FormatType formatType) {
        byte [] data = null;
        switch (formatType) {
            case EMULATOR:
                data = getEmulatorFileData();
                break;

            case BINARY:
                data = getBinaryData();
                break;

            case READABLE:
                data = getASCIIData();
                break;
        }

        return data;
    }

    private byte [] getEmulatorFileData() {
        List <Byte> data = new ArrayList<>();
        for (TapeBlock block : blocks) {
            data.addAll(block.getEmulatorDataAsList());
        }

        return convertToByteArray(data);
    }

    private byte [] getBinaryData() {
        List <Byte> data = new ArrayList<>();
        for (TapeBlock block : blocks) {
            switch (block.getType()) {
                case DATA:
                case UNKNOWN:
                    data.addAll(block.getPayloadDataAsList());
                    break;
                case END_OF_FILE:
                case NAMEFILE:
                    break;
            }
        }

        return convertToByteArray(data);
    }

    private byte [] getASCIIData() {
        switch(headerBlockDescribesFileOfType()) {
            case BASIC:
                return getASCIIFromBasicProgram();

            default:
            case DATA:
            case MACHINE_CODE:
            case UNKNOWN:
                return convertBinaryDataToASCII();
        }
    }

    private byte [] getASCIIFromBasicProgram() {
        BasicProgram.ProgramType programType;
        if (ASCIIFlagIsSet()) {
            return convertBinaryDataToASCII();
        }

        switch (fileType) {
            case DRAGON32:
                programType = BasicProgram.ProgramType.DRAGON;
                break;
            case TRS80:
            default:
                programType = BasicProgram.ProgramType.COCO;
                break;
        }

        BasicProgram basicProgram = new BasicProgram(getBinaryData(), programType);
        return basicProgram.toString().getBytes();
    }

    private boolean ASCIIFlagIsSet() {
        if (blocks.size() < 1)
            return false;

        return blocks.get(0).ASCIIFlagIsSet();
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
                return getExtension() + ".bin";
            case EMULATOR:
                return getExtension() + ".CAS";
            case READABLE:
                return getExtension() + ".txt";
        }
    }

    @Override
    public String getExtension() {
        switch (fileType) {
            case DRAGON32:
                return "dragon32";
            case TRS80:
            default:
                return "trs80";
        }
    }

    @Override
    public boolean containsErrors() {
        if (atLeastOneError)
            return true;

        for (TapeBlock block : blocks)
            if (block.blockHasErrors())
                return true;

        return false;
    }

    private TapeBlock.FileDataType headerBlockDescribesFileOfType() {
        if (blocks.size() < 1)
            return TapeBlock.FileDataType.UNKNOWN;
        else
            return blocks.get(0).getFileDataTypeIfthisIsNameFile();
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

        if (!(o instanceof TapeFile))
            return false;

        if (fileType != ((TapeFile) o).fileType)
            return false;

        TapeFile other = (TapeFile) o;
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
