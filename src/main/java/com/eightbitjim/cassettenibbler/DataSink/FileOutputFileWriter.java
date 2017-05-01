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

import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputFileWriter implements FileStreamConsumer {
    private String directoryPath;
    private boolean outputFilesWithErrors;
    private TapeFile.FormatType formatType;

    public FileOutputFileWriter(String directory, boolean outputFilesWithErrors) {
        this.directoryPath = directory;
        this.outputFilesWithErrors = outputFilesWithErrors;
        this.formatType = null; // Output all types
    }

    public FileOutputFileWriter setOutputFileType(TapeFile.FormatType formatType) {
        this.formatType = formatType;
        return this;
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        if (outputFilesWithErrors || !file.containsErrors())
            outputSingleFile(file);
    }

    private void outputSingleFile(TapeFile file) {
        if (formatType != null)
            outputSingleFileToDirectoryOfType(file, formatType);
        else {
            for (TapeFile.FormatType currentFormatType : TapeFile.FormatType.values()) {
                outputSingleFileToDirectoryOfType(file, currentFormatType);
            }
        }
    }

    private void outputSingleFileToDirectoryOfType(TapeFile file, TapeFile.FormatType formatType) {
        byte [] data = file.getDataBytesOfType(formatType);
        if (data != null) {
            try {
                String filename = file.getFilenameFor(formatType);

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(getFilePath(directoryPath, filename)));
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                System.err.println("Error writing file " + file.getFilename() + ":" + e.toString());
            }
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
