package com.loom.server.store;

import com.loom.server.model.Asset;
import com.loom.server.model.AssetType;
import com.loom.server.model.Memory;
import com.loom.server.model.Plan;
import com.loom.server.model.Project;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ObsidianAssetWriter {

    public Path writeAsset(Path serverVaultRoot, Project project, AssetType type, String title, String content, List<String> tags) {
        Objects.requireNonNull(serverVaultRoot, "serverVaultRoot");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");

        try {
            YearMonth yearMonth = YearMonth.now(ZoneOffset.UTC);
            Path targetDir = serverVaultRoot
                    .resolve(slugify(project.name()))
                    .resolve(type.name().toLowerCase(Locale.ROOT))
                    .resolve(String.valueOf(yearMonth.getYear()))
                    .resolve(String.format(Locale.ROOT, "%02d", yearMonth.getMonthValue()));
            Files.createDirectories(targetDir);
            String fileName = slugify(title) + "-" + UUID.randomUUID().toString().substring(0, 8) + ".md";
            Path targetFile = targetDir.resolve(fileName);
            String markdown = toMarkdown(project, type, title, content, tags);
            Files.writeString(targetFile, markdown, StandardCharsets.UTF_8);
            return targetFile;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Obsidian asset", exception);
        }
    }

    public String toMarkdown(Project project, AssetType type, String title, String content, List<String> tags) {
        List<String> safeTags = tags == null ? List.of() : List.copyOf(tags);
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: ").append(title).append('\n');
        builder.append("projectId: ").append(project.id()).append('\n');
        builder.append("projectName: ").append(project.name()).append('\n');
        builder.append("type: ").append(type.name()).append('\n');
        builder.append("tags: [");
        for (int i = 0; i < safeTags.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(safeTags.get(i));
        }
        builder.append("]\n");
        builder.append("createdAt: ").append(Instant.now()).append('\n');
        builder.append("---\n\n");
        builder.append("# ").append(title).append("\n\n");
        builder.append(content).append('\n');
        return builder.toString();
    }

    public Asset toAssetRecord(
            Project project,
            AssetType type,
            String title,
            String contentRef,
            Path storagePath,
            String sourceConversationId,
            String sourcePlanId,
            String sourceNodeId,
            List<String> tags
    ) {
        return new Asset(
                UUID.randomUUID().toString(),
                project.id(),
                type,
                title,
                contentRef,
                storagePath.toString(),
                sourceConversationId,
                sourcePlanId,
                sourceNodeId,
                tags == null ? List.of() : List.copyOf(tags),
                Instant.now()
        );
    }

    public String slugify(String value) {
        String normalized = value == null ? "asset" : value.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "asset" : normalized;
    }
}
