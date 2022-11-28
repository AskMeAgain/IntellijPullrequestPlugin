package io.github.askmeagain.pullrequest;

import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffExtension;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.simple.SimpleDiffViewer;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TextExtension extends DiffExtension {

  @Getter(lazy = true)
  private final PullrequestService pullrequestService = PullrequestService.getInstance();

  @Override
  public void onViewerCreated(FrameDiffTool.@NotNull DiffViewer viewer, @NotNull DiffContext context, @NotNull DiffRequest request) {
    var left = ((SimpleDiffViewer) viewer).getEditor1();
    var right = ((SimpleDiffViewer) viewer).getEditor2();

    var textAttributes = new TextAttributes(null, JBColor.RED, null, null, Font.PLAIN);

    var index = request.getUserData(PullrequestToolWindow.TEST_123);
    var reviewComments = getPullrequestService().getMergeRequests().get(index).getFiles().get(0).getReviewComments();

    for (var reviewComment : reviewComments) {
      var textRange = reviewComment.getTextRange();
      var markupModel = left.getMarkupModel();

      markupModel.addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.ERROR, textAttributes, HighlighterTargetArea.EXACT_RANGE);
    }

    left.addEditorMouseMotionListener(new OnHoverOverCommentListener(reviewComments));

  }
}
