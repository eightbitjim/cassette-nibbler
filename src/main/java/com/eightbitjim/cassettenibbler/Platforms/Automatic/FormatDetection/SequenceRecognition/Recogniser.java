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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.SequenceRecognition;

public abstract class Recogniser {
    public Item [] itemsToRecognise;
    public String name;

    public int getMatchWeightAgainst(byte [] dataBuffer) {
        int weight = 0;
        for (int position = 0; position < dataBuffer.length; position++) {
            weight += sumOfItemWeightsAtPosition(dataBuffer, position);
        }

        return weight;
    }

    private int sumOfItemWeightsAtPosition(byte [] dataBuffer, int position) {
        int sumOfWeights = 0;
        for (Item itemToCompare : itemsToRecognise) {
            if (itemIsAtPosition(itemToCompare, dataBuffer, position))
                sumOfWeights += itemToCompare.weight;
        }

        return sumOfWeights;
    }

    private boolean itemIsAtPosition(Item itemToCompare, byte [] dataBuffer, int position) {
        int offset;
        for (offset = 0; offset < itemToCompare.values.length && position + offset < dataBuffer.length - 1; offset++) {
            int currentByte = Byte.toUnsignedInt(dataBuffer[position + offset]);
            if (currentByte != itemToCompare.values[offset])
                break;
        }

        return offset == itemToCompare.values.length;
    }

    protected void addAllItemsFrom(Recogniser otherRecogniser) {
        Item [] updatedItemArray = new Item[itemsToRecognise.length + otherRecogniser.itemsToRecognise.length];
        int count = 0;
        for (Item item : itemsToRecognise)
            updatedItemArray[count++] = item;

        for (Item item : otherRecogniser.itemsToRecognise)
            updatedItemArray[count++] = item;
    }

}
