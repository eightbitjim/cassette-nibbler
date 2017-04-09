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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction;

public class AcornDataCRC {
    private int high, low;
    private static final int BIT7 = 1 << 7;

    public AcornDataCRC() {
        reset();
    }

    public void reset() {
        high = 0;
        low = 0;
    }

    public void addByte(int byteToAdd) {
        high ^= byteToAdd;
        for (int x = 1; x < 9; x++) {
            int t = 0;
            if ((high & BIT7) != 0) {
                high ^= 0x08;
                low ^= 0x10;
                t = 1;
            }

            int highlow = high * 256 + low;
            highlow = (highlow * 2 + t) & 0xffff;
            high = highlow / 256;
            low = highlow % 256;
        }
    }

    public int getCRCHigh() {
        return high;
    }

    public int getCRCLow() {
        return low;
    }
}
