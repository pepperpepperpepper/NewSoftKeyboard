/*
 * Copyright (C) 2025 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voiceime.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Utility helpers for manipulating audio files used by speech-to-text backends. */
public final class SpeechToTextFileUtils {

    private static final String TAG = "SpeechToTextFileUtils";

    private SpeechToTextFileUtils() {
        // Utility class.
    }

    /**
     * Copies {@code sourceFile} into {@code targetDirectoryPath}.
     *
     * @param sourceFile original audio file
     * @param targetDirectoryPath directory path, will be created if missing
     * @param prefix new filename prefix
     * @param extension new filename extension without dot
     * @return copied {@link File} or {@code null} if copy fails
     */
    @Nullable
    public static File copyToDirectory(
            @NonNull File sourceFile,
            @NonNull String targetDirectoryPath,
            @NonNull String prefix,
            @NonNull String extension) {
        if (!sourceFile.exists()) {
            Log.w(TAG, "Source file does not exist: " + sourceFile.getAbsolutePath());
            return null;
        }

        File destinationDir = new File(targetDirectoryPath);
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            Log.w(TAG, "Failed to create directory: " + destinationDir.getAbsolutePath());
            return null;
        }

        String timestamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File targetFile = new File(destinationDir, prefix + "_" + timestamp + "." + extension);

        try (FileInputStream inStream = new FileInputStream(sourceFile);
             FileOutputStream outStream = new FileOutputStream(targetFile);
             FileChannel inChannel = inStream.getChannel();
             FileChannel outChannel = outStream.getChannel()) {
            outChannel.transferFrom(inChannel, 0, inChannel.size());
            return targetFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy audio file", e);
            return null;
        }
    }
}
