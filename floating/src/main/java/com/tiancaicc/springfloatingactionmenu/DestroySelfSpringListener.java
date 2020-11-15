package com.tiancaicc.springfloatingactionmenu;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringListener;

/**
 * Copyright (C) 2016 tiancaiCC
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
public class DestroySelfSpringListener implements SpringListener {
    private static final String TAG = "DestroySelfSpringListener";

    private ViewGroup mSpringMenu;
    private View view;
    private boolean mInOpen;

    public DestroySelfSpringListener(ViewGroup root,View fab,boolean isOpn){
        this.mSpringMenu = root;
        this.view = fab;
        this.mInOpen = isOpn;
    }
    @Override
    public void onSpringUpdate(Spring spring) {

    }

    @Override
    public void onSpringAtRest(Spring spring) {
        spring.removeAllListeners();
        spring.destroy();
        spring = null;
        if(mSpringMenu != null && !mInOpen) {
            mSpringMenu.removeView(view);
        }
    }

    @Override
    public void onSpringActivate(Spring spring) {

        if(mSpringMenu != null && mSpringMenu.indexOfChild(view) == -1) {
           mSpringMenu.addView(view);
        }
    }

    @Override
    public void onSpringEndStateChange(Spring spring) {

    }
}
