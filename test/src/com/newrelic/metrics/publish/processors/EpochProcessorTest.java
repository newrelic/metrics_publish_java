package com.newrelic.metrics.publish.processors;

import static org.junit.Assert.*;

import org.junit.Test;

public class EpochProcessorTest {

    @Test
    public void testWithTwoValidPollCycles() throws InterruptedException {
        EpochProcessor counter = new EpochProcessor();
        Number firstProcess = counter.process(5);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(6);

        assertNull(firstProcess);
        assertEquals(1.0, secondProcess.doubleValue(), 0.1);
    }

    @Test
    public void testWithNullSecondPollCycle() throws InterruptedException {
        EpochProcessor counter = new EpochProcessor();
        Number firstProcess = counter.process(5);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(null);
        resetTimer(counter, 1);
        Number thirdProcess = counter.process(6);
        resetTimer(counter, 1);
        Number fourthProcess = counter.process(7);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertNull(thirdProcess);
        assertEquals(1.0, fourthProcess.doubleValue(), 0.1);
    }

    @Test
    public void testNullFirstPollCycle() throws InterruptedException {
        EpochProcessor counter = new EpochProcessor();
        Number firstProcess = counter.process(null);
        resetTimer(counter, 1);
        Number secondProcess = counter.process(5);
        resetTimer(counter, 1);
        Number thirdProcess = counter.process(6);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertEquals(1.0, thirdProcess.doubleValue(), 0.1);
    }

    private void resetTimer(EpochProcessor counter, int secondsToRemove) {
        counter.lastTime.setTime(counter.lastTime.getTime() - (secondsToRemove * 1000));
    }

}
