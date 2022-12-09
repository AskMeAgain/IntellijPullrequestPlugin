package io.github.askmeagain.pullrequest.gui.nodes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import io.github.askmeagain.pullrequest.gui.nodes.interfaces.NodeBehaviour;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class BaseTreeNode extends DefaultMutableTreeNode implements NodeBehaviour {

  @Override
  public void refresh() {
    for (int i = 0; i < getChildCount(); i++) {
      var child = getChildAt(i);
      if (child instanceof NodeBehaviour) {
        var casted = (NodeBehaviour) child;
        casted.refresh();
      }
    }
  }

  @Override
  public void onCreation() {

  }

  @Override
  public void beforeExpanded() {

  }

  @Override
  public void onClick() {

  }

  protected Project getActiveProject() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      Window window = WindowManager.getInstance().suggestParentWindow(project);
      if (window != null && window.isActive()) {
        return project;
      }
    }
    throw new RuntimeException("Could not find active project");
  }
}
