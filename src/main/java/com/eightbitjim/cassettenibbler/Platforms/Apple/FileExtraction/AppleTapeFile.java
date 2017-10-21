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

package com.eightbitjim.cassettenibbler.Platforms.Apple.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.Apple.Formats.ApplesoftBasicProgram;
import com.eightbitjim.cassettenibbler.Platforms.General.Formats.BinaryToASCII;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.Collection;
import java.util.LinkedList;

public class AppleTapeFile extends TapeFile {
    public enum Type { INTEGER_BASIC, APPLESOFT_BASIC, APPLESOFT_SHAPE_TABLE, APPLESOFT_ARRAY, UNKNOWN}
    private Type type;
    private TapeExtractionLogging logging;
    private LinkedList <AppleFileBlock> blocks;
    private boolean atLeastOneError;

    public AppleTapeFile(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        blocks = new LinkedList<>();
        type = Type.UNKNOWN;
        atLeastOneError = false;
    }

    public void addBlock(AppleFileBlock block) {
        blocks.add(block);
        processBlock();
    }

    public void isInError() {
        atLeastOneError = true;
    }

    private void processBlock() {
        if (blocks.size() == 1)
            assumeBlockIsMachineCode();
        else
            decideFileSizeBasedOnFirstBlock();
    }

    private void assumeBlockIsMachineCode() {
        type = Type.UNKNOWN;
    }

    private void decideFileSizeBasedOnFirstBlock() {
        byte [] blockData = blocks.getFirst().getRawDataAsByteArray();
        int blockLength = blockData.length;

        if (blockLength == 4) {
            logging.writeFileParsingInformation("Header was 3 bytes long plus checksum. Will assume it is AppleSoft Basic");
            type = Type.APPLESOFT_BASIC;
            return;
        }

        if (blockLength == 3) {
            logging.writeFileParsingInformation("Header was 2 bytes long plus checksum. Will assume it is Integer Basic");
            type = Type.INTEGER_BASIC;
            return;
        }

        logging.writeFileParsingInformation("Initial block was incorrect length for a header.");
        type = Type.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof AppleTapeFile))
            return false;

        return super.equals(o);
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public int length() {
        if (type == Type.UNKNOWN)
            return 0; // For time being, if we don't know the file type then we don't believe its contents.
                      // Unfortunately this will prevent loading machine code files.

        return getPayloadData().length;
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();
        filenameBuilder.append("unnamed");
        filenameBuilder.append(".").append(getExtension());
        return filenameBuilder.toString();
    }

    @Override
    public String getExtension() {
        String typeString;
        switch (type) {
            case APPLESOFT_ARRAY:
                typeString = "applesoftArray";
                break;
            case APPLESOFT_BASIC:
                typeString = "applesoftBasic";
                break;
            case APPLESOFT_SHAPE_TABLE:
                typeString = "applesoftShapeTable";
                break;
            case INTEGER_BASIC:
                typeString = "integerBasic";
                break;
            case UNKNOWN:
            default:
                typeString = "unknown";
                break;
        }

        return typeString;
    }

    @Override
    public boolean containsErrors() {
        boolean errorValue = false;
        for (AppleFileBlock block : blocks)
            errorValue |= !block.checksumIsCorrect();

        errorValue |= atLeastOneError;
        return errorValue;
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getASCIIData();
            case EMULATOR:
                return getTAPFileData();
            case BINARY:
            default:
                return getPayloadData();
        }
    }

    private byte [] getASCIIData() {
        switch (type) {
            case INTEGER_BASIC:
            case APPLESOFT_BASIC:
                return getBasicProgram();

            case APPLESOFT_ARRAY:
            case UNKNOWN:
            case APPLESOFT_SHAPE_TABLE:
            default:
                return convertBinaryToASCII();
        }
    }

    private byte [] convertBinaryToASCII() {
        return BinaryToASCII.removeUnprintableCharactersFrombinaryCharacterArray(getPayloadData());
    }

    private byte [] getPayloadData() {
        LinkedList <Byte> data = new LinkedList<>();
        switch (blocks.size()) {
            case 2: // Header and data
                data.addAll(blocks.get(1).getRawData());

            default: // Assume data is in first block
                data.addAll(blocks.get(0).getRawData());
        }

        if (data.size() > 0)
            data.removeLast(); // Remove checksum

        return convertByteListToArray(data);
    }

    private byte [] convertByteListToArray(Collection<Byte> input) {
        byte [] output = new byte[input.size()];
        int position = 0;
        for (Byte value : input)
            output[position++] = value;

        return output;
    }

    private byte [] getBasicProgram() {
        ApplesoftBasicProgram basicProgram = new ApplesoftBasicProgram(getPayloadData());
        return basicProgram.toString().getBytes();
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "apple2.bin";
            case EMULATOR:
                return "apple2.emulator";
            case READABLE:
                return "apple2.txt";
        }
    }

    private byte [] getTAPFileData() {
        return getPayloadData(); // TODO
    }
}
