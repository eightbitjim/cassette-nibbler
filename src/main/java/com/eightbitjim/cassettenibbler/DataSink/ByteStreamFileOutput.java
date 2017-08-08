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

package com.eightbitjim.cassettenibbler.DataSink;

import com.eightbitjim.cassettenibbler.ByteStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ByteStreamFileOutput implements ByteStreamConsumer {
    private TapeExtractionLogging logging;
    private FileOutputStream outputStream;

    public ByteStreamFileOutput(String directory, String filename, String channelName) throws FileNotFoundException {
        outputStream = new FileOutputStream(getFilePath(directory, filename));
        logging = TapeExtractionLogging.getInstance(channelName);
    }

    @Override
    public void pushByte(int valueOfByte, long currentTimeIndex, long erroneousPulsesBeforeThisByte, boolean silenceBeforeThisByte) {
        if (outputStream == null) {
            logging.writeProgramOrEnvironmentError(currentTimeIndex, "Attempt to write to byte outfile file that doesn't exist.");
            return;
        }

        if (valueOfByte == ByteStreamConsumer.END_OF_STREAM)
            return;

        try {
            outputStream.write(valueOfByte);
        } catch (IOException e) {
            logging.writeProgramOrEnvironmentError(currentTimeIndex, "Exception while writing to byte output file: " + e.toString());
        }
    }

    private String getFilePath(String directory, String filename) {
        StringBuilder pathToReturn = new StringBuilder();
        pathToReturn.append(directory);
        if (!directory.endsWith("/") && !directory.endsWith("\\"))
            pathToReturn.append("/");

        pathToReturn.append(filename);
        return pathToReturn.toString();
    }
}
