package backend.model;

/** Where a piece of demand evidence came from. */
public enum SignalType {
    /** A completed purchase. */
    PURCHASE,
    /** An order/request placed with a vendor (not yet completed). */
    PRE_ORDER,
    /** A customer explicitly asked for a product in their area. */
    REQUEST,
    /** A product search. */
    SEARCH,
    /** A product/vendor view. */
    VIEW,
    /** Derived by the engine from a purchase cycle; never stored. */
    REPLENISHMENT
}
