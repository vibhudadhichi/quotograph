package com.stanleyidesis.livewallpaperquotes.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.stanleyidesis.livewallpaperquotes.LWQApplication;
import com.stanleyidesis.livewallpaperquotes.LWQPreferences;
import com.stanleyidesis.livewallpaperquotes.R;
import com.stanleyidesis.livewallpaperquotes.api.controller.LWQAlarmController;
import com.stanleyidesis.livewallpaperquotes.api.db.Category;
import com.stanleyidesis.livewallpaperquotes.api.event.ImageSaveEvent;
import com.stanleyidesis.livewallpaperquotes.api.event.PreferenceUpdateEvent;
import com.stanleyidesis.livewallpaperquotes.api.event.WallpaperEvent;
import com.stanleyidesis.livewallpaperquotes.ui.UIUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Copyright (c) 2015 Stanley Idesis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * LWQSettingsActivity.java
 * @author Stanley Idesis
 *
 * From Live-Wallpaper-Quotes
 * https://github.com/stanidesis/live-wallpaper-quotes
 *
 * Please report any issues
 * https://github.com/stanidesis/live-wallpaper-quotes/issues
 *
 * Date: 07/11/2015
 */
public class LWQSettingsActivity extends LWQWallpaperActivity implements SeekBar.OnSeekBarChangeListener {

    // Flags
    final static int FLAG_NO_CHANGE   = -1;
    final static int FLAG_REVEAL      = 1;
    final static int FLAG_HIDE        = 2;
    final static int FLAG_ROTATE      = 4;
    final static int FLAG_NO_ROTATE   = 8;
    final static int FLAG_ENABLE      = 16;
    final static int FLAG_DISABLE     = 32;
    final static int FLAG_SELECTED    = 64;
    final static int FLAG_UNSELECTED  = 128;

    enum ActivityState {
        INITIAL(SilkScreenState.HIDDEN,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_HIDE),
        REVEAL_CONTROLS(SilkScreenState.OBSCURED,
                FLAG_REVEAL | FLAG_ENABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_ENABLE | FLAG_UNSELECTED,
                FLAG_ENABLE | FLAG_UNSELECTED,
                FLAG_ENABLE | FLAG_UNSELECTED,
                FLAG_ENABLE | FLAG_UNSELECTED,
                FLAG_ENABLE | FLAG_UNSELECTED,
                FLAG_ENABLE),
        REVEAL_WALLPAPER(SilkScreenState.REVEALED,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_DISABLE | FLAG_UNSELECTED,
                FLAG_HIDE),
        REVEAL_ADJUSTMENTS(SilkScreenState.OBSCURED,
                FLAG_REVEAL | FLAG_ENABLE,
                FLAG_REVEAL | FLAG_ENABLE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_SELECTED,
                FLAG_NO_CHANGE,
                FLAG_UNSELECTED,
                FLAG_NO_CHANGE),
        REVEAL_SETTINGS(SilkScreenState.OBSCURED,
                FLAG_NO_CHANGE,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_REVEAL | FLAG_ENABLE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_UNSELECTED,
                FLAG_NO_CHANGE,
                FLAG_SELECTED,
                FLAG_NO_CHANGE),
        REVEAL_WALLPAPER_EDIT_MODE(SilkScreenState.REVEALED,
                FLAG_HIDE | FLAG_DISABLE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE,
                FLAG_NO_CHANGE);
//        SAVE_TO_DISK,
//        SKIP;

        ActivityState(SilkScreenState silkScreenState,
                      int controlFlags,
                      int adjustableSettingsFlags,
                      int settingsFlags,
                      int actionShareFlags,
                      int actionSaveFlags,
                      int actionAdjustFlags,
                      int actionSkipFlags,
                      int actionSettingsFlags,
                      int progressBarFlags) {
            this.silkScreenState = silkScreenState;
            this.controlFlags = controlFlags;
            this.adjustableSettingsFlags = adjustableSettingsFlags;
            this.settingsFlags = settingsFlags;
            this.actionShareFlags = actionShareFlags;
            this.actionSaveFlags = actionSaveFlags;
            this.actionAdjustFlags = actionAdjustFlags;
            this.actionSkipFlags = actionSkipFlags;
            this.actionSettingsFlags = actionSettingsFlags;
            this.progressBarFlags = progressBarFlags;
        }

