package com.customatics.LeaptestForBamboo.impl;

/**
 * Created by Роман on 09.03.2017.
 */
public class MutableBoolean {
    private boolean value;

    public MutableBoolean(boolean value)
    {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
