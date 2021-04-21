package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.collections.LinearHashSet;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;

abstract class AbstractFactsMap<K extends MemoryKey> {
    private final LinearHashSet<MapKey<K>> data;
    private final BiPredicate<MapKey<K>, IntToValueHandle> search;
    private final int myModeOrdinal;
    private final BiPredicate<MapKey<K>, K> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private final Function<MapKey<K>, MemoryKey> ENTRY_MAPPER = entry -> entry.key;

    AbstractFactsMap(KeyMode myMode, int minCapacity) {
        this.search = this::sameData;
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
    }

    abstract boolean sameData(MapKey<K> mapEntry, IntToValueHandle key);

    abstract K newKeyInstance(IntToValueHandle fieldValues, int hash);

    final ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    public final void add(IntToValueHandle key, int keyHash, Collection<FactHandleVersioned> factHandles) {
        data.resize();
        int addr = data.findBinIndex(key, keyHash, search);
        MapKey<K> entry = data.get(addr);
        if (entry == null) {
            K k = newKeyInstance(key, keyHash);
            k.setMetaValue(myModeOrdinal);
            entry = new MapKey<>(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        for (FactHandleVersioned h : factHandles) {
            entry.facts.add(h);
        }

    }

    final boolean hasKey(int hash, IntToValueHandle key) {
        int addr = data.findBinIndex(key, hash, search);
        return data.get(addr) != null;
    }

    private int addr(K key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @SuppressWarnings("unchecked")
    final ReIterator<FactHandleVersioned> values(MemoryKey k) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr((K) k);
        MapKey<K> entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    final void merge(AbstractFactsMap<K> other) {
        other.data.forEachDataEntry(this::merge);
        other.data.clear();
    }

    private void merge(MapKey<K> otherEntry) {
        otherEntry.key.setMetaValue(myModeOrdinal);
        int addr = addr(otherEntry.key);
        MapKey<K> found = data.get(addr);
        if (found == null) {
            this.data.add(otherEntry);
        } else {
            found.facts.consume(otherEntry.facts);
        }
    }


    public final void clear() {
        data.clear();
    }

    @Override
    public final String toString() {
        return data.toString();
    }

    static class MapKey<K> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        MapKey(K key) {
            this.key = key;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapKey<?> mapKey = (MapKey<?>) o;
            return this.key.equals(mapKey.key);
        }

        @Override
        public final int hashCode() {
            return key.hashCode();
        }

    }
}
