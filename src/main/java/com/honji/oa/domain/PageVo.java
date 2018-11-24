package com.honji.oa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PageVo implements Serializable {
    private long total;
    private List<? extends Object> rows;
}
