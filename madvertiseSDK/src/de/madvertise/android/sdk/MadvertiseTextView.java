/*
 * Copyright 2012 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.madvertise.android.sdk;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;

/**
 * RelativeLayout to show a text ad.
 */
class MadvertiseTextView extends RelativeLayout {

    private AnimationEndListener mAnimationListener;

    private int mTextWidth;

    private int mLineCount;

    private float mDp;

    public MadvertiseTextView(final Context context, final String bannerText, final int textSize,
            final int textColor, final AnimationEndListener listener) {
        super(context);

        mAnimationListener = listener;
        mDp = getContext().getApplicationContext().getApplicationContext().getResources()
                .getDisplayMetrics().density;

        RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final TextView adTextView = new TextView(getContext().getApplicationContext());
        adTextView.setGravity(Gravity.CENTER);
        adTextView.setText(bannerText);
        adTextView.setTextSize(textSize);
        adTextView.setTextColor(textColor);
        adTextView.setTypeface(Typeface.DEFAULT_BOLD);
        adTextView.setLayoutParams(adParams);
        adTextView.setId(12345);
        adTextView.setMaxLines(2);

        addView(adTextView);

        final int bannerWidth = (int) (MadvertiseUtil.MMA_BANNER_WIDTH * mDp);
        final int bannerHeight = (int) (MadvertiseUtil.MMA_BANNER_HEIGHT * mDp);
        final Paint paint = new Paint();
        paint.setTextSize(textSize);
        mLineCount = 1;
        if (paint.measureText(bannerText) * mDp > bannerWidth) {
            mLineCount++;
        }

        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                mTextWidth = adTextView.getWidth();
                RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                adParams.topMargin = bannerHeight / 2 - mLineCount * textSize;
                adParams.leftMargin = bannerWidth / 2 - mTextWidth / 2;
                adTextView.setLayoutParams(adParams);
            }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) (235 * mDp);
        params.topMargin = (int) (39 * mDp);

        final TextView providerTextView = new TextView(getContext().getApplicationContext());
        providerTextView.setText(MadvertiseUtil.AD_PROVIDER_TEXT);
        providerTextView.setTextSize(MadvertiseUtil.TEXT_SIZE_PROVIDER);
        providerTextView.setLayoutParams(params);
        addView(providerTextView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // This view type will only be shown in MMA-banners.
        int width = (int) (MadvertiseUtil.MMA_BANNER_WIDTH * mDp);
        int height = (int) (MadvertiseUtil.MMA_BANNER_WIDTH * mDp);
        setMeasuredDimension(width, height);
    }

    /**
     * This is needed because of a sad Android-bug: onAnimationEnd() will not be
     * called in the {@link Animation.AnimationListener}, so we have to listen
     * to this event in the views
     */
    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        if (mAnimationListener != null) {
            mAnimationListener.onAnimationEnd();
        }
    }
}
