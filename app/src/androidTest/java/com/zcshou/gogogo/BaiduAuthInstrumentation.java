package com.zcshou.gogogo;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import com.baidu.lbsapi.auth.LBSAuthManager;
import org.json.JSONObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Reports authentication status without disclosing the API key or response tokens. */
public class BaiduAuthInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        CountDownLatch done = new CountDownLatch(1);
        LBSAuthManager.getInstance(getTargetContext()).authenticate(true, "lbs_androidsdk", null,
            (status, response) -> {
                String message = "";
                try { message = new JSONObject(response).optString("message"); }
                catch (Exception ignored) { }
                result.putString("stream", "Baidu SDK authentication status=" + status
                    + "; message=" + message.replaceAll("[A-Za-z0-9]{32}", "[redacted]"));
                done.countDown();
            });
        try {
            if (!done.await(40, TimeUnit.SECONDS)) result.putString("stream", "Baidu authentication timed out");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finish(Activity.RESULT_OK, result);
    }
}
