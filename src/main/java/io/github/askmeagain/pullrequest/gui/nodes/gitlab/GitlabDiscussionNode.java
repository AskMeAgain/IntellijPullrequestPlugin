package io.github.askmeagain.pullrequest.gui.nodes.gitlab;

import io.github.askmeagain.pullrequest.dto.application.MergeRequestDiscussion;
import io.github.askmeagain.pullrequest.dto.application.ReviewComment;
import io.github.askmeagain.pullrequest.gui.nodes.BaseTreeNode;
import lombok.RequiredArgsConstructor;

import javax.swing.tree.DefaultMutableTreeNode;

@RequiredArgsConstructor
public class GitlabDiscussionNode extends BaseTreeNode {

  private final MergeRequestDiscussion gitlabDiscussion;

  @Override
  public String toString() {
    return "Discussion: " + gitlabDiscussion.getDiscussionId();
  }

  @Override
  public void onCreation() {
    gitlabDiscussion.getReviewComments()
        .stream()
        .map(ReviewComment::toString)
        .map(DefaultMutableTreeNode::new)
        .forEach(this::add);
  }
}