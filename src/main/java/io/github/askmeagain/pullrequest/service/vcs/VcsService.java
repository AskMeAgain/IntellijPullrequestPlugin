package io.github.askmeagain.pullrequest.service.vcs;

import io.github.askmeagain.pullrequest.dto.MergeRequest;

import java.util.List;

public interface VcsService {

  List<MergeRequest> getMergeRequests();

}
