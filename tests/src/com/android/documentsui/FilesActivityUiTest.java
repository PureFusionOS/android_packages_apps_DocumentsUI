/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui;

import static com.android.documentsui.StubProvider.ROOT_0_ID;
import static com.android.documentsui.StubProvider.ROOT_1_ID;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiObject;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.Suppress;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.android.documentsui.model.RootInfo;

@LargeTest
public class FilesActivityUiTest extends ActivityTest<FilesActivity> {

    public FilesActivityUiTest() {
        super(FilesActivity.class);
    }

    @Override
    protected RootInfo getInitialRoot() {
        return null;
    }

    @Override
    public void initTestFiles() throws RemoteException {
        mDocsHelper.createFolder(rootDir0, dirName1);

        mDocsHelper.createDocument(rootDir0, "text/plain", "file0.log");
        mDocsHelper.createDocument(rootDir0, "image/png", "file1.png");
        mDocsHelper.createDocument(rootDir0, "text/csv", "file2.csv");

        mDocsHelper.createDocument(rootDir1, "text/plain", "anotherFile0.log");
        mDocsHelper.createDocument(rootDir1, "text/plain", "poodles.text");
    }

    public void testRootsListed() throws Exception {
        initTestFiles();

        // Should also have Drive, but that requires pre-configuration of devices
        // We omit for now.
        bots.roots.assertRootsPresent(
                "Images",
                "Videos",
                "Audio",
                "Downloads",
                ROOT_0_ID,
                ROOT_1_ID);

        // Separate logic for "Documents" root, which presence depends on the config setting
        if (Shared.shouldShowDocumentsRoot(context, new Intent(DocumentsContract.ACTION_BROWSE))) {
            bots.roots.assertRootsPresent("Documents");
        } else {
            bots.roots.assertRootsAbsent("Documents");
        }
    }

