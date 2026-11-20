package io.sourceripple.source.application.fetch.failure;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class RobotsDisallowedException extends ApiException {

    public RobotsDisallowedException(String url) {
        super(ErrorCode.ROBOTS_DISALLOWED, "robots.txt does not allow SourceRippleBot to fetch " + url + ".");
    }
}
