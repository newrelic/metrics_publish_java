package com.newrelic.metrics.publish.processors;

import static org.junit.Assert.*;

import org.junit.Test;

public class EpochCounterTest {

    @Test
    public void testWithTwoValidPollCycles() throws InterruptedException {
        EpochCounter counter = new EpochCounter();
        Number firstProcess = counter.process(5);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(6);

        assertNull(firstProcess);
        assertEquals(1.0f, secondProcess);
    }

    @Test
    public void testWithNullSecondPollCycle() throws InterruptedException {
        EpochCounter counter = new EpochCounter();
        Number firstProcess = counter.process(5);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(null);
        resetTimer(counter, 1);
        Number thirdProcess = counter.process(6);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertNull(thirdProcess);
    }

    @Test
    public void testNullFirstPollCycle() throws InterruptedException {
        EpochCounter counter = new EpochCounter();
        Number firstProcess = counter.process(null);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(5);
        resetTimer(counter, 1);
        Number thirdProcess = counter.process(6);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertEquals(1.0f, thirdProcess);
    }

    private void resetTimer(EpochCounter counter, int secondsToRemove) {
        counter.lastTime.setTime(counter.lastTime.getTime() - (secondsToRemove * 1000));
    }

}
