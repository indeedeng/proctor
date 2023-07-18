package com.indeed.proctor.webapp.controllers;

/** @author parker */
public class JsonResponse<T> {
    public T data;
    public boolean success;
    public String msg;

    public JsonResponse(T data, boolean success, String msg) {
        this.data = data;
        this.success = success;
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMsg() {
        return msg;
    }
}
