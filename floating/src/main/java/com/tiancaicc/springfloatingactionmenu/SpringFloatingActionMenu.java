package com.tiancaicc.springfloatingactionmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.IntDef;
import androidx.core.content.ContextCompat;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.ui.Util;
import com.tumblr.backboard.performer.MapPerformer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;


/**
 * Copyright (C) 2016 tiancaiCC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class SpringFloatingActionMenu extends FrameLayout {

    private static final String TAG = "SpringFloatingMenu";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ANIMATION_TYPE_BLOOM})
    public @interface ANIMATION_TYPE {
    }

    public static final int ANIMATION_TYPE_BLOOM = 0;

    private Context mContext;

    private View mRevealCircle;

    private ArrayList<MenuItem> mMenuItems;

    private ArrayList<MenuItemView> mMenuItemViews;

    private ViewGroup mContainerView;

    private ArrayList<OnMenuActionListener> mActionListeners;

    private OnFabClickListener mOnFabClickListener;

    @ColorRes
    private int mRevealColor;

    private int mMenuItemCount;

    private int mGravity;

    private int mMarginSize = 16;

    private View mFAB;

    private boolean mMenuOpen = false;

    private int mRevealDuration = 300;
    private ViewGroup rootView;

    // fix animation not stable caused by FAB click too fast
    private boolean mAnimating = false;

    public SpringFloatingActionMenu(Builder builder, ViewGroup rootView) {
        super(builder.context);
        this.mContext = builder.context;
        this.mMenuItems = builder.menuItems;
        this.mMenuItemCount = builder.menuItems.size();
        this.mGravity = builder.gravity;
        this.mFAB = builder.fab;
        this.mActionListeners = builder.actionListeners;
        this.mRevealColor = builder.revealColor;
        this.mOnFabClickListener = builder.onFabClickListener;
        this.rootView = rootView;
        init(mContext);
    }

    public SpringFloatingActionMenu(Context context) {
        this(context, null, 0);
    }

    public SpringFloatingActionMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpringFloatingActionMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {

        this.mContext = context;
        // this mContainerView will be added when animation happened,
        // see DestroySelfSpringListener.onSpringActivate()
        mContainerView = new FrameLayout(context);
        //add reveal circle, it will at bottom position
        mContainerView.addView(mRevealCircle = generateRevealCircle());

        //generate and add menuItemViews
        mMenuItemViews = generateMenuItemViews();
        for (MenuItemView menuItemView : mMenuItemViews) {
            mContainerView.addView(menuItemView);
            addOnMenuActionListener(menuItemView);
        }
        mMenuItemViews.get(0).bringToFront();
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //add self to root view
        rootView.addView(this);
        bringToFront();
        mContainerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
            }
        });
    }

    public void clickFab() {
        if (mAnimating) {
            return;
        }
        if (mOnFabClickListener != null) {
            mOnFabClickListener.onClcik();
        }
        if (mMenuOpen) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    public void closeMenu() {
        if (mAnimating) {
            return;
        }
        hideMenu();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Resources resources = getResources();
        Activity context = (Activity) mContext;

        // layout FAB and follow circles
        int fabWidth = mFAB.getMeasuredWidth();
        int fabHeight = mFAB.getMeasuredHeight();
        int bottomInsets = Utils.getInsetsBottom(context, this);

        int fabX = 0;
        int fabY = 0;
        if (mGravity == (Gravity.BOTTOM | Gravity.END)) {
            int marginX = Utils.dpToPx(mMarginSize, resources);
            int marginY = Utils.dpToPx(mMarginSize * 4, resources) + bottomInsets;
            fabX = right - fabWidth - marginX;
            fabY = bottom - fabHeight - marginY;
        } else if (mGravity == (Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM)) {
            int marginY = Utils.dpToPx(mMarginSize, resources) + bottomInsets;
            fabX = right / 2 - fabWidth / 2;
            fabY = bottom - fabHeight - marginY;
        } else {
            throw new IllegalStateException("gravity only support bottom center and bottom right");
        }

        int fabCenterX = fabX + fabWidth / 2;
        int fabCenterY = fabY + fabHeight / 2;

        int x = fabCenterX - mRevealCircle.getWidth() / 2;
        int y = fabCenterY - mRevealCircle.getHeight() / 2;
        mRevealCircle.layout(x, y, x + mRevealCircle.getMeasuredWidth(), y + mRevealCircle.getMeasuredHeight());

        //layout menu items
        layoutMenuItems(left, top, right, bottom);
    }

    private void layoutMenuItems(int left, int top, int right, int bottom) {
        layoutMenuItemsAsGrid(left, top, right, bottom);
    }

    private void layoutMenuItemsAsGrid(int left, int top, int right, int bottom) {
        Resources resources = getResources();

        int edgeGap = Util.dpToPx(24, resources);

        int colCount = 3;
        int rowCount = mMenuItemCount % colCount == 0 ? mMenuItemCount / colCount : mMenuItemCount / colCount + 1;
        Log.d(TAG, "row count:" + rowCount);

        int itemHeight = mMenuItemViews.get(0).getMeasuredHeight();
        int itemWidth = mMenuItemViews.get(0).getMeasuredWidth();

        int containerWidth = getMeasuredWidth();
        int containerHeight = getMeasuredHeight();

        int itemGap = (containerWidth - 2 * edgeGap - colCount * itemWidth) / (colCount - 1);
        //top and bottom gap
        int tbGap = (containerHeight - rowCount * itemHeight - itemGap * (rowCount - 1)) / 2;

        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            for (int colIdx = 0; colIdx < colCount; colIdx++) {
                int idxInItem = rowIdx * colCount + colIdx;
                if (idxInItem < mMenuItemCount) {
                    View item = mMenuItemViews.get(rowIdx * colCount + colIdx);
                    Log.d(TAG, "menu item index:" + (rowIdx * colCount + colIdx));
                    int itemLeft = edgeGap + colIdx * itemWidth + colIdx * itemGap;
                    int itemTop = tbGap + rowIdx * itemGap + rowIdx * itemHeight;
                    item.layout(itemLeft, itemTop, itemLeft + itemWidth, itemTop + itemHeight);
                } else {
                    break;
                }
            }
        }
    }

    private ArrayList<MenuItemView> generateMenuItemViews() {
        ArrayList<MenuItemView> menuItemViews = new ArrayList<>(mMenuItems.size());
        for (MenuItem item : mMenuItems) {
            MenuItemView menuItemView = new MenuItemView(mContext, item);
            menuItemView.setLayoutParams(Utils.createWrapParams());
            menuItemViews.add(menuItemView);
        }
        return menuItemViews;
    }

    private View generateRevealCircle() {
        int diameter = Utils.getDimension(mContext, R.dimen.fab_size_normal);
        View view = new View(mContext);
        OvalShape ovalShape = new OvalShape();
        ShapeDrawable shapeDrawable = new ShapeDrawable(ovalShape);
        shapeDrawable.getPaint().setColor(ContextCompat.getColor(mContext, mRevealColor));
        view.setBackgroundDrawable(shapeDrawable);
        LayoutParams lp = new LayoutParams(diameter, diameter);
        view.setLayoutParams(lp);

        // Make view clickable to avoid clicks on any view located behind the menu
        view.setClickable(true);

        // note it is invisible, but will be visible while  animating
        view.setVisibility(View.INVISIBLE);
        return view;
    }

    private void addOnMenuActionListener(OnMenuActionListener listener) {
        this.mActionListeners.add(listener);
    }

    public void showMenu() {
        Log.d(TAG, "showMenu");
        applyBloomOpenAnimation();
        revealIn();
        for (OnMenuActionListener listener : mActionListeners) {
            listener.onMenuOpen();
        }
        mMenuOpen = !mMenuOpen;
    }

    public void hideMenu() {
        Log.d(TAG, "hideMenu");
        applyBloomCloseAnimation();
        revealOut();
        for (OnMenuActionListener listener : mActionListeners) {
            listener.onMenuClose();
        }
        mMenuOpen = !mMenuOpen;
    }

    public boolean isMenuOpen() {
        return mMenuOpen;
    }

    private void revealIn() {
        mRevealCircle.setVisibility(View.VISIBLE);
        mRevealCircle.animate()
                .scaleX(100)
                .scaleY(100)
                .setDuration(mRevealDuration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mAnimating = false;
                        animation.removeAllListeners();
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        mAnimating = true;
                    }
                })
                .start();
    }

    private void revealOut() {
        mRevealCircle.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(mRevealDuration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mRevealCircle.setVisibility(View.INVISIBLE);
                        animation.removeAllListeners();
                        mAnimating = false;
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        mAnimating = true;
                    }
                })
                .start();
    }

    private void applyBloomOpenAnimation() {
        final SpringSystem springSystem = SpringSystem.create();

        for (int i = 0; i < mMenuItemCount; i++) {
            // create the springs that control movement
            final Spring springX = springSystem.createSpring();
            final Spring springY = springSystem.createSpring();

            MenuItemView menuItemView = mMenuItemViews.get(i);
            springX.addListener(new MapPerformer(menuItemView, View.X, mFAB.getLeft(), menuItemView.getLeft()));
            springY.addListener(new MapPerformer(menuItemView, View.Y, mFAB.getTop(), menuItemView.getTop()));
            DestroySelfSpringListener destroySelfSpringListener = new DestroySelfSpringListener(this, mContainerView, true);
            springX.addListener(destroySelfSpringListener);
            springY.addListener(destroySelfSpringListener);
            springX.setEndValue(1);
            springY.setEndValue(1);
        }
    }

    private void applyBloomCloseAnimation() {
        final SpringSystem springSystem = SpringSystem.create();

        for (int i = 0; i < mMenuItemCount; i++) {
            // create the springs that control movement
            final Spring springX = springSystem.createSpring();
            final Spring springY = springSystem.createSpring();

            MenuItemView menuItemView = mMenuItemViews.get(i);
            springX.addListener(new MapPerformer(menuItemView, View.X, menuItemView.getLeft(), mFAB.getLeft()));
            springY.addListener(new MapPerformer(menuItemView, View.Y, menuItemView.getTop(), mFAB.getTop()));
            DestroySelfSpringListener destroySelfSpringListener = new DestroySelfSpringListener(this, mContainerView, false);
            springX.addListener(destroySelfSpringListener);
            springY.addListener(destroySelfSpringListener);
            springX.setEndValue(1);
            springY.setEndValue(1);
        }
    }

    public static class Builder {

        private Context context;

        private ArrayList<MenuItem> menuItems = new ArrayList<>();

        private View fab;

        private int gravity = Gravity.BOTTOM | Gravity.END;

        @ColorRes
        private int revealColor = android.R.color.holo_purple;

        private OnFabClickListener onFabClickListener;

        private ArrayList<OnMenuActionListener> actionListeners = new ArrayList<>();

        public Builder(Context context) {
            this.context = context;
        }

        public SpringFloatingActionMenu build(ViewGroup rootView) {
            return new SpringFloatingActionMenu(this,  rootView);
        }

        public Builder addMenuItem(@ColorRes int bgColor, int icon, String label,
                                   @ColorRes int textColor, View.OnClickListener onClickListener) {
            menuItems.add(new MenuItem(bgColor, icon, label, textColor, onClickListener));
            return this;
        }

        public Builder addMenuItem(@ColorRes int bgColor, int icon, String label,
                                   @ColorRes int textColor, int diameter, View.OnClickListener onClickListener) {
            menuItems.add(new MenuItem(bgColor, icon, label, textColor, diameter, onClickListener));
            return this;
        }

        public Builder fab(View fab) {
            this.fab = fab;
            return this;
        }

        public Builder gravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder onMenuActionListner(OnMenuActionListener listener) {
            actionListeners.add(listener);
            return this;
        }

        public Builder revealColor(@ColorRes int color) {
            this.revealColor = color;
            return this;
        }

        public Builder onFabClickListener(OnFabClickListener listener) {
            this.onFabClickListener = listener;
            return this;
        }
    }

}
