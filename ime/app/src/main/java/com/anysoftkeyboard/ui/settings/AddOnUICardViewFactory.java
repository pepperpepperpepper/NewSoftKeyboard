package com.anysoftkeyboard.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.TextViewCompat;
import com.anysoftkeyboard.base.utils.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the UI card views for add-on messages. Extracted from {@link MainFragment} to trim it.
 */
final class AddOnUICardViewFactory {

  interface LinkHandler {
    void onLink(@NonNull String rawUrl);
  }

  interface DestinationHandler {
    void onDestination(@NonNull String destination);
  }

  private AddOnUICardViewFactory() {}

  @Nullable
  static View create(
      @NonNull Context context,
      @NonNull AddOnUICard card,
      @NonNull LinkHandler linkHandler,
      @NonNull DestinationHandler destinationHandler) {
    Logger.d("AddOnUICard", "create() called for: " + card.getTitle());
    try {
      CardView cardView = new CardView(context);
      cardView.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
      cardView.setRadius(8f);
      cardView.setCardElevation(8f);
      cardView.setContentPadding(16, 16, 16, 16);

      LinearLayout contentContainer = new LinearLayout(context);
      contentContainer.setOrientation(LinearLayout.VERTICAL);
      contentContainer.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      cardView.addView(contentContainer);

      TextView titleView = new TextView(context);
      TextViewCompat.setTextAppearance(titleView, android.R.style.TextAppearance_Medium);
      titleView.setTextColor(Color.parseColor("#D84315"));
      titleView.setPadding(8, 8, 8, 8);
      titleView.setText("🎯 " + card.getTitle());
      contentContainer.addView(titleView);

      String sanitizedMessage = sanitizeMessage(card.getMessage());
      String[] segments = sanitizedMessage.split("(?i)<br\\s*/?>\\s*<br\\s*/?>");
      for (String segment : segments) {
        if (TextUtils.isEmpty(segment.trim())) continue;
        TextView messageView = new TextView(context);
        TextViewCompat.setTextAppearance(messageView, android.R.style.TextAppearance_Small);
        messageView.setTextColor(Color.parseColor("#BF360C"));
        messageView.setPadding(12, 8, 8, 4);

        Spanned spanned = HtmlCompat.fromHtml(segment, HtmlCompat.FROM_HTML_MODE_COMPACT);
        SpannableStringBuilder sb = new SpannableStringBuilder(spanned);
        URLSpan[] urlSpans = sb.getSpans(0, sb.length(), URLSpan.class);
        List<String> linkTargets = new ArrayList<>();

        for (URLSpan urlSpan : urlSpans) {
          int start = sb.getSpanStart(urlSpan);
          int end = sb.getSpanEnd(urlSpan);
          sb.removeSpan(urlSpan);
          URLSpan replacementSpan = createAddOnLinkSpan(urlSpan.getURL(), linkHandler);
          if (replacementSpan != null) {
            sb.setSpan(replacementSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            linkTargets.add(urlSpan.getURL());
          }
        }

        if (!linkTargets.isEmpty()) {
          messageView.setText(sb, TextView.BufferType.SPANNABLE);
          messageView.setMovementMethod(LinkMovementMethod.getInstance());
          messageView.setLinksClickable(true);
          messageView.setFocusable(true);
          messageView.setClickable(true);
          if (linkTargets.size() == 1) {
            String singleLink = linkTargets.get(0);
            messageView.setOnClickListener(v -> linkHandler.onLink(singleLink));
          }
        } else {
          messageView.setText(sb);
        }

        contentContainer.addView(messageView);
      }

      if (!TextUtils.isEmpty(card.getTargetFragment())) {
        cardView.setOnClickListener(v -> destinationHandler.onDestination(card.getTargetFragment()));
        cardView.setClickable(true);
      }

      Logger.d("AddOnUICard", "Created card view for: " + card.getTitle());
      return cardView;
    } catch (Exception e) {
      Logger.e("AddOnUICard", "Exception creating card view for " + card.getTitle(), e);
      return null;
    }
  }

  @Nullable
  private static URLSpan createAddOnLinkSpan(
      @Nullable String rawUrl, @NonNull LinkHandler linkHandler) {
    if (TextUtils.isEmpty(rawUrl)) {
      return null;
    }
    return new URLSpan(rawUrl) {
      @Override
      public void onClick(@NonNull View widget) {
        linkHandler.onLink(rawUrl);
      }
    };
  }

  @NonNull
  private static String sanitizeMessage(@NonNull String rawMessage) {
    return rawMessage.replaceAll("<a\\s+href=([^\"'>\\s]+)>", "<a href=\"$1\">");
  }
}
