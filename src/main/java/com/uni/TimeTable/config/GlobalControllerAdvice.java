package com.uni.TimeTable.config;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        String currentUri = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        model.addAttribute("currentUri", currentUri);
    }
}
