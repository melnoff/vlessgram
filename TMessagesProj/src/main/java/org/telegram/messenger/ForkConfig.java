package org.telegram.messenger;

import android.content.SharedPreferences;

/**
 * Configuration flags for fork-specific features.
 * Settings are stored in the global "forkconfig" SharedPreferences.
 */
public class ForkConfig {

    private static final String PREFS_NAME = "forkconfig";

    public static final String KEY_HIDE_STORIES = "hide_stories";
    public static final String KEY_DISABLE_UPDATE_CHECK = "disable_update_check";
    public static final String KEY_HIDE_AVATAR_STORY_RINGS = "hide_avatar_story_rings";
    public static final String KEY_DISABLE_AVATAR_STORY_TAP = "disable_avatar_story_tap";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, 0);
    }

    public static boolean hideStories() {
        return prefs().getBoolean(KEY_HIDE_STORIES, true);
    }

    public static boolean disableUpdateCheck() {
        return prefs().getBoolean(KEY_DISABLE_UPDATE_CHECK, false);
    }

    public static boolean hideAvatarStoryRings() {
        return prefs().getBoolean(KEY_HIDE_AVATAR_STORY_RINGS, true);
    }

    public static boolean disableAvatarStoryTap() {
        return prefs().getBoolean(KEY_DISABLE_AVATAR_STORY_TAP, true);
    }

    public static void setBool(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean getBool(String key, boolean defaultValue) {
        return prefs().getBoolean(key, defaultValue);
    }
}
