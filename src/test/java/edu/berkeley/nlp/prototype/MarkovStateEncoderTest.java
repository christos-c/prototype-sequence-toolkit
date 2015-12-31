package edu.berkeley.nlp.prototype;

import edu.berkeley.nlp.prototype.MarkovStateEncoder.MarkovState;
import edu.berkeley.nlp.util.CollectionUtils;
import fig.basic.Indexer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MarkovStateEncoderTest {

    @Test
    public void testEncoder() {
        Indexer<String> labels = new Indexer<String>(CollectionUtils.makeList("A", "B"));
        MarkovStateEncoder<String> encoder = new MarkovStateEncoder<String>(labels, "START", "STOP", 1);
        List<MarkovState<String>> states = encoder.getStates();
        assertEquals(4, states.size());
        assertEquals("A", states.get(0).getCurrentLabel());
        assertEquals(1, states.get(1).getIndex());
        assertEquals("STOP", states.get(3).getCurrentLabel());
    }
}