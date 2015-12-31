package edu.berkeley.nlp.util;

import java.util.Iterator;

public class IterableAdapter {

    public interface Convertor<S, T> {
        T convert(S s);
    }

    public static <S, T> Iterable<T> adapt(final Iterable<S> iterable, final Convertor<S, T> convertor) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                final Iterator<S> origIt = iterable.iterator();
                return new Iterator<T>() {

                    public boolean hasNext() {
                        return origIt.hasNext();
                    }

                    public T next() {
                        return convertor.convert(origIt.next());
                    }

                    public void remove() {
                        origIt.remove();
                    }
                };
            }

        };
    }

}
