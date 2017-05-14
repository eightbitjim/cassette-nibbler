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

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AtariTapeBlock {

    public enum BlockType { FULL_DATA_RECORD, LAST_DATA_RECORD, EOF, UNKNOWN }
    private BlockType type;
    private List<Byte> data;
    private transient int bytesReceived;
    private transient int checksum;
    private boolean errors;

    private static final int BLOCK_DATA_SIZE = 128 + 2;

    private static final byte MARKER_BYTE_VALUE = 0x55;
    private static final int FULL_DATA_RECORD_CONTROL_BYTE = 0xfc;
    private static final int LAST_DATA_RECORD_CONTROL_BYTE = 0xfa;
    private static final int EOF_CONTROL_BYTE = 0xfe;

    private TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    public AtariTapeBlock() {
        data = new ArrayList<>();
        this.type = BlockType.UNKNOWN;
        bytesReceived = 0;
        initialiseChecksum();
        errors = false;
    }

    private void initialiseChecksum() {
        checksum = 0;
        addToChecksum(MARKER_BYTE_VALUE);
        addToChecksum(MARKER_BYTE_VALUE);
    }

    public void addByte(byte b) {
        data.add(b);
        bytesReceived++;
        checkForControlByte();
        dealWithChecksum(b);
    }

    public boolean blockHasErrors() {
        return errors;
    }

    private void checkForControlByte() {
        if (bytesReceived != 1)
            return;

        int controlByte = getControlByte();
        switch (controlByte) {
            case FULL_DATA_RECORD_CONTROL_BYTE:
                type = BlockType.FULL_DATA_RECORD;
                break;

            case LAST_DATA_RECORD_CONTROL_BYTE:
                type = BlockType.LAST_DATA_RECORD;
                break;

            case EOF_CONTROL_BYTE:
                type = BlockType.EOF;
                break;

            default:
                type = BlockType.UNKNOWN;
                errors = true;
                break;
        }

        logging.writeFileParsingInformation("Block type " + type);
    }

    public int getControlByte() {
        if (data.size() > 0)
            return Byte.toUnsignedInt(data.get(0));
        else
            return 0;
    }

    private void dealWithChecksum(byte nextByte) {
        if (bytesReceived < BLOCK_DATA_SIZE)
            addToChecksum(nextByte);
        else
            checkChecksum(nextByte);
    }

    private void addToChecksum(byte nextByte) {
        checksum += Byte.toUnsignedInt(nextByte);
        if ((checksum & 0xff00) != 0) {
            checksum += 1;
            checksum &= 0x00ff;
        }
    }

    private void checkChecksum(byte checksumByteFromFile) {
        int checksumReceived = Byte.toUnsignedInt(checksumByteFromFile);
        logging.writeFileParsingInformation("Computed checksum " + checksum + ", received " + checksumReceived);
        if (checksumReceived != checksum) {
            logging.writeFileParsingInformation("Invalid checksum");
            errors = true;
        }
    }

    public byte [] getDataAsArray() {
        int dataPayloadSize = getdataPayloadSize();

        byte [] array = new byte[data.size()];
        int position = 0;
        int count = 0;
        for (Byte b : data) {
            if (count >= dataPayloadSize)
                break;

            if (count > 0 && count < BLOCK_DATA_SIZE - 1) {
                array[position] = b;
                position++;
            }

            count++;
        }

        return array;
    }

    public List<Byte> getDataAsList() {
        int dataPayloadSize = getdataPayloadSize();
        LinkedList <Byte> dataPayload = new LinkedList<>();

        int count = 0;
        for (Byte b : data) {
            if (count >= dataPayloadSize)
                break;

            if (count > 0 && count < BLOCK_DATA_SIZE - 1) {
                dataPayload.add(b);
            }

            count++;
        }

        return dataPayload;
    }

    private int getdataPayloadSize() {
        switch (type) {
            case UNKNOWN:
            case FULL_DATA_RECORD:
                return 0x80;

            case LAST_DATA_RECORD:
                if (data.size() < BLOCK_DATA_SIZE - 3)
                    return data.size() - 3;
                else
                    return Byte.toUnsignedInt(data.get(BLOCK_DATA_SIZE - 2));

            default:
            case EOF:
                return 0;
        }
    }

    public BlockType getType() {
        return type;
    }

    public int getLength() {
        if (data.size() > 2)
            return data.size() - 2;
        else
            return 0;
    }

    public String getFilename() {
        return "unnamed";
    }

    public boolean moreBytesNeeded() {
        return data.size() < BLOCK_DATA_SIZE;
    }
}
