package com.library.model;

public enum BookCondition {

    GOOD("Tốt / nguyên vẹn", 0L),

    SCRATCHED("Cũ / trầy xước nhẹ", 20000L),

    DAMAGED("Rách / hư hỏng nhẹ", 50000L),

    HEAVILY_DAMAGED("Hư hỏng nặng", 100000L),

    LOST("Mất sách", 200000L);

    private final String displayName;
    private final Long fee;

    BookCondition(String displayName, Long fee) {
        this.displayName = displayName;
        this.fee = fee;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getFee() {
        return fee;
    }
}