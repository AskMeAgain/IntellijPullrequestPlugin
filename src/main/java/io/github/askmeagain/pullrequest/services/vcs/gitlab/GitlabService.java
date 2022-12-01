package io.github.askmeagain.pullrequest.services.vcs.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import io.github.askmeagain.pullrequest.dto.application.MergeRequest;
import io.github.askmeagain.pullrequest.dto.application.PullrequestPluginState;
import io.github.askmeagain.pullrequest.dto.application.ReviewComment;
import io.github.askmeagain.pullrequest.dto.gitlab.diffs.GitlabMergeRequestFileDiff;
import io.github.askmeagain.pullrequest.dto.gitlab.discussion.GitlabDiscussionResponse;
import io.github.askmeagain.pullrequest.dto.gitlab.mergerequest.GitlabMergeRequestResponse;
import io.github.askmeagain.pullrequest.services.PluginManagementService;
import io.github.askmeagain.pullrequest.services.vcs.VcsService;
import io.github.askmeagain.pullrequest.settings.PersistenceManagementService;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public final class GitlabService implements VcsService {

  @Getter(lazy = true)
  private final PullrequestPluginState state = PersistenceManagementService.getInstance().getState();

  public static GitlabService getInstance() {
    return ApplicationManager.getApplication().getService(GitlabService.class);
  }

  @SneakyThrows
  public List<MergeRequest> getMergeRequests() {
    return getGitlabMergeRequests()
        .stream()
        .map(pr -> MergeRequest.builder()
            .id(pr.getIid() + "")
            .name(pr.getTitle())
            .sourceBranch(pr.getSource_branch())
            .targetBranch(pr.getTarget_branch())
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getFilesOfPr(String mergeRequestId) {
    var projectId = getState().getGitlabProjects().get(0);
    return GitlabRestClient.getInstance().getMergeRequestDiff(projectId, mergeRequestId).stream()
        .map(GitlabMergeRequestFileDiff::getNew_path)
        .collect(Collectors.toList());
  }

  @Override
  public List<ReviewComment> getCommentsOfPr() {
    return getDiscussionsOfPr()
        .stream()
        .map(GitlabDiscussionResponse::getNotes)
        .flatMap(Collection::stream)
        .map(x -> ReviewComment.builder()
            .line(x.getPosition().getOld_line())
            .text(x.getBody())
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public String getFileOfBranch(String branch, String filePath) {
    var projectId = getState().getGitlabProjects().get(0);
    return GitlabRestClient.getInstance().getFileOfBranch(projectId, filePath, branch);
  }

  @SneakyThrows
  private List<GitlabDiscussionResponse> getDiscussionsOfPr() {
    //GET /projects/:id/merge_requests/:merge_request_iid/discussions
    var exampleString = getReadString("gitlab-discussion-example.json");
    var actualResponse = new ObjectMapper().readValue(exampleString, GitlabDiscussionResponse[].class);
    return List.of(actualResponse);
  }

  @SneakyThrows
  private List<GitlabMergeRequestResponse> getGitlabMergeRequests() {
    var projectId = getState().getGitlabProjects().get(0);
    return GitlabRestClient.getInstance().getMergeRequests(projectId);
  }

  @SneakyThrows
  private static String getReadString(String path) {
    return new String(PluginManagementService.class.getClassLoader().getResourceAsStream(path).readAllBytes());
  }
}