        SilkScreenState silkScreenState;
        int controlFlags;
        int adjustableSettingsFlags;
        int settingsFlags;
        int actionShareFlags;
        int actionSaveFlags;
        int actionAdjustFlags;
        int actionSkipFlags;
        int actionSettingsFlags;
        int progressBarFlags;

        boolean controlFlagSet(int compareWith) {
            return (controlFlags & compareWith) > 0;
        }

        boolean adjustableSettingsFlagSet(int compareWith) {
            return (adjustableSettingsFlags & compareWith) > 0;
        }

        boolean settingsFlagSet(int compareWith) {
            return (settingsFlags & compareWith) > 0;
        }

        boolean progressBarFlagsSet(int compareWith) {
            return (progressBarFlags & compareWith) > 0;
        }
    }

    Runnable changeStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (stateBlockingDeque.isEmpty()) {
                return;
            }
            ActivityState nextActivityState = null;
            try {
                nextActivityState = stateBlockingDeque.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (nextActivityState == null || nextActivityState == activityState) {
                return;
            }

            long longestAnimation = 0l;

            // SilkScreenState
            final SilkScreenState newSilkScreenState = nextActivityState.silkScreenState;
            if (LWQSettingsActivity.this.silkScreenState != newSilkScreenState) {
                longestAnimation = 300l; // TODO HAX
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateSilkScreen(newSilkScreenState);
                    }
                });
            }

            // Control flags
            int controlFlags = (int) wallpaperActionsContainer.getTag(R.id.view_tag_flags);
            if (nextActivityState.controlFlags != controlFlags && nextActivityState.controlFlags != FLAG_NO_CHANGE) {
                // Enable/disable
                if (nextActivityState.controlFlagSet(FLAG_DISABLE) || nextActivityState.controlFlagSet(FLAG_ENABLE)) {
                    final boolean enabled = nextActivityState.controlFlagSet(FLAG_ENABLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            wallpaperActionsContainer.setEnabled(enabled);
                        }
                    });
                }
                // Reveal/Hide
                if (nextActivityState.controlFlagSet(FLAG_HIDE) || nextActivityState.controlFlagSet(FLAG_REVEAL)) {
                    longestAnimation = Math.max(longestAnimation, 300l); // TODO HAX
                    final boolean dismiss = nextActivityState.controlFlagSet(FLAG_HIDE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            animateControls(dismiss);
                        }
                    });
                }
                wallpaperActionsContainer.setTag(R.id.view_tag_flags, nextActivityState.controlFlags);
            }

            // Adjustable Settings flags
            int adjustableSettingsFlags = (int) adjustableSettingsContainer.getTag(R.id.view_tag_flags);
            if (nextActivityState.adjustableSettingsFlags != adjustableSettingsFlags && nextActivityState.adjustableSettingsFlags != FLAG_NO_CHANGE) {
                // Enable/disable
                if (nextActivityState.adjustableSettingsFlagSet(FLAG_DISABLE) || nextActivityState.adjustableSettingsFlagSet(FLAG_ENABLE)) {
                    final boolean enable = nextActivityState.adjustableSettingsFlagSet(FLAG_ENABLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIUtils.setViewAndChildrenEnabled(adjustableSettingsContainer, enable);
                        }
                    });
                }
                // Reveal/Hide
                if (nextActivityState.adjustableSettingsFlagSet(FLAG_HIDE) || nextActivityState.adjustableSettingsFlagSet(FLAG_REVEAL)) {
                    final boolean dismiss = nextActivityState.adjustableSettingsFlagSet(FLAG_HIDE);
                    longestAnimation = Math.max(longestAnimation, dismiss ? 300l : 600l); // TODO HAX
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            animateSettingsContainer(adjustableSettingsContainer, dismiss);
                        }
                    });
                }
                adjustableSettingsContainer.setTag(R.id.view_tag_flags, nextActivityState.adjustableSettingsFlags);
            }

            // Settings flags
            int settingsFlags = (int) settingsContainer.getTag(R.id.view_tag_flags);
            if (nextActivityState.settingsFlags != settingsFlags && nextActivityState.settingsFlags != FLAG_NO_CHANGE) {
                // Enable/disable
                if (nextActivityState.settingsFlagSet(FLAG_DISABLE) || nextActivityState.settingsFlagSet(FLAG_ENABLE)) {
                    final boolean enable = nextActivityState.settingsFlagSet(FLAG_ENABLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIUtils.setViewAndChildrenEnabled(settingsContainer, enable);
                        }
                    });
                }
                // Reveal/Hide
                if (nextActivityState.settingsFlagSet(FLAG_HIDE) || nextActivityState.settingsFlagSet(FLAG_REVEAL)) {
                    final boolean dismiss = nextActivityState.settingsFlagSet(FLAG_HIDE);
                    longestAnimation = Math.max(longestAnimation, dismiss ? 300l : 600l); // TODO HAX
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            animateSettingsContainer(settingsContainer, dismiss);
                        }
                    });
                }
                settingsContainer.setTag(R.id.view_tag_flags, nextActivityState.settingsFlags);
            }

            // Actions
            int [] actionButtonFlags = new int [] {nextActivityState.actionShareFlags,
                    nextActivityState.actionSaveFlags, nextActivityState.actionAdjustFlags,
                    nextActivityState.actionSkipFlags, nextActivityState.actionSettingsFlags};
            View [] actionViews = new View[] {shareButton, saveButton, adjustButton, skipButton, settingsButton};
            for (int i = 0; i < actionButtonFlags.length; i++) {
                final View actionView = actionViews[i];
                int newButtonFlags = actionButtonFlags[i];
                int buttonFlags = (int) actionView.getTag(R.id.view_tag_flags);
                if (buttonFlags == newButtonFlags || newButtonFlags == FLAG_NO_CHANGE) {
                    continue;
                }
                // Enable/disable
                final boolean disable = (newButtonFlags & FLAG_DISABLE) > 0;
                final boolean enable = (newButtonFlags & FLAG_ENABLE) > 0;
                if (disable || enable) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actionView.setEnabled(enable);
                        }
                    });
                }

                // Selected/Unselected
                final boolean selected = (newButtonFlags & FLAG_SELECTED) > 0;
                final boolean unselected = (newButtonFlags & FLAG_UNSELECTED) > 0;
                if ((selected || unselected) && (actionView.isSelected() != selected)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actionView.setSelected(selected);
                        }
                    });
                }

                // Rotate/Stop rotate
                boolean rotate = (newButtonFlags & FLAG_ROTATE) > 0;
                boolean stopRotate = (newButtonFlags & FLAG_NO_ROTATE) > 0;
                if (stopRotate) {
                    final Object tag = actionView.getTag(R.id.view_tag_animator);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (tag instanceof Animator) {
                                Animator animator = (Animator) tag;
                                animator.end();
                            }
                        }
                    });
                    actionView.setTag(R.id.view_tag_animator, null);
                } else if (rotate) {
                    final Animator rotationAnimator = AnimatorInflater.loadAnimator(LWQSettingsActivity.this, R.animator.progress_rotation);
                    rotationAnimator.setTarget(actionViews[i]);
                    actionView.setTag(R.id.view_tag_animator, rotationAnimator);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Object tag = actionView.getTag(R.id.view_tag_animator);
                            if (tag instanceof Animator) {
                                ((Animator) tag).start();
                            }
                        }
                    });
                }
                actionView.setTag(R.id.view_tag_flags, actionButtonFlags[i]);
            }

            // Progress bar
            int progressBarFlags = (int) progressBar.getTag(R.id.view_tag_flags);
            if (nextActivityState.progressBarFlags != progressBarFlags && nextActivityState.progressBarFlags != FLAG_NO_CHANGE) {
                // Reveal/Hide
                if (nextActivityState.progressBarFlagsSet(FLAG_HIDE) || nextActivityState.progressBarFlagsSet(FLAG_REVEAL)) {
                    final boolean dismiss = nextActivityState.progressBarFlagsSet(FLAG_HIDE);
                    longestAnimation = Math.max(longestAnimation, 150l); // TODO HAX
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.animate()
                                    .alpha(dismiss ? 0f : 1f)
                                    .setDuration(150)
                                    .setInterpolator(new AccelerateDecelerateInterpolator())
                                    .start();
                        }
                    });
                }
                progressBar.setTag(R.id.view_tag_flags, nextActivityState.progressBarFlags);
            }
            activityState = nextActivityState;
            try {
                Thread.sleep(longestAnimation);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    // Current ActivityState
    ActivityState activityState = null;
    // State Queue
    BlockingDeque<ActivityState> stateBlockingDeque = new LinkedBlockingDeque<>();
    // Executes state changes
    ExecutorService changeStateExecutorService = Executors.newSingleThreadScheduledExecutor();

    Timer revealControlsTimer;
    TimerTask revealControlsTimerTask = new TimerTask() {
        @Override
        public void run() {
            changeState(ActivityState.REVEAL_CONTROLS);
        }
    };

    // ProgressBar
    ProgressBar progressBar;
    // Autopilot Views
    View settingsContainer;
    // Adjustable Views
    View adjustableSettingsContainer;
    // Wallpaper Actions
    View wallpaperActionsContainer;
    View shareButton;
    View saveButton;
    View adjustButton;
    View skipButton;
    View settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, 0);
        setContentView(R.layout.activity_lwq_settings);

        // Setup Autopilot stuff
        setupAutopilotSettings();
        // Setup Adjustable stuff
        setupAdjustableSettings();
        // Setup Wallpaper actions
        setupWallpaperActions();
        // Setup progress bar
        setupProgressBar();
        // Setup touch to dismiss
        setupTouchToDismiss();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        changeState(ActivityState.INITIAL);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    void setupAutopilotSettings() {
        settingsContainer = findViewById(R.id.group_lwq_settings_autopilot);
        settingsContainer.setAlpha(0f);
        settingsContainer.setTag(R.id.view_tag_flags, FLAG_HIDE | FLAG_DISABLE);
        UIUtils.setViewAndChildrenEnabled(settingsContainer, false);

        final List<String> backgroundCategories = LWQApplication.getWallpaperController().getBackgroundCategories();
        final String imageCategoryPreference = LWQPreferences.getImageCategoryPreference();
        int currentSelection = 0;
        for (String category : backgroundCategories) {
            if (category.equalsIgnoreCase(imageCategoryPreference)) {
                currentSelection = backgroundCategories.indexOf(category);
                break;
            }
        }
        ArrayAdapter<String> imageCategoryAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item,
                backgroundCategories);
        imageCategoryAdapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
        Spinner imageCategorySpinner = (Spinner) settingsContainer.findViewById(R.id.spinner_lwq_autopilot_settings_image_category);
        imageCategorySpinner.setAdapter(imageCategoryAdapter);
        imageCategorySpinner.setSelection(currentSelection);
        imageCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                LWQPreferences.setImageCategoryPreference(LWQApplication.getWallpaperController().getBackgroundCategories().get(index));
                // TODO toast or the slidy thingy from the bottom that says LWQ will apply settings to your next wallpaper
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        final String [] refreshPreferenceOptions = getResources().getStringArray(R.array.refresh_preference_options);
        ArrayAdapter<String> refreshOptionsAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item,
                refreshPreferenceOptions);
        refreshOptionsAdapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
        Spinner refreshSpinner = (Spinner) settingsContainer.findViewById(R.id.spinner_lwq_autopilot_settings_interval);
        refreshSpinner.setAdapter(refreshOptionsAdapter);
        refreshSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                final int[] refreshValues = getResources().getIntArray(R.array.refresh_preference_values);
                LWQPreferences.setRefreshPreference(refreshValues[index]);
                LWQAlarmController.resetAlarm();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        updateRefreshSpinner();

        final List<Category> categories = Category.listAll(Category.class);
        Collections.sort(categories, new Comparator<Category>() {
            @Override
            public int compare(Category categoryA, Category categoryB) {
                return categoryA.name.compareTo(categoryB.name);
            }
        });
        final String quoteCategoryPreference = LWQPreferences.getQuoteCategoryPreference();
        int selectedQuoteCategory = 0;
        final String [] quoteCategoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            quoteCategoryNames[i] = categories.get(i).name;
            if (quoteCategoryNames[i].equalsIgnoreCase(quoteCategoryPreference)) {
                selectedQuoteCategory = i;
            }
        }

        ArrayAdapter<String> quoteCategoriesAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item,
                quoteCategoryNames);
        quoteCategoriesAdapter.setDropDownViewResource(R.layout.spinner_drop_down_item);
        Spinner quoteCategorySpinner = (Spinner) settingsContainer.findViewById(R.id.spinner_lwq_autopilot_settings_quote_category);
        quoteCategorySpinner.setAdapter(quoteCategoriesAdapter);
        quoteCategorySpinner.setSelection(selectedQuoteCategory);
        quoteCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                LWQPreferences.setQuoteCategoryPreference(quoteCategoryNames[index]);
                // TODO toast or the slidy thingy from the bottom that says LWQ will apply settings to your next wallpaper
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        UIUtils.setViewAndChildrenEnabled(settingsContainer, false);
    }

    void updateRefreshSpinner() {
        Spinner refreshSpinner = (Spinner) settingsContainer.findViewById(R.id.spinner_lwq_autopilot_settings_interval);
        final AdapterView.OnItemSelectedListener onItemSelectedListener = refreshSpinner.getOnItemSelectedListener();
        refreshSpinner.setOnItemSelectedListener(null);
        final long refreshPreference = LWQPreferences.getRefreshPreference();
        final int[] refreshValues = getResources().getIntArray(R.array.refresh_preference_values);
        for (int i = 0; i < refreshValues.length; i++) {
            if (refreshPreference == refreshValues[i]) {
                refreshSpinner.setSelection(i);
                break;
            }
        }
        refreshSpinner.setOnItemSelectedListener(onItemSelectedListener);
    }

    void setupAdjustableSettings() {
        adjustableSettingsContainer = findViewById(R.id.group_lwq_settings_adjustable);
        adjustableSettingsContainer.setAlpha(0f);
        adjustableSettingsContainer.setTag(R.id.view_tag_flags, FLAG_HIDE | FLAG_ENABLE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            adjustableSettingsContainer.findViewById(R.id.rl_lwq_adjustable_settings_blur).setVisibility(View.GONE);
        } else {
            SeekBar blurBar = (SeekBar) adjustableSettingsContainer.findViewById(R.id.sb_lwq_adjustable_settings_blur);
            blurBar.setProgress(LWQPreferences.getBlurPreference());
            blurBar.setOnSeekBarChangeListener(this);
        }

        SeekBar dimBar = (SeekBar) adjustableSettingsContainer.findViewById(R.id.sb_lwq_adjustable_settings_dim);
        dimBar.setProgress(LWQPreferences.getDimPreference());
        dimBar.setOnSeekBarChangeListener(this);
        UIUtils.setViewAndChildrenEnabled(adjustableSettingsContainer, false);
    }

    void setupWallpaperActions() {
        wallpaperActionsContainer = findViewById(R.id.group_lwq_settings_wallpaper_actions);
        wallpaperActionsContainer.setTag(R.id.view_tag_flags, FLAG_HIDE | FLAG_ENABLE);
        shareButton = wallpaperActionsContainer.findViewById(R.id.btn_wallpaper_actions_share);
        saveButton = wallpaperActionsContainer.findViewById(R.id.btn_wallpaper_actions_save);
        adjustButton = wallpaperActionsContainer.findViewById(R.id.btn_wallpaper_actions_adjust);
        skipButton = wallpaperActionsContainer.findViewById(R.id.btn_wallpaper_actions_skip);
        settingsButton = wallpaperActionsContainer.findViewById(R.id.btn_wallpaper_actions_settings);
        View [] buttons = new View[] {shareButton, saveButton, adjustButton, skipButton, settingsButton};
        for (View button : buttons) {
            button.setAlpha(0f);
            button.setTranslationY(button.getHeight() * 2f);
            button.setTag(R.id.view_tag_flags, FLAG_ENABLE);
        }
        adjustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                if (activityState != ActivityState.REVEAL_ADJUSTMENTS) {
                    view.setSelected(true);
                    changeState(ActivityState.REVEAL_ADJUSTMENTS);
                } else {
                    view.setSelected(false);
                    changeState(ActivityState.REVEAL_CONTROLS);
                }
            }
        });
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                if (activityState != ActivityState.REVEAL_SETTINGS) {
                    view.setSelected(true);
                    changeState(ActivityState.REVEAL_SETTINGS);
                } else {
                    view.setSelected(false);
                    changeState(ActivityState.REVEAL_CONTROLS);
                }
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent(getString(R.string.action_share)));
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                sendBroadcast(new Intent(getString(R.string.action_save)));
            }
        });
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                LWQApplication.getWallpaperController().generateNewWallpaper();
            }
        });
    }

    void setupProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.pb_lwq_settings);
        progressBar.setAlpha(0f);
        progressBar.setTag(R.id.view_tag_flags, FLAG_HIDE);
    }

    void setupTouchToDismiss() {
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (revealControlsTimerTask.scheduledExecutionTime() > 0) {
                    revealControlsTimerTask.cancel();
                }
                // TODO
                if (activityState == ActivityState.REVEAL_WALLPAPER) {
                    changeState(ActivityState.REVEAL_CONTROLS);
                } else {
                    changeState(ActivityState.REVEAL_WALLPAPER);
                }
            }
        });
    }

    void animateSettingsContainer(View container, boolean dismiss) {
        Animator animator = dismiss ? AnimatorInflater.loadAnimator(this, R.animator.exit_to_left) :
                AnimatorInflater.loadAnimator(this, R.animator.enter_from_right);
        animator.setTarget(container);
        animator.start();
    }

    void animateControls(boolean dismiss) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(generateAnimator(shareButton, dismiss, 0),
                generateAnimator(saveButton, dismiss, 15),
                generateAnimator(adjustButton, dismiss, 30),
                generateAnimator(skipButton, dismiss, 45),
                generateAnimator(settingsButton, dismiss, 60));
        animatorSet.start();
    }

    AnimatorSet generateAnimator(View target, boolean dismiss, long startDelay) {
        float [] fadeIn = new float[] {0f, 1f};
        float [] fadeOut = new float[] {1f, 0f};
        final ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(target, "alpha", dismiss ? fadeOut : fadeIn);
        alphaAnimator.setDuration(240);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        final ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(target, "translationY", dismiss ? (target.getHeight() * 2f) : 0f);
        translationAnimator.setDuration(240);
        translationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setStartDelay(startDelay);
        animatorSet.playTogether(alphaAnimator, translationAnimator);
        return animatorSet;
    }

    void changeState(ActivityState activityState) {
        try {
            stateBlockingDeque.put(activityState);
            changeStateExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        changeStateRunnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    void didFinishDrawing() {
        // TODO
        if (activityState == ActivityState.INITIAL) {
            changeState(ActivityState.REVEAL_WALLPAPER);
            revealControlsTimer = new Timer();
            revealControlsTimer.schedule(revealControlsTimerTask, DateUtils.SECOND_IN_MILLIS * 3);
        }
    }

    @Override
    public void onEvent(PreferenceUpdateEvent preferenceUpdateEvent) {
        super.onEvent(preferenceUpdateEvent);
        if (preferenceUpdateEvent.getPreferenceKeyId() == R.string.preference_key_refresh) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateRefreshSpinner();
                }
            });
        }
    }

    public void onEvent(ImageSaveEvent imageSaveEvent) {
        // TODO
    }

    @Override
    public void onEvent(final WallpaperEvent wallpaperEvent) {
        super.onEvent(wallpaperEvent);
        // TODO
    }

    // SeekBar Listener

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
        if (seekBar.getId() == R.id.sb_lwq_adjustable_settings_blur) {
            LWQPreferences.setBlurPreference(value);
        } else {
            LWQPreferences.setDimPreference(value);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO
        changeState(ActivityState.REVEAL_WALLPAPER_EDIT_MODE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO
        changeState(ActivityState.REVEAL_ADJUSTMENTS);
    }

}