package com.washmycar.controller;

import com.washmycar.dto.Dtos.RejectRequest;
import com.washmycar.dto.Dtos.StationResponse;
import com.washmycar.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stations")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/pending")
    public List<StationResponse> pending() {
        return admin.pending();
    }

    @PatchMapping("/{id}/approve")
    public StationResponse approve(@PathVariable Long id) {
        return admin.approve(id);
    }

    @PatchMapping("/{id}/reject")
    public StationResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
        return admin.reject(id, req.motif());
    }
}
