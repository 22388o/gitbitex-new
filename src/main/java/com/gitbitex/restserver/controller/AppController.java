package com.gitbitex.restserver.controller;

import com.gitbitex.entity.App;
import com.gitbitex.entity.User;
import com.gitbitex.repository.AppRepository;
import com.gitbitex.restserver.model.AppDto;
import com.gitbitex.restserver.model.CreateAppRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppController {
    private final AppRepository appRepository;

    @GetMapping("/apps")
    public List<AppDto> getApps(@RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<App> apps = appRepository.findByUserId(currentUser.getUserId());
        return apps.stream().map(this::appDto).collect(Collectors.toList());
    }

    @PostMapping("/apps")
    public AppDto createApp(CreateAppRequest request, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        App app = new App();
        app.setAppId(UUID.randomUUID().toString());
        app.setUserId(currentUser.getUserId());
        app.setAccessKey(UUID.randomUUID().toString());
        app.setSecretKey(UUID.randomUUID().toString());
        app.setName(request.getName());
        appRepository.save(app);

        return appDto(app);
    }

    @DeleteMapping("/apps/{appId}")
    public void deleteApp(@PathVariable String appId, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        App app = appRepository.findByAppId(appId);
        if (app == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!app.getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        appRepository.deleteById(app.getId());
    }

    private AppDto appDto(App app) {
        AppDto appDto = new AppDto();
        appDto.setId(app.getAppId());
        appDto.setName(app.getName());
        appDto.setKey(app.getAccessKey());
        appDto.setSecret(app.getSecretKey());
        appDto.setCreatedAt(app.getCreatedAt() != null ? app.getCreatedAt().toInstant().toString() : null);
        return appDto;
    }
}
