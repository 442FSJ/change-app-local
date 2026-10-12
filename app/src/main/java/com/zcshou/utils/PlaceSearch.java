package com.zcshou.utils;

import android.os.Handler;
import android.os.Looper;

import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

/** Debounces typing and discards responses from an older query or a closed view. */
public final class PlaceSearch {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OnGetSuggestionResultListener listener;
    private SuggestionSearch search;
    private int generation;
    private boolean destroyed;

    public PlaceSearch(OnGetSuggestionResultListener listener) {
        this.listener = listener;
    }

    public void request(String query, String city, boolean immediate) {
        cancel();
        if (destroyed || query == null || query.trim().isEmpty()) return;
        final int requestGeneration = generation;
        final String keyword = query.trim();
        final String region = city == null || city.trim().isEmpty() ? "全国" : city.trim();
        handler.postDelayed(() -> {
            if (destroyed || requestGeneration != generation) return;
            search = SuggestionSearch.newInstance();
            search.setOnGetSuggestionResultListener(result -> {
                if (!destroyed && requestGeneration == generation) listener.onGetSuggestionResult(result);
            });
            try {
                boolean accepted = search.requestSuggestion(new SuggestionSearchOption()
                        .keyword(keyword).city(region).citylimit(false));
                if (!accepted) reportRequestError();
            } catch (IllegalArgumentException | IllegalStateException e) {
                reportRequestError();
            }
        }, immediate ? 0 : 450);
    }

    private void reportRequestError() {
        SuggestionResult result = new SuggestionResult();
        result.error = SearchResult.ERRORNO.REQUEST_ERROR;
        listener.onGetSuggestionResult(result);
    }

    public void cancel() {
        generation++;
        handler.removeCallbacksAndMessages(null);
        if (search != null) {
            search.destroy();
            search = null;
        }
    }

    public void destroy() {
        destroyed = true;
        cancel();
    }

    public static String errorMessage(SuggestionResult result) {
        if (result == null) return "地点搜索暂未返回结果，请稍后重试";
        if (result.error == null || result.error == SearchResult.ERRORNO.NO_ERROR) {
            return result.getAllSuggestions() == null || result.getAllSuggestions().isEmpty()
                    ? "没有匹配地点，请尝试输入城市和地名" : null;
        }
        switch (result.error) {
            case KEY_ERROR:
            case PERMISSION_UNFINISHED:
            case NO_ADVANCED_PERMISSION:
                return "百度地点搜索鉴权失败，请检查密钥、应用签名及服务权限（" + result.error + "）";
            case NETWORK_ERROR:
            case NETWORK_TIME_OUT:
                return "地点搜索网络异常，请检查网络后重试";
            case RESULT_NOT_FOUND:
                return "没有匹配地点，请尝试输入城市和地名";
            default:
                return "百度地点搜索失败（" + result.error + "），请稍后重试或检查服务配额";
        }
    }
}
