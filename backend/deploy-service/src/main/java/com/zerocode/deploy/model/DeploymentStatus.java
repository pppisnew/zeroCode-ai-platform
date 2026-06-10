package com.zerocode.deploy.model;

public enum DeploymentStatus {
    PLANNED("planned"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String value;

    DeploymentStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