    public void testFilesListed() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);
        bots.directory.assertDocumentsPresent("file0.log", "file1.png", "file2.csv");
    }

    public void testLoadsDownloadsDirectoryByDefault() throws Exception {
        initTestFiles();

        device.waitForIdle();
        bots.main.assertWindowTitle("Downloads");
    }

    public void testRootClickSetsWindowTitle() throws Exception {
        initTestFiles();

        bots.roots.openRoot("Downloads");
        bots.main.assertWindowTitle("Downloads");
    }

    public void testFilesList_LiveUpdate() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);
        mDocsHelper.createDocument(rootDir0, "yummers/sandwich", "Ham & Cheese.sandwich");

        bots.directory.waitForDocument("Ham & Cheese.sandwich");
        bots.directory.assertDocumentsPresent(
                "file0.log", "file1.png", "file2.csv", "Ham & Cheese.sandwich");
    }

    public void testCreateDirectory() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.main.openOverflowMenu();
        bots.main.menuNewFolder().click();
        bots.main.setDialogText("Kung Fu Panda");

        bots.keyboard.pressEnter();

        bots.directory.assertDocumentsPresent("Kung Fu Panda");
    }

    public void testOpenBreadcrumb() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.directory.openDocument(dirName1);
        if (bots.main.isTablet()) {
            openBreadcrumbTabletHelper();
        } else {
            openBreadcrumbPhoneHelper();
        }
        bots.main.assertBreadcrumbItemsPresent(dirName1, "TEST_ROOT_0");
        bots.main.clickBreadcrumbItem("TEST_ROOT_0");

        bots.directory.assertDocumentsPresent(dirName1);
    }

    private void openBreadcrumbTabletHelper() throws Exception {
    }

    private void openBreadcrumbPhoneHelper() throws Exception {
        bots.main.clickDropdownBreadcrumb();
    }

    public void testDeleteDocument() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.directory.clickDocument("file1.png");
        device.waitForIdle();
        bots.main.menuDelete().click();

        bots.main.clickDialogOkButton();

        bots.directory.assertDocumentsAbsent("file1.png");
    }

    public void testKeyboard_CutDocument() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.directory.clickDocument("file1.png");
        device.waitForIdle();
        bots.main.pressKey(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON);

        device.waitForIdle();

        bots.roots.openRoot(ROOT_1_ID);
        bots.main.pressKey(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);

        bots.directory.waitForDocument("file1.png");
        bots.directory.assertDocumentsPresent("file1.png");

        bots.roots.openRoot(ROOT_0_ID);
        bots.directory.assertDocumentsAbsent("file1.png");
    }

    public void testKeyboard_CopyDocument() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.directory.clickDocument("file1.png");
        device.waitForIdle();
        bots.main.pressKey(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON);

        device.waitForIdle();

        bots.roots.openRoot(ROOT_1_ID);
        bots.main.pressKey(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);

        bots.directory.waitForDocument("file1.png");
        bots.directory.assertDocumentsPresent("file1.png");

        bots.roots.openRoot(ROOT_0_ID);
        bots.directory.assertDocumentsPresent("file1.png");
    }

    public void testDeleteDocument_Cancel() throws Exception {
        initTestFiles();

        bots.roots.openRoot(ROOT_0_ID);

        bots.directory.clickDocument("file1.png");
        device.waitForIdle();
        bots.main.menuDelete().click();

        bots.main.clickDialogCancelButton();

        bots.directory.assertDocumentsPresent("file1.png");
    }

    // Tests that pressing tab switches focus between the roots and directory listings.
    @Suppress
    public void testKeyboard_tab() throws Exception {
        bots.main.pressKey(KeyEvent.KEYCODE_TAB);
        bots.roots.assertHasFocus();
        bots.main.pressKey(KeyEvent.KEYCODE_TAB);
        bots.directory.assertHasFocus();
    }

    // Tests that arrow keys do not switch focus away from the dir list.
    @Suppress
    public void testKeyboard_arrowsDirList() throws Exception {
        for (int i = 0; i < 10; i++) {
            bots.main.pressKey(KeyEvent.KEYCODE_DPAD_LEFT);
            bots.directory.assertHasFocus();
        }
        for (int i = 0; i < 10; i++) {
            bots.main.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);
            bots.directory.assertHasFocus();
        }
    }

    // Tests that arrow keys do not switch focus away from the roots list.
    public void testKeyboard_arrowsRootsList() throws Exception {
        bots.main.pressKey(KeyEvent.KEYCODE_TAB);
        for (int i = 0; i < 10; i++) {
            bots.main.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);
            bots.roots.assertHasFocus();
        }
        for (int i = 0; i < 10; i++) {
            bots.main.pressKey(KeyEvent.KEYCODE_DPAD_LEFT);
            bots.roots.assertHasFocus();
        }
    }

    // We don't really need to test the entirety of download support
    // since downloads is (almost) just another provider.
    @Suppress
    public void testDownload_Queued() throws Exception {
        DownloadManager dm = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        // This downloads ends up being queued (because DNS can't be resolved).
        // We'll still see an entry in the downloads UI with a "Queued" label.
        dm.enqueue(new Request(Uri.parse("http://hammychamp.toodles")));

        bots.roots.openRoot("Downloads");
        bots.directory.assertDocumentsPresent("Queued");
    }

    @Suppress
    public void testDownload_RetryUnsuccessful() throws Exception {
        DownloadManager dm = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        // This downloads fails! But it'll still show up.
        dm.enqueue(new Request(Uri.parse("http://www.google.com/hamfancy")));

        bots.roots.openRoot("Downloads");
        UiObject doc = bots.directory.findDocument("Unsuccessful");
        doc.waitForExists(TIMEOUT);

        int toolType = Configurator.getInstance().getToolType();
        Configurator.getInstance().setToolType(MotionEvent.TOOL_TYPE_FINGER);
        doc.click();
        Configurator.getInstance().setToolType(toolType);

        assertTrue(bots.main.findDownloadRetryDialog().exists());

        device.pressBack(); // to clear the dialog.
    }
}
