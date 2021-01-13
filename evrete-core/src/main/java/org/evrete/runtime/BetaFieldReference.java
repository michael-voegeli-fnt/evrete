package org.evrete.runtime;

import org.evrete.api.TypeField;

import java.util.Objects;

public final class BetaFieldReference {
    private final FactType factType;
    private final int fieldIndex;

    public BetaFieldReference(FactType factType, TypeField field) {
        this.factType = factType;
        this.fieldIndex = factType.findFieldPosition(field);
    }

    public FactType getFactType() {
        return factType;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetaFieldReference that = (BetaFieldReference) o;
        return fieldIndex == that.fieldIndex &&
                factType.equals(that.factType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factType, fieldIndex);
    }
}
