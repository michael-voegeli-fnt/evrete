/*
package org.evrete.spi.minimal;

import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.AbstractLinearHash;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

class FieldsFactMapOld extends AbstractLinearHash<ValueRowImpl> {
    private static final ToIntFunction<Object> HASH_FUNCTION = Object::hashCode;
    private static final BiPredicate<ValueRowImpl, ValueRowImpl> EQ_FUNCTION_TYPED = ValueRowImpl::equals;
    private static final Function<ValueRowImpl, ValueRow> IMPL_MAPPING = v -> v;

    ReIterator<ValueRow> keyIterator() {
        return iterator(IMPL_MAPPING);
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    public void addAll(FieldsFactMapOld other) {
        resize(this.size() + other.size() + 1);
        other.forEachDataEntry(this::insertOtherNoResize);
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return IDENTITY_EQUALS;
    }

    private void insertOtherNoResize(ValueRowImpl other) {
        int hash = other.hashCode();
        int addr = findBinIndex(other, hash, EQ_FUNCTION_TYPED);
        ValueRowImpl found = get(addr);
        if (found == null) {
            saveDirect(other, addr);
        } else {
            //TODO !!!!!
            throw new UnsupportedOperationException();
            //found.mergeDataFrom(other);
        }
    }
}
*/