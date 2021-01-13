package org.evrete.helper;

import org.evrete.api.ReIterable;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface IterableCollection<T> extends IterableOver<T>, ReIterable<T> {

    boolean add(T element);

    long size();

    void delete(Predicate<T> predicate);

    void clear();

    Stream<T> stream();
}
