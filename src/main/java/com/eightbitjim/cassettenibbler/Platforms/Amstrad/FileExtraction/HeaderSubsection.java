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

public class HeaderSubsection extends FileSubsection {

    private static final int HEADER_DATA_SIZE = 0x1c;

    public enum FileType {
        BASIC,
        ENCRYPTED_BASIC,
        BINARY,
        ASCII
    }

    private static final int FILE_TYPE_BASIC = 0x00;
    private static final int FILE_TYPE_ENCTYPTED_BASIC = 0x01;
    private static final int FILE_TYPE_BINARY = 0x02;
    private static final int FILE_TYPE_ASCII = 0x16;

    private static final int BLOCK_NUMBER_POSITION = 0x10;
    private static final int LAST_BLOCK_FLAG_POSITION = 0x11;
    private static final int FILE_TYPE_POSITION = 0x12;
    private static final int BLOCK_LENGTH_POSITION = 0x13;
    private static final int BLOCK_LOAD_ADDRESS_POSITION = 0x15;
    private static final int FIRST_BLOCK_FLAG_POSITION = 0x17;
    private static final int TOTAL_FILE_LENGTH_POSITION = 0x18;
    private static final int START_ADDRESS_POSITION = 0x1a;
    private static final int FILENAME_LENGTH = 0x10;

    public HeaderSubsection() {
        super();
    }

    private boolean headerIsValid() {
        return data.size() >= HEADER_DATA_SIZE;
    }


    private int getByteValueAt(int position) {
        return Byte.toUnsignedInt(data.get(position));
    }

    private int getTwoByteValueAt(int position) {
        return getByteValueAt(position) + getByteValueAt(position + 1) * 0x100;
    }

    public String getFilename() {
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < FILENAME_LENGTH; i++) {
            char c = (char)getByteValueAt(i);
            if (c == 0)
                break;
            else
                filename.append(c);
        }

        return filename.toString();
    }

    public FileType getFileType() {
        int fileTypeValue = getByteValueAt(FILE_TYPE_POSITION);
        switch (fileTypeValue) {
            case FILE_TYPE_BASIC:
                return FileType.BASIC;
            case FILE_TYPE_ENCTYPTED_BASIC:
                return FileType.ENCRYPTED_BASIC;
            case FILE_TYPE_BINARY:
            default:
                return FileType.BINARY;
            case FILE_TYPE_ASCII:
                return FileType.ASCII;
        }
    }

    public int getBlockNumber() {
        return getByteValueAt(BLOCK_NUMBER_POSITION);
    }

    public boolean isLastBLock() {
        return getByteValueAt(LAST_BLOCK_FLAG_POSITION) != 0x00;
    }

    public int getBlockLengthInBytes() {
        return getTwoByteValueAt(BLOCK_LENGTH_POSITION);
    }

    public int getBlockLoadAddress() {
        return getTwoByteValueAt(BLOCK_LOAD_ADDRESS_POSITION);
    }

    public boolean isFirstBLock() {
        return getByteValueAt(FIRST_BLOCK_FLAG_POSITION) != 0x00;
    }

    public int getTotalFileLength() {
        return getTwoByteValueAt(TOTAL_FILE_LENGTH_POSITION);
    }

    public int getProgramStartAddress() {
        return getTwoByteValueAt(START_ADDRESS_POSITION);
    }
}
