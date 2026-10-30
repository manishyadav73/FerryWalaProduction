package backend.model;

/** Shared by vendor shops and products. Kept as a fixed enum so demand
 *  scoring and filters never depend on free-text spelling. */
public enum Category {
    VEGETABLES,
    FRUITS,
    DAIRY,
    GROCERY,
    SNACKS,
    TEA,
    JUICE,
    OTHER
}
