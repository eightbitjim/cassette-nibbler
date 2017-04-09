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

package com.eightbitjim.cassettenibbler;

public class Sample {
    public static final double END_OF_STREAM = Double.NaN;
    public static final double MIN_VALUE = -0.999;
    public static final double MAX_VALUE = 0.999;
    public double normalizedValue;

    public boolean isEndOfStream() {
        return Double.isNaN(normalizedValue);
    }

    public void setValue(double value) {
        normalizedValue = value;
        clipValue();
    }

    private void clipValue() {
        if (normalizedValue < MIN_VALUE)
            normalizedValue = MIN_VALUE;

        if (normalizedValue > MAX_VALUE)
            normalizedValue = MAX_VALUE;
    }
}
