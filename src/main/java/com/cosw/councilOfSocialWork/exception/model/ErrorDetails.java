package com.cosw.councilOfSocialWork.exception.model;

import java.util.List;

public record ErrorDetails(
        String title,
        int status,
        String error,
        List<String> errorsList) {
}
