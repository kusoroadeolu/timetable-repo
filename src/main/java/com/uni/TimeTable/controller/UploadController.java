package com.uni.TimeTable.controller;

import com.uni.TimeTable.service.TimetableService;
import com.uni.TimeTable.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UploadController {
    private final TimetableService timetableService;
    private final UploadService uploadService;

    @GetMapping("/overseer/upload-planner")
    public String showUploadPlannerForm(Model model) {
        model.addAttribute("schools", timetableService.getAllSchools());
        return "overseer-upload-planner";
    }

    @PostMapping("/overseer/upload-planner/academic-planner")
    public String uploadAcademicPlanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolId") Long schoolId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        UploadService.UploadResult result = uploadService.uploadAcademicPlanner(file, schoolId);

        if (result.getSuccessMessage() != null) {
            redirectAttributes.addFlashAttribute("success", result.getSuccessMessage());
        }
        if (result.getErrorMessage() != null) {
            redirectAttributes.addFlashAttribute("error", result.getErrorMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/rooms")
    public String uploadRooms(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        UploadService.UploadResult result = uploadService.uploadRooms(file);

        if (result.getSuccessMessage() != null) {
            redirectAttributes.addFlashAttribute("success", result.getSuccessMessage());
        }
        if (result.getErrorMessage() != null) {
            redirectAttributes.addFlashAttribute("error", result.getErrorMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/lecturer-availability")
    public String uploadLecturerAvailability(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        UploadService.UploadResult result = uploadService.uploadLecturerAvailability(file);

        if (result.getSuccessMessage() != null) {
            redirectAttributes.addFlashAttribute("success", result.getSuccessMessage());
        }
        if (result.getErrorMessage() != null) {
            redirectAttributes.addFlashAttribute("error", result.getErrorMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/timetable-sheet")
    public String uploadTimetableSheet(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        UploadService.UploadResult result = uploadService.uploadTimetableSheet(file);

        if (result.getSuccessMessage() != null) {
            redirectAttributes.addFlashAttribute("success", result.getSuccessMessage());
        }
        if (result.getErrorMessage() != null) {
            redirectAttributes.addFlashAttribute("error", result.getErrorMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

}