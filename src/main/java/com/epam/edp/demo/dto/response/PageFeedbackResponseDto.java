package com.epam.edp.demo.dto.response;

import java.util.List;
import java.util.Map;

public class PageFeedbackResponseDto {
    private Data data;

    public PageFeedbackResponseDto() {
        this.data = new Data();
    }

    public PageFeedbackResponseDto(int totalPages, long totalElements, int size, List<FeedbackResponseDto> content,
                                   int number, List<Map<String, Object>> sort, boolean first, boolean last,
                                   int numberOfElements, Map<String, Object> pageable, boolean empty) {
        this.data = new Data(new Feedbacks(totalPages, totalElements, size, content, number,
                sort, first, last, numberOfElements, pageable, empty));
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    // Inner Data class
    public static class Data {
        private Feedbacks feedbacks;

        public Data() {}

        public Data(Feedbacks feedbacks) {
            this.feedbacks = feedbacks;
        }

        public Feedbacks getFeedbacks() {
            return feedbacks;
        }

        public void setFeedbacks(Feedbacks feedbacks) {
            this.feedbacks = feedbacks;
        }
    }

    // Inner Feedbacks class that contains the original PageFeedbackResponseDto fields
    public static class Feedbacks {
        private int totalPages;
        private long totalElements;
        private int size;
        private List<FeedbackResponseDto> content;
        private int number;
        private List<Map<String, Object>> sort;
        private boolean first;
        private boolean last;
        private int numberOfElements;
        private Map<String, Object> pageable;
        private boolean empty;

        public Feedbacks() {}

        public Feedbacks(int totalPages, long totalElements, int size, List<FeedbackResponseDto> content,
                         int number, List<Map<String, Object>> sort, boolean first, boolean last,
                         int numberOfElements, Map<String, Object> pageable, boolean empty) {
            this.totalPages = totalPages;
            this.totalElements = totalElements;
            this.size = size;
            this.content = content;
            this.number = number;
            this.sort = sort;
            this.first = first;
            this.last = last;
            this.numberOfElements = numberOfElements;
            this.pageable = pageable;
            this.empty = empty;
        }

        // Getters and setters
        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public List<FeedbackResponseDto> getContent() {
            return content;
        }

        public void setContent(List<FeedbackResponseDto> content) {
            this.content = content;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public List<Map<String, Object>> getSort() {
            return sort;
        }

        public void setSort(List<Map<String, Object>> sort) {
            this.sort = sort;
        }

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public boolean isLast() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }

        public int getNumberOfElements() {
            return numberOfElements;
        }

        public void setNumberOfElements(int numberOfElements) {
            this.numberOfElements = numberOfElements;
        }

        public Map<String, Object> getPageable() {
            return pageable;
        }

        public void setPageable(Map<String, Object> pageable) {
            this.pageable = pageable;
        }

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }
    }
}