package com.example.leavemanagement.shared.api;

import com.example.leavemanagement.shared.application.LocalDemoResetService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local-demo")
@RequestMapping("/api/admin/local-demo")
public class LocalDemoResetController {
    private final LocalDemoResetService resets;

    public LocalDemoResetController(LocalDemoResetService resets) {
        this.resets = resets;
    }

    @PostMapping("/reset")
    public LocalDemoResetService.ResetResult reset() {
        return resets.reset();
    }
}
