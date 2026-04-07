package com.template.server.service;

import com.template.server.config.TemplateProperties;
import com.template.server.model.BootstrapPayload;
import com.template.server.model.ExtensionPoint;
import com.template.server.model.ReleaseOverview;
import com.template.server.model.SetupTask;
import com.template.server.model.WorkspaceSettings;
import com.template.server.repository.SettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateProperties templateProperties;
    private final SettingsRepository settingsRepository;

    public TemplateService(TemplateProperties templateProperties, SettingsRepository settingsRepository) {
        this.templateProperties = templateProperties;
        this.settingsRepository = settingsRepository;
    }

    public BootstrapPayload getBootstrap() {
        WorkspaceSettings settings = settingsRepository.get();
        return new BootstrapPayload(
                coalesce(templateProperties.getAppName(), "Template Infrastructure Stack"),
                coalesce(templateProperties.getDescription(), "Generic three-app monorepo template with API, web shell, and node agent."),
                settings,
                new ReleaseOverview(
                        templateProperties.getRelease().getInstallRoot(),
                        templateProperties.getRelease().getSystemdUnit(),
                        templateProperties.getRelease().getRegistry()
                ),
                List.of(
                        new SetupTask("env", "Review environment variables", "Copy .env.example and set deployment secrets before the first release.", settings.docsUrl()),
                        new SetupTask("ci", "Validate CI and release", "Run validation locally where possible and confirm GitHub Actions can build and publish images.", settings.docsUrl()),
                        new SetupTask("extend", "Customize the template", "Replace placeholder modules in the API, web shell, and node agent with product logic.", settings.docsUrl())
                ),
                List.of(
                        new ExtensionPoint("API Modules", "apps/server/src/main/java", "Add domain controllers, services, and migrations here."),
                        new ExtensionPoint("Web Shell", "apps/web/src", "Replace the placeholder dashboard with your product routes and components."),
                        new ExtensionPoint("Agent Probes", "apps/node/src/main/java", "Add custom probes or workload collectors to the node agent.")
                )
        );
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
