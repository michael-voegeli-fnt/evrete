package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValue;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.util.Bits;
//TODO !!!! create one-field implementation

/**
 * A runtime representation of a fact, ready for insert operation
 */
class RuntimeFact {
    private static final Bits EMPTY = new Bits();
    final Object[] fieldValues;
    final FactHandleVersioned factHandle;
    final Bits alphaTests;
    final ActiveField[] activeFields;

    RuntimeFact(TypeMemoryState typeMemoryState, FactHandleVersioned factHandle, FactRecord factRecord) {
        this.factHandle = factHandle;
        this.activeFields = typeMemoryState.activeFields;
        this.fieldValues = new Object[activeFields.length];
        for (int i = 0; i < fieldValues.length; i++) {
            this.fieldValues[i] = activeFields[i].readValue(factRecord.instance);
        }
        AlphaEvaluator[] alphaEvaluators = typeMemoryState.alphaEvaluators;
        if (alphaEvaluators.length == 0) {
            this.alphaTests = EMPTY;
        } else {
            FieldToValue func = field -> fieldValues[field.getValueIndex()];
            this.alphaTests = new Bits();
            for (AlphaEvaluator alphaEvaluator : alphaEvaluators) {
                if (alphaEvaluator.test(func)) {
                    this.alphaTests.set(alphaEvaluator.getIndex());
                }
            }
        }
    }

    boolean sameValues(RuntimeFact other) {
        if (other == null) return false;
        for (int i = 0; i < fieldValues.length; i++) {
            if (fieldValues[i] != other.fieldValues[i]) {
                return false;
            }
        }
        return false;
    }
}