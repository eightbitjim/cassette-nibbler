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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.FrequencyAnalysis;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Analysis {
    int numberOfValuesToCount;
    int [] occurrences;
    int totalOccurrences;
    String name;

    protected static final int DONT_COUNT_THIS_VALUE = -1;

    TapeExtractionLogging logging;

    public Analysis(String name, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        numberOfValuesToCount = 256;
        reset();
        this.name = name;
    }

    public Analysis(String name, int numberOfValues) {
        numberOfValuesToCount = numberOfValues;
        reset();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void reset() {
        totalOccurrences = 0;
        occurrences = new int[numberOfValuesToCount];
    }

    public void dontCountValue(int value) {
        if (value < 0 || value >= numberOfValuesToCount)
            return;

        occurrences[value] = DONT_COUNT_THIS_VALUE;
    }

    public boolean isCountingValue(int value) {
        if (value < 0 || value >= numberOfValuesToCount)
            return false;

        return occurrences[value] != DONT_COUNT_THIS_VALUE;
    }

    public String getVisualAnalysis() {
        StringBuilder builder = new StringBuilder();
        for (int count = 0; count < numberOfValuesToCount; count++) {
            builder.append(count).append(": ");
            int percent = (int)(getFractionOfOccurencesForValue(count) * 1000.0);
            for (int i = 0; i < percent; i++)
                builder.append("+");

            builder.append("\n");
        }

        return builder.toString();
    }

    public String getDataAsString() {
        StringBuilder builder = new StringBuilder();
        for (int value : occurrences) {
            builder.append(value).append(", ");
        }

        return builder.toString();
    }

    public void addOccurrence(int value) {
        if (value < 0 || value >= numberOfValuesToCount) {
            return;
        }

        if (occurrences[value] == DONT_COUNT_THIS_VALUE)
            return;

        occurrences[value]++;
        totalOccurrences++;
    }

    public int getOccurrencesForValue(int value) {
        if (value < 0 || value >= numberOfValuesToCount || occurrences[value] == DONT_COUNT_THIS_VALUE) {
            return 0;
        }

        return occurrences[value];
    }

    public double getFractionOfOccurencesForValue(int value) {
        if (totalOccurrences == 0)
            return 0.0;

        double valueToReturn = (double)getOccurrencesForValue(value) / (double)totalOccurrences;
        return valueToReturn;
    }

    public double differenceFrom(Analysis other) {
        double sumOfDifference = 0.0;
        if (other.numberOfValuesToCount != numberOfValuesToCount) {
            logging.writeProgramOrEnvironmentError(0, "Cannot compare frequency analysis if there are different value ranges");
            return Double.MAX_VALUE;
        }

        for (int num = 0; num < numberOfValuesToCount; num++) {
            if (isCountingValue(num) && other.isCountingValue(num))
                sumOfDifference += Math.abs(getFractionOfOccurencesForValue(num) - other.getFractionOfOccurencesForValue(num));
        }

        return sumOfDifference;
    }

    public void computeTotalOccurrences() {
        totalOccurrences = 0;
        for (int count : occurrences)
            totalOccurrences += count;
    }

    public void groupNearbyValues() {
        double threashold = 0.002;
        for (int i = 1; i < occurrences.length; i++) {
            if (getFractionOfOccurencesForValue(i) > threashold &&
                    getFractionOfOccurencesForValue(i - 1) > threashold) {
                occurrences[i] += occurrences[i - 1];
                occurrences[i - 1] = 0;
            }
        }
    }

    public List<Integer> getTopNValueIndices(int n) {
        List<Integer> returnList = new LinkedList<>();

        int lastValueIndex = addTopValueBelowN(returnList, Integer.MAX_VALUE);
        for (int i = 0; i < n - 1; i++) {
            if (lastValueIndex != -1)
                lastValueIndex = addTopValueBelowN(returnList, occurrences[lastValueIndex]);
        }

        Collections.sort(returnList);
        return returnList;
    }

    private int addTopValueBelowN(List<Integer> list, int below) {
        double threashold = 0.003;
        int index = getTopValueIndexWhoseValueIsBelowN(below);
        if (index != -1 && getFractionOfOccurencesForValue(index) > threashold) {
            list.add(index);
            return index;
        } else {
            return -1;
        }
    }

    private int getTopValueIndexWhoseValueIsBelowN(int n) {
        int topValue = Integer.MIN_VALUE;
        int topValueIndex = -1;
        for (int i = 1; i < occurrences.length - 1; i++) { // Ignore top and bottom values as these are maxed out
            if (occurrences[i] > topValue && occurrences[i] < n) {
                topValue = occurrences[i];
                topValueIndex = i;
            }
        }

        return topValueIndex;
    }
}
