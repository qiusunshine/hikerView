package com.example.hikerview.ui.home;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.annimon.stream.function.Consumer;
import com.bumptech.glide.Glide;
import com.example.hikerview.R;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.constants.CollectionTypeConstant;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.event.OnArticleListRuleChangedEvent;
import com.example.hikerview.event.OnBackEvent;
import com.example.hikerview.event.OnRefreshEvent;
import com.example.hikerview.event.OnTopEvent;
import com.example.hikerview.event.home.LastClickShowEvent;
import com.example.hikerview.event.home.OnPageChangeEvent;
import com.example.hikerview.event.home.OnRefreshPageEvent;
import com.example.hikerview.event.home.OnRefreshWebViewEvent;
import com.example.hikerview.event.home.OnRefreshX5HeightEvent;
import com.example.hikerview.event.web.DestroyEvent;
import com.example.hikerview.event.web.OnEvalJsEvent;
import com.example.hikerview.model.BigTextDO;
import com.example.hikerview.model.ViewCollection;
import com.example.hikerview.model.ViewHistory;
import com.example.hikerview.service.exception.ParseException;
import com.example.hikerview.service.parser.BaseParseCallback;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.service.parser.LazyRuleParser;
import com.example.hikerview.service.parser.PageParser;
import com.example.hikerview.service.parser.WebViewParser;
import com.example.hikerview.ui.base.BaseCallback;
import com.example.hikerview.ui.base.BaseFragment;
import com.example.hikerview.ui.browser.PictureListActivity;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.browser.util.HttpRequestUtil;
import com.example.hikerview.ui.detail.DetailUIHelper;
import com.example.hikerview.ui.download.DownloadChooser;
import com.example.hikerview.ui.download.DownloadDialogUtil;
import com.example.hikerview.ui.download.DownloadManager;
import com.example.hikerview.ui.download.DownloadTask;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.home.enums.HomeActionEnum;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListModel;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleJO;
import com.example.hikerview.ui.home.model.AutoPageData;
import com.example.hikerview.ui.home.model.RouteBlocker;
import com.example.hikerview.ui.home.model.article.extra.LongTextExtra;
import com.example.hikerview.ui.home.model.article.extra.RichTextExtra;
import com.example.hikerview.ui.home.model.article.extra.X5Extra;
import com.example.hikerview.ui.home.view.ArticleListIsland;
import com.example.hikerview.ui.home.view.RuleMenuPopup;
import com.example.hikerview.ui.home.webview.ArticleWebViewHolder;
import com.example.hikerview.ui.setting.file.FileDetailAdapter;
import com.example.hikerview.ui.setting.file.FileDetailPopup;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.ui.video.VideoChapter;
import com.example.hikerview.ui.video.model.RemotePlaySource;
import com.example.hikerview.ui.video.remote.LivePlayerHelper;
import com.example.hikerview.ui.view.CustomCopyPopup;
import com.example.hikerview.ui.view.PopImageLoader;
import com.example.hikerview.ui.view.PopImageLoaderNoView;
import com.example.hikerview.ui.view.SmartRefreshLayout;
import com.example.hikerview.utils.AutoImportHelper;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.DebugUtil;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.FilterUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.ScreenUtil;
import com.example.hikerview.utils.ShareUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.WebUtil;
import com.example.hikerview.utils.rule.ShareRuleUtil;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.ImageViewerPopupView;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.lxj.xpopup.interfaces.OnSrcViewUpdateListener;
import com.lxj.xpopup.interfaces.XPopupImageLoader;
import com.org.lqtk.fastscroller.RecyclerFastScroller;
import com.tencent.smtt.sdk.WebView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2017/10/16
 * 时间：At 18:31
 */

public class ArticleListFragment extends BaseFragment implements BaseCallback<ArticleList> {
    private static final String TAG = "ArticleListFragment";
    private RecyclerView recyclerView;
    private int page = 1;
    private ArticleListAdapter adapter;
    private ArticleListModel articleListModel;
    private ArticleListRule articleListRule;
    private ArticleListRule articleListRuleDTO;
    private int lastVisibleItem = 0;
    private GridLayoutManager gridLayoutManager;
    private boolean isLoading = false;
    private AppCompatImageView progressView1, progressView2;
    private SmartRefreshLayout smartRefreshLayout;
    private int headerSize = -1;
    private String firstPageData, lastPageData;
    private boolean hasShowLastClick = false;
    private LoadingPopupView loadingPopupView;
    private LoadingPopupView loadingScanPopupView;
    private boolean noHistory = false;
    private boolean noRecordHistory = false;
    private boolean memoryPos = true;
    private RecyclerFastScroller fastScroller;
    private String myUrl = "";
    private Map<Integer, Integer> imageUrlPosMap;
    private ArticleWebViewHolder webViewHolder;
    private RelativeLayout webViewContainer;
    //批量下载选集中
    private boolean batchDownloadSelecting = false;
    private boolean batchDlanSelecting = false;
    private View notify_bg;
    private TextView notify_text;
    private int batchDownloadCount;
    private AtomicInteger batchDlanParseCount = new AtomicInteger(0);
    private List<RemotePlaySource> batchDlanParams = new ArrayList<>();
    private LoadListener loadListener;
    private String onRefreshJS;
    private String onCloseJS;
    private FileDetailPopup detailPopup;
    private WebViewParser webViewParser = new WebViewParser();

    private boolean immersiveTheme;

    public ArticleListFragment() {
        super();
    }

    public ArticleListFragment(boolean lazy) {
        super(lazy);
    }

