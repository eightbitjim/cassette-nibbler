package com.eightbitjim.cassettenibbler.Platforms.Other.MPFI.FileExtraction;

import com.eightbitjim.cassettenibbler.*;

public class MPFIByteFrame {
    private static final int FRAME_SIZE = 10;
    private static final int START_BIT_POSITION = 0;
    private static final int STOP_BIT_POSITION = 9;

    private static final int MEDIUM_PULSES_IN_A_ZERO = 2;
    private static final int MEDIUM_PULSES_IN_A_ONE = 4;

    private boolean bits[];
    private int currentBit;
    private int mediumPulses;
    private boolean isError;

    public static final int MORE_BITS_NEEDED = -1;
    public static final int ERROR = -2;

    public MPFIByteFrame() {
        bits = new boolean[FRAME_SIZE];
        reset();
    }

    public void reset() {
        currentBit = 0;
        mediumPulses = 0;
        isError = false;
    }

    public int addPulseAndReturnByteOrStatus(char pulse) {
        switch (pulse) {
            case PulseStreamConsumer.SHORT_PULSE:
                // Has this terminated a run of medium pulses?
                if (mediumPulses > 0) {
                    if (mediumPulses == MEDIUM_PULSES_IN_A_ZERO)
                        addBit(false);
                    else if (mediumPulses == MEDIUM_PULSES_IN_A_ONE)
                        addBit(true);
                    else {
                        isError = true;
                    }
                }
                break;

            case PulseStreamConsumer.MEDIUM_PULSE:
                mediumPulses++;
                break;

            default:
                // Unexpected pulse type
                isError = true;
        }

        if (isError) {
            reset();
            return ERROR;
        } else if (byteIsComplete()) {
            reset();
            return byteValue();
        }
        else
            return MORE_BITS_NEEDED;
    }

    private void addBit(boolean isOne) {
        bits[currentBit] = isOne;
        mediumPulses = 0;

        // Is this the start bit?
        if (currentBit == START_BIT_POSITION && isOne) {
            // Should not start with a 1 bit.
            isError = true;
        } else if (currentBit == STOP_BIT_POSITION && !isOne) {
            // Should not end with a 0 bit.
            isError = true;
        } else
            currentBit++;
    }

    private boolean byteIsComplete() {
        return currentBit == FRAME_SIZE;
    }

    private int byteValue() {
        int value = 0;
        for (int i = 1; i < 9; i++)
            if (bits[i])
                value |= (1 << (i - 1));

        return value;
    }
}
