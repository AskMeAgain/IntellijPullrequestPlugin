package io.github.askmeagain.pullrequest.gui.nodes.gitlab;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import io.github.askmeagain.pullrequest.gui.nodes.BaseTreeNode;
import io.github.askmeagain.pullrequest.services.DataRequestService;
import io.github.askmeagain.pullrequest.services.vcs.gitlab.GitlabService;
import lombok.RequiredArgsConstructor;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

@RequiredArgsConstructor
public class GitlabMergeRequestNode extends BaseTreeNode {

  private final String display;
  private final String mergeRequestId;
  private final Tree tree;
  private final Project project;
  private final String sourceBranch;
  private final String targetBranch;
  private final String connectionName;

  private final GitlabService gitlabService = GitlabService.getInstance();

  @Override
  public String toString() {
    return String.format("%s: %s", mergeRequestId, display);
  }

  @Override
  public void onCreation() {
    //so this is expandable
    this.add(new DefaultMutableTreeNode("hidden"));
  }

  @Override
  public void onExpanded() {
    this.removeAllChildren();

    gitlabService.getFilesOfPr(connectionName, mergeRequestId)
        .stream()
        .map(file -> new GitlabFileNode(project, sourceBranch, targetBranch, file, mergeRequestId, connectionName, tree))
        .forEach(this::add);

    var model = (DefaultTreeModel) tree.getModel();
    model.reload();
  }
}
