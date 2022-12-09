package io.github.askmeagain.pullrequest.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.FormBuilder;
import io.github.askmeagain.pullrequest.dto.application.ConnectionConfig;
import io.github.askmeagain.pullrequest.dto.application.VcsImplementation;
import io.github.askmeagain.pullrequest.settings.integrations.GitlabIntegrationPanelFactory;
import io.github.askmeagain.pullrequest.settings.integrations.IntegrationFactory;
import lombok.Getter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PullrequestSettingsWindow {

  private final JPanel panel;

  private final JComboBox<VcsImplementation> selectedVcsImplementation = new ComboBox<>(new VcsImplementation[]{
      VcsImplementation.GITLAB,
      VcsImplementation.TEST
  });

  @Getter
  private final List<ConnectionConfig> connectionConfigs;
  @Getter
  private final ColorPanel mergeRequestColor = new ColorPanel();
  @Getter
  private final ColorPanel fileColor = new ColorPanel();
  @Getter
  private final ColorPanel mergeRequestHintsInDiffView = new ColorPanel();

  public PullrequestSettingsWindow(Map<String, ConnectionConfig> configMap) {
    this.connectionConfigs = new ArrayList<>(configMap.values());
    var tabbedPane = new JBTabbedPane();

    var addProjectButton = new JButton("Add Project");
    addProjectButton.addActionListener(a -> {
      if (selectedVcsImplementation.getSelectedItem() == VcsImplementation.GITLAB) {
        var gitlabConnectionPanel = new GitlabIntegrationPanelFactory(new ConnectionConfig("New Gitlab Connection"));
        var component = gitlabConnectionPanel.create();
        this.connectionConfigs.add(gitlabConnectionPanel.getConfig());
        tabbedPane.insertTab("New Gitlab Connection", null, component, "", tabbedPane.getSelectedIndex());
      } else {
        System.out.println("Not implemented");
      }
    });

    var addTab = FormBuilder.createFormBuilder()
        .addLabeledComponent("Select vcs integration", selectedVcsImplementation)
        .addComponent(addProjectButton)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();

    for (int i = 0; i < connectionConfigs.size(); i++) {
      var connection = connectionConfigs.get(i);
      var impl = resolveComponent(connection);
      var component = impl.create();
      connectionConfigs.set(i, impl.getConfig());
      tabbedPane.addTab(connection.getName(), component);
    }

    tabbedPane.addTab("Add Connection", addTab);

    panel = FormBuilder.createFormBuilder()
        .addComponent(tabbedPane)
        .addSeparator()
        .addComponent(colorPickers())
        .addComponentFillVertically(new JPanel(), 10)
        .getPanel();
  }

  private IntegrationFactory resolveComponent(ConnectionConfig connectionConfig) {
    if (connectionConfig.getVcsImplementation() == VcsImplementation.GITLAB) {
      return new GitlabIntegrationPanelFactory(connectionConfig);
    }
    throw new RuntimeException("whatever");
  }

  private JPanel colorPickers() {
    return FormBuilder.createFormBuilder()
        .addLabeledComponent("MergeRequests", mergeRequestColor)
        .addLabeledComponent("Files", fileColor)
        .addLabeledComponent("MergeRequest comment hints", mergeRequestHintsInDiffView)
        .addComponentFillVertically(new JPanel(), 10)
        .getPanel();
  }

  public JComponent getPreferredFocusedComponent() {
    return panel;
  }
}