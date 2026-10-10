package model;

import java.math.BigDecimal;

// One row of brief Table 2: a node range and its price.
public class LANNodeTier {

    private final int tierId;
    private final int minNodes;
    private final int maxNodes;
    private final BigDecimal cost;

    public LANNodeTier(int tierId, int minNodes, int maxNodes, BigDecimal cost) {
        this.tierId = tierId;
        this.minNodes = minNodes;
        this.maxNodes = maxNodes;
        this.cost = cost;
    }

    public int getTierId() { return tierId; }
    public int getMinNodes() { return minNodes; }
    public int getMaxNodes() { return maxNodes; }
    public BigDecimal getCost() { return cost; }

    @Override
    public String toString() {
        return minNodes + " to " + maxNodes + " nodes";
    }
}