package io.github.askmeagain.pullrequest.gui.nodes.gitlab;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import io.github.askmeagain.pullrequest.dto.TransferKey;
import io.github.askmeagain.pullrequest.dto.application.MergeRequestDiscussion;
import io.github.askmeagain.pullrequest.dto.application.ReviewFile;
import io.github.askmeagain.pullrequest.services.DataRequestService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GitlabFileNode extends DefaultMutableTreeNode implements NodeBehaviour {

  private final Project project;
  private final String sourceBranch;
  private final String targetBranch;
  private final String filePath;
  private final String mergeRequestId;
  private final String connectionName;
  private final Tree tree;


  private final DataRequestService dataRequestService = DataRequestService.getInstance();

  @Override
  public void onClick() {
    var sourceFile = dataRequestService.getFileOfBranch(connectionName, sourceBranch, filePath);
    var targetFile = dataRequestService.getFileOfBranch(connectionName, targetBranch, filePath);

    var comments = dataRequestService.getCommentsOfPr(connectionName, mergeRequestId);

    var sourceComments = comments.stream().filter(MergeRequestDiscussion::isSourceDiscussion).collect(Collectors.toList());
    var targetComments = comments.stream().filter(x -> !x.isSourceDiscussion()).collect(Collectors.toList());

    var sourceReviewFile = ReviewFile.builder()
        .fileContent(sourceFile)
        .fileName(sourceBranch)
        .reviewDiscussions(sourceComments)
        .build();

    var targetReviewFile = ReviewFile.builder()
        .fileContent(targetFile)
        .fileName(targetBranch)
        .reviewDiscussions(targetComments)
        .build();

    var content1 = DiffContentFactory.getInstance().create(sourceFile);
    var content2 = DiffContentFactory.getInstance().create(targetFile);
    var request = new SimpleDiffRequest(
        filePath,
        content2,
        content1,
        targetBranch,
        sourceBranch
    );

    request.putUserData(TransferKey.AllDiscussions, comments);

    request.putUserData(TransferKey.DataContextKeySource, sourceReviewFile);
    request.putUserData(TransferKey.DataContextKeyTarget, targetReviewFile);

    request.putUserData(TransferKey.FileName, filePath);
    request.putUserData(TransferKey.ConnectionName, connectionName);
    request.putUserData(TransferKey.MergeRequestId, mergeRequestId);

    DiffManager.getInstance().showDiff(project, request);

    for (var comment : comments) {
      var discussionNode = new DiscussionNode(comment);
      discussionNode.onCreation();
      this.add(discussionNode);
      tree.expandPath(new TreePath(this.getPath()));
    }
  }

  @Override
  public String toString() {
    return filePath;
  }

  @Override
  public void refresh() {

  }

  @Override
  public void onCreation() {

  }

  @Override
  public void onExpanded() {

  }
}
