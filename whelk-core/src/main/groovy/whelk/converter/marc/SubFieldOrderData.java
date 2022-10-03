package whelk.converter.marc;

public final class SubFieldOrderData implements Comparable {
    final int definedOrder;
    final boolean isNotNumber;
    final String fieldName;

    public SubFieldOrderData(final Integer definedOrder, final boolean isNotNumber, final String fieldName) {
        this.definedOrder = definedOrder;
        this.isNotNumber = isNotNumber;
        this.fieldName = fieldName;
    }

    @Override
    public int compareTo(Object o) {
        SubFieldOrderData otherData = (SubFieldOrderData) o;

        int result = Integer.compare(definedOrder, otherData.definedOrder);
        if (result != 0) return result;

        result = Boolean.compare(isNotNumber, otherData.isNotNumber);
        if (result != 0) return result;

        return fieldName.compareTo(otherData.fieldName);
    }

    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(definedOrder) | Boolean.hashCode(isNotNumber) | fieldName.hashCode();
    }
}
