package com.loom.node.service;

import com.loom.node.model.NodeSnapshot;

public interface NodeSnapshotProvider {

    NodeSnapshot captureSnapshot();
}
