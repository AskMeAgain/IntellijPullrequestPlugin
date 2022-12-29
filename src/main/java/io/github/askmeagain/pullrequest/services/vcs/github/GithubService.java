package io.github.askmeagain.pullrequest.services.vcs.github;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import io.github.askmeagain.pullrequest.dto.application.*;
import io.github.askmeagain.pullrequest.dto.github.comment.GithubMergeRequestCommentRequest;
import io.github.askmeagain.pullrequest.dto.github.diffs.GithubDiffResponse;
import io.github.askmeagain.pullrequest.dto.github.discussions.GithubDiscussionResponse;
import io.github.askmeagain.pullrequest.dto.github.mergerequest.Assignee;
import io.github.askmeagain.pullrequest.dto.gitlab.discussionnote.GitlabAddCommentToDiscussionRequest;
import io.github.askmeagain.pullrequest.services.PasswordService;
import io.github.askmeagain.pullrequest.services.StateService;
import io.github.askmeagain.pullrequest.services.vcs.VcsService;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class GithubService implements VcsService {

  @Getter(lazy = true)
  private final PullrequestPluginState state = StateService.getInstance().getState();

  private final Map<String, GithubApi> apisPerConnection = new HashMap<>();

  private final PasswordService passwordService = PasswordService.getInstance();

  public static GithubService getInstance() {
    return ApplicationManager.getApplication().getService(GithubService.class);
  }

  private GithubApi getOrCreateApi(ConnectionConfig connection) {
    var name = connection.getName();

    if (!apisPerConnection.containsKey(name)) {
      var url = String.format("%s%s",
          connection.getConfigs().get("githubUrl"),
          connection.getConfigs().get("userName")
      );

      var api = Feign.builder()
          .requestInterceptor(template -> template.header("Authorization", "Bearer " + getToken(connection)))
          .client(new OkHttpClient())
          .encoder(new JacksonEncoder())
          .decoder(new JacksonDecoder())
          .target(GithubApi.class, url);
      apisPerConnection.put(name, api);
    }

    return apisPerConnection.get(name);
  }

  @Override
  public List<MergeRequest> getMergeRequests(String projectId, ConnectionConfig connectionName) {
    return getOrCreateApi(connectionName).getMergeRequests(projectId).stream()
        .map(x -> MergeRequest.builder()
            //TODO switch to reviewers
            .commitSha(x.getHead().getSha())
            .reviewer(x.getAssignees().stream().map(Assignee::getAvatar_url).collect(Collectors.toList()))
            .targetBranch(x.getBase().getRef())
            .sourceBranch(x.getHead().getRef())
            .id(x.getNumber() + "")
            .name(x.getTitle())
            .approved(true)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public void addCommentToThread(
      String projectId,
      ConnectionConfig connectionName,
      String mergeRequestId,
      String discussionId,
      GitlabAddCommentToDiscussionRequest request
  ) {
    getOrCreateApi(connectionName).addCommentToThread();
  }

  @Override
  public List<String> getFilesOfPr(String projectId, ConnectionConfig connectionName, String mergeRequestId) {
    return getOrCreateApi(connectionName).getMergerequestDiff(projectId, mergeRequestId).stream()
        .map(GithubDiffResponse::getFilename)
        .collect(Collectors.toList());
  }

  @Override
  public List<MergeRequestDiscussion> getCommentsOfPr(
      String projectId,
      ConnectionConfig connectionName,
      String mergeRequestId,
      String filePath
  ) {
    var discussions = getOrCreateApi(connectionName).getDiscussions(projectId, mergeRequestId);

    var map = discussions.stream()
        .filter(x -> x.getIn_reply_to_id() != null)
        .collect(Collectors.groupingBy(GithubDiscussionResponse::getIn_reply_to_id));

    return discussions.stream()
        .filter(x -> x.getPath().equals(filePath))
        .filter(x -> x.getIn_reply_to_id() == null)
        .map(x -> MergeRequestDiscussion.builder()
            .line(x.getLine() - 1)
            .isSourceDiscussion(x.getSide().equals("RIGHT"))
            .discussionId(x.getId() + "")
            .reviewComment(ReviewComment.builder()
                .text(x.getBody())
                .discussionId(x.getId() + "")
                .author(x.getAuthor_association())
                .build())
            .reviewComments(map.getOrDefault(x.getId() + "", Collections.emptyList()).stream()
                .map(y -> ReviewComment.builder()
                    .text(y.getBody())
                    .discussionId(y.getIn_reply_to_id())
                    .author(y.getAuthor_association())
                    .build())
                .collect(Collectors.toList()))
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public FileResponse getFileOfBranch(String projectId, ConnectionConfig connectionName, String branch, String filePath) {
    var encodedFilePath = encodePath(filePath);

    var response = getOrCreateApi(connectionName).getFileOfBranch(projectId, encodedFilePath, branch);

    return FileResponse.builder()
        .fileContent(new String(Base64.getDecoder().decode(response.getContent().replaceAll("\n", ""))))
        .commitId(response.getSha())
        .build();
  }

  @Override
  public void addMergeRequestComment(
      String projectId,
      ConnectionConfig connectionName,
      String mergeRequestId,
      CommentRequest comment
  ) {
    getOrCreateApi(connectionName).addMergeRequestComment(
        GithubMergeRequestCommentRequest.builder()
            .commit_id(comment.getCommitId())
            .body(comment.getText())
            .line(comment.getLine() + 1)
            .side(comment.isSourceComment() ? "LEFT" : "RIGHT")
            .path(comment.isSourceComment() ? comment.getOldFileName() : comment.getNewFileName())
            .build(),
        projectId,
        mergeRequestId
    );
  }

  public void approveMergeRequest(String projectId, ConnectionConfig connection, String mergeRequestId) {
    throw new NotImplementedException("asd");
    //getOrCreateApi(connection).approveMergeRequest(projectId, mergeRequestId);
  }

  private String getToken(ConnectionConfig connection) {
    return passwordService.getPassword(connection.getName());
  }

  public static String encodePath(String path) {
    return path
        .replaceAll("/", "%2F")
        .replaceAll(" ", "%20")
        .replaceAll("-", "%2D");
  }
}
