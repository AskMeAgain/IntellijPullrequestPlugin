package io.github.askmeagain.pullrequest.gui.dialogs;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import io.github.askmeagain.pullrequest.TriConsumer;
import io.github.askmeagain.pullrequest.dto.application.MergeRequestDiscussion;
import io.github.askmeagain.pullrequest.dto.application.ReviewComment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class DiscussionPopup {

  private final List<MergeRequestDiscussion> discussions;
  private final BiConsumer<String, String> onSend;
  private final TriConsumer<String, String, String> onEditComment;
  private final BiConsumer<String, String> onDeleteComment;
  private final JTextArea textArea = new JTextArea();
  private final JButton sendButton = new JButton("Send");
  private final JButton refreshButton = new JButton("Refresh");
  private JTabbedPane tabPanel;
  @Getter
  private String id;

  @Getter
  private JBPopup popup;

  public void create() {
    //INTENDED use jTabbedPane here because of scroll_tab_layout hiding title
    tabPanel = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(tabPanel, tabPanel)
        .setRequestFocus(true)
        .createPopup();

    for (var discussion : discussions) {
      var discussionPanel = createPopup(discussion);
      tabPanel.addTab(discussion.getDiscussionId() + "(" + discussion.getReviewComments().size() + ")", discussionPanel);
    }
  }

  public void refresh(List<MergeRequestDiscussion> discussions) {
    //TODO make this safer
    for (int i = 0; i < tabPanel.getTabCount(); i++) {
      var commentScrollPane = (JBScrollPane) tabPanel.getComponentAt(i);
      var newPanel = createNewCommentChainPanel(discussions.get(i));
      commentScrollPane.setViewportView(newPanel);
    }
  }

  @NotNull
  private JPanel createPopup(MergeRequestDiscussion discussion) {
    var commentScrollPane = new JBScrollPane(createNewCommentChainPanel(discussion));
    var sendTextField = new JBScrollPane(textArea);

    id = discussion.getDiscussionId();

    var dialogPanel = FormBuilder.createFormBuilder()
        .addComponent(commentScrollPane)
        .addComponent(sendTextField)
        .addLabeledComponent(sendButton, refreshButton)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();

    sendTextField.setPreferredSize(new Dimension(400, 100));
    commentScrollPane.setPreferredSize(new Dimension(400, 200));

    sendButton.addActionListener(actionEvent -> onSend.accept(textArea.getText(), discussion.getDiscussionId()));

    return dialogPanel;
  }

  private JPanel createNewCommentChainPanel(MergeRequestDiscussion discussion) {
    var panelBuilder = FormBuilder.createFormBuilder();

    for (var reviewComment : discussion.getReviewComments()) {
      var label = getTextField(discussion.getDiscussionId(), reviewComment);
      panelBuilder = panelBuilder.addComponent(label);
    }

    return panelBuilder.addComponentFillVertically(new JPanel(), 10).getPanel();
  }

  @NotNull
  private JPanel getTextField(String discussionId, ReviewComment reviewComment) {
    var comment = reviewComment.toString();
    var noteId = reviewComment.getNoteId();

    var fakeLabel = new JTextField(comment);
    fakeLabel.setEditable(false);
    fakeLabel.setBorder(null);
    fakeLabel.setBackground(null);
    fakeLabel.setPreferredSize(new Dimension(317, 50));

    var panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(fakeLabel);
    var preferredSize = new Dimension(30, 30);

    var editButton = new JButton("E");
    editButton.addActionListener(l -> {
      onEditComment.consume(textArea.getText(), discussionId, noteId);
      fakeLabel.setText(textArea.getText());
      textArea.setText("");
    });
    editButton.setPreferredSize(preferredSize);
    panel.add(editButton);

    var deleteButton = new JButton("X");
    deleteButton.addActionListener(l -> {
      onDeleteComment.accept(discussionId, noteId);
      panel.getParent().remove(panel);
    });
    deleteButton.setPreferredSize(preferredSize);
    panel.add(deleteButton);

    return panel;
  }
}