    public ArticleListFragment instance(ArticleListRule articleListRule1) {
        ArticleListRule articleListRule;
        try {
            articleListRule = articleListRule1.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            articleListRule = articleListRule1;
        }
        String colType = articleListRule.getCol_type();
        if (!TextUtils.isEmpty(colType)) {
            articleListRule.setCol_type(colType);
        } else {
            articleListRule.setCol_type(ArticleColTypeEnum.MOVIE_3.getCode());
        }
        if (!TextUtils.isEmpty(articleListRule.getClass_name())) {
            articleListRule.setFirstHeader("class");
        } else if (!TextUtils.isEmpty(articleListRule.getArea_name())) {
            articleListRule.setFirstHeader("area");
        } else if (!TextUtils.isEmpty(articleListRule.getYear_name())) {
            articleListRule.setFirstHeader("year");
        } else if (!TextUtils.isEmpty(articleListRule.getSort_name())) {
            articleListRule.setFirstHeader("sort");
        } else {
            articleListRule.setFirstHeader("class");
        }
        this.articleListRule = articleListRule;
        try {
            this.articleListRuleDTO = articleListRule.clone();
            if (StringUtil.isEmpty(articleListRuleDTO.getUrl())) {
                noHistory = true;
                noRecordHistory = true;
            } else {
                if (articleListRuleDTO.getUrl().contains("#noHistory#")) {
                    noHistory = true;
                    articleListRuleDTO.setUrl(StringUtils.replaceOnce(articleListRuleDTO.getUrl(), "#noHistory#", ""));
                }
                if (articleListRuleDTO.getUrl().contains("#noRecordHistory#")) {
                    noRecordHistory = true;
                    articleListRuleDTO.setUrl(StringUtils.replaceOnce(articleListRuleDTO.getUrl(), "#noRecordHistory#", ""));
                }
                if (articleListRuleDTO.getUrl().contains("#autoPage#")) {
                    noRecordHistory = true;
                    noHistory = true;
                }
            }
            if (!TextUtils.isEmpty(articleListRuleDTO.getArea_url())) {
                articleListRuleDTO.setArea_url(articleListRuleDTO.getArea_url().split("&")[0]);
            }
            if (!TextUtils.isEmpty(articleListRuleDTO.getClass_url())) {
                articleListRuleDTO.setClass_url(articleListRuleDTO.getClass_url().split("&")[0]);
            }
            if (!TextUtils.isEmpty(articleListRuleDTO.getYear_url())) {
                articleListRuleDTO.setYear_url(articleListRuleDTO.getYear_url().split("&")[0]);
            }
            if (!TextUtils.isEmpty(articleListRuleDTO.getSort_url())) {
                articleListRuleDTO.setSort_url(articleListRuleDTO.getSort_url().split("&")[0]);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void setImmersiveTheme(boolean immersiveTheme) {
        this.immersiveTheme = immersiveTheme;
    }

    public String getMyUrl() {
        return myUrl;
    }

    @Override
    protected int initLayout() {
        return R.layout.fragment_article_list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    protected void initView() {
        recyclerView = findView(R.id.frag_article_list_recycler_view);
        progressView1 = findView(R.id.progress_image_view1);
        ((Animatable) progressView1.getDrawable()).start();
        progressView2 = findView(R.id.progress_image_view2);
        webViewContainer = findView(R.id.webview_container);
        gridLayoutManager = new GridLayoutManager(getContext(), 60);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter == null) {
                    Timber.d("getSpanSize: adapter is null");
                    return 60;
                }
                if (recyclerView.getAdapter() != adapter) {
                    return 60;
                }
                return ArticleColTypeEnum.getSpanCountByItemType(adapter.getItemViewType(position));
            }
        });
        //设置多种布局
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        articleListModel = new ArticleListModel().withUrlParseCallBack(url -> myUrl = url);
        smartRefreshLayout = findView(R.id.refresh_layout);
        smartRefreshLayout.setOnRefreshListener(refreshLayout -> refresh());
        fastScroller = findView(R.id.frag_article_list_recycler_scroller);
        fastScroller.attachRecyclerView(recyclerView);
        initAdapter();
        fastScroller.setTouchTargetWidth(10);
        fastScroller.setMarginLeft(10);
        fastScroller.setMinItemCount(200);
        fastScroller.setBarColor(getContext().getResources().getColor(R.color.transparent));
        fastScroller.setDrawable(getContext().getResources().getDrawable(R.drawable.fastscroll_handle), getContext().getResources().getDrawable(R.drawable.fastscroll_handle));
        fastScroller.setMaxScrollHandleHeight(DisplayUtil.dpToPx(getContext(), 40));
        fastScroller.setPressedListener(pressed -> {
            boolean enable = !pressed;
            smartRefreshLayout.setEnableOverScrollDrag(enable);
            smartRefreshLayout.setEnableOverScrollBounce(enable);
            smartRefreshLayout.setEnableRefresh(enable);
        });
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        notify_bg = findView(R.id.notify_bg);
        notify_text = notify_bg.findViewById(R.id.notify_text);
        notify_bg.setOnClickListener(v -> {
            if (batchDownloadSelecting) {
                ToastMgr.shortBottomCenter(getContext(), "已关闭批量下载模式");
                batchDownloadSelecting = false;
                notify_bg.setVisibility(View.GONE);
                batchDownloadCount = 0;
                notify_text.setText("完 成");
            } else {
                if (batchDlanParseCount.get() > 0) {
                    ToastMgr.shortCenter(getContext(), "有剧集在动态解析中，请稍候");
                    return;
                }
                if (!CollectionUtil.isEmpty(batchDlanParams)) {
                    ToastMgr.shortBottomCenter(getContext(), "已开始批量投屏并播放");
                    LivePlayerHelper.finishBatchDlan(getActivity(), loadingScanPopupView, new ArrayList<>(batchDlanParams));
                }
                batchDlanParseCount.set(0);
                batchDlanSelecting = false;
                notify_bg.setVisibility(View.GONE);
                batchDownloadCount = 0;
                batchDlanParams.clear();
                notify_text.setText("完 成");
            }
        });
        notify_bg.setOnLongClickListener(v -> {
            v.setVisibility(View.INVISIBLE);
            ToastMgr.shortBottomCenter(getContext(), "已临时隐藏悬浮按钮");
            v.postDelayed(() -> {
                if (getActivity() == null || !isAdded() || isDetached() || getActivity().isFinishing()) {
                    return;
                }
                v.setVisibility(View.VISIBLE);
            }, 5000);
            return true;
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBack(OnBackEvent event) {
        Activity activity = getActivity();
        // 限制能生效的 Activity
        if (activity == null || activity.getClass() != FilmListActivity.class) {
            return;
        }
        // 限制调用一次只能返回一次
        FilmListActivity filmListActivity = (FilmListActivity) activity;
        if (filmListActivity.isOnPause()) {
            return;
        }
        if (event.isRefreshPage()) {
            Intent intentData = new Intent();
            intentData.putExtra("refreshPage", event.isRefreshPage()); // 封装需要返回的数据
            intentData.putExtra("scrollTop", event.isScrollTop());
            activity.setResult(Activity.RESULT_OK, intentData);
        }
        activity.onBackPressed();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvalJs(OnEvalJsEvent event) {
        if (getActivity() == null || getActivity().isFinishing() || !getUserVisibleHint()) {
            return;
        }
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            webViewHolder.getWebView().evaluateJavascript(event.getJs(), null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void finishParse(DestroyEvent event) {
        if (getActivity() == null || getActivity().isFinishing() || !getUserVisibleHint()) {
            return;
        }
        webViewParser.finishParse(getContext(), event.getUrl());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTop(OnTopEvent event) {
        if (getActivity() == null || getActivity().getClass() != MainActivity.class) {
            return;
        }
        scrollTop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageChange(OnPageChangeEvent event) {
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            if (StringUtils.equals(articleListRuleDTO.getTitle(), event.getTitle())) {
                webViewHolder.getWebView().onResume();
            } else {
                String muteJs = JSManager.instance(getContext()).getJsByFileName("mute");
                if (!TextUtils.isEmpty(muteJs)) {
                    webViewHolder.getWebView().evaluateJavascript(muteJs, null);
                }
                webViewHolder.getWebView().onPause();
            }
        }
    }

    public void scrollTop() {
        if (getUserVisibleHint()) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshWebView(OnRefreshWebViewEvent event) {
        if (isOnPause()) {
            return;
        }
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            webViewHolder.setUrl(event.getUrl());
            webViewHolder.getWebView().loadUrl(event.getUrl());
            updateWebViewHeight();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnRefreshX5Height(OnRefreshX5HeightEvent event) {
        if (isOnPause()) {
            return;
        }
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            updateModeByDesc(event.getDesc());
            updateWebViewHeight();
            updateWebViewHeightForList(event);
        }
    }

    private void updateWebViewHeightForList(OnRefreshX5HeightEvent event) {
        if (webViewHolder.getMode() == ArticleWebViewHolder.Mode.FLOAT) {
            return;
        }
        if (CollectionUtil.isEmpty(adapter.getList())) {
            return;
        }
        for (int i = 0; i < adapter.getList().size(); i++) {
            ArticleList datum = adapter.getList().get(i);
            if (ArticleColTypeEnum.getByCode(datum.getType()) == ArticleColTypeEnum.X5_WEB_VIEW) {
                datum.setDesc(event.getDesc());
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private boolean isOnPause() {
        if (getActivity() instanceof FilmListActivity) {
            FilmListActivity activity = (FilmListActivity) getActivity();
            if (activity.isOnPause()) {
                return true;
            }
        }
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity.isOnPause()) {
                return true;
            }
        }
        if (!getUserVisibleHint()) {
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshPage(OnRefreshPageEvent event) {
//        if (getActivity() == null || getActivity().getClass() != MainActivity.class) {
//            return;
//        }
        if (getActivity() instanceof FilmListActivity) {
            FilmListActivity activity = (FilmListActivity) getActivity();
            if (activity.isOnPause()) {
                return;
            }
        }
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity.isOnPause()) {
                return;
            }
        }
        if (getUserVisibleHint()) {
            if (event.isScrollTop()) {
                recyclerView.scrollToPosition(0);
            }
            page = 1;
            loading(true);
            articleListModel
                    .params(getContext(), page, true, articleListRuleDTO, notLoadPreRule())
                    .process(HomeActionEnum.ARTICLE_LIST_NEW, ArticleListFragment.this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefresh(OnRefreshEvent event) {
//        if (getActivity() == null || getActivity().getClass() != MainActivity.class) {
//            return;
//        }
        if (getUserVisibleHint()) {
            recyclerView.scrollToPosition(0);
            smartRefreshLayout.autoRefresh(200);
        }
    }

    private void refresh() {
        page = 1;
        this.loading(true);
        if (StringUtil.isNotEmpty(onRefreshJS)) {
            HeavyTaskUtil.executeNewTask(() -> {
                JSEngine.getInstance().evalJS(onRefreshJS, "");
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        articleListModel
                                .params(getContext(), page, true, articleListRuleDTO, notLoadPreRule())
                                .process(HomeActionEnum.ARTICLE_LIST_NEW, this);
                    });
                }
            });
            return;
        }
        articleListModel
                .params(getContext(), page, true, articleListRuleDTO, notLoadPreRule())
                .process(HomeActionEnum.ARTICLE_LIST_NEW, this);
    }

    private boolean notLoadPreRule() {
        if (getActivity() != null && getActivity().getIntent().getBooleanExtra("fromHistory", false)) {
            //从历史记录或者收藏页面过来的，需要执行预加载JS
            return false;
        }
        return getActivity() instanceof FilmListActivity;
    }

    @Override
    protected void initData() {
        this.loading(true);
        articleListModel
                .params(getContext(), page, false, articleListRuleDTO, notLoadPreRule())
                .process(HomeActionEnum.ARTICLE_LIST, this);
    }

    private synchronized List<ArticleList> initHeaders() {
        List<ArticleList> lists = new ArrayList<>();
        if (!immersiveTheme) {
            if (!(getActivity() instanceof FilmListActivity)) {
                lists.add(ArticleList.newBigBlank());
            }
            lists.add(ArticleList.newBlank());
            lists.add(ArticleList.newBlank());
            lists.add(ArticleList.newBlank());
        }
        if (immersiveTheme) {
            FrameLayout body = findView(R.id.bodyContainer);
            if (body != null) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) body.getLayoutParams();
                layoutParams.setMargins(0, 0, 0, 0);
            }
        }
        if (!TextUtils.isEmpty(articleListRule.getClass_name())) {
            ArticleList recommends3 = new ArticleList();
            recommends3.setType("header");
            recommends3.setTitle(articleListRule.getClass_name());
            recommends3.setUrl(articleListRule.getClass_url());
            recommends3.setDesc("class");
            lists.add(recommends3);
        }
        if (!TextUtils.isEmpty(articleListRule.getArea_name())) {
            ArticleList recommends1 = new ArticleList();
            recommends1.setType("header");
            recommends1.setTitle(articleListRule.getArea_name());
            recommends1.setUrl(articleListRule.getArea_url());
            recommends1.setDesc("area");
            lists.add(recommends1);
        }
        if (!TextUtils.isEmpty(articleListRule.getYear_name())) {
            ArticleList recommends2 = new ArticleList();
            recommends2.setType("header");
            recommends2.setDesc("year");
            recommends2.setTitle(articleListRule.getYear_name());
            recommends2.setUrl(articleListRule.getYear_url());
            lists.add(recommends2);
        }
        if (!TextUtils.isEmpty(articleListRule.getSort_name())) {
            ArticleList recommends2 = new ArticleList();
            recommends2.setType("header");
            recommends2.setDesc("sort");
            recommends2.setTitle(articleListRule.getSort_name());
            recommends2.setUrl(articleListRule.getSort_url());
            lists.add(recommends2);
        }
        return lists;
    }

    private void initAdapter() {
        adapter = new ArticleListAdapter(getActivity(), getContext(), recyclerView, new ArrayList<>(), this.articleListRuleDTO);
        fastScroller.attachAdapter(adapter);
        adapter.setOnItemClickListener(new ArticleListAdapter.OnItemClickListener() {
            @Override
            public void onUrlClick(View view, int position, String url) {
                memoryPos = false;
                String ruleUrl = myUrl;
                if (StringUtil.isEmpty(ruleUrl)) {
                    ruleUrl = articleListRuleDTO.getUrl();
                }
                url = StringUtil.autoFixUrl(ruleUrl, url);
                clickItem(view, position, url);
                memoryPos = true;
            }

            @Override
            public void onClick(View view, int position) {
                if (ArticleColTypeEnum.RICH_TEXT.equals(ArticleColTypeEnum.getByCode(adapter.getList().get(position).getType()))) {
                    if (StringUtil.isNotEmpty(adapter.getList().get(position).getExtra())) {
                        RichTextExtra extra = JSON.parseObject(adapter.getList().get(position).getExtra(), RichTextExtra.class);
                        if (extra.isClick()) {
                            recyclerView.smoothScrollBy(0, smartRefreshLayout.getMeasuredHeight());
                        }
                    }
                    return;
                } else if (ArticleColTypeEnum.LONG_TEXT.equals(ArticleColTypeEnum.getByCode(adapter.getList().get(position).getType()))) {
                    if (StringUtil.isNotEmpty(adapter.getList().get(position).getExtra())) {
                        LongTextExtra extra = JSON.parseObject(adapter.getList().get(position).getExtra(), LongTextExtra.class);
                        if (extra.isClick()) {
                            recyclerView.smoothScrollBy(0, smartRefreshLayout.getMeasuredHeight());
                        }
                    }
                    return;
                }
                memoryPos = true;
                clickItem(view, position);
            }

            @Override
            public void onLoadMore(View view, int position) {
                if (isLoading) {
                    return;
                }
                if (!articleListRuleDTO.getUrl().contains("fypage")) {
                    ToastMgr.shortBottomCenter(getContext(), "没有下一页了哦！");
                    return;
                }
                isLoading = true;
                page++;
                loading(true);
//                adapter.getList().remove(adapter.getList().size() - 1);
                articleListModel
                        .params(getContext(), page, false, articleListRuleDTO, notLoadPreRule())
                        .process(HomeActionEnum.ARTICLE_LIST, ArticleListFragment.this);
            }

            @Override
            public void onLongClick(View view, final int position) {
                String url = adapter.getList().get(position).getUrl();
                String colType = adapter.getList().get(position).getType();
//                if (StringUtil.isEmpty(url) && !ArticleColTypeEnum.RICH_TEXT.getCode().equals(colType) && !ArticleColTypeEnum.LONG_TEXT.getCode().equals(colType)) {
//                    ToastMgr.shortBottomCenter(getContext(), "地址为空");
//                    return;
//                }
                if (!StringUtil.isEmpty(url) && url.startsWith("hiker://home@") && !ArticleColTypeEnum.LONG_TEXT.getCode().equals(colType)) {
                    longClickForRuleUrl(url);
                    return;
                }
                String[] urlRule = (url == null ? "" : url).split("@rule=");
                String[] lazyRule = urlRule[0].split("@lazyRule=");
                if (ArticleColTypeEnum.RICH_TEXT.getCode().equals(colType) || ArticleColTypeEnum.LONG_TEXT.getCode().equals(colType)) {
                    //content
                    AlertDialog dialog = new AlertDialog.Builder(getContext())
                            .setItems(new String[]{"自由复制", "调试数据"}, (dialog1, which) -> {
                                dealLongMenuClick(which == 0 ? "自由复制" : "调试数据", position, lazyRule);
                                dialog1.dismiss();
                            }).create();
                    dialog.show();
                    if (dialog.getWindow() != null) {
                        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                        int width = ScreenUtil.getScreenWidth(getActivity());
                        if (width > 0) {
                            lp.width = 2 * width / 3;
                            dialog.getWindow().setAttributes(lp);
                        }
                    }
                } else {
                    new XPopup.Builder(getContext())
                            .asCustom(new RuleMenuPopup(getActivity(),
                                    iconTitle -> dealLongMenuClick(iconTitle.getTitle(), position, lazyRule),
                                    Arrays.asList("访问链接", "下载资源", "批量下载", "批量投屏", "外部打开", "网站首页", "调试数据", "复制链接")))
                            .show();
                }
            }

            @Override
            public void onClassClick(View view, String url, String urltype) {
                switch (urltype) {
                    case "year":
                        articleListRuleDTO.setYear_url(url);
                        articleListRuleDTO.setFirstHeader("year");
                        break;
                    case "class":
                        articleListRuleDTO.setClass_url(url);
                        articleListRuleDTO.setFirstHeader("class");
                        break;
                    case "area":
                        articleListRuleDTO.setArea_url(url);
                        articleListRuleDTO.setFirstHeader("area");
                        break;
                    case "sort":
                        articleListRuleDTO.setSort_url(url);
                        articleListRuleDTO.setFirstHeader("sort");
                        break;
                }
                page = 1;
                loading(true);
                articleListModel
                        .params(getContext(), page, true, articleListRuleDTO, notLoadPreRule())
                        .process(HomeActionEnum.ARTICLE_LIST_NEW, ArticleListFragment.this);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(adapter.getDividerItem());
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                Log.d(TAG, "onScrollStateChanged: " + adapter.getItemCount() + ", " + lastVisibleItem);
                if ((newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 == adapter.getItemCount())
                        || (lastVisibleItem == -1 && adapter.getItemCount() <= 10)) {
                    if (!articleListRuleDTO.getUrl().contains("fypage")) {
                        if (articleListRuleDTO.getUrl().contains("#autoPage#")) {
                            autoPage();
                        }
                        return;
                    }
                    page++;
                    loading(true);
//                    adapter.getList().remove(adapter.getList().size() - 1);
                    articleListModel
                            .params(getContext(), page, false, articleListRuleDTO, notLoadPreRule())
                            .process(HomeActionEnum.ARTICLE_LIST, ArticleListFragment.this);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();
            }
        });
    }

    private void autoPage() {
        if (!(getActivity() instanceof FilmListActivity)) {
            return;
        }
        FilmListActivity activity = (FilmListActivity) getActivity();
        AutoPageData pageData = activity.getParentData();
        if (pageData == null || CollectionUtil.isEmpty(pageData.getNextData())) {
            return;
        }
        try {
            page++;
            loading(true);
            ArticleListRule rule = articleListRuleDTO.clone();
            //ParentData只存了当前页及后面的内容
            if (page > pageData.getNextData().size()) {
                loading(false);
                page--;
//                ToastMgr.shortBottomCenter(getContext(), "到底啦~");
                return;
            }
            ArticleList currArticleList = pageData.getNextData().get(page - 1);
            String[] urls = rule.getUrl().split(";");
            urls[0] = currArticleList.getUrl().split("@rule=")[0].split(";")[0];
            rule.setUrl(StringUtil.arrayToString(urls, 0, ";"));
            rule.setParams(currArticleList.getExtra());
            articleListModel
                    .params(getContext(), page, false, rule, notLoadPreRule())
                    .process(HomeActionEnum.ARTICLE_LIST, ArticleListFragment.this);
            if (!pageData.isNoRecordHistory()) {
                //更新历史记录，且记录上一章，因为会提前加载
                ArticleList articleList = pageData.getNextData().get(page - 2);
                List<ViewCollection> collections = null;
                try {
                    collections = LitePal.where("CUrl = ? and MTitle = ?", pageData.getCUrl(), pageData.getMTitle()).limit(1).find(ViewCollection.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String click = articleList.getTitle();
                click = click + "@@" + (pageData.getCurrentPos() + page - 2);
                if (!CollectionUtil.isEmpty(collections)) {
                    if (StringUtil.isEmpty(click) || click.length() > 25) {
                        return;
                    }
                    collections.get(0).setLastClick(click);
                    collections.get(0).save();
                }
                ((FilmListActivity) activity).setLastClick(new LastClickShowEvent(click));
                HeavyTaskUtil.updateHistoryLastClick(pageData.getMTitle(), pageData.getCUrl(), click);
            }
        } catch (Exception e) {
            loading(false);
            page--;
            ToastMgr.shortBottomCenter(getContext(), "出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void dealLongMenuClick(String text, int position, String[] lazyRule) {
        switch (text) {
            case "自由复制":
                new XPopup.Builder(getContext())
                        .asCustom(new CustomCopyPopup(getContext()).with(null, adapter.getList().get(position).getTitle()))
                        .show();
                break;
            case "下载资源":
                downloadSource(position, lazyRule);
                break;
            case "批量下载":
                if (batchDlanSelecting) {
                    ToastMgr.shortCenter(getContext(), "当前正在批量投屏中，不支持此操作");
                    return;
                }
                new XPopup.Builder(getContext())
                        .asConfirm("批量下载", "点击确定后，点击页面中要下载的内容即可快速添加到下载队列，添加完成后请点击右下角的完成按钮结束批量下载模式", () -> {
                            batchDownloadSelecting = true;
                            notify_bg.setVisibility(View.VISIBLE);
                            ToastMgr.shortBottomCenter(getContext(), "已开启批量下载模式");
                        }).show();
                break;
            case "批量投屏":
                if (batchDownloadSelecting) {
                    ToastMgr.shortCenter(getContext(), "当前正在批量投屏中，不支持此操作");
                    return;
                }
                new XPopup.Builder(getContext())
                        .asConfirm("批量投屏", "点击确定后，点击页面中要投屏的内容即可快速添加到投屏队列，注意是投屏到世界直播，不是传统投屏！添加完成后请点击右下角的完成按钮结束批量投屏模式", () -> {
                            batchDlanSelecting = true;
                            notify_bg.setVisibility(View.VISIBLE);
                            ToastMgr.shortBottomCenter(getContext(), "已开启批量投屏模式");
                        }).show();
                break;
            case "更多分享":
                if (!(getActivity() instanceof MainActivity)) {
                    ToastMgr.shortCenter(getContext(), "当前页面不支持此操作");
                    break;
                }
                ShareRuleUtil.shareRuleByMoreWay(getActivity(), articleListRule);
                break;
            case "快速搜索":
                String t = adapter.getList().get(position).getTitle();
                if (StringUtil.isEmpty(t)) {
                    ToastMgr.shortCenter(getContext(), "标题为空，无法搜索");
                } else {
                    String group = PreferenceMgr.getString(getContext(), "searchGroup", "全部");
                    String engineTitle = PreferenceMgr.getString(getContext(), "searchEngine", "百度");
                    RouteBlocker.isRoute(getActivity(), "hiker://search?simple=false&ruleGroup=" + group + "&rule=" + engineTitle + "&s=" + t);
                }
                break;
            case "访问链接":
                if (TextUtils.isEmpty(lazyRule[0])) {
                    ToastMgr.shortBottomCenter(getContext(), "链接为空，请检查规则");
                } else {
                    Bundle extraDataBundle = new Bundle();
                    String viewCollectionExtraData = getViewCollectionExtraData();
                    if (!StringUtil.isEmpty(viewCollectionExtraData)) {
                        extraDataBundle.putString("viewCollectionExtraData", viewCollectionExtraData);
                    }
                    if (getActivity() != null && getActivity().getTitle() != null) {
                        extraDataBundle.putString("film", getActivity().getTitle().toString());
                    }
                    WebUtil.goWebWithExtraData(getContext(), HttpParser.getFirstPageUrl(lazyRule[0]), extraDataBundle);
                }
                break;
            case "复制链接":
                if (TextUtils.isEmpty(lazyRule[0])) {
                    ToastMgr.shortBottomCenter(getContext(), "链接为空，请检查规则");
                } else {
                    ClipboardUtil.copyToClipboard(getContext(), HttpParser.getFirstPageUrl(lazyRule[0]));
                }
                break;
            case "编辑规则":
                if (!(getActivity() instanceof MainActivity)) {
                    ToastMgr.shortCenter(getContext(), "当前页面不支持此操作");
                    break;
                }
                Intent intent = new Intent(getContext(), ArticleListRuleEditActivity.class);
                String data = JSON.toJSONString(articleListRule);
                intent.putExtra("data", data);
                intent.putExtra("edit", true);
                startActivity(intent);
                break;
            case "分享规则":
                ShareRuleUtil.shareRule(getContext(), articleListRule);
                break;
            case "外部打开":
                if (TextUtils.isEmpty(lazyRule[0])) {
                    ToastMgr.shortBottomCenter(getContext(), "链接为空，请检查规则");
                } else {
                    ShareUtil.findChooserToDeal(getContext(), HttpParser.getFirstPageUrl(lazyRule[0]));
                }
                break;
            case "网站首页":
                WebUtil.goWeb(getContext(), StringUtil.getHomeUrl(articleListRuleDTO.getUrl()));
                break;
            case "调试数据":
                showPosDetail(position);
                break;
        }
    }

    private String[] getDetailData(int position) {
        ArticleList articleList = adapter.getList().get(position);
        String url = StringUtil.isNotEmpty(articleList.getUrl()) && articleList.getUrl().length() > 200 ?
                articleList.getUrl().substring(0, 200) + "..." : articleList.getUrl();
        return new String[]{
                "页面：" + myUrl,
                "标题：" + articleList.getTitle(),
                "描述：" + articleList.getDesc(),
                "类型：" + articleList.getType(),
                "链接：" + url,
                "图片：" + articleList.getPic(),
                "附加：" + articleList.getExtra()
        };
    }

    private void showPosDetail(int position) {
        detailPopup = new FileDetailPopup(getActivity(), "调试数据", getDetailData(position))
                .withClickListener(new FileDetailAdapter.OnClickListener() {
                    @Override
                    public void click(String text) {
                        longClick(null, text);
                    }

                    @Override
                    public void longClick(View view, String text) {
                        String t;
                        Consumer<String> updater;
                        if (text.startsWith("链接：")) {
                            t = adapter.getList().get(position).getUrl();
                            updater = s -> adapter.getList().get(position).setUrl(s);
                        } else if (text.startsWith("标题：")) {
                            t = adapter.getList().get(position).getTitle();
                            updater = s -> adapter.getList().get(position).setTitle(s);
                        } else if (text.startsWith("描述：")) {
                            t = adapter.getList().get(position).getDesc();
                            updater = s -> adapter.getList().get(position).setDesc(s);
                        } else if (text.startsWith("类型：")) {
                            t = adapter.getList().get(position).getType();
                            updater = s -> adapter.getList().get(position).setType(s);
                        } else if (text.startsWith("图片：")) {
                            t = adapter.getList().get(position).getPic();
                            updater = s -> adapter.getList().get(position).setPic(s);
                        } else if (text.startsWith("附加：")) {
                            t = adapter.getList().get(position).getExtra();
                            updater = s -> adapter.getList().get(position).setExtra(s);
                        } else if (text.startsWith("页面：")) {
                            ClipboardUtil.copyToClipboard(getContext(), myUrl, false);
                            ToastMgr.shortCenter(getContext(), "页面链接已复制到剪贴板");
                            return;
                        } else {
                            return;
                        }
                        new XPopup.Builder(getContext())
                                .asInputConfirm("编辑数据", "点击确定后，界面数据会临时修改为传入的数据，刷新后失效",
                                        t, null, s -> {
                                            updater.accept(s);
                                            adapter.notifyItemChanged(position);
                                            if (detailPopup != null) {
                                                detailPopup.updateData(getDetailData(position));
                                            }
                                        }, null, R.layout.xpopup_confirm_input).show();
                    }
                });
        new XPopup.Builder(getContext())
                .asCustom(detailPopup).show();
    }

    /**
     * 快速批量下载，后台动态解析
     *
     * @param position
     * @param lazyRule
     */
    private void fastDownload(int position, String[] lazyRule) {
        parseLazyRuleForUrl(position, "下载", lazyRule, new BaseParseCallback<String>() {
            @Override
            public void start() {
                ToastMgr.shortBottomCenter(getContext(), "已加入动态解析队列");
            }

            @Override
            public void success(String data) {
                String title = getItemTitle(position);
                if (UrlDetector.isVideoOrMusic(data)) {
                    data = UrlDetector.clearTag(data);
                    int downloader = PreferenceMgr.getInt(getContext(), "defaultDownloader", 0);
                    if (downloader != 0) {
                        DownloadChooser.startDownload(getActivity(), downloader, title, data);
                        return;
                    }
                    DownloadTask downloadTask = new DownloadTask(
                            UUIDUtil.genUUID(), null, null, null, data, data, title, 0L);
                    downloadTask.setFilm(getFilm());
                    DownloadManager.instance().addTask(downloadTask);
                    ToastMgr.shortBottomCenter(getContext(), title + "已加入下载队列");
                } else {
                    ToastMgr.shortBottomCenter(getContext(), title + "非视频格式，不支持下载");
                }
            }

            @Override
            public void error(String msg) {

            }
        });
    }

    /**
     * 批量投屏
     *
     * @param position
     * @param lazyRule
     */
    private void fastDlan(int position, String[] lazyRule) {
        parseLazyRuleForUrl(position, "投屏", lazyRule, new BaseParseCallback<String>() {
            @Override
            public void start() {
                batchDlanParseCount.getAndAdd(1);
                ToastMgr.shortBottomCenter(getContext(), "已加入动态解析队列");
            }

            @Override
            public void success(String data) {
                batchDlanParseCount.getAndDecrement();
                String title = getItemTitle(position);
                if (UrlDetector.isVideoOrMusic(data)) {
                    data = UrlDetector.clearTag(data);
                    RemotePlaySource httpParams = new RemotePlaySource();
                    httpParams.setTitle(HttpParser.encodeUrl(title.replace(",", "_")));
                    httpParams.setUrl(HttpParser.encodeUrl(data));
                    String json = JSON.toJSONString(httpParams);
                    for (int i = 0; i < batchDlanParams.size(); i++) {
                        if (json.equals(JSON.toJSONString(batchDlanParams.get(i)))) {
                            batchDlanParams.remove(i);
                            ToastMgr.shortBottomCenter(getContext(), title + "已移出投屏队列");
                            batchDownloadCount--;
                            notify_text.setText(("完成 (" + batchDownloadCount + ")"));
                            return;
                        }
                    }
                    batchDlanParams.add(httpParams);
//                    ToastMgr.shortBottomCenter(getContext(), title + "已加入投屏队列");
                    batchDownloadCount++;
                    notify_text.setText(("完成 (" + batchDownloadCount + ")"));
                } else {
                    ToastMgr.shortBottomCenter(getContext(), title + "非视频格式，不支持投屏");
                }
            }

            @Override
            public void error(String msg) {
                batchDlanParseCount.getAndDecrement();
            }
        });
    }

    /**
     * 长按下载资源
     *
     * @param position
     * @param lazyRule
     */
    private void downloadSource(int position, String[] lazyRule) {
        parseLazyRuleForUrl(position, "下载", lazyRule, new BaseParseCallback<String>() {
            @Override
            public void start() {
                if (loadingPopupView == null) {
                    loadingPopupView = new XPopup.Builder(getContext()).asLoading();
                }
                loadingPopupView.setTitle("动态解析规则中，请稍候");
                loadingPopupView.show();
            }

            @Override
            public void success(String data) {
                if (UrlDetector.isVideoOrMusic(data)) {
                    data = UrlDetector.clearTag(data);
                    DownloadDialogUtil.showEditDialog(getActivity(), getItemTitle(position), data, getFilm());
                } else {
                    ToastMgr.shortBottomCenter(getContext(), getItemTitle(position) + "非视频格式，不支持下载");
                }
                if (loadingPopupView != null) {
                    loadingPopupView.dismiss();
                }
            }

            @Override
            public void error(String msg) {
                if (loadingPopupView != null) {
                    loadingPopupView.dismiss();
                }
            }
        });
    }

    private String getFilm() {
        if (getActivity() == null || getActivity().getTitle() == null) {
            return null;
        } else {
            return getActivity().getTitle().toString();
        }
    }

    /**
     * 解析动态解析
     *
     * @param operation
     * @param lazyRule
     * @param callback
     */
    private void parseLazyRuleForUrl(int position, String operation, String[] lazyRule, BaseParseCallback<String> callback) {
        if (TextUtils.isEmpty(lazyRule[0])) {
            ToastMgr.shortBottomCenter(getContext(), "链接为空，请检查规则");
        } else {
            if (!lazyRule[0].startsWith("http") && !lazyRule[0].startsWith("x5Play://")) {
                if (lazyRule.length <= 1) {
                    ToastMgr.shortBottomCenter(getContext(), "该地址不支持" + operation);
                    return;
                }
            }
            String urlWithUa = HttpParser.getUrlAppendUA(articleListRule.getUrl(), articleListRule.getUa());
            String codeAndHeader = DetailUIHelper.getCodeAndHeader(urlWithUa, lazyRule);
            if (lazyRule.length > 1) {
                LazyRuleParser.parse(getActivity(), articleListRule, lazyRule, codeAndHeader, myUrl, new BaseParseCallback<String>() {
                    @Override
                    public void start() {
                        callback.start();
                    }

                    @Override
                    public void success(String url) {
                        if (webViewParser.canParse(url)) {
                            webViewParser.parse(getActivity(), url, adapter.getList().get(position).getExtra(), callback::success);
                            return;
                        }
                        callback.success(url);
                    }

                    @Override
                    public void error(String msg) {
                        callback.error(msg);
                    }
                });
            } else {
                callback.success(lazyRule[0]);
            }
        }
    }

    /**
     * 根据规则的url实现长按操作
     *
     * @param url
     */
    private void longClickForRuleUrl(String url) {
        String[] s = url.split("hiker://home@");
        if (s.length != 2 || StringUtil.isEmpty(s[1])) {
            ToastMgr.shortBottomCenter(getContext(), "地址有误：" + url);
            return;
        }
        String ruleName = null;
        String[] rules = s[1].split("\\|\\|");
        if (rules.length > 1) {
            for (String rule : rules) {
                if (rule.startsWith("http")) {
                    //有http链接直接到网页
                    WebUtil.goWeb(getContext(), rule);
                    return;
                } else {
                    //找对应规则，看看有没有
                    ArticleListRule articleListRule = LitePal.where("title = ?", rule).findFirst(ArticleListRule.class);
                    if (articleListRule != null) {
                        //找到就结束
                        ruleName = rule;
                        break;
                    }
                }
            }
            if (ruleName == null) {
                ToastMgr.shortBottomCenter(getContext(), "找不到对应的规则：" + s[1]);
                return;
            }
        } else {
            ruleName = s[1];
        }
        ArticleListRule articleListRule = LitePal.where("title = ?", ruleName).findFirst(ArticleListRule.class);
        if (articleListRule == null) {
            ToastMgr.shortBottomCenter(getContext(), "找不到对应的规则：" + ruleName);
            return;
        }
        new XPopup.Builder(getContext())
                .asCenterList("管理规则：" + ruleName, new String[]{"编辑规则", "分享规则", "删除规则", "分享完整编码", "分享明文规则"},
                        ((option, text) -> {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            try {
                                switch (text) {
                                    case "编辑规则":
                                        Intent intent = new Intent(getContext(), ArticleListRuleEditActivity.class);
                                        String data = JSON.toJSONString(articleListRule);
                                        intent.putExtra("data", data);
                                        intent.putExtra("edit", true);
                                        startActivity(intent);
                                        break;
                                    case "分享完整编码":
                                        ArticleListRuleJO ruleJO12 = new ArticleListRuleJO(articleListRule);
                                        String shareRulePrefix = PreferenceMgr.getString(getContext(), "shareRulePrefix", "");
                                        String command = AutoImportHelper.getCommand(shareRulePrefix, JSON.toJSONString(ruleJO12, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        command = new String(Base64.encode(command.getBytes(), Base64.NO_WRAP));
                                        ClipboardUtil.copyToClipboardForce(getContext(), "rule://" + StringUtil.replaceLineBlank(command), false);
                                        ToastMgr.shortBottomCenter(getContext(), "口令已复制到剪贴板");
                                        break;
                                    case "分享明文规则":
                                        ArticleListRuleJO ruleJO3 = new ArticleListRuleJO(articleListRule);
                                        String originalRuls = JSON.toJSONString(ruleJO3, JSONPreFilter.getSimpleFilter());
                                        if (FilterUtil.hasFilterWord(originalRuls)) {
                                            ToastMgr.shortBottomCenter(getContext(), "规则含有违禁词，禁止分享");
                                            return;
                                        }
                                        String base64Rule = "base64://@" + ruleJO3.getTitle() + "@" + new String(Base64.encode(originalRuls.getBytes(), Base64.NO_WRAP));
                                        AutoImportHelper.shareWithCommand(getContext(), StringUtil.replaceLineBlank(base64Rule), AutoImportHelper.HOME_RULE_V2);
                                        break;
                                    case "分享规则":
                                        ShareRuleUtil.shareRule(getContext(), articleListRule);
                                        break;
                                    case "删除规则":
                                        new XPopup.Builder(getContext())
                                                .asConfirm("温馨提示", "确定删除规则‘" + articleListRule.getTitle() + "’吗？注意删除后无法恢复！", new OnConfirmListener() {
                                                    @Override
                                                    public void onConfirm() {
                                                        try {
                                                            String title = articleListRule.getTitle();
                                                            articleListRule.delete();
                                                            HeavyTaskUtil.executeNewTask(() -> {
                                                                BigTextDO bigTextDO = LitePal.where("key = ?", BigTextDO.ARTICLE_LIST_ORDER_KEY).findFirst(BigTextDO.class);
                                                                if (bigTextDO != null) {
                                                                    String value = bigTextDO.getValue();
                                                                    if (StringUtil.isNotEmpty(value)) {
                                                                        Map<String, Integer> orderMap = JSON.parseObject(value, new TypeReference<Map<String, Integer>>() {
                                                                        });
                                                                        orderMap.remove(title);
                                                                        bigTextDO.setValue(JSON.toJSONString(orderMap));
                                                                        bigTextDO.save();
                                                                    }
                                                                }
                                                            });
                                                            EventBus.getDefault().post(new OnArticleListRuleChangedEvent());
                                                            ToastMgr.shortBottomCenter(getContext(), "删除成功");
                                                            if (getActivity() instanceof FilmListActivity && !getActivity().isFinishing()) {
                                                                refresh();
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }).show();
                                        break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }))
                .show();
    }

    private void clickItem(@Nullable View view, int position) {
        if (position < 0 || position >= adapter.getList().size()) {
            return;
        }
        String url = adapter.getList().get(position).getUrl();
        if (ArticleColTypeEnum.input.getCode().equals(adapter.getList().get(position).getType())) {
            if (view != null && view.getTag() instanceof EditText) {
                EditText edit = (EditText) view.getTag();
                String key = edit.getText() == null ? "" : edit.getText().toString();
                if (url.startsWith("js:")) {
                    url = StringUtils.replaceOnce(url, "js:", "");
                }
                String finalUrl = url;
                HeavyTaskUtil.executeNewTask(() -> {
                    String result = JSEngine.getInstance().evalJS(finalUrl, key);
                    if (StringUtil.isNotEmpty(result) && !"undefined".equalsIgnoreCase(result)
                            && getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> clickItem(view, position, result));
                    }
                });
            }
        } else {
            clickItem(view, position, url);
        }
    }

    private void clickItem(@Nullable View view, int position, String url) {
        if (TextUtils.isEmpty(url)) {
            ToastMgr.shortCenter(getContext(), "链接为空，规则有误！");
            return;
        }
        if ("hiker://debug".equals(url)) {
            Intent intent1 = new Intent(getContext(), ArticleListRuleEditActivity.class);
            String data = JSON.toJSONString(articleListRule);
            intent1.putExtra("data", data);
            intent1.putExtra("edit", true);
            intent1.putExtra("debug", true);
            startActivity(intent1);
            return;
        }
        if (RouteBlocker.isRoute(getActivity(), url)) {
            return;
        }
        if (url.startsWith("base64://")) {
            url = StringUtils.replaceOnce(url, "base64://", "");
            url = new String(Base64.decode(url.trim(), Base64.NO_WRAP));
        }
        if (PageParser.isPageUrl(url)) {
            toNextPage(position, url);
            return;
        } else if (DetailUIHelper.dealUrlSimply(getActivity(), articleListRuleDTO, url, u -> clickItem(view, position, u))) {
            if (webViewHolder != null) {
                String originalUrl = adapter.getList().get(position).getUrl();
                if (originalUrl != null && (originalUrl.contains("refreshX5WebView") || originalUrl.contains("refreshPage"))) {
                    memoryPosition(position);
                } else if (url.contains("x5WebView://") || url.contains("refreshPage")) {
                    memoryPosition(position);
                }
            }
            return;
        }
        String[] urlRule = url.split("@rule=");
        String[] lazyRule = urlRule[0].split("@lazyRule=");
        if (batchDownloadSelecting) {
            //批量下载
            batchDownloadCount++;
            notify_text.setText(("完成 (" + batchDownloadCount + ")"));
            fastDownload(position, lazyRule);
            return;
        } else if (batchDlanSelecting) {
            //批量投屏
            fastDlan(position, lazyRule);
            return;
        }
        String urlWithUa = HttpParser.getUrlAppendUA(articleListRule.getUrl(), articleListRule.getUa());
        String codeAndHeader = DetailUIHelper.getCodeAndHeader(urlWithUa, lazyRule);
        if (lazyRule.length > 1) {
            dealLazyRule(view, position, lazyRule, codeAndHeader);
            return;
        }

        //标题处理，将html转成纯文本
        String title = getItemTitle(position);

        if (urlRule.length > 1) {
            String r = StringUtil.arrayToString(urlRule, 1, "@rule=");
            String nextRule = r.split("==>")[0];
            String[] rules = nextRule.split(";");
            String colType = articleListRule.getCol_type();
            if (rules.length == 6 && !r.startsWith("js:")) {
                //有col_type，提取，并且修正规则
                colType = rules[5];
                nextRule = StringUtil.arrayToString(rules, 0, 5, ";");
            }
            String s1 = StringUtil.arrayToString(r.split("==>"), 1, "==>");
            if (StringUtil.isNotEmpty(s1)) {
                s1 = "==>" + s1;
            } else {
                s1 = "";
            }
            String rule = nextRule + s1;
            dealRule(articleListRule,
                    urlRule[0], codeAndHeader,
                    rule, colType,
                    title, position);
            return;
        }
        if (StringUtil.isNotEmpty(articleListRule.getDetail_find_rule())) {
            dealRule(articleListRule,
                    url, codeAndHeader,
                    articleListRule.getDetail_find_rule(), articleListRule.getDetail_col_type(),
                    title, position);
            return;
        }
        dealWithUrl(view, url, position, null);
    }

    private void toNextPage(int position, String url) {
        HeavyTaskUtil.executeNewTask(() -> {
            try {
                ArticleListRule nextPage = PageParser.getNextPage(articleListRule, url, adapter.getList().get(position).getExtra());
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        //标题处理，将html转成纯文本
                        String title = getItemTitle(position);
                        Intent intent = new Intent(getContext(), FilmListActivity.class);
                        intent.putExtra("data", JSON.toJSONString(nextPage));
                        intent.putExtra("title", title);
                        if (StringUtil.isNotEmpty(adapter.getList().get(position).getPic())) {
                            intent.putExtra("picUrl", adapter.getList().get(position).getPic());
                        }
                        if (nextPage != null && nextPage.getUrl().contains("#autoPage#")) {
                            List<ArticleList> nextData = new ArrayList<>(adapter.getList().subList(position, adapter.getList().size()));
                            FilmListActivity.tempParentData = new AutoPageData(nextData, articleListRule.getUrl(),
                                    DetailUIHelper.getActivityTitle(getActivity()), position, noRecordHistory);
                        }
                        memoryPosition(position);
                        getActivity().startActivityForResult(intent, FilmListActivity.REFRESH_PAGE_CODE);
                    });
                }
            } catch (ParseException e) {
                e.printStackTrace();
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> ToastMgr.shortCenter(getContext(), e.getMessage()));
                }
            }
        });
    }

    private String getItemTitle(int position) {
        return DetailUIHelper.getItemTitle(getActivity(), adapter.getList().get(position).getTitle());
    }

    private void dealRule(ArticleListRule articleListRule, String url, String codeAndHeader, String rule, String colType, String title, int position) {
        ArticleListRule articleListRule1 = new ArticleListRule();
        if (!url.contains(";")) {
            articleListRule1.setUrl(url + codeAndHeader);
        } else {
            articleListRule1.setUrl(url);
        }
        articleListRule1.setUa(articleListRule.getUa());
        articleListRule1.setFind_rule(rule);
        articleListRule1.setCol_type(colType);
        articleListRule1.setGroup(articleListRule.getGroup());
        articleListRule1.setPreRule(articleListRule.getPreRule());
        articleListRule1.setTitle(articleListRule.getTitle());
        articleListRule1.setLast_chapter_rule(articleListRule.getLast_chapter_rule());
        articleListRule1.setPages(articleListRule.getPages());
        Intent intent = new Intent(getContext(), FilmListActivity.class);
        if (articleListRule1.getUrl().contains("#autoPage#")) {
            List<ArticleList> nextData = new ArrayList<>(adapter.getList().subList(position, adapter.getList().size()));
            FilmListActivity.tempParentData = new AutoPageData(nextData, this.articleListRule.getUrl(),
                    DetailUIHelper.getActivityTitle(getActivity()), position, noRecordHistory);
        }
        intent.putExtra("data", JSON.toJSONString(articleListRule1));
        intent.putExtra("title", title);
        try {
            if (StringUtil.isNotEmpty(adapter.getList().get(position).getPic())) {
                intent.putExtra("picUrl", adapter.getList().get(position).getPic());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        memoryPosition(position);
        getActivity().startActivityForResult(intent, FilmListActivity.REFRESH_PAGE_CODE);
    }

    private void dealLazyRule(View view, int position, String[] lazyRule, String codeAndHeader) {
        LazyRuleParser.parse(getActivity(), articleListRule, lazyRule, codeAndHeader, myUrl, new BaseParseCallback<String>() {
            @Override
            public void start() {
                if (loadingPopupView == null) {
                    loadingPopupView = new XPopup.Builder(getContext()).asLoading();
                }
                loadingPopupView.setTitle("动态解析规则中，请稍候");
                if (!lazyRule[0].contains("#noLoading#")) {
                    loadingPopupView.show();
                }
            }

            @Override
            public void success(String data) {
                dealWithUrl(view, data, position, codeAndHeader);
                if (loadingPopupView != null) {
                    loadingPopupView.dismiss();
                }
            }

            @Override
            public void error(String msg) {
                if (loadingPopupView != null) {
                    loadingPopupView.dismiss();
                }
            }
        });
    }

    private void dealWithUrl(@Nullable View view, String url, int position, @Nullable String codeAndHeader) {
        if (position < 0 || position >= adapter.getList().size()) {
            return;
        }
        if (url.contains("#noHistory#")) {
            url = StringUtils.replaceOnce(url, "#noHistory#", "");
            memoryPos = false;
        }
        if (PageParser.isPageUrl(url)) {
            toNextPage(position, url);
            return;
        } else if (DetailUIHelper.dealUrlSimply(getActivity(), articleListRuleDTO, url, u -> dealWithUrl(view, u, position, codeAndHeader))) {
            if (webViewHolder != null) {
                String originalUrl = adapter.getList().get(position).getUrl();
                if (originalUrl != null && (originalUrl.contains("refreshX5WebView") || originalUrl.contains("refreshPage"))) {
                    memoryPosition(position);
                } else if (url != null && (url.contains("x5WebView://") || url.contains("refreshPage"))) {
                    memoryPosition(position);
                }
            }
            return;
        }
        //标题处理，将html转成纯文本
        String itemTitle = getItemTitle(position);
        String activityTitle = DetailUIHelper.getActivityTitle(getActivity());
        String intentTitle = activityTitle;
        if (StringUtil.isNotEmpty(intentTitle)) {
            intentTitle = intentTitle + "-";
        }
        String lowUrl = url.toLowerCase();
        if (lowUrl.startsWith("javascript")) {
            //JS
            memoryPosition(position);
            WebUtil.goWeb(getContext(), url);
            return;
        } else if (url.startsWith("x5://")) {
            String title = StringUtils.replaceOnceIgnoreCase(url, "x5://", "");
            ArticleListRule articleListRule1 = new ArticleListRule();
            articleListRule1.setUrl("hiker://empty");
            articleListRule1.setFind_rule("js:setResult([{\n" +
                    "    url:\"" + title + "\",\n" +
                    "desc:\"100%&&float\",\n" +
                    "extra:{canBack: true}\n" +
                    "}]);");
            articleListRule1.setCol_type(ArticleColTypeEnum.X5_WEB_VIEW.getCode());
            articleListRule1.setGroup(articleListRule.getGroup());
            articleListRule1.setTitle(articleListRule.getTitle());
            articleListRule1.setPages(articleListRule.getPages());
            Intent intent = new Intent(getContext(), FilmListActivity.class);
            intent.putExtra("data", JSON.toJSONString(articleListRule1));
            intent.putExtra("title", intentTitle + adapter.getList().get(position).getTitle());
            getActivity().startActivityForResult(intent, FilmListActivity.REFRESH_PAGE_CODE);
            memoryPosition(position);
            return;
        } else if (webViewParser.canParse(url)) {
            boolean start = webViewParser.parse(getActivity(), url, adapter.getList().get(position).getExtra(), u -> {
                if (loadingPopupView != null && loadingPopupView.isShow()) {
                    loadingPopupView.dismiss();
                }
                dealWithUrl(view, u, position, codeAndHeader);
            });
            if (start) {
                if (loadingPopupView == null) {
                    loadingPopupView = new XPopup.Builder(getContext()).asLoading();
                }
                loadingPopupView.setTitle("动态解析规则中，请稍候");
                loadingPopupView.show();
            }
            return;
        } else if (lowUrl.startsWith("pics://")) {
            boolean fromLastPage = isClickLastPageBtn(articleListRule.getUrl(), activityTitle, position);
            memoryPosition(position);
            //图片漫画类型
            String realRule = StringUtils.replaceOnceIgnoreCase(url, "pics://", "");
            String[] urls = realRule.split("&&");
            ArrayList<String> imageUrls = new ArrayList<>(Arrays.asList(urls));
            Intent intent = new Intent(getContext(), PictureListActivity.class);
            intent.putStringArrayListExtra("pics", imageUrls);
            intent.putExtra("rule", JSON.toJSONString(articleListRule));
            intent.putExtra("url", articleListRule.getUrl());
            intent.putExtra("title", DetailUIHelper.getTitleText(adapter.getList().get(position).getTitle()));
            List<VideoChapter> chapters1 = getChapters(url, position, intentTitle, codeAndHeader);
            long current = PlayerChooser.putChapters(chapters1);
            intent.putExtra("chapters", current);
            if (getActivity() instanceof FilmListActivity) {
                intent.putExtra("CUrl", articleListRule.getUrl());
                intent.putExtra("MTitle", activityTitle);
            }
            intent.putExtra("fromLastPage", fromLastPage);
            int nowPage = 0;
            for (int i = 0; i < chapters1.size(); i++) {
                if (chapters1.get(i).isUse()) {
                    nowPage = i;
                }
            }
            intent.putExtra("nowPage", nowPage);
            startActivity(intent);
            return;
        }

        String ext = HttpRequestUtil.getFileExtensionFromUrl(url);
        if (UrlDetector.isImage(url)) {
            //检测是否是图片
            ImageView imageView = null;
            if (view instanceof ImageView) {
                imageView = (ImageView) view;
            } else if (view != null) {
                imageView = view.findViewById(R.id.item_reult_img);
            }
            XPopupImageLoader imageLoader;
            if (imageView == null) {
                imageLoader = new PopImageLoaderNoView(articleListRule.getUrl());
            } else {
                imageLoader = new PopImageLoader(imageView, articleListRule.getUrl());
            }
            if (url.equals(adapter.getList().get(position).getUrl())) {
                if (imageUrlPosMap == null) {
                    imageUrlPosMap = new HashMap<>();
                } else {
                    imageUrlPosMap.clear();
                }
                List<Object> imageUrls = new ArrayList<>();
                int selectPos = 0;
                for (int i = 0; i < adapter.getList().size(); i++) {
                    ArticleList articleList = adapter.getList().get(i);
                    if ("header".equals(articleList.getType())) {
                        continue;
                    }
                    if (UrlDetector.isImage(articleList.getUrl())) {
                        imageUrls.add(articleList.getUrl());
                        imageUrlPosMap.put(imageUrls.size() - 1, i);
                        if (url.equals(articleList.getUrl())) {
                            selectPos = imageUrls.size() - 1;
                        }
                    }
                }
                new XPopup.Builder(getContext())
                        .asImageViewer(imageView, selectPos, imageUrls, false,
                                true, getResources().getColor(R.color.gray_rice), -1, -1, true, Color.rgb(32, 36, 46), new OnSrcViewUpdateListener() {
                                    @Override
                                    public void onSrcViewUpdate(@NonNull ImageViewerPopupView popupView, int position) {
                                        // 作用是当Pager切换了图片，需要更新源View
                                        try {
                                            popupView.updateSrcView(null);
                                            Integer itemPos = imageUrlPosMap.get(position);
                                            if (itemPos != null && itemPos < adapter.getList().size() && itemPos >= 0) {
                                                recyclerView.scrollToPosition(itemPos);
//                                        View itemView = gridLayoutManager.findViewByPosition(itemPos);
//                                        if (itemView != null) {
//                                            View imgView = itemView.findViewById(R.id.item_reult_img);
//                                            if (imgView instanceof ImageView) {
//                                                popupView.updateSrcView((ImageView) imgView);
//                                                return;
//                                            }
//                                        }
                                            }
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, imageLoader)
                        .show();
            } else {
                new XPopup.Builder(getContext())
                        .asImageViewer(imageView, url, imageLoader)
                        .show();
            }
        } else if (UrlDetector.isVideoOrMusic(url)) {
            //音乐或者视频
            memoryPosition(position);
            if (url.equals(adapter.getList().get(position).getUrl()) || StringUtil.isNotEmpty(codeAndHeader)
                    || webViewParser.canParse(adapter.getList().get(position).getUrl())) {
                List<VideoChapter> chapters = getChapters(url, position, intentTitle, codeAndHeader);
                Bundle extraDataBundle = new Bundle();
                String viewCollectionExtraData = getViewCollectionExtraData();
                if (!StringUtil.isEmpty(viewCollectionExtraData)) {
                    extraDataBundle.putString("viewCollectionExtraData", viewCollectionExtraData);
                }
                if (getActivity() != null && getActivity().getTitle() != null) {
                    extraDataBundle.putString("film", getActivity().getTitle().toString());
                }
                if (getActivity() instanceof FilmListActivity) {
                    PlayerChooser.startPlayer(getActivity(), chapters, articleListRule.getUrl(), activityTitle, extraDataBundle);
                } else {
                    PlayerChooser.startPlayer(getActivity(), chapters, null, null, extraDataBundle);
                }
                return;
            }
            PlayerChooser.startPlayer(getActivity(), itemTitle, url);
        } else if (lowUrl.startsWith("http") && "apk".equalsIgnoreCase(ext)) {
            DownloadDialogUtil.showEditApkDialog(getActivity(), url, url);
        } else if (lowUrl.startsWith("http") || lowUrl.startsWith("file://")) {
            //链接
            memoryPosition(position);
            Bundle extraDataBundle = new Bundle();
            String viewCollectionExtraData = getViewCollectionExtraData();
            if (!StringUtil.isEmpty(viewCollectionExtraData)) {
                extraDataBundle.putString("viewCollectionExtraData", viewCollectionExtraData);
            }
            if (getActivity() != null && getActivity().getTitle() != null) {
                extraDataBundle.putString("film", getActivity().getTitle().toString());
            }
            WebUtil.goWebWithExtraData(getContext(), url, extraDataBundle);
        } else if (lowUrl.startsWith("magnet") || lowUrl.startsWith("thunder") || lowUrl.startsWith("ftp") || lowUrl.startsWith("ed2k")) {
            //常用第三方软件
            ShareUtil.findChooserToDeal(getContext(), url);
        } else {
            ToastMgr.shortBottomCenter(getContext(), "未知链接：" + url);
        }
    }

    private void addChapterPic(VideoChapter videoChapter, int pos) {
        if (StringUtil.isNotEmpty(adapter.getList().get(pos).getPic())
                && !"*".equals(adapter.getList().get(pos).getPic())) {
            videoChapter.setPicUrl(adapter.getList().get(pos).getPic());
        } else if (getActivity() != null) {
            String picUrl = getActivity().getIntent().getStringExtra("picUrl");
            videoChapter.setPicUrl(picUrl);
        }
    }

    private List<VideoChapter> getChapters(String url, int position, String title, String codeAndHeader) {
        List<VideoChapter> chapters = new ArrayList<>();
        for (int i = 0; i < position; i++) {
            if ("header".equals(adapter.getList().get(i).getType())) {
                continue;
            }
            VideoChapter videoChapter = new VideoChapter();
            videoChapter.setMemoryTitle(adapter.getList().get(i).getTitle());
            videoChapter.setTitle(title + DetailUIHelper.getTitleText(adapter.getList().get(i).getTitle()));
            videoChapter.setUrl(adapter.getList().get(i).getUrl());
            videoChapter.setExtra(adapter.getList().get(i).getExtra());
            addChapterPic(videoChapter, i);
            videoChapter.setUse(false);
            if (StringUtil.isNotEmpty(codeAndHeader)) {
                videoChapter.setCodeAndHeader(codeAndHeader);
                videoChapter.setOriginalUrl(adapter.getList().get(i).getUrl());
            }
            chapters.add(videoChapter);
        }
        VideoChapter videoChapter = new VideoChapter();
        videoChapter.setMemoryTitle(adapter.getList().get(position).getTitle());
        videoChapter.setTitle(title + DetailUIHelper.getTitleText(adapter.getList().get(position).getTitle()));
        videoChapter.setUrl(url);
        videoChapter.setUse(true);
        videoChapter.setExtra(adapter.getList().get(position).getExtra());
        addChapterPic(videoChapter, position);
        if (StringUtil.isNotEmpty(codeAndHeader)) {
            videoChapter.setCodeAndHeader(codeAndHeader);
            videoChapter.setOriginalUrl(adapter.getList().get(position).getUrl());
        }
        chapters.add(videoChapter);
        for (int i = position + 1; i < adapter.getList().size(); i++) {
            VideoChapter chapter = new VideoChapter();
            chapter.setTitle(title + DetailUIHelper.getTitleText(adapter.getList().get(i).getTitle()));
            chapter.setMemoryTitle(adapter.getList().get(i).getTitle());
            chapter.setUrl(adapter.getList().get(i).getUrl());
            chapter.setExtra(adapter.getList().get(i).getExtra());
            addChapterPic(chapter, i);
            chapter.setUse(false);
            if (StringUtil.isNotEmpty(codeAndHeader)) {
                chapter.setCodeAndHeader(codeAndHeader);
                chapter.setOriginalUrl(adapter.getList().get(i).getUrl());
            }
            chapters.add(chapter);
        }
        return chapters;
    }

    /**
     * 判断是否点击的是上次播放的页面
     *
     * @return
     */
    private boolean isClickLastPageBtn(String CUrl, String MTitle, int position) {
        List<ViewHistory> histories = null;
        try {
            histories = LitePal.where("url = ? and title = ? and type = ?", CUrl, MTitle, CollectionTypeConstant.DETAIL_LIST_VIEW).limit(1).find(ViewHistory.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(histories)) {
            String click = adapter.getList().get(position).getTitle();
            String lastClick = histories.get(0).getLastClick();
            if (StringUtil.isNotEmpty(lastClick) && lastClick.contains("@@")) {
                String[] clicks = lastClick.split("@@");
                if (clicks.length == 2) {
                    if (clicks[1].equals(String.valueOf(position)) || clicks[0].equals(click)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 记忆上次点击位置
     *
     * @param position
     */
    private void memoryPosition(int position) {
        if (!memoryPos) {
            return;
        }
        Activity activity = getActivity();
        if (activity instanceof FilmListActivity) {
            if (noHistory) {
                Timber.d("memoryPosition: noHistory");
                return;
            }
            String title = DetailUIHelper.getActivityTitle(getActivity());
            List<ViewCollection> collections = null;
            try {
                collections = LitePal.where("CUrl = ? and MTitle = ?", articleListRule.getUrl(), title).limit(1).find(ViewCollection.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String click = adapter.getList().get(position).getTitle();
            click = click + "@@" + position;
            if (!CollectionUtil.isEmpty(collections)) {
                if (StringUtil.isEmpty(click) || click.length() > 25) {
                    return;
                }
                collections.get(0).setLastClick(click);
                collections.get(0).save();
            }
            ((FilmListActivity) activity).setLastClick(new LastClickShowEvent(click));
            HeavyTaskUtil.updateHistoryLastClick(title, articleListRule.getUrl(), click);
        }
    }

    /**
     * 记忆上次点击位置
     */
    private void deletePosition() {
        Activity activity = getActivity();
        if (activity instanceof FilmListActivity) {
            String title = activity.getIntent().getStringExtra("title");
            List<ViewCollection> collections = null;
            try {
                collections = LitePal.where("CUrl = ? and MTitle = ?", articleListRule.getUrl(), title).limit(1).find(ViewCollection.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(collections)) {
                collections.get(0).setLastClick("");
                collections.get(0).save();
            }
            ((FilmListActivity) activity).setLastClick(new LastClickShowEvent(""));
            HeavyTaskUtil.updateHistoryLastClick(title, articleListRule.getUrl(), "");
        }
    }

    /**
     * 找到并更新WebViewHolder
     *
     * @param data
     */
    private void updateWebViewHolder(List<ArticleList> data) {
        if (CollectionUtil.isNotEmpty(data)) {
            for (ArticleList datum : data) {
                if (ArticleColTypeEnum.getByCode(datum.getType()) == ArticleColTypeEnum.X5_WEB_VIEW) {
                    if (webViewHolder == null) {
                        synchronized (this) {
                            if (webViewHolder == null) {
                                webViewHolder = new ArticleWebViewHolder();
                                webViewHolder.setLazyRuleConsumer(this::consumeLazyRuleForWebView);
                            }
                        }
                    }
                    webViewHolder.setUrl(datum.getUrl());
                    updateModeByDesc(datum.getDesc());
                    if (StringUtil.isNotEmpty(datum.getExtra())) {
                        X5Extra extra = JSON.parseObject(datum.getExtra(), X5Extra.class);
                        webViewHolder.setX5Extra(extra);
                    }
                    break;
                }
            }
        }
    }

    private void consumeLazyRuleForWebView(String url) {
        String[] lazyRule = url.split("@lazyRule=");
        String urlWithUa = HttpParser.getUrlAppendUA(articleListRule.getUrl(), articleListRule.getUa());
        String codeAndHeader = DetailUIHelper.getCodeAndHeader(urlWithUa, lazyRule);
        if (lazyRule.length > 1) {
            for (int i = 0; i < adapter.getList().size(); i++) {
                if (ArticleColTypeEnum.getByCode(adapter.getList().get(i).getType()) == ArticleColTypeEnum.X5_WEB_VIEW) {
                    dealLazyRule(webViewHolder.getWebView(), i, lazyRule, codeAndHeader);
                    break;
                }
            }
        }
    }

    private void updateModeByDesc(String desc) {
        webViewHolder.setExtra(desc);
        if (StringUtil.isNotEmpty(desc)) {
            String[] extra = desc.split("&&");
            for (String s : extra) {
                s = s.trim();
                if (s.equalsIgnoreCase(ArticleWebViewHolder.Mode.FLOAT.name())) {
                    webViewHolder.setMode(ArticleWebViewHolder.Mode.FLOAT);
                    break;
                } else if (s.equalsIgnoreCase(ArticleWebViewHolder.Mode.LIST.name())) {
                    webViewHolder.setMode(ArticleWebViewHolder.Mode.LIST);
                    break;
                }
            }
        }
    }

    /**
     * UI线程刷新WebView
     */
    private void updateWebView() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (webViewHolder != null && webViewHolder.getWebView() == null) {
            WebView webView = new WebView(getActivity());
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            webView.setLayoutParams(layoutParams);
            webViewHolder.setHeight(layoutParams.height);
            webViewHolder.setWebView(webView);
            webViewHolder.initWebView(getActivity());
            if (webViewHolder.getX5Extra() != null) {
                if (StringUtil.isNotEmpty(webViewHolder.getX5Extra().getUa())) {
                    webView.getSettings().setUserAgent(webViewHolder.getX5Extra().getUa());
                }
            }
            adapter.setWebViewHolder(webViewHolder);
        }
        if (webViewHolder != null) {
            if (webViewHolder.getMode() == ArticleWebViewHolder.Mode.FLOAT) {
                if (webViewContainer.getChildCount() <= 0) {
                    int height = ArticleWebViewHolder.getHeightByExtra(webViewHolder.getUrl(), webViewHolder.getExtra());
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webViewContainer.getLayoutParams();
                    layoutParams.height = DisplayUtil.dpToPx(getContext(), height);
                    webViewContainer.setLayoutParams(layoutParams);
                    webViewHolder.setHeight(layoutParams.height);
                    if (webViewHolder.getWebView().getParent() != null) {
                        ((RelativeLayout) webViewHolder.getWebView().getParent()).removeAllViews();
                    }
                    webViewContainer.addView(webViewHolder.getWebView());
                } else {
                    //更新高度
                    updateWebViewHeight();
                }
            } else if (webViewContainer.getChildCount() > 0) {
                webViewContainer.removeAllViews();
            }
        }
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            if (webViewHolder.getX5Extra() != null) {
                if (StringUtil.isNotEmpty(webViewHolder.getX5Extra().getUa())) {
                    webViewHolder.getWebView().getSettings().setUserAgent(webViewHolder.getX5Extra().getUa());
                }
            }
            if (StringUtil.isNotEmpty(webViewHolder.getUrl()) &&
                    !webViewHolder.getUrl().equals(webViewHolder.getWebView().getUrl())) {
                //链接更新了，load新链接
                webViewHolder.getWebView().loadUrl(webViewHolder.getUrl());
            } else if (StringUtil.isNotEmpty(webViewHolder.getUrl()) &&
                    webViewHolder.getUrl().equals(webViewHolder.getWebView().getUrl())) {
                //链接没有更新，reload一下
                webViewHolder.getWebView().reload();
            } else if (StringUtil.isEmpty(webViewHolder.getUrl()) &&
                    StringUtil.isNotEmpty(webViewHolder.getWebView().getUrl())) {
                //链接变空，但是原来有加载链接，那就静音
                String muteJs = JSManager.instance(getContext()).getJsByFileName("mute");
                if (!TextUtils.isEmpty(muteJs)) {
                    webViewHolder.getWebView().evaluateJavascript(muteJs, null);
                }
            }
        }
    }

    private void updateWebViewHeight() {
        if (webViewHolder.getMode() == ArticleWebViewHolder.Mode.FLOAT) {
            int height = ArticleWebViewHolder.getHeightByExtra(webViewHolder.getUrl(), webViewHolder.getExtra());
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webViewContainer.getLayoutParams();
            int px = DisplayUtil.dpToPx(getContext(), height);
            if (px == layoutParams.height) {
                return;
            }
            if (webViewHolder.getAnim() != null && webViewHolder.getAnim().isRunning()) {
                webViewHolder.getAnim().cancel();
            }
            //高度变为0，静音
            if (px == 0) {
                String muteJs = JSManager.instance(getContext()).getJsByFileName("mute");
                if (!TextUtils.isEmpty(muteJs)) {
                    webViewHolder.getWebView().evaluateJavascript(muteJs, null);
                }
            }
            int lastHeight = layoutParams.height;
            webViewHolder.setHeight(px);
            ValueAnimator anim = ValueAnimator.ofInt(lastHeight, px);
            anim.setDuration(300);
            anim.addUpdateListener(animation -> {
                int value = (Integer) animation.getAnimatedValue();
                layoutParams.height = value;
                webViewContainer.setLayoutParams(layoutParams);
            });
            anim.start();
            webViewHolder.setAnim(anim);
        }
    }

    @Override
    public void bindArrayToView(final String actionType, final List<ArticleList> data) {
        String dataStr = JSON.toJSONString(data);
        updateWebViewHolder(data);
        try {
            requireActivity().runOnUiThread(() -> {
                try {
                    Timber.d("bindArrayToView: ");
                    smartRefreshLayout.finishRefresh(true);
                    updateWebView();
                    if (HomeActionEnum.ARTICLE_LIST_NEW.equals(actionType)) {
                        Glide.get(getContext()).clearMemory();
                        firstPageData = dataStr;
                        int size = adapter.getList().size();
                        if (size == 0) {
                            data.addAll(0, initHeaders());
                        }
                        boolean hasHeader = false;
                        for (int i = size - 1; i >= 0; i--) {
                            if (!adapter.getList().get(i).getType().equals("header")) {
                                if (!hasHeader) {
                                    adapter.getList().remove(i);
                                }
                            } else {
                                hasHeader = true;
                            }
                        }
                        if (size != 0 && adapter.getList().size() == 0) {
                            //全部被上一步remove掉了，然后data又没有header，那么再次初始化一下header
                            data.addAll(0, initHeaders());
                        }
                        adapter.notifyDataChanged();
                        int start = adapter.getList().size() - 1;
                        //                    if (articleListRuleDTO.getUrl().contains("fypage")) {
                        //                        data.add(addFooter());
                        //                    }
                        adapter.getList().addAll(data);
                        adapter.notifyRangeChanged(start, data.size());
                    } else if (HomeActionEnum.ARTICLE_LIST.equals(actionType)) {
                        if (headerSize == -1) {
                            headerSize = initHeaders().size();
                        }
                        if (adapter.getList().size() == 0) {
                            data.addAll(0, initHeaders());
                        }
                        final int start = adapter.getList().size() - 1;
                        //                    if (articleListRuleDTO.getUrl().contains("fypage")) {
                        //                        data.add(addFooter());
                        //                    }
                        if (CollectionUtil.isNotEmpty(data)) {
                            if (!dataStr.equals(firstPageData) && !dataStr.equals(lastPageData)) {
                                adapter.getList().addAll(data);
                                adapter.notifyRangeChanged(start, data.size());
                            }
                        }
                        lastPageData = dataStr;
                    }
                    loading(false);
                    showLastClick();
                    if (loadListener != null) {
                        loadListener.complete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示上次播放位置
     */
    private void showLastClick() {
        if (hasShowLastClick) {
            return;
        }
        hasShowLastClick = true;
        Activity activity = getActivity();
        if (activity instanceof FilmListActivity) {
            String title = activity.getIntent().getStringExtra("title");
            List<ViewCollection> collections = null;
            try {
                collections = LitePal.where("CUrl = ? and MTitle = ?", articleListRule.getUrl(), title).limit(1).find(ViewCollection.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(collections)) {
                String click = collections.get(0).getLastClick();
                if (StringUtil.isNotEmpty(click)) {
                    requireActivity().runOnUiThread(() -> {
                        int pos = findPosByTitle(click);
                        if (pos > 0) {
                            recyclerView.scrollToPosition(pos);
                        }
                        ((FilmListActivity) getActivity()).showLastClick(new LastClickShowEvent(click));
                    });
                }
            } else {
                List<ViewHistory> histories = null;
                try {
                    histories = LitePal.where("url = ? and title = ? and type = ?", articleListRule.getUrl(), title, CollectionTypeConstant.DETAIL_LIST_VIEW).limit(1).find(ViewHistory.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!CollectionUtil.isEmpty(histories)) {
                    String click = histories.get(0).getLastClick();
                    if (StringUtil.isNotEmpty(click)) {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            requireActivity().runOnUiThread(() -> {
                                if (getActivity() != null && !getActivity().isFinishing()) {
                                    int pos = findPosByTitle(click);
                                    if (pos > 0) {
                                        recyclerView.scrollToPosition(pos);
                                    }
                                    ((FilmListActivity) getActivity()).showLastClick(new LastClickShowEvent(click));
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private String getViewCollectionExtraData() {
        return DetailUIHelper.getViewCollectionExtraData(getActivity(), articleListRule.getUrl());
    }

    public int findPosByTitle(String title) {
        if (StringUtil.isEmpty(title)) {
            return 0;
        }
//        Log.d(TAG, "clickByTitle: " + title);
        int minPosOffset = 100000;
        int pos = -1;
        boolean checkPos = title.contains("@@");
        String[] s = title.split("@@");
        int lastClickPos = 0;
        if (checkPos) {
            try {
                lastClickPos = Integer.parseInt(s[1]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
//        Log.d(TAG, "clickByTitle: " + lastClickPos);
        for (int i = 0; i < adapter.getList().size(); i++) {
            if (s[0].equals(adapter.getList().get(i).getTitle())) {
                if (!checkPos) {
                    return pos;
                } else {
                    //找到距离之前的记录最近的一个位置
                    int posOffset = Math.abs(lastClickPos - i);
//                    Log.d(TAG, "clickByTitle:posOffset: " + posOffset);
                    if (minPosOffset > posOffset) {
                        pos = i;
                        minPosOffset = posOffset;
//                        Log.d(TAG, "clickByTitle: " + posOffset + ", " + pos);
                    }
                }
            }
        }
        return pos;
    }

    public void clickByTitle(String title) {
        if (StringUtil.isEmpty(title)) {
            return;
        }
        int pos = -1;
        pos = findPosByTitle(title);
        if (pos != -1) {
            clickItem(null, pos);
        }
    }

    public void clickNextByTitle(String title) {
        if (StringUtil.isEmpty(title)) {
            return;
        }
        int pos = -1;
        pos = findPosByTitle(title);
        if (pos != -1) {
            clickItem(null, pos + 1);
        }
    }

    @Override
    public void bindObjectToView(String actionType, ArticleList data) {
        if (StringUtil.isEmpty(actionType) || data == null) {
            return;
        }
        switch (actionType) {
            case "onRefresh":
                onRefreshJS = data.getTitle();
                break;
            case "onClose":
                onCloseJS = data.getTitle();
                break;
            default:
                break;
        }
    }

    @Override
    public void error(String title, String msg, String code, Exception e) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                //隐藏弹窗
                if (getActivity() instanceof ArticleListIsland) {
                    ArticleListIsland island = (ArticleListIsland) getActivity();
                    island.hideLoading();
                }
                smartRefreshLayout.finishRefresh(true);
                loading(false);
                DebugUtil.showErrorMsg(getActivity(), getContext(), "规则执行过程中出现错误", msg, "home@" + articleListRuleDTO.getTitle(), e);
            });
        }
    }

    /**
     * page加减必须在该方法调用之前
     *
     * @param isLoading
     */
    @Override
    public void loading(boolean isLoading) {
        try {
            if (isLoading) {
                if (page == 1) {
                    progressView1.setVisibility(View.VISIBLE);
                    ((Animatable) progressView1.getDrawable()).start();
                } else {
                    progressView2.setVisibility(View.VISIBLE);
                    ((Animatable) progressView2.getDrawable()).start();
                }
            } else {
                if (page == 1) {
                    progressView1.setVisibility(View.INVISIBLE);
                    ((Animatable) progressView1.getDrawable()).stop();
                } else {
                    progressView2.setVisibility(View.GONE);
                    ((Animatable) progressView2.getDrawable()).stop();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (loadingDialog != null) {
//            if (isLoading && SplashUtil.canShowDialog(getContext())) {
//                loadingDialog.show();
//            } else {
//                loadingDialog.dismiss();
//            }
//        }
    }

    public boolean isNoRecordHistory() {
        return noRecordHistory;
    }

    public void setNoRecordHistory(boolean noRecordHistory) {
        this.noRecordHistory = noRecordHistory;
    }

    @Override
    public void onDestroy() {
        try {
            if (webViewHolder != null && webViewHolder.getWebView() != null) {
                webViewHolder.getWebView().onPause();
                webViewHolder.getWebView().destroy();
            }
            webViewParser.destroy();
            if (StringUtil.isNotEmpty(onCloseJS)) {
                HeavyTaskUtil.executeNewTask(() -> {
                    JSEngine.getInstance().evalJS(onCloseJS, "");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public LoadListener getLoadListener() {
        return loadListener;
    }

    public void setLoadListener(LoadListener loadListener) {
        this.loadListener = loadListener;
    }

    public interface LoadListener {
        void complete();
    }

    @Override
    public boolean onBackPressed() {
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            if (webViewHolder.getWebView().canGoBack()) {
                if (webViewHolder.getX5Extra() != null && webViewHolder.getX5Extra().isCanBack()) {
                    webViewHolder.getWebView().goBack();
                    return true;
                }
            }
        }
        return super.onBackPressed();
    }

    private boolean scrollPage(boolean down) {
        for (int i = 0; i < adapter.getList().size() && i < 100; i++) {
            ArticleList articleList = adapter.getList().get(i);
            if (ArticleColTypeEnum.RICH_TEXT.equals(ArticleColTypeEnum.getByCode(articleList.getType()))) {
                if (StringUtil.isNotEmpty(articleList.getExtra())) {
                    RichTextExtra extra = JSON.parseObject(articleList.getExtra(), RichTextExtra.class);
                    if (extra.isClick()) {
                        int height = smartRefreshLayout.getMeasuredHeight();
                        recyclerView.smoothScrollBy(0, down ? height : -height);
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return scrollPage(true);
            case KeyEvent.KEYCODE_VOLUME_UP:
                return scrollPage(false);
        }
        return super.onKeyDown(keyCode, event);
    }

    public List<ArticleList> getArticleList() {
        return adapter == null ? new ArrayList<>() : adapter.getList();
    }
}
