package io.github.askmeagain.pullrequest.services.vcs.github;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import io.github.askmeagain.pullrequest.dto.application.*;
import io.github.askmeagain.pullrequest.dto.gitlab.discussionnote.GitlabAddCommentToDiscussionRequest;
import io.github.askmeagain.pullrequest.services.PasswordService;
import io.github.askmeagain.pullrequest.services.StateService;
import io.github.askmeagain.pullrequest.services.vcs.VcsService;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
          connection.getConfigs().get("githubUrl")
      );

      var api = Feign.builder()
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
            .targetBranch(x.getHead().getRef())
            .sourceBranch("whatever")
            .id(x.getNumber() + "")
            .name(x.getTitle())
            .approved(true)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public void addCommentToThread(String projectId, ConnectionConfig connectionName, String mergeRequestId, String discussionId, GitlabAddCommentToDiscussionRequest request) {

  }

  @Override
  public List<String> getFilesOfPr(String projectId, ConnectionConfig connectionName, String mergeRequestId) {
    return null;
  }

  @Override
  public List<MergeRequestDiscussion> getCommentsOfPr(String projectId, ConnectionConfig connectionName, String mergeRequestId) {
    return null;
  }

  @Override
  public String getFileOfBranch(String projectId, ConnectionConfig connectionName, String branch, String filePath) {
    return null;
  }

  @Override
  public void addMergeRequestComment(String projectId, ConnectionConfig connectionName, String mergeRequestId, CommentRequest comment) {

  }

  private String getToken(ConnectionConfig connection) {
    return passwordService.getPassword(connection.getName());
  }

}
