package com.loom.server.context;

import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;

public record ContextSnapshotRecordedEvent(ContextSnapshotView snapshot, ContextPanelView context) {
}
