package com.gateflow.tracker.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private Integer code;
    private String message;
    private PageData<T> data;

    @Data
    public static class PageData<T> {
        private List<T> list;
        private Long total;
        private Integer page;
        private Integer size;
        private Integer pages;
    }

    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer size) {
        PageData<T> pageData = new PageData<>();
        pageData.setList(list);
        pageData.setTotal(total);
        pageData.setPage(page);
        pageData.setSize(size);
        pageData.setPages((int) Math.ceil((double) total / size));

        PageResponse<T> response = new PageResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(pageData);
        return response;
    }
}