package com.org.lqtk.fastscroller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerFastScroller extends FrameLayout {

    private static final int DEFAULT_AUTO_HIDE_DELAY = 1500;

    private int minItemCount = 200;

    protected final View mBar;//滑道
    protected final View mHandle;//手指触碰滑块
    private int mHiddenTranslationX;//隐藏移动的X距离
    private final Runnable mHide;
    private int mMinScrollHandleHeight;//滑块的最小高度 默认56dp
    private int mMaxScrollHandleHeight;//滑块的最大高度 默认56dp
    protected OnTouchListener mOnTouchListener;
    private onHandlePressedListener pressedListener;

    int mAppBarLayoutOffset;

    RecyclerView mRecyclerView;

    AnimatorSet mAnimator;
    boolean mAnimatingIn;

    private int mHideDelay;//多少时间后隐藏 ms
    private boolean mHidingEnabled;//是否可隐藏
    private int mHandleNormalColor;//不设置图片的正常bar
    private int mHandlePressedColor;//不设置图片的按下bar
    private int mBarColor;//滑道颜色
    private int mTouchTargetWidth;//手指触碰的宽度 默认40dp
    private int mTouchTargetMaxWidth;//手指触碰的最大宽度 默认56dp
    private int mBarInset;//距离边缘的距离
    private Drawable normalDrawable;//正常图片
    private Drawable pressedDrawable;//按下图片
    private int margiLeft = 20;//距离左边的距离 默认20dp

    private boolean isDrawable;//滑块是否由图片显示
    private boolean mHideOverride;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            updateHandleColorsAndInset();
            requestLayout();
        }
    };

    public RecyclerFastScroller(Context context) {
        this(context, null, 0);
    }

    public RecyclerFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecyclerFastScroller(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerFastScroller, defStyleAttr, defStyleRes);

        mBarColor = a.getColor(
                R.styleable.RecyclerFastScroller_rfs_barColor,
                RecyclerFastScrollerUtils.resolveColor(context, R.attr.colorControlNormal));

        mHandleNormalColor = a.getColor(
                R.styleable.RecyclerFastScroller_rfs_handleNormalColor,
                RecyclerFastScrollerUtils.resolveColor(context, R.attr.colorControlNormal));

        mHandlePressedColor = a.getColor(
                R.styleable.RecyclerFastScroller_rfs_handlePressedColor,
                RecyclerFastScrollerUtils.resolveColor(context, R.attr.colorAccent));

        normalDrawable = context.getResources().getDrawable(R.drawable.change_scroll_press);

        pressedDrawable = context.getResources().getDrawable(R.drawable.change_scroll_unpress);

        mTouchTargetWidth = a.getDimensionPixelSize(
                R.styleable.RecyclerFastScroller_rfs_touchTargetWidth,
                RecyclerFastScrollerUtils.convertDpToPx(context, 40));

        mHideDelay = a.getInt(R.styleable.RecyclerFastScroller_rfs_hideDelay,
                DEFAULT_AUTO_HIDE_DELAY);

        mHidingEnabled = a.getBoolean(R.styleable.RecyclerFastScroller_rfs_hidingEnabled, true);

        a.recycle();

        int fiftySix = RecyclerFastScrollerUtils.convertDpToPx(context, 56);
        setLayoutParams(new ViewGroup.LayoutParams(fiftySix, ViewGroup.LayoutParams.MATCH_PARENT));

        mBar = new View(context);
        mHandle = new View(context);
        addView(mBar);
        addView(mHandle);

        mMinScrollHandleHeight = fiftySix;
        mMaxScrollHandleHeight = fiftySix;
        mTouchTargetMaxWidth = fiftySix;
        margiLeft = RecyclerFastScrollerUtils.convertDpToPx(getContext(), margiLeft);

        refreshView(mTouchTargetWidth);

        int twenty = mTouchTargetWidth;
        mHiddenTranslationX = (RecyclerFastScrollerUtils.isRTL(getContext()) ? 1 : -1) * twenty;

        mHide = new Runnable() {
            @Override
            public void run() {
                if (!mHandle.isPressed()) {
                    if (mAnimator != null && mAnimator.isStarted()) {
                        mAnimator.cancel();
                    }
                    mAnimator = new AnimatorSet();
                    ObjectAnimator animator2 = ObjectAnimator.ofFloat(RecyclerFastScroller.this, View.TRANSLATION_X,
                            mHiddenTranslationX);
                    animator2.setInterpolator(new FastOutLinearInInterpolator());
                    animator2.setDuration(150);
                    mHandle.setEnabled(false);
                    mAnimator.play(animator2);
                    mAnimator.start();
                }
            }
        };

        mHandle.setOnTouchListener(new OnTouchListener() {
            private float mInitialBarHeight;
            private float mLastPressedYAdjustedToInitial;
            private int mLastAppBarLayoutOffset;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mOnTouchListener != null) {
                    mOnTouchListener.onTouch(v, event);
                }
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mHandle.setPressed(true);
                    if (getPressedListener() != null) {
                        getPressedListener().handle(true);
                    }
                    mRecyclerView.stopScroll();

                    int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;

                    mRecyclerView.startNestedScroll(nestedScrollAxis);

                    mInitialBarHeight = mBar.getHeight();
                    mLastPressedYAdjustedToInitial = event.getY() + mHandle.getY() + mBar.getY();
                    mLastAppBarLayoutOffset = mAppBarLayoutOffset;
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float newHandlePressedY = event.getY() + mHandle.getY() + mBar.getY();
                    int barHeight = mBar.getHeight();
                    float newHandlePressedYAdjustedToInitial =
                            newHandlePressedY + (mInitialBarHeight - barHeight);

                    float deltaPressedYFromLastAdjustedToInitial =
                            newHandlePressedYAdjustedToInitial - mLastPressedYAdjustedToInitial;

                    int dY = (int) ((deltaPressedYFromLastAdjustedToInitial / mInitialBarHeight) *
                            mRecyclerView.computeVerticalScrollRange());

                    updateRvScroll(dY + mLastAppBarLayoutOffset - mAppBarLayoutOffset);

                    mLastPressedYAdjustedToInitial = newHandlePressedYAdjustedToInitial;
                    mLastAppBarLayoutOffset = mAppBarLayoutOffset;
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mLastPressedYAdjustedToInitial = -1;

                    mRecyclerView.stopNestedScroll();

                    mHandle.setPressed(false);
                    if (getPressedListener() != null) {
                        getPressedListener().handle(false);
                    }
                    postAutoHide();
                }

                return true;
            }
        });

        setTranslationX(mHiddenTranslationX);
    }

    @ColorInt
    public int getHandlePressedColor() {
        return mHandlePressedColor;
    }

    /**
     * 设置滑块普通背景
     *
     * @param colorPressed
     */
    public void setHandlePressedColor(@ColorInt int colorPressed) {
        mHandlePressedColor = colorPressed;
        updateHandleColorsAndInset();
    }

    @ColorInt
    public int getHandleNormalColor() {
        return mHandleNormalColor;
    }

    /**
     * 设置滑块按下背景
     *
     * @param colorNormal
     */
    public void setHandleNormalColor(@ColorInt int colorNormal) {
        mHandleNormalColor = colorNormal;
        updateHandleColorsAndInset();
    }

    @ColorInt
    public int getBarColor() {
        return mBarColor;
    }

    /**
     * 设置滑块是否显示为图片
     *
     * @param b
     */
    public void touchIsDrawable(boolean b) {
        isDrawable = b;
        updateHandleColorsAndInset();
    }

    public boolean isTouchDrawable() {
        return isDrawable;
    }

    /**
     * 设置滑块图片
     *
     * @param normal  普通图片
     * @param pressed 按下图片
     */
    public void setDrawable(Drawable normal, Drawable pressed) {
        normalDrawable = normal;
        pressedDrawable = pressed;
        isDrawable = true;
        updateHandleColorsAndInset();
    }

    /**
     * 设置滑道的颜色
     *
     * @param scrollBarColor 颜色值
     */
    public void setBarColor(@ColorInt int scrollBarColor) {
        mBarColor = scrollBarColor;
        updateBarColorAndInset();
    }

    /**
     * 获取延时消失时间
     *
     * @return
     */
    public int getHideDelay() {
        return mHideDelay;
    }

    /**
     * 设置多少时间后隐藏滑道
     *
     * @param hideDelay 延时时间
     */
    public void setHideDelay(int hideDelay) {
        mHideDelay = hideDelay;
    }

    /**
     * 获取滑块宽
     *
     * @return
     */
    public int getTouchTargetWidth() {
        return mTouchTargetWidth;
    }

    /**
     * 设置左边距离 px
     *
     * @param toLeft 单位dp
     */
    public void setMarginLeft(int toLeft) {
        int margin = RecyclerFastScrollerUtils.convertDpToPx(getContext(), toLeft);
        mTouchTargetWidth = mTouchTargetWidth - margiLeft + margin;
        margiLeft = margin;
        refreshView(mTouchTargetWidth);
    }

    /**
     * 设置滑块最大宽度 px
     *
     * @param maxWidth 单位dp
     */
    public void setmTouchTargetMaxWidth(int maxWidth) {
        if (getContext() != null) {
            mTouchTargetMaxWidth = RecyclerFastScrollerUtils.convertDpToPx(getContext(), maxWidth);
        } else {
            mTouchTargetMaxWidth = maxWidth;
        }
        refreshView(mTouchTargetWidth);
    }

    /**
     * 单位px
     *
     * @param touchTargetWidth 单位dp
     */
    public void setTouchTargetWidth(int touchTargetWidth) {
        mTouchTargetWidth = RecyclerFastScrollerUtils.convertDpToPx(getContext(), touchTargetWidth) + margiLeft;
        refreshView(mTouchTargetWidth);
    }

    /**
     * 刷新
     *
     * @param touchTargetWidth 滑块宽度 单位px
     */
    private void refreshView(int touchTargetWidth) {

        if (mTouchTargetWidth > mTouchTargetMaxWidth) {
            mTouchTargetWidth = mTouchTargetMaxWidth;
        }

        if (mTouchTargetMaxWidth < margiLeft) {
            mBarInset = 0;
        } else {
            mBarInset = margiLeft;
        }

        setTranslationX();

        mBar.setLayoutParams(new LayoutParams(touchTargetWidth, ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.END));
        mHandle.setLayoutParams(new LayoutParams(touchTargetWidth, ViewGroup.LayoutParams.WRAP_CONTENT, GravityCompat.END));

        updateHandleColorsAndInset();
        updateBarColorAndInset();
    }

    /**
     * 获取滑块是否隐藏
     *
     * @return
     */
    public boolean isHidingEnabled() {
        return mHidingEnabled;
    }

    /**
     * 设置x隐藏距离
     */
    private void setTranslationX() {
        int twenty = mBarInset + mTouchTargetWidth;
        mHiddenTranslationX = (RecyclerFastScrollerUtils.isRTL(getContext()) ? 1 : -1) * twenty;
    }

    /**
     * 设置是否隐藏
     *
     * @param hidingEnabled
     */
    public void setHidingEnabled(boolean hidingEnabled) {
        mHidingEnabled = hidingEnabled;
        if (hidingEnabled) {
            postAutoHide();
        }
    }

    /**
     * 更新滑块位置信息
     */
    private void updateHandleColorsAndInset() {
        StateListDrawable drawable = new StateListDrawable();

        if (!RecyclerFastScrollerUtils.isRTL(getContext())) {
            if (isTouchDrawable()) {
                drawable.addState(View.PRESSED_ENABLED_STATE_SET,
                        new InsetDrawable(pressedDrawable, mBarInset, 0, 0, 0));
                drawable.addState(View.EMPTY_STATE_SET,
                        new InsetDrawable(normalDrawable, mBarInset, 0, 0, 0));
            } else {
                drawable.addState(View.PRESSED_ENABLED_STATE_SET,
                        new InsetDrawable(new ColorDrawable(mHandlePressedColor), mBarInset, 0, 0, 0));
                drawable.addState(View.EMPTY_STATE_SET,
                        new InsetDrawable(new ColorDrawable(mHandleNormalColor), mBarInset, 0, 0, 0));
            }
        } else {
            if (isTouchDrawable()) {
                drawable.addState(View.PRESSED_ENABLED_STATE_SET,
                        new InsetDrawable(pressedDrawable, 0, 0, mBarInset, 0));
                drawable.addState(View.EMPTY_STATE_SET,
                        new InsetDrawable(normalDrawable, 0, 0, mBarInset, 0));
            } else {
                drawable.addState(View.PRESSED_ENABLED_STATE_SET,
                        new InsetDrawable(new ColorDrawable(mHandlePressedColor), 0, 0, mBarInset, 0));
                drawable.addState(View.EMPTY_STATE_SET,
                        new InsetDrawable(new ColorDrawable(mHandleNormalColor), 0, 0, mBarInset, 0));
            }
        }

        RecyclerFastScrollerUtils.setViewBackground(mHandle, drawable);
    }

    /**
     * 更新滑道信息
     */
    private void updateBarColorAndInset() {
        Drawable drawable;

        if (!RecyclerFastScrollerUtils.isRTL(getContext())) {
            drawable = new InsetDrawable(new ColorDrawable(mBarColor), mBarInset, 0, 0, 0);
        } else {
            drawable = new InsetDrawable(new ColorDrawable(mBarColor), 0, 0, mBarInset, 0);
        }
        drawable.setAlpha(57);
        RecyclerFastScrollerUtils.setViewBackground(mBar, drawable);
    }

    /**
     * 绑定recycle人View
     * 如果mRecyclerView已经绑定adapter则无需调用attachAdapter，反之则需要调用attachAdapter
     *
     * @param recyclerView
     */
    public void attachRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerFastScroller.this.show(true);
            }
        });
        if (recyclerView.getAdapter() != null) attachAdapter(recyclerView.getAdapter());
    }

    /**
     * 绑定adapter
     * 如果mRecyclerView已经绑定adapter则无需调用attachAdapter，反之则需要调用attachAdapter
     *
     * @param adapter
     */
    public void attachAdapter(@Nullable RecyclerView.Adapter adapter) {
        if (mAdapter == adapter) return;
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mAdapterObserver);
        }
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mAdapterObserver);
        }
        mAdapter = adapter;
        updateHandleColorsAndInset();
    }

    /**
     * 设置触摸事件
     *
     * @param listener
     */
    public void setOnHandleTouchListener(OnTouchListener listener) {
        mOnTouchListener = listener;
    }

    /**
     * 显示和隐藏fastscroller
     *
     * @param animate 是否显示隐藏显示动画
     */
    public void show(final boolean animate) {
        requestLayout();

        post(new Runnable() {
            @Override
            public void run() {
                if (mHideOverride) {
                    return;
                }

                if (mAdapter != null && mAdapter.getItemCount() < minItemCount) {
                    return;
                }

                mHandle.setEnabled(true);
                if (animate) {
                    if (!mAnimatingIn && getTranslationX() != 0) {
                        if (mAnimator != null && mAnimator.isStarted()) {
                            mAnimator.cancel();
                        }
                        mAnimator = new AnimatorSet();
                        ObjectAnimator animator = ObjectAnimator.ofFloat(RecyclerFastScroller.this, View.TRANSLATION_X, 0);
                        animator.setInterpolator(new LinearOutSlowInInterpolator());
                        animator.setDuration(100);
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mAnimatingIn = false;
                            }
                        });
                        mAnimatingIn = true;
                        mAnimator.play(animator);
                        mAnimator.start();
                    }
                } else {
                    setTranslationX(0);
                }
                postAutoHide();
            }
        });
    }

    /**
     * 自动隐藏
     */
    void postAutoHide() {
        if (mRecyclerView != null && mHidingEnabled) {
            mRecyclerView.removeCallbacks(mHide);
            mRecyclerView.postDelayed(mHide, mHideDelay);
        }
    }

    /**
     * 重绘页面
     *
     * @param changed
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mRecyclerView == null) return;

        int scrollOffset = mRecyclerView.computeVerticalScrollOffset() + mAppBarLayoutOffset;
        int verticalScrollRange = mRecyclerView.computeVerticalScrollRange()
                + mRecyclerView.getPaddingBottom();

        int barHeight = mBar.getHeight();
        float ratio = (float) scrollOffset / (verticalScrollRange - barHeight);

        int calculatedHandleHeight = (int) ((float) barHeight / verticalScrollRange * barHeight);
        if (calculatedHandleHeight < mMinScrollHandleHeight) {
            calculatedHandleHeight = mMinScrollHandleHeight;
        }

        if (calculatedHandleHeight > mMaxScrollHandleHeight) {
            calculatedHandleHeight = mMaxScrollHandleHeight;
        }

        if (calculatedHandleHeight >= barHeight) {
            setTranslationX(mHiddenTranslationX);
            mHideOverride = true;
            return;
        }

        mHideOverride = false;

        float y = ratio * (barHeight - calculatedHandleHeight);

        Log.d("eventprint", mHandle.getBottom() + ",2");
        mHandle.layout(mHandle.getLeft(), (int) y, mHandle.getRight(), (int) y + calculatedHandleHeight);
    }


    /**
     * 更新recyclerView位置
     *
     * @param dY
     */
    void updateRvScroll(int dY) {
        if (mRecyclerView != null && mHandle != null) {
            try {
                mRecyclerView.scrollBy(0, dY);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public onHandlePressedListener getPressedListener() {
        return pressedListener;
    }

    public void setPressedListener(onHandlePressedListener pressedListener) {
        this.pressedListener = pressedListener;
    }

    public interface onHandlePressedListener {
        void handle(boolean pressed);
    }

    public void setMaxScrollHandleHeight(int mMaxScrollHandleHeight) {
        this.mMaxScrollHandleHeight = mMaxScrollHandleHeight;
    }


    public void setMinItemCount(int minItemCount) {
        this.minItemCount = minItemCount;
    }
}