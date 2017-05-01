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
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.*;

public class Directory implements FileStreamConsumer, FileStreamProvider {
    private List<TapeFile> files;

    public Directory() {
        files = new LinkedList<>();
    }

    private List <FileStreamConsumer> consumers = new LinkedList<>();

    public void addFile(TapeFile file) {
        if (file != null && !files.contains(file))
            files.add(file);
    }

    public void removeFile(TapeFile file) {
         files.remove(file);
    }

    public List <TapeFile> getList() {
        return files;
    }

    @Override
    public String toString() {
        StringBuffer directoryString = new StringBuffer();
        if (files.size() == 0) {
            directoryString.append("No files found");
        }

        int count = 0;
        for (TapeFile file : files) {
            directoryString.append(count++);
            directoryString.append(" ");
            directoryString.append(file.getFilenameFor(TapeFile.FormatType.BINARY)+ ": " + file.length() + " bytes ");
            directoryString.append("\n");
        }

        return directoryString.toString();
    }

    public TapeFile getNumberedFileOrNull(int n) {
        if (n < 0 || n > files.size() - 1)
            return null;

        return files.get(n);
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        if (file != null)
            addFile(file);

        pushFileToConsumers(file, currentTimeIndex);
    }

    @Override
    public void registerFileStreamConsumer(FileStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterFileStreamConsumer(FileStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    private void pushFileToConsumers(TapeFile file, long currentTimeIndex) {
        for (FileStreamConsumer consumer : consumers)
            consumer.pushFile(file, currentTimeIndex);
    }
}
