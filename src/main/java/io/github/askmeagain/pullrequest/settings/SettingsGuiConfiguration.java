package io.github.askmeagain.pullrequest.settings;

import com.intellij.openapi.options.Configurable;
import io.github.askmeagain.pullrequest.services.PasswordService;
import io.github.askmeagain.pullrequest.services.StateService;
import io.github.askmeagain.pullrequest.settings.integrations.IntegrationFactory;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.askmeagain.pullrequest.services.PluginUtils.toHexColor;

public class SettingsGuiConfiguration implements Configurable {

  @Getter(lazy = true)
  private final StateService stateService = StateService.getInstance();
  private PullrequestSettingsWindow settingsComponent;

  private final PasswordService passwordService = PasswordService.getInstance();

  @Override
  @Nls(capitalization = Nls.Capitalization.Title)
  public String getDisplayName() {
    return "Pullrequest Settings";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return settingsComponent.getPreferredFocusedComponent();
  }

  @Override
  public JComponent createComponent() {
    var state = getStateService().getState();

    settingsComponent = new PullrequestSettingsWindow(state.getMap());

    settingsComponent.getFileColor().setSelectedColor(Color.decode(state.getFileColor()));
    settingsComponent.getMergeRequestColor().setSelectedColor(Color.decode(state.getMergeRequestColor()));
    settingsComponent.getMergeRequestHintsInDiffView().setSelectedColor(Color.decode(state.getMergeRequestCommentHint()));

    return settingsComponent.getPreferredFocusedComponent();
  }

  @Override
  public boolean isModified() {
    var state = getStateService().getState();

    var colorChanged1 = !Objects.equals(Color.decode(state.getFileColor()), settingsComponent.getFileColor().getSelectedColor());
    var colorChanged2 = !Objects.equals(Color.decode(state.getMergeRequestCommentHint()), settingsComponent.getMergeRequestHintsInDiffView().getSelectedColor());
    var colorChanged3 = !Objects.equals(Color.decode(state.getMergeRequestColor()), settingsComponent.getMergeRequestColor().getSelectedColor());

    var anythingChanged = colorChanged1 || colorChanged2 || colorChanged3;
    if (anythingChanged) {
      return true;
    }

    if (state.getConnectionConfigs().size() != settingsComponent.getIntegrationPanels().size()) {
      return true;
    }

    for (int i = 0; i < state.getConnectionConfigs().size(); i++) {
      var s = state.getConnectionConfigs().get(i);
      var s2 = settingsComponent.getIntegrationPanels().get(i).getConfig();
      if (!Objects.equals(s, s2)) {
        return true;
      }

      var p1 = passwordService.getPassword(s.getName());
      var p2 = settingsComponent.getIntegrationPanels().get(i).getPassword();

      if (!Objects.equals(p1, p2)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void apply() {
    var state = getStateService().getState();

    var map = settingsComponent.getIntegrationPanels()
        .stream()
        .peek(x -> passwordService.setPassword(x.getConfig().getName(), x.getPassword()))
        .map(IntegrationFactory::getConfig)
        .collect(Collectors.toList());

    state.setFileColor(toHexColor(settingsComponent.getFileColor().getSelectedColor()));
    state.setMergeRequestColor(toHexColor(settingsComponent.getMergeRequestColor().getSelectedColor()));
    state.setMergeRequestCommentHint(toHexColor(settingsComponent.getMergeRequestHintsInDiffView().getSelectedColor()));

    state.setConnectionConfigs(map);
  }

  @Override
  public void reset() {
    //nothing to do. This is a bug, since this is getting executed multiple times for no reason
  }

  @Override
  public void disposeUIResources() {
    settingsComponent = null;
  }
}