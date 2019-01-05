package com.honji.oa.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public enum ProcessStatus implements Serializable {
    UNDER_AUDIT("审核中"), FINISHED("已完成"), ENDED("已终止");

    @Setter
    @Getter
    private String description;
    private ProcessStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String toValue() {
        return this.description;
    }
}
