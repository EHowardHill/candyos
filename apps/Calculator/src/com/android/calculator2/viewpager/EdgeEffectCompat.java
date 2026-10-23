/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.calculator2.viewpager;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.EdgeEffect;

/**
 * KitKat UI P16 port helper: the 4.4 support-v4 EdgeEffectCompat API surface
 * ({@link #onPull} and {@link #onRelease} return whether the effect is still
 * running), implemented directly over the framework {@link EdgeEffect} which
 * the KitKat compat class delegated to. API 34 framework onPull/onRelease
 * return void, so the boolean contract lives here.
 */
public class EdgeEffectCompat {

    private final EdgeEffect mEdge;

    /**
     * Constructs an EdgeEffectCompat with the given context.
     */
    public EdgeEffectCompat(Context context) {
        mEdge = new EdgeEffect(context);
    }

    public void setSize(int width, int height) {
        mEdge.setSize(width, height);
    }

    public boolean onPull(float deltaDistance) {
        mEdge.onPull(deltaDistance);
        return !mEdge.isFinished();
    }

    public boolean onRelease() {
        mEdge.onRelease();
        return !mEdge.isFinished();
    }

    public boolean isFinished() {
        return mEdge.isFinished();
    }

    public boolean draw(Canvas canvas) {
        return mEdge.draw(canvas);
    }

    public void finish() {
        mEdge.finish();
    }
}