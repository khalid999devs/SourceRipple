package io.sourceripple.workspace.application.project;

import java.util.UUID;

public interface ProjectArchiveListener {

    void onArchived(UUID projectId);
}
