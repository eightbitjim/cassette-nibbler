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

import com.eightbitjim.cassettenibbler.Transition;
import com.eightbitjim.cassettenibbler.IntervalStreamConsumer;

public class IntervalStreamPrinter implements IntervalStreamConsumer {
    private static final double NANOSECONDS_IN_A_SECOND = 1000000000.0;

    @Override
    public void pushInterval(Transition transition, double currentTimeIndex) {
        System.out.print(transition.transitionedToHigh ? "H" : "L");
        System.out.print((long)Math.floor(transition.secondsSinceLastTransition * NANOSECONDS_IN_A_SECOND));
        System.out.println();
    }
}
