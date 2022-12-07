package io.github.askmeagain.pullrequest.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.ui.treeStructure.Tree;
import io.github.askmeagain.pullrequest.dto.application.PullrequestPluginState;
import io.github.askmeagain.pullrequest.gui.nodes.gitlab.GitlabConnectionNode;
import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

@Service
public final class PluginManagementService {

  private final PullrequestPluginState state = StateService.getInstance().getState();

  private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
  @Getter
  private final Tree tree = new Tree(rootNode);

  public void refreshList() {
    System.out.println("Refresh list!");

    rootNode.removeAllChildren();

    for (var connection : state.getConnectionConfigs()) {
      var connectionNode = new GitlabConnectionNode(connection, tree);
      connectionNode.onCreation();
      rootNode.add(connectionNode);
    }

    var model = (DefaultTreeModel) tree.getModel();
    model.reload();
  }

  public static PluginManagementService getInstance() {
    return ApplicationManager.getApplication().getService(PluginManagementService.class);
  }

}
