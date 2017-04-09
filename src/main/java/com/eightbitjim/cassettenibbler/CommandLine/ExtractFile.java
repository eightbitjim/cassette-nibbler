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

package com.eightbitjim.cassettenibbler.CommandLine;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.DataSink.AudioFileOutput;
import com.eightbitjim.cassettenibbler.DataSink.Directory;
import com.eightbitjim.cassettenibbler.DataSource.AudioInputLibrary.AudioInput;
import com.eightbitjim.cassettenibbler.Platforms.Amstrad.AmstradPlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.Commodore.CommodorePlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.Filters.*;
import com.eightbitjim.cassettenibbler.DataSource.DummySampleSource;
import com.eightbitjim.cassettenibbler.DataSource.PulseSourceFromInputStream;
import com.eightbitjim.cassettenibbler.Platforms.Acorn.AcornPlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.MSX.MSXPlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.Oric.OricPlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.SinclairPlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.AutomaticPlatformProvider;
import com.eightbitjim.cassettenibbler.Utilities.PrintableString;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ExtractFile {

    enum InputType { WAV, PULSES }
    enum OutputDestination { DIRECTORY_LISTING, DIRECTORY, TAR, STANDARD_OUT }

    private OutputDestination outputDestination = OutputDestination.DIRECTORY;
    private TapeFile.FormatType outputFileType = null;

    private Collection<Platform> availablePlatforms = new LinkedList<>();
    private String outputDirectory = ".";

    private InputType inputType;

    private boolean error = false;
    private boolean invertWaveform = false;
    private double volumeMultiplier = 1.0;
    private boolean differentiateSignal = false;
    private boolean lowPassFilter = false;
    private double lowPassFilterCutoff = 4800.0;

    private boolean highPassFilter = false;
    private double highPassFilterCutoff = 600.0;
    private boolean disableDefaultFilters = false;

    private boolean needToDisplayHelp = false;
    private CommandLineProgressIndicator progressIndicator;
    private String configurationString;
    private List<String> inputFilenames;
    private InputStream inputStream;
    private String soundOutput;

    private Collection<Platform> chosenPlatforms;

    private Directory directory;
    private TimeCounter counter;
    private AudioInput wavSource;
    private PulseSourceFromInputStream pulseSource;
    private SampleStreamProvider connector;
    private transient TapeExtractionOptions options;

    public static final void main(String args[]) {
        ExtractFile obj = new ExtractFile();
        try {
            obj.runWithArguments(args);
        } catch (Throwable t) {
            System.err.println("Exception: " + t.toString());
            t.printStackTrace(System.err);
        }
    }

    public ExtractFile() {
        inputFilenames = new LinkedList<>();
        directory = new Directory();
        inputType = InputType.WAV;
        setUpOptions();
        preparePlatforms();
        chosenPlatforms = new LinkedList<>();
    }

    private void setUpOptions() {
        options = TapeExtractionOptions.getInstance();
        options.setLogging(TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS);
        options.setAttemptToRecoverCorruptedFiles(true).setAllowIncorrectFrameChecksums(true).setAllowIncorrectFileChecksums(true);
    }

    private void runWithArguments(String [] args) throws IOException, PlatformAccessError, UnsupportedAudioFileException {
        parseArguments(args);
        if (error)
            System.exit(10);

        if (needToDisplayHelp) {
            displayHelp();
            return;
        }

        int fileCount = 0;
        int numberOfFiles = inputFilenames.size();
        if (numberOfFiles == 0)
            numberOfFiles = 1;

        preparePlatformList();
        printIntroductionText();
        configurePlatforms();
        linkDestinationToPlatforms();

        counter = new TimeCounter();
        configureProgessIndicator();

        List <InputStream> streamList = getDataStreamList();
        for (InputStream stream : streamList) {
            fileCount++;
            updateProgressPercent(fileCount * 100 / numberOfFiles);
            inputStream = stream;
            extractFromInputStream();
            inputStream.close();
        }

        printOrGetFilesFromDirecotry();
    }

    private void updateProgressPercent(int percent) {
        if (progressIndicator == null)
            return;

        progressIndicator.setProgressPercent(percent);
    }

    private void extractFromInputStream() throws IOException, PlatformAccessError, UnsupportedAudioFileException {
        if (inputType == InputType.WAV) {
            AudioInput wavFile = new AudioInput(inputStream);
            Amplify amplifier = new Amplify(volumeMultiplier * (invertWaveform ? -1.0 : 1.0));
            wavFile.registerSampleStreamConsumer(amplifier);

            connector = amplifier;

            if (differentiateSignal) {
                Differentiate differentiate = new Differentiate();
                connector.registerSampleStreamConsumer(differentiate);
                connector = differentiate;
            }

            if (lowPassFilter) {
                LowPass lowPassFilter = new LowPass(lowPassFilterCutoff);
                connector.registerSampleStreamConsumer(lowPassFilter);
                connector = lowPassFilter;
            }

            if (highPassFilter) {
                HighPass highPassFilter = new HighPass(highPassFilterCutoff);
                connector.registerSampleStreamConsumer(highPassFilter);
                connector = highPassFilter;
            }

            wavSource = wavFile;
            wavSource.registerSampleStreamConsumer(counter);
            pulseSource = new PulseSourceFromInputStream(inputStream);

            if (soundOutput != null) {
                AudioFileOutput audioFileOutput = new AudioFileOutput(soundOutput);
                connector.registerSampleStreamConsumer(audioFileOutput);
            }

        } else if (inputType == InputType.PULSES) {
            pulseSource = new PulseSourceFromInputStream(inputStream);
            connector = new DummySampleSource();
        }

        linkSourceToPlatforms();
        runThroughSource();
    }

    private void configureProgessIndicator() {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS) {
            progressIndicator = new CommandLineProgressIndicator("Progress");
            for (Platform platform : chosenPlatforms) {
                try {
                    platform.getFileOutputPoint().registerFileStreamConsumer(progressIndicator);
                } catch (PlatformAccessError platformAccessError) {

                }
            }
        }
    }

    private void printIntroductionText() {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS) {
            printTitle();
            printOptions();
            printPlatformList();
        }
    }

    private void printTitle() {
        printDivider();
        System.err.println(Version.title);
    }

    private void printOptions() {
        printDivider();
        System.err.println(options.toString());
    }

    private void printPlatformList() {
        System.err.print("Platforms enabled: ");
        for (Platform platform : chosenPlatforms) {
            System.err.print(platform.getName());
            System.err.print(" ");
        }

        System.err.println();
    }

    private void printDivider() {
        int dividerLength = 80;
        while (dividerLength-- > 0)
            System.err.print("-");

        System.err.println();
    }

    private void configurePlatforms() {
        for (Platform platform : availablePlatforms) {
            platform.setConfigurationString(configurationString);
        }
    }

    private void addPlatformsExceptDeduction() {
        for (Platform platform : availablePlatforms) {
            if (!platform.hasHighProcessingOverhead())
                chosenPlatforms.add(platform);
        }
    }

    private void preparePlatformList() {
        if (chosenPlatforms.isEmpty()) {
            addPlatformsExceptDeduction();
        }
    }

    private void linkSourceToPlatforms() throws PlatformAccessError {
        for (Platform platform : chosenPlatforms) {
            switch (inputType) {
                case WAV:
                    linkSourceToCorrectInputPoint(platform);
                    break;
                case PULSES:
                    pulseSource.registerPulseStreamConsumer(platform.getPulseInputPoint());
                    break;
            }
        }
    }

    private void linkSourceToCorrectInputPoint(Platform platform) throws PlatformAccessError {
        if (disableDefaultFilters)
            connector.registerSampleStreamConsumer(platform.getPostFilterWaveformInputPoint());
        else
            connector.registerSampleStreamConsumer(platform.getWaveformInputPoint());
    }

    private void linkDestinationToPlatforms() throws PlatformAccessError {
        for (Platform platform : chosenPlatforms) {
            platform.getFileOutputPoint().registerFileStreamConsumer(directory);
        }
    }

    private void parseArguments(String [] args) {
        if ((args.length == 1) && (args[0].equals("-help")))
            needToDisplayHelp = true;
        else
        {
            boolean inputSourceSpecified = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-destination=listing":
                        outputDestination = OutputDestination.DIRECTORY_LISTING;
                        break;
                    case "-destination=directory":
                        outputDestination = OutputDestination.DIRECTORY;
                        break;
                    case "-destination=stdout":
                        outputDestination = OutputDestination.STANDARD_OUT;
                        break;
                    case "-output=binary":
                        outputFileType = TapeFile.FormatType.BINARY;
                        break;
                    case "-output=text":
                        outputFileType = TapeFile.FormatType.READABLE;
                        break;
                    case "-output=emulator":
                        outputFileType = TapeFile.FormatType.EMULATOR;
                        break;
                    case "-stdin":
                        inputSourceSpecified = true;
                        break;
                    case "-nofilters":
                        disableDefaultFilters = true;
                        break;
                    case "-invert":
                        invertWaveform = true;
                        break;
                    case "-input=wav":
                        inputType = inputType.WAV;
                        break;
                    case "-input=pulses":
                        inputType = inputType.PULSES;
                        break;
                    case "-differentiate":
                        differentiateSignal = true;
                        break;
                    case "-intact-files-only":
                        options.setAttemptToRecoverCorruptedFiles(false).setAllowIncorrectFrameChecksums(false).setAllowIncorrectFileChecksums(false);
                        break;
                    case "-logging=verbose":
                        options.setLogging(TapeExtractionOptions.LoggingMode.VERBOSE);
                        break;
                    case "-logging=minimal":
                        options.setLogging(TapeExtractionOptions.LoggingMode.MINIMAL);
                        break;
                    case "-logging=none":
                        options.setLogging(TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS);
                        break;
                    case "-logging=parsing":
                        options.setLogging(TapeExtractionOptions.LoggingMode.FILE_PARSING_PULSES);
                        break;
                    default:
                        if (args[i].startsWith("-config=")) {
                            configurationString = args[i].substring("-config=".length());
                            break;
                        }

                        if (args[i].startsWith("-output-directory=")) {
                            outputDirectory = args[i].substring("-output-directory=".length());
                            break;
                        }

                        if (args[i].startsWith("-sound-output=")) {
                            soundOutput = args[i].substring("-sound-output=".length());
                            break;
                        }

                        if (args[i].startsWith("-lowpass=")) {
                            lowPassFilterCutoff = Double.parseDouble((args[i].substring("-lowpass=".length())));
                            lowPassFilter = true;
                            break;
                        }

                        if (args[i].startsWith("-highpass=")) {
                            highPassFilterCutoff = Double.parseDouble((args[i].substring("-highpass=".length())));
                            highPassFilter = true;
                            break;
                        }

                        if (args[i].startsWith("-volume=")) {
                            volumeMultiplier = Double.parseDouble((args[i].substring("-volume=".length())));
                            break;
                        }

                        if (args[i].startsWith("-platform=")) {
                            processPlatformParameter(args[i]);
                            break;
                        }

                        if (args[i].startsWith("-")) {
                            System.err.println("Unknown command line option: " + args[i] + ".");
                            needToDisplayHelp = true;
                            return;
                        }

                        addFilenameToInputList(args[i]);
                        inputSourceSpecified = true;
                        break;
                }
            }

            if (!inputSourceSpecified)
                needToDisplayHelp = true;
        }
    }

    private void addFilenameToInputList(String filename) {
        inputFilenames.add(filename);
    }

    private void processPlatformParameter(String parameter) {
        String platformName = parameter.substring("-platform=".length());

        if (platformName.equals("any")) {
            addPlatformsExceptDeduction();
            return;
        }

        Platform matchedPlatform = null;
        for (Platform platform : availablePlatforms) {
            if (platform.getName().equals(platformName)) {
                matchedPlatform = platform;
                break;
            }
        }

        if (matchedPlatform == null) {
            System.err.println("No such platform: " + platformName);
            needToDisplayHelp = true;
        } else {
            chosenPlatforms.add(matchedPlatform);
        }
    }

    private void displayHelp() {
        System.err.println("cassette-nibbler [options] [inputFile1] [inputFile2] ...");
        System.err.println();
        System.err.println("Extracts from wav data to stdout. Input from one or more specified files, or use -stdin option.\nOptions:\n");
        System.err.println("-help: displays this message");
        System.err.println("-stdin: use input from standard in instead of file input");
        System.err.println("-destination=<type>, selects what to output. One of:");
        System.err.println("   directory: output files to the current directory, or specify using -output-directory");
        System.err.println("   listing: list filenames to standard out");
        System.err.println("   stdout: output file contents to standard out");
        System.err.println("-output=<type>, chooses format of output files. One of (defaults to all):");
        System.err.println("   binary: binary file content");
        System.err.println("   text: text, e.g. ASCII or Basic listing");
        System.err.println("   emulator: file suitable to load into an emulator");
        System.err.println("-output-directory=<dir>, if -destination=directory, this specifies the output directory");
        System.err.println("-platform=<platform>, selects a platform. One of the following.");
        System.err.println("   any: all the platforms listed below (default). Or,");

        enumeratePlatforms(System.err);

        System.err.println("-config=<settings>: provide optional configuration settings to the chosen platform");
        System.err.println("-invert: invert the incoming waveform before processing");
        System.err.println("-differentiate: differentiate the input signal before processing");
        System.err.println("-nofilters: disable default high and low pass filters on all platforms");
        System.err.println("-lowpass=<freq>: pass signal through a low pass filter before processing, cutoff specified in hz");
        System.err.println("-highass=<freq>: pass signal through a high pass filter before processing, cutoff specified in hz");
        System.err.println("-volume=<1.0, etc>: amount to multiply incoming signal by");
        System.err.println("-intact-files-only: only recover files that appear complete and with no errors");
        System.err.println("-sound-output=<filename>: output sample data after filters to WAV audio file");
        System.err.println("-logging=<logging mode>, the amount of logging to send to stderr");
        System.err.println("   (Recommend redirecting stderr to a file for verbose logging)");
        System.err.println("   none: no logging");
        System.err.println("   minimal: errors only and major pieces of information");
        System.err.println("   verbose: lots of detail (very long)");
        System.err.println("   parsing: outputs pulses that can be edited and loaded back in via -input=pulses. You need");
        System.err.println("            to specify one platform with -platform= otherwise different platforms will overlap.");
        System.err.println("            Redirect standard error to a file to save the pulses in a text file.");
        System.err.println("-input=pulses: reads a pulse file instead of an audio file. The pulse file can be");
        System.err.println("               output with -logging=parsing");
    }

    private void enumeratePlatforms(PrintStream out) {
        if (availablePlatforms.isEmpty()) {
            out.println("   No platforms available.");
            return;
        }

        for (Platform platform: availablePlatforms) {
            out.print("   ");
            out.print(platform.getName());
            out.print(": ");
            if (platform.hasHighProcessingOverhead()) {
                out.print("(will not be included by '-platform=any') ");
            }

            out.println(platform.getDescription());
        }
    }

    private void printOrGetFilesFromDirecotry() throws IOException {
        switch (outputDestination) {
            case DIRECTORY:
                outputFilesToDirectory();
                break;
            case DIRECTORY_LISTING:
                displayDirectoryContents();
                break;
            case STANDARD_OUT:
                outputFilesToStandardOut();
                break;
        }
    }

    private void outputFilesToStandardOut() {
        for (TapeFile file : directory.getList()) {
            outputSingleFileToStandardOut(file);
        }
    }

    private void outputSingleFileToStandardOut(TapeFile file) {
        if (outputFileType != null)
            outputFileOfType(file, outputFileType);
        else {
            for (TapeFile.FormatType formatType : TapeFile.FormatType.values()) {
                outputFileOfType(file, formatType);
            }
        }
    }

    private void outputFileOfType(TapeFile file, TapeFile.FormatType formatType) {
        try {
            byte [] data = file.getDataBytesOfType(formatType);
            if (data != null)
                System.out.write(data);

        } catch (IOException e) {
            System.err.println("Problem outputing data to standard out: " + e.toString());
        }
    }

    private void outputFilesToDirectory() {
        for (TapeFile file : directory.getList()) {
            outputSingleFileOfType(file);
        }
    }

    private void outputSingleFileOfType(TapeFile file) {
        if (outputFileType != null)
            outputSingleFileToDirectoryOfType(file, outputFileType);
        else {
            for (TapeFile.FormatType formatType : TapeFile.FormatType.values()) {
                outputSingleFileToDirectoryOfType(file, formatType);
            }
        }
    }

    private void outputSingleFileToDirectoryOfType(TapeFile file, TapeFile.FormatType formatType) {
        byte [] data = file.getDataBytesOfType(formatType);
        if (data != null) {
            try {
                StringBuilder filename = new StringBuilder();
                filename.append(file.getFilename());
                filename.append(".").append(Integer.toHexString(file.hashCode()));
                if (file.containsErrors())
                    filename.append(".ERR");

                filename.append(".").append(file.getFileExtensionForType(formatType));

                String completedFilename = filename.toString();
                completedFilename = PrintableString.convertToSuitableFilename(completedFilename);

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(getFilePath(outputDirectory, completedFilename)));
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                System.err.println("Error writing file " + file.getFilename() + ":" + e.toString());
            }
        }
    }

    private void displayDirectoryContents() {
        List <TapeFile> files = directory.getList();
        int count = 0;
        for (TapeFile file : files) {
            System.err.print("" + count + " " + file.getFilename() + ": " + file.length() + " bytes ");
            System.err.println();
            count++;
        }
    }

    private List <InputStream> getDataStreamList() throws FileNotFoundException {
        List <InputStream> streamList = new LinkedList<>();
        if (inputFilenames.isEmpty()) {
            streamList.add(System.in);
        } else {
            int count = 1;
            for (String filename : inputFilenames) {
                count++;
                streamList.add(new BufferedInputStream(new FileInputStream(filename)));
            }
        }

        return streamList;
    }

    private void runThroughSource() throws IOException {
        switch (inputType) {
            case WAV:
                runThroughWavFile();
                break;
            case PULSES:
                default:
                runThroughPulses();
                break;
        }
    }

    private void runThroughPulses() throws IOException {
        int result;
        do {
            result = pulseSource.getNextPulseAndPushToConsumers();
        } while (result == PulseSourceFromInputStream.SUCCESS);
    }

    private void runThroughWavFile() {
        wavSource.processFile();
    }

    private void preparePlatforms() {
        addCommodorePlatforms();
        addAcornPlatforms();
        addSinclairPlatforms();
        addOricPlatforms();
        addMSXPlatforms();
        addAmstradPlatforms();
        addAnalysisPlatforms();
    }

    private void addCommodorePlatforms() {
        Collection<Platform> commodorePlatforms = new CommodorePlatformProvider().getPlatforms();
        availablePlatforms.addAll(commodorePlatforms);
    }

    private void addAcornPlatforms() {
        Collection<Platform> acornPlatforms = new AcornPlatformProvider().getPlatforms();
        availablePlatforms.addAll(acornPlatforms);
    }

    private void addSinclairPlatforms() {
        Collection<Platform> sinclairPlatforms = new SinclairPlatformProvider().getPlatforms();
        availablePlatforms.addAll(sinclairPlatforms);
    }

    private void addOricPlatforms() {
        Collection<Platform> oricPlatforms = new OricPlatformProvider().getPlatforms();
        availablePlatforms.addAll(oricPlatforms);
    }

    private void addMSXPlatforms() {
        Collection<Platform> msxPlatforms = new MSXPlatformProvider().getPlatforms();
        availablePlatforms.addAll(msxPlatforms);
    }

    private void addAmstradPlatforms() {
        Collection<Platform> amstradPlatforms = new AmstradPlatformProvider().getPlatforms();
        availablePlatforms.addAll(amstradPlatforms);
    }

    private void addAnalysisPlatforms() {
        Collection<Platform> analysisPlatforms = new AutomaticPlatformProvider().getPlatforms();
        availablePlatforms.addAll(analysisPlatforms);
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
