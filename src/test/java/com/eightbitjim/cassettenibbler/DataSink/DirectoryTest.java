/*
 * Copyright (C) 2017 James Lean.
 *
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
import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;
import com.eightbitjim.cassettenibbler.TapeFile;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectoryTest implements FileStreamConsumer {

    private static final String FILE1_NAME = "file1";
    private static final String FILE2_NAME = "file2";
    private static final String FILE3_NAME = "file3";

    private static final String FILE1_TYPE = "bin";
    private static final String FILE2_TYPE = "bin";
    private static final String FILE3_TYPE = null;

    private static final long FILE1_TIME = 1234L;
    private static final long FILE2_TIME = 1235L;
    private static final long FILE3_TIME = -1435345L;


    private List <TapeFile> fileList;
    private List <Long> timeIndexList;

    @Before
    public void setupDirectoryTests() {

    }

    private TapeFile getDummyFile1() {
        GenericTapeFile file = new GenericTapeFile();
        file.filename = FILE1_NAME;
        file.type = FILE1_TYPE;
        file.data = new int [] {1, 2, 3, 4};
        return file;
    }

    private TapeFile getDummyFile2() {
        GenericTapeFile file = new GenericTapeFile();
        file.filename = FILE2_NAME;
        file.type = FILE2_TYPE;
        file.data = new int [] {5};
        return file;
    }

    private TapeFile getDummyFile3() {
        GenericTapeFile file = new GenericTapeFile();
        file.filename = FILE3_NAME;
        file.data = null;
        return file;
    }

    @Test
    public void testEmptyDirectory() throws Throwable {
        Directory directory = new Directory();
        testEmptyDirectory(directory);

        directory.addFile(getDummyFile1());
        TapeFile getBack = directory.getNumberedFileOrNull(0);

    }

    @Test
    public void testNoSink() throws Throwable {
        Directory directory = new Directory();
        testEmptyDirectory(directory);
        testOnefile(directory);
        testOneFileTwice(directory);
        testTwoFiles(directory);
        testEmptyFile(directory);
    }

    private void testEmptyDirectory(Directory directory) throws Throwable {
        assertTrue(directory.getNumberedFileOrNull(0) == null);
        assertTrue(directory.getNumberedFileOrNull(-1) == null);
        assertTrue(directory.getNumberedFileOrNull(Integer.MAX_VALUE) == null);
        assertTrue(directory.getNumberedFileOrNull(Integer.MIN_VALUE) == null);

        List<TapeFile> files = directory.getList();
        assertTrue(files != null);
        assertTrue(files.size() == 0);
        assertTrue(files.toString() != null);
        assertTrue(files.toString().length() != 0);
    }

    private void testOnefile(Directory directory) throws Throwable {
        assertTrue(directory.getList().size() == 0);

        directory.addFile(getDummyFile1());
        assertTrue(directory.getList().size() == 1);

        assertTrue(directory.getNumberedFileOrNull(-1) == null);
        assertTrue(directory.getNumberedFileOrNull(1) == null);

        TapeFile file = directory.getNumberedFileOrNull(0);
        assertTrue(file != null);
        assertTrue(file.getFilename().equals(FILE1_NAME + "." + FILE1_TYPE));
        assertTrue(file.getExtension().equals(FILE1_TYPE));
        assertTrue(file.equals(getDummyFile1()));
        assertFalse(file.equals(getDummyFile2()));

        directory.removeFile(null);
        assertTrue(directory.getList().size() == 1);

        directory.removeFile(getDummyFile2());
        assertTrue(directory.getList().size() == 1);

        directory.removeFile(directory.getNumberedFileOrNull(0));
        assertTrue(directory.getList().size() == 0);
    }

    private void testOneFileTwice(Directory directory) throws Throwable {
        testOnefile(directory);
        testOnefile(directory);
    }

    private void testTwoFiles(Directory directory) throws Throwable {
        assertTrue(directory.getList().size() == 0);

        directory.addFile(getDummyFile1());
        assertTrue(directory.getList().size() == 1);

        directory.addFile(getDummyFile2());
        assertTrue(directory.getList().size() == 2);

        assertTrue(directory.getNumberedFileOrNull(-1) == null);
        assertTrue(directory.getNumberedFileOrNull(2) == null);

        TapeFile file = directory.getNumberedFileOrNull(0);
        assertTrue(file != null);
        assertTrue(file.equals(getDummyFile1()));
        assertFalse(file.equals(getDummyFile2()));

        file = directory.getNumberedFileOrNull(1);
        assertTrue(file != null);
        assertTrue(file.equals(getDummyFile2()));
        assertFalse(file.equals(getDummyFile1()));

        directory.removeFile(null);
        assertTrue(directory.getList().size() == 2);

        directory.removeFile(getDummyFile3());
        assertTrue(directory.getList().size() == 2);

        directory.removeFile(directory.getNumberedFileOrNull(0));
        assertTrue(directory.getList().size() == 1);
        assertTrue(directory.getNumberedFileOrNull(0).equals(getDummyFile2()));
        assertFalse(directory.getNumberedFileOrNull(0).equals(getDummyFile1()));

        directory.removeFile(directory.getNumberedFileOrNull(0));
        assertTrue(directory.getList().size() == 0);
    }

    private void testEmptyFile(Directory directory) throws Throwable {
        assertTrue(directory.getList().size() == 0);

        directory.addFile(getDummyFile3());
        assertTrue(directory.getList().size() == 1);

        assertTrue(directory.getNumberedFileOrNull(-1) == null);
        assertTrue(directory.getNumberedFileOrNull(1) == null);

        TapeFile file = directory.getNumberedFileOrNull(0);
        assertTrue(file != null);
        assertTrue(file.getFilename().equals(FILE3_NAME));
        assertTrue(file.getExtension().equals("general"));
        assertTrue(file.equals(getDummyFile3()));
        assertFalse(file.equals(getDummyFile2()));

        directory.removeFile(null);
        assertTrue(directory.getList().size() == 1);

        directory.removeFile(getDummyFile2());
        assertTrue(directory.getList().size() == 1);

        directory.removeFile(directory.getNumberedFileOrNull(0));
        assertTrue(directory.getList().size() == 0);
    }

    @Test
    public void testFileStreamConsumer() {
        Directory directory = new Directory();
        assertTrue(directory.getList().size() == 0);

        directory.pushFile(getDummyFile1(), FILE1_TIME);
        assertTrue(directory.getList().size() == 1);

        directory.pushFile(getDummyFile2(), FILE2_TIME);
        assertTrue(directory.getList().size() == 2);

        assertTrue(directory.getNumberedFileOrNull(0).equals(getDummyFile1()));
        assertTrue(directory.getNumberedFileOrNull(1).equals(getDummyFile2()));
    }

    @Test
    public void testFileStreamProvider() {
        Directory directory = new Directory();
        directory.registerFileStreamConsumer(this);

        fileList = new LinkedList<>();
        timeIndexList = new LinkedList<>();

        assertTrue(directory.getList().size() == 0);

        directory.pushFile(getDummyFile1(), FILE1_TIME);
        assertTrue(directory.getList().size() == 1);

        directory.pushFile(getDummyFile2(), FILE2_TIME);
        assertTrue(directory.getList().size() == 2);

        assertTrue(fileList.size() == 2);
        assertTrue(timeIndexList.size() == 2);

        assertTrue(fileList.get(0).equals(getDummyFile1()));
        assertTrue(fileList.get(1).equals(getDummyFile2()));
    }

    @Test
    public void testDeregister() {
        Directory directory = new Directory();
        directory.registerFileStreamConsumer(this);

        fileList = new LinkedList<>();
        timeIndexList = new LinkedList<>();

        assertTrue(directory.getList().size() == 0);

        directory.pushFile(getDummyFile1(), FILE1_TIME);
        assertTrue(directory.getList().size() == 1);

        directory.deregisterFileStreamConsumer(this);

        directory.pushFile(getDummyFile2(), FILE2_TIME);
        assertTrue(directory.getList().size() == 2);

        assertTrue(fileList.size() == 1);
        assertTrue(timeIndexList.size() == 1);
        assertTrue(fileList.get(0).equals(getDummyFile1()));
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        fileList.add(file);
        timeIndexList.add(currentTimeIndex);
    }

}
