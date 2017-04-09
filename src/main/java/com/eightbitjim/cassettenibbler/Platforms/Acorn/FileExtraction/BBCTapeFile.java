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
import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;

import java.util.LinkedList;
import java.util.List;

public class BBCTapeFile extends GenericTapeFile {
    List<BBCFileBlock> blocks = new LinkedList<>();

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
    public String toString() {
        return getFilename();
    }

    @Override
    public String getFilename() {
        String filename = super.getFilename();
        return filename;
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return new BBCBasicProgram(getRawData()).toString().getBytes();
            case EMULATOR:
                // TODO -- implement this
                // Fall through
            default:
                return super.getDataBytesOfType(formatType);
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
