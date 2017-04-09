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

public class c64BasicRecogniser extends Recogniser {
    public c64BasicRecogniser() {
        name = "c64basic";
        itemsToRecognise = new Item [] {

            new HeavyItem('5', '3', '2', '8', '0'),
                new HeavyItem('5', '3', '2', '8', '1'),
                new HeavyItem('5', '4', '2', '9', '6'),
                new HeavyItem('p', 'r', 'e', 's', 's')
        };
    }
}
