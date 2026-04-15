package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ForkConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Custom fork settings - toggles for fork-specific features.
 */
public class ForkSettingsActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("VLessGram Settings");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        frameLayout.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(linearLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0));

        // === Stories section ===
        HeaderCell storiesHeader = new HeaderCell(context);
        storiesHeader.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        storiesHeader.setText("Stories");
        linearLayout.addView(storiesHeader);

        addCheckCell(context, linearLayout,
                "Hide stories bar",
                "Hide the stories panel at the top of the chat list",
                ForkConfig.KEY_HIDE_STORIES, true, true);

        addCheckCell(context, linearLayout,
                "Hide story rings on avatars",
                "Don't show colored rings around avatars with stories",
                ForkConfig.KEY_HIDE_AVATAR_STORY_RINGS, true, true);

        addCheckCell(context, linearLayout,
                "Don't open stories on avatar tap",
                "Tapping avatar in chat list won't open stories",
                ForkConfig.KEY_DISABLE_AVATAR_STORY_TAP, true, false);

        TextInfoPrivacyCell storiesInfo = new TextInfoPrivacyCell(context);
        storiesInfo.setText("Restart app to apply story-related changes");
        linearLayout.addView(storiesInfo);

        // === Updates section ===
        HeaderCell updatesHeader = new HeaderCell(context);
        updatesHeader.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        updatesHeader.setText("Updates");
        linearLayout.addView(updatesHeader);

        addCheckCell(context, linearLayout,
                "Disable update checks",
                "Don't check for app updates automatically",
                ForkConfig.KEY_DISABLE_UPDATE_CHECK, false, false);

        ShadowSectionCell shadow = new ShadowSectionCell(context);
        linearLayout.addView(shadow);

        // === About section ===
        HeaderCell aboutHeader = new HeaderCell(context);
        aboutHeader.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        aboutHeader.setText("About");
        linearLayout.addView(aboutHeader);

        TextInfoPrivacyCell aboutCell = new TextInfoPrivacyCell(context);
        aboutCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        StringBuilder about = new StringBuilder();
        about.append("VLessGram — Telegram-based client with embedded VLESS proxy support.\n\n");

        try {
            android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            about.append("Version: ").append(pi.versionName).append(" (").append(pi.versionCode).append(")\n");
            about.append("Package: ").append(pi.packageName).append("\n");
        } catch (Exception ignored) {}

        about.append("Build: based on Telegram Android 12.6.4\n\n");
        about.append("Contact: mail@melnoff.com\n");
        about.append("Source: https://github.com/melnoff/vlessgram");
        aboutCell.setText(about.toString());
        linearLayout.addView(aboutCell);

        // === Credits ===
        HeaderCell creditsHeader = new HeaderCell(context);
        creditsHeader.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        creditsHeader.setText("Credits & Licenses");
        linearLayout.addView(creditsHeader);

        TextInfoPrivacyCell creditsCell = new TextInfoPrivacyCell(context);
        creditsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        creditsCell.setText(
                "• Telegram for Android — GNU GPL v2.0\n" +
                "  Copyright © Telegram FZ-LLC, Telegram Messenger Inc.\n" +
                "  https://github.com/DrKLO/Telegram\n\n" +
                "• hiddify-next-core — GNU GPL v3.0\n" +
                "  Copyright © Hiddify\n" +
                "  https://github.com/hiddify/hiddify-next-core\n" +
                "  Used as embedded VLESS/sing-box engine\n\n" +
                "• sing-box — GNU GPL v3.0\n" +
                "  Copyright © nekohasekai (SagerNet)\n" +
                "  https://github.com/SagerNet/sing-box\n" +
                "  Universal proxy platform used via hiddify-core\n\n" +
                "• NanoHTTPD — Modified BSD License\n" +
                "  Used for the in-app debug log viewer\n\n" +
                "This fork is distributed under GNU GPL v3.0 to be compatible with all upstream licenses.\n" +
                "Source code: https://github.com/melnoff/vlessgram"
        );
        linearLayout.addView(creditsCell);

        ShadowSectionCell shadowEnd = new ShadowSectionCell(context);
        linearLayout.addView(shadowEnd);

        return fragmentView;
    }

    private void addCheckCell(Context context, LinearLayout parent, String title, String subtitle,
                              String prefKey, boolean defaultValue, boolean divider) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setTextAndValueAndCheck(title, subtitle, ForkConfig.getBool(prefKey, defaultValue), true, divider);
        cell.setOnClickListener(v -> {
            boolean newValue = !ForkConfig.getBool(prefKey, defaultValue);
            ForkConfig.setBool(prefKey, newValue);
            cell.setChecked(newValue);
        });
        parent.addView(cell);
    }
}
