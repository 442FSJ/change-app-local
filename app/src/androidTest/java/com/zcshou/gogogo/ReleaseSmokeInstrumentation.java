package com.zcshou.gogogo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import android.os.Bundle;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;


/** Framework-only black-box test: also runs against the delivered, minified release APK. */
public class ReleaseSmokeInstrumentation extends Instrumentation {
    private long mapWaitMs = 4000;
    private static void assertTrue(boolean value) { if (!value) throw new AssertionError("Assertion failed"); }
    private static void assertTrue(String message, boolean value) { if (!value) throw new AssertionError(message); }
    private static void assertNotNull(Object value) { if (value == null) throw new AssertionError("Unexpected null"); }
    private static void assertNotNull(String message, Object value) { if (value == null) throw new AssertionError(message); }

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments);
        if (arguments != null) mapWaitMs = Long.parseLong(arguments.getString("mapWaitMs", "4000")); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            deliveredReleaseCanSearchSelectAndMockLocation();
            result.putString("stream", "\nPASS: delivered release search, selection and Android mock GPS.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private int id(Activity activity, String name) {
        return activity.getResources().getIdentifier(name, "id", activity.getPackageName());
    }

    private EditText findInput(View view) {
        if (view instanceof EditText && view.isShown()) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findInput(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    public void deliveredReleaseCanSearchSelectAndMockLocation() throws Exception {
        Instrumentation instrumentation = this;
        Context context = instrumentation.getTargetContext();
        Activity activity = instrumentation.startActivitySync(new Intent()
                .setClassName(context.getPackageName(), "com.zcshou.gogogo.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                assertTrue(activity.findViewById(id(activity, "action_search")).performClick());
                EditText input = findInput(activity.getWindow().getDecorView());
                assertNotNull("Search field not visible", input);
                input.setText("上海东方明珠");
            });
            AtomicInteger count = new AtomicInteger();
            long deadline = SystemClock.elapsedRealtime() + 35000;
            while (count.get() == 0 && SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync(() -> {
                    ListView list = activity.findViewById(id(activity, "search_list_view"));
                    if (list.getAdapter() != null) count.set(list.getAdapter().getCount());
                });
                SystemClock.sleep(200);
            }
            assertTrue("Delivered APK search returned no selectable places", count.get() > 0);
            instrumentation.waitForIdleSync();
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), "release-search.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            screenshot.recycle();
            instrumentation.runOnMainSync(() -> {
                ListView list = activity.findViewById(id(activity, "search_list_view"));
                assertNotNull(list.getChildAt(0));
                list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
            });
            SystemClock.sleep(mapWaitMs);
            Bitmap mapScreenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), "release-map.png"))) {
                mapScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            mapScreenshot.recycle();
            instrumentation.runOnMainSync(() -> {
                activity.findViewById(id(activity, "faBtnStart")).performClick();
            });
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location fix = null;
            deadline = SystemClock.elapsedRealtime() + 10000;
            while (SystemClock.elapsedRealtime() < deadline) {
                fix = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (fix != null && fix.isMock() && fix.getLongitude() > 121 && fix.getLongitude() < 122
                        && fix.getLatitude() > 31 && fix.getLatitude() < 32) break;
                SystemClock.sleep(200);
            }
            assertNotNull(fix);
            assertTrue(fix.isMock());
            assertTrue(fix.getLongitude() > 121 && fix.getLongitude() < 122);
            assertTrue(fix.getLatitude() > 31 && fix.getLatitude() < 32);
            instrumentation.runOnMainSync(() -> activity.findViewById(id(activity, "faBtnStart")).performClick());
            instrumentation.waitForIdleSync();
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
    }
}
