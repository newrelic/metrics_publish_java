package com.newrelic.metrics.publish.processors;

import static org.junit.Assert.*;

import org.junit.Test;

public class EpochProcessorTest {

    @Test
    public void testWithTwoValidPollCycles() throws InterruptedException {
        EpochProcessor processor = new EpochProcessor();
        Number firstProcess = processor.process(5);
        resetTimer(processor, 1);
        Number secondProcess = processor.process(6);

        assertNull(firstProcess);
        assertEquals(1.0, secondProcess.doubleValue(), 0.1);
    }

    @Test
    public void testWithNullSecondPollCycle() throws InterruptedException {
        EpochProcessor processor = new EpochProcessor();
        Number firstProcess = processor.process(5);
        resetTimer(processor, 1);
        Number secondProcess = processor.process(null);
        resetTimer(processor, 1);
        Number thirdProcess = processor.process(6);
        resetTimer(processor, 1);
        Number fourthProcess = processor.process(7);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertNull(thirdProcess);
        assertEquals(1.0, fourthProcess.doubleValue(), 0.1);
    }

    @Test
    public void testNullFirstPollCycle() throws InterruptedException {
        EpochProcessor processor = new EpochProcessor();
        Number firstProcess = processor.process(null);
        resetTimer(processor, 1);
        Number secondProcess = processor.process(5);
        resetTimer(processor, 1);
        Number thirdProcess = processor.process(6);

        assertNull(firstProcess);
        assertNull(secondProcess);
        assertEquals(1.0, thirdProcess.doubleValue(), 0.1);
    }

    private void resetTimer(EpochProcessor processor, int secondsToRemove) {
        processor.lastTime.setTime(processor.lastTime.getTime() - (secondsToRemove * 1000));
    }

}
