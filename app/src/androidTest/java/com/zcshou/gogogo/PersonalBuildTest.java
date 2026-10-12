package com.zcshou.gogogo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.view.MenuItem;
import android.widget.ListView;
import android.graphics.Bitmap;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.zcshou.service.ServiceGo;
import com.zcshou.utils.PlaceSearch;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

/** Live integration checks, intended for an isolated emulator with location/overlay/mock grants. */
@RunWith(AndroidJUnit4.class)
public class PersonalBuildTest {
    private SuggestionResult search(String city) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<SuggestionResult> response = new AtomicReference<>();
        AtomicReference<PlaceSearch> request = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            PlaceSearch search = new PlaceSearch(result -> {
                response.set(result);
                done.countDown();
            });
            request.set(search);
            search.request("上海东方明珠", city, true);
        });
        try {
            assertTrue("Baidu SDK search timed out", done.await(35, TimeUnit.SECONDS));
            return response.get();
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> request.get().destroy());
        }
    }

    private void assertShanghaiResult(String city) throws Exception {
        SuggestionResult result = search(city);
        assertNotNull(result);
        assertEquals(PlaceSearch.errorMessage(result), SearchResult.ERRORNO.NO_ERROR, result.error);
        assertNotNull(result.getAllSuggestions());
        boolean found = false;
        for (SuggestionResult.SuggestionInfo item : result.getAllSuggestions()) {
            if (item.pt != null && item.pt.longitude > 121 && item.pt.longitude < 122
                    && item.pt.latitude > 31 && item.pt.latitude < 32) found = true;
        }
        assertTrue("Expected a selectable Shanghai POI with coordinates", found);
    }

    @Test public void searchWithCityAndBeforeFirstLocation() throws Exception {
        try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            assertShanghaiResult("上海");
            assertShanghaiResult(null);
        }
    }

    @Test public void mainScreenSearchShowsSelectablePlaces() throws Exception {
        try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            activity.onActivity(main -> {
                MainActivity.mCurrentCity = null;
                Toolbar toolbar = main.findViewById(R.id.toolbar);
                MenuItem item = toolbar.getMenu().findItem(R.id.action_search);
                item.expandActionView();
                ((SearchView) item.getActionView()).setQuery("上海东方明珠", false);
            });
            AtomicInteger count = new AtomicInteger();
            long deadline = SystemClock.elapsedRealtime() + 35000;
            while (count.get() == 0 && SystemClock.elapsedRealtime() < deadline) {
                activity.onActivity(main -> {
                    ListView results = main.findViewById(R.id.search_list_view);
                    if (results.getAdapter() != null) count.set(results.getAdapter().getCount());
                });
                SystemClock.sleep(200);
            }
            assertTrue("Main search UI did not show selectable places", count.get() > 0);
            Bitmap screen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    .getExternalFilesDir(null), "search-smoke.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                screen.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            screen.recycle();
            onData(anything()).inAdapterView(withId(R.id.search_list_view)).atPosition(0).perform(click());
        }
    }

    @Test public void android16MockLocationServiceStartsAndStops() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LocationManager locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Intent service = new Intent(context, ServiceGo.class)
                .putExtra(MainActivity.LNG_MSG_ID, 121.4998)
                .putExtra(MainActivity.LAT_MSG_ID, 31.2397)
                .putExtra(MainActivity.ALT_MSG_ID, 10.0);
        try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            try {
                context.startForegroundService(service);
                Location fix = null;
                long deadline = SystemClock.elapsedRealtime() + 10000;
                while (SystemClock.elapsedRealtime() < deadline) {
                    fix = locations.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (fix != null && fix.isMock() && Math.abs(fix.getLatitude() - 31.2397) < 0.00001) break;
                    SystemClock.sleep(200);
                }
                assertNotNull("No GPS location produced", fix);
                assertTrue("Expected Android's mock provider", fix.isMock());
                assertEquals(31.2397, fix.getLatitude(), 0.00001);
                assertEquals(121.4998, fix.getLongitude(), 0.00001);
            } finally {
                context.stopService(service);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            }
        }
    }
}
