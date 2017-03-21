package com.customatics.LeaptestForBamboo.impl;

public class ErrorCodeException extends Exception{

    public ErrorCodeException(Integer code, String status) {
        super("Code:" + code.toString() + " Status:" + status + " !");
    }
}
