package edu.berkeley.nlp.classify;

import edu.berkeley.nlp.util.Counter;

import java.io.Serializable;

/**
 * Feature extractors process input instances into feature counters.
 *
 * @author Dan Klein
 */
public interface FeatureExtractor<I, O> extends Serializable {
    Counter<O> extractFeatures(I instance);
}
