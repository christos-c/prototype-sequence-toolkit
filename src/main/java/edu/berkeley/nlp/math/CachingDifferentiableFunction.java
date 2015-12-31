package edu.berkeley.nlp.math;

import fig.basic.Pair;

import java.util.Arrays;

public abstract class CachingDifferentiableFunction implements DifferentiableFunction {

    double[] lastX;
    double[] lastGradient;
    double lastValue;

    protected abstract Pair<Double, double[]> calculate(double[] x);

    private void ensureCache(double[] x) {
        if (!isCached(x)) {
            Pair<Double, double[]> result = calculate(x);
            lastValue = result.getFirst();
            lastX = DoubleArrays.clone(x);
            lastGradient = DoubleArrays.clone(result.getSecond());
        }
    }

    private boolean isCached(double[] x) {
        return lastX != null && Arrays.equals(x, lastX);
    }


    public double[] derivativeAt(double[] x) {
        ensureCache(x);
        return lastGradient;
    }

    public double valueAt(double[] x) {
        ensureCache(x);
        return lastValue;
    }

    public abstract int dimension();

}
