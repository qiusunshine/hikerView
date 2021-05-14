/*
 * Copyright 2018. who<980008027@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.hikerview.ui.js.editor;

import com.example.hikerview.ui.js.javascript.Token;
import com.example.hikerview.ui.js.javascript.TokenStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2018/2/12.
 */

public class JsTextViewCodeParser implements Runnable {
    private static ExecutorService parserThreadPool = Executors.newSingleThreadExecutor();
    boolean running;
    CodeTextView codeText;
    //start:改变的位置
    int start;
    //before:删除的数量
    int before;
    //count:添加的数量
    int count;

    public JsTextViewCodeParser(CodeTextView codeText) {
        this.codeText = codeText;
    }

    public void run() {
        //颜色移动位置
        //start:改变的位置
        //before:删除的数量
        //count:添加的数量
        //如果添加的数量大于删除的数量，右移动;否则，左移动
        int[] codeColors = codeText.getCodeColors();
        if (count > before) {
            //添加
            //右移动[1,2,3,0,0] >> [0,0,1,2,3]
            int off = count - before;
            for (int i = codeColors.length - 1; i > start + off && i > 1; i--) {
                if (reparse) {
                    break;
                }
                codeColors[i] = codeColors[i - off];
            }
        } else {
            //删除
            //左移动 [0,0,1,2,3] >> [1,2,3,0,0]
            int off = before - count;
            for (int i = start; i + off < codeColors.length; i++) {
                if (reparse) {
                    break;
                }
                codeColors[i] = codeColors[i + off];
            }
        }
        if (running) {
            codeText.postInvalidate();
        }
        try {
            TokenStream ts = new TokenStream(null, codeText.getText()
                    .toString(), 0);
            while (running) {
                if (reparse) {
                    break;
                }
                try {
                    int token = ts.getToken();
                    if (token == Token.EOF) {
                        codeText.postInvalidate();
                        break;
                    }
                    int color = Token.getColor(token);
                    for (int i = ts.getTokenBeg(); i <= ts.getTokenEnd(); i++) {
                        codeColors[i] = color;
                    }
                } catch (Exception e) {
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        if (reparse) {
            reparse = false;
            reparse();
        }
        running = false;
    }

    private boolean reparse;

    private void reparse() {
        parserThreadPool.execute(this);
    }

    public synchronized void parse(int start, int before,
                                   int count) {
        if (running) {
            reparse = true;
            return;
        }
        running = true;
        this.start = start;
        this.before = before;
        this.count = count;
        parserThreadPool.execute(this);
    }

}
