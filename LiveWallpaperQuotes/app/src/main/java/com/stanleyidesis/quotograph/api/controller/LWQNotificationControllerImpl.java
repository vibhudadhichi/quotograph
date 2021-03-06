package com.stanleyidesis.quotograph.api.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.stanleyidesis.quotograph.LWQApplication;
import com.stanleyidesis.quotograph.R;
import com.stanleyidesis.quotograph.api.event.ImageSaveEvent;
import com.stanleyidesis.quotograph.api.event.WallpaperEvent;
import com.stanleyidesis.quotograph.ui.UIUtils;
import com.stanleyidesis.quotograph.ui.activity.LWQActivateActivity;
import com.stanleyidesis.quotograph.ui.activity.LWQSaveWallpaperActivity;

import java.io.File;

import de.greenrobot.event.EventBus;

/**
 * Copyright (c) 2016 Stanley Idesis
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
 * LWQNotificationControllerImpl.java
 * @author Stanley Idesis
 *
 * From Quotograph
 * https://github.com/stanidesis/quotograph
 *
 * Please report any issues
 * https://github.com/stanidesis/quotograph/issues
 *
 * Date: 09/19/2015
 */
public class LWQNotificationControllerImpl implements LWQNotificationController {

    static final int REQUEST_CODE_SHARE = 0xA;
    static final int REQUEST_CODE_VIEW = 0xB;

    static final int PRIMARY_NOTIF_ID = 1;
    static final int GEN_FAILURE_NOTIF_ID = 2;

    boolean newWallpaperIncoming = false;

    public LWQNotificationControllerImpl() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void postNewWallpaperNotification() {
        dismissWallpaperGenerationFailureNotification();
        final LWQWallpaperController wallpaperController = LWQApplication.getWallpaperController();
        // Compress background to reasonable Square size
        final Bitmap backgroundImage = wallpaperController.getBackgroundImage();
        if (backgroundImage == null) {
            return;
        }
        final Bitmap notificationBitmap = chopToCenterSquare(backgroundImage);

        Context context = LWQApplication.get();

        // Establish basic options
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        notificationBuilder.setContentInfo(wallpaperController.getAuthor());
        notificationBuilder.setContentTitle(context.getString(R.string.new_quotograph));
        notificationBuilder.setContentText(String.format("\"%s\"", wallpaperController.getQuote()));
        notificationBuilder.setLargeIcon(notificationBitmap);
        notificationBuilder.setOngoing(false);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setSmallIcon(R.mipmap.ic_stat);
        notificationBuilder.setTicker(String.format("New quote from %s", wallpaperController.getAuthor()));
        notificationBuilder.setWhen(System.currentTimeMillis());

        // Create BigTextStyle
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(wallpaperController.getQuote());
        bigTextStyle.setBigContentTitle(wallpaperController.getAuthor());
        bigTextStyle.setSummaryText(context.getString(R.string.new_quotograph));
        notificationBuilder.setStyle(bigTextStyle);

        // Set Content Intent
        Intent mainIntent = new Intent(context, LWQActivateActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, mainIntent, 0));

        // Add Share Action
        Intent shareIntent = new Intent(context.getString(R.string.action_share));
        final PendingIntent shareBroadcast = PendingIntent.getBroadcast(context, 0, shareIntent, 0);
        final NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(R.mipmap.ic_share_white,
                context.getString(R.string.share), shareBroadcast).build();
        notificationBuilder.addAction(shareAction);

        // Add save to disk
        Intent saveToDiskIntent = new Intent(context, LWQSaveWallpaperActivity.class);
        final PendingIntent saveToDiskActivity = PendingIntent.getActivity(context, 0, saveToDiskIntent, 0);
        final NotificationCompat.Action saveToDiskAction = new NotificationCompat.Action.Builder(R.mipmap.ic_save_white,
                context.getString(R.string.save_to_disk), saveToDiskActivity).build();
        notificationBuilder.addAction(saveToDiskAction);

