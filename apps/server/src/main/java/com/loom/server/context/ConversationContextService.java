package com.loom.server.context;

import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationContextService {
    private final ContextSnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ContextPanelAssembler assembler = new ContextPanelAssembler();

    public ConversationContextService(ContextSnapshotRepository snapshotRepository, ApplicationEventPublisher eventPublisher) {
        this.snapshotRepository = snapshotRepository;
        this.eventPublisher = eventPublisher;
    }

    public ContextPanelView assemble(ConversationContextInputs inputs) {
        return assembler.assemble(inputs, resolveSnapshots(inputs));
    }

    public ContextSnapshotView storeSnapshot(ConversationContextInputs inputs) {
        List<ContextSnapshotView> snapshots = resolveSnapshots(inputs);
        ContextSnapshotRecord record = new ContextSnapshotRecord(
                "context-snapshot-" + UUID.randomUUID(),
                inputs.projectId(),
                inputs.conversationId(),
                ContextSnapshotKind.from(inputs.snapshotKind()).wireValue(),
                assembler.renderSnapshotContent(inputs, snapshots),
                inputs.updatedAt()
        );
        ContextSnapshotView snapshot = snapshotRepository.save(record).toView();
        eventPublisher.publishEvent(new ContextSnapshotRecordedEvent(snapshot, assembler.assemble(inputs, prepend(snapshot, snapshots))));
        return snapshot;
    }

    public ContextPanelView assembleAndStore(ConversationContextInputs inputs) {
        ContextSnapshotView snapshot = storeSnapshot(inputs);
        List<ContextSnapshotView> snapshots = snapshotRepository.listByConversation(inputs.projectId(), inputs.conversationId())
                .stream()
                .map(ContextSnapshotRecord::toView)
                .toList();
        return assembler.assemble(inputs, prepend(snapshot, snapshots));
    }

    public List<ContextSnapshotView> listSnapshots(String projectId, String conversationId) {
        return snapshotRepository.listByConversation(projectId, conversationId).stream().map(ContextSnapshotRecord::toView).toList();
    }

    private List<ContextSnapshotView> resolveSnapshots(ConversationContextInputs inputs) {
        if (inputs.snapshots() != null) {
            return List.copyOf(inputs.snapshots());
        }
        return snapshotRepository.listByConversation(inputs.projectId(), inputs.conversationId())
                .stream()
                .map(ContextSnapshotRecord::toView)
                .toList();
    }

    private List<ContextSnapshotView> prepend(ContextSnapshotView snapshot, List<ContextSnapshotView> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of(snapshot);
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(snapshot), snapshots.stream())
                .distinct()
                .toList();
    }
}
