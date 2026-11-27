package io.sourceripple.workspace.application;

import io.sourceripple.shared.api.error.status.NotFoundException;

public class OrganizationNotFoundException extends NotFoundException {

    public OrganizationNotFoundException() {
        super("The organization was not found.");
    }
}