        // Add Skip Action
        Intent skipIntent = new Intent(context.getString(R.string.action_change_wallpaper));
        final PendingIntent skipBroadcast = PendingIntent.getBroadcast(context, 0, skipIntent, 0);
        final NotificationCompat.Action skipAction = new NotificationCompat.Action.Builder(R.mipmap.ic_skip_next_white,
                context.getString(R.string.skip), skipBroadcast).build();
        notificationBuilder.addAction(skipAction);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PRIMARY_NOTIF_ID, notificationBuilder.build());

        notificationBitmap.recycle();
    }

    @Override
    public void dismissNewWallpaperNotification() {
        NotificationManager notificationManager = (NotificationManager) LWQApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PRIMARY_NOTIF_ID);
    }

    @Override
    public void postWallpaperGenerationFailureNotification() {
        // Two actions: Network Settings & Try Again
        LWQApplication context = LWQApplication.get();
        // Establish basic options
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setCategory(Notification.CATEGORY_ERROR);
        notificationBuilder.setContentInfo(context.getString(R.string.app_name));
        notificationBuilder.setContentTitle(context.getString(R.string.notification_generation_failure_title));
        notificationBuilder.setContentText(context.getString(R.string.notification_generation_failure_content));
        notificationBuilder.setTicker(context.getString(R.string.notification_generation_failure_ticker));
        notificationBuilder.setOngoing(false);
        notificationBuilder.setShowWhen(true);
        notificationBuilder.setLocalOnly(true);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        notificationBuilder.setWhen(System.currentTimeMillis());

        // Create BigTextStyle
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(R.string.notification_generation_failure_title));
        bigTextStyle.bigText(context.getString(R.string.notification_generation_failure_content));
        notificationBuilder.setStyle(bigTextStyle);

        // Add Settings Action
        Intent settingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        final PendingIntent settingsBroadcast = PendingIntent.getActivity(context, 0, settingsIntent, 0);
        final NotificationCompat.Action settingsAction = new NotificationCompat.Action.Builder(R.mipmap.ic_settings_white,
                context.getString(R.string.notification_generation_failure_action_settings), settingsBroadcast).build();
        notificationBuilder.addAction(settingsAction);

        // Add Skip Action
        Intent skipIntent = new Intent(context.getString(R.string.action_change_wallpaper));
        final PendingIntent skipBroadcast = PendingIntent.getBroadcast(context, 0, skipIntent, 0);
        final NotificationCompat.Action skipAction = new NotificationCompat.Action.Builder(R.mipmap.ic_refresh_white_36dp,
                context.getString(R.string.notification_generation_failure_action_try_again), skipBroadcast).build();
        notificationBuilder.addAction(skipAction);

        // Set Skip as main action
        notificationBuilder.setContentIntent(skipBroadcast);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(GEN_FAILURE_NOTIF_ID, notificationBuilder.build());
    }

    @Override
    public void dismissWallpaperGenerationFailureNotification() {
        NotificationManager notificationManager = (NotificationManager) LWQApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GEN_FAILURE_NOTIF_ID);
    }

    @Override
    public void postSavedWallpaperReadyNotification(Uri filePath, Uri imageUri) {
        final Bitmap savedImage = BitmapFactory.decodeFile(filePath.getPath());
        final Bitmap notificationBitmap = chopToCenterSquare(savedImage);
        savedImage.recycle();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(LWQApplication.get());
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        notificationBuilder.setContentTitle(LWQApplication.get().getString(R.string.notification_title_save_image));
        notificationBuilder.setContentText(LWQApplication.get().getString(R.string.notification_content_save_image));
        notificationBuilder.setLargeIcon(notificationBitmap);
        notificationBuilder.setOngoing(false);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setSmallIcon(R.mipmap.ic_stat);
        notificationBuilder.setTicker(LWQApplication.get().getString(R.string.notification_title_save_image));
        notificationBuilder.setWhen(System.currentTimeMillis());

        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        bigPictureStyle.bigPicture(notificationBitmap);
        bigPictureStyle.bigLargeIcon(notificationBitmap);

        notificationBuilder.setStyle(bigPictureStyle);

        // Set content Intent
        final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(imageUri, "image/*");
        final Intent viewChooser = Intent.createChooser(viewIntent, LWQApplication.get().getString(R.string.view_using));
        final PendingIntent viewActivity = PendingIntent.getActivity(LWQApplication.get(),
                REQUEST_CODE_VIEW, viewChooser, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(viewActivity);

        // Add Share Action
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath.getPath())));
        final Intent shareChooser = Intent.createChooser(shareIntent, LWQApplication.get().getString(R.string.share_using));
        final PendingIntent shareActivity = PendingIntent.getActivity(LWQApplication.get(),
                REQUEST_CODE_SHARE, shareChooser, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(R.mipmap.ic_share_white,
                LWQApplication.get().getString(R.string.share), shareActivity).build();
        notificationBuilder.addAction(shareAction);

        NotificationManager notificationManager = (NotificationManager) LWQApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(savedImage.hashCode(), notificationBuilder.build());
        notificationBitmap.recycle();
    }

    @Override
    public void postWallpaperSaveFailureNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(LWQApplication.get());
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setCategory(Notification.CATEGORY_ERROR);
        notificationBuilder.setContentTitle(LWQApplication.get().getString(R.string.notification_title_save_failed));
        notificationBuilder.setContentText(LWQApplication.get().getString(R.string.notification_content_save_failed));
        notificationBuilder.setOngoing(false);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setSmallIcon(R.mipmap.ic_stat);
        notificationBuilder.setTicker(String.format(LWQApplication.get().getString(R.string.notification_title_save_failed)));
        notificationBuilder.setWhen(System.currentTimeMillis());

        Intent saveToDiskIntent = new Intent(LWQApplication.get(), LWQSaveWallpaperActivity.class);
        final PendingIntent saveToDiskActivity = PendingIntent.getActivity(LWQApplication.get(), 0, saveToDiskIntent, 0);
        notificationBuilder.setContentIntent(saveToDiskActivity);

        NotificationManager notificationManager = (NotificationManager) LWQApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(3, notificationBuilder.build());
    }

    @Override
    protected void finalize() throws Throwable {
        EventBus.getDefault().unregister(this);
        super.finalize();
    }

    public void onEvent(WallpaperEvent wallpaperEvent) {
        if (wallpaperEvent.getStatus() == WallpaperEvent.Status.GENERATED_NEW_WALLPAPER) {
            newWallpaperIncoming = !wallpaperEvent.didFail();
        } else if (wallpaperEvent.getStatus() == WallpaperEvent.Status.RETRIEVED_WALLPAPER) {
            if (newWallpaperIncoming && LWQApplication.isWallpaperActivated()) {
                postNewWallpaperNotification();
                newWallpaperIncoming = false;
            }
        } else if (wallpaperEvent.getStatus() == WallpaperEvent.Status.GENERATING_NEW_WALLPAPER
                && wallpaperEvent.didFail()) {
            postWallpaperGenerationFailureNotification();
        }
    }

    public void onEvent(ImageSaveEvent imageSaveEvent) {
        if (imageSaveEvent.didFail()) {
            postWallpaperSaveFailureNotification();
        } else {
            postSavedWallpaperReadyNotification(imageSaveEvent.getFileUri(), imageSaveEvent.getContentUri());
        }
    }

    private Bitmap chopToCenterSquare(Bitmap original) {
        int squareDim = Math.min(original.getWidth(), original.getHeight());
        int squareYCoord = (int) (original.getHeight() / 2.0) - (int) (squareDim / 2.0);
        int squareXCoord = (int) (original.getWidth() / 2.0) - (int) (squareDim / 2.0);

        final Point size = UIUtils.getRealScreenSize();
        int screenWidth = Math.min(size.x, size.y);
        int maxBitmapDim = (int) (screenWidth * .3f);
        // Calculate center
        float scale = ((float) maxBitmapDim) / ((float) squareDim);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        return Bitmap.createBitmap(original, squareXCoord, squareYCoord,
                squareDim, squareDim, matrix, true);
    }
}
