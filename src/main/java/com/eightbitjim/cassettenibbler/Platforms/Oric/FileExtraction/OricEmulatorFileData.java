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

package com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction;

import java.util.ArrayList;
import java.util.List;

public class OricEmulatorFileData {
    private static final byte LEADER_BYTE_VALUE = 0x68;
    private static final byte SYNC_BYTE_VALUE = 0x16;
    private static final byte START_BYTE_VALUE = 0x24;
    private static final int NUMBER_OF_LEADER_BYTES = 0x80;
    private static final int NUMBER_OF_SYNC_BYTES = 0x3;


    List<Byte> fileData;
    public OricEmulatorFileData(byte [] headerData, byte [] rawFileData) {
        fileData = new ArrayList<>();
        if (headerData == null || rawFileData == null)
            return;
      
        addSyncBytes();
        addStartByte();
        addFileData(headerData);
        addFileData(rawFileData);
    }

    private void addLeaderBuffer() {
        for (int i = 0; i < NUMBER_OF_LEADER_BYTES; i++)
            fileData.add(LEADER_BYTE_VALUE);
    }

    private void addSyncBytes() {
        for (int i = 0; i < NUMBER_OF_SYNC_BYTES; i++)
            fileData.add(SYNC_BYTE_VALUE);
    }

    private void addStartByte() {
        fileData.add(START_BYTE_VALUE);
    }

    private void addFileData(byte [] data) {
        for (byte b : data) {
            fileData.add(b);
        }
    }

    public byte [] getOricTAPFileData() {
        return convertToByteArray(fileData);
    }

    private byte [] convertToByteArray(List <Byte> list) {
        byte [] data = new byte[list.size()];
        int position = 0;
        for (Byte b: list) {
            data[position] = b;
            position++;
        }

        return data;
    }
}
