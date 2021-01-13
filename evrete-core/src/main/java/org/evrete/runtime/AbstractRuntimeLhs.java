package org.evrete.runtime;

import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;


public abstract class AbstractRuntimeLhs {
    final RuntimeFact[][] factState;
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final RhsFactGroupAlpha alphaFactGroup;
    private final RhsFactGroupBeta[] betaFactGroups;

    AbstractRuntimeLhs(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AbstractLhsDescriptor descriptor) {
        this.descriptor = descriptor;
        //this.parent = parent;
        assert parent == null; // No aggregate groups yet
        // Init end nodes first
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        //private final AbstractRuntimeLhs parent;
        RhsFactGroup[] rhsFactGroups = new RhsFactGroup[allFactGroups.length];
        ValueRow[][] keyState = new ValueRow[allFactGroups.length][];
        this.factState = new RuntimeFact[allFactGroups.length][];

        RhsFactGroupAlpha alpha = null;
        for (RhsFactGroupDescriptor groupDescriptor : allFactGroups) {
            int groupIndex = groupDescriptor.getFactGroupIndex();
            this.factState[groupIndex] = new RuntimeFact[groupDescriptor.getTypes().length];

            RhsFactGroup factGroup;
            if (groupDescriptor.isLooseGroup()) {
                factGroup = alpha = new RhsFactGroupAlpha(rule, groupDescriptor, factState);
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                if (finalNode != null) {
                    BetaEndNode endNode = new BetaEndNode(rule, finalNode);
                    endNodes.add(endNode);
                    factGroup = new RhsFactGroupBeta(groupDescriptor, endNode, keyState, factState);
                } else {
                    RuntimeFactTypeKeyed singleType = rule.resolve(groupDescriptor.getTypes()[0]);
                    factGroup = new RhsFactGroupBeta(groupDescriptor, singleType, keyState, factState);
                }
            }
            rhsFactGroups[groupIndex] = factGroup;
        }
        this.betaFactGroups = CollectionUtils.filter(RhsFactGroupBeta.class, rhsFactGroups, group -> !group.isAlpha());
        this.alphaFactGroup = alpha;
    }

    AbstractRuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        this(rule, null, descriptor);
    }


    public RhsFactGroupAlpha getAlphaFactGroup() {
        return alphaFactGroup;
    }

    public RhsFactGroupBeta[] getBetaFactGroups() {
        return betaFactGroups;
    }

    Collection<BetaEndNode> getEndNodes() {
        return endNodes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "descriptor=" + descriptor +
                '}';
    }
}
