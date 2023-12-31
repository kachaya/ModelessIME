/*
 * Copyright 2023-2024 kachaya
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

package io.github.kachaya.ime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StrokeView extends KeyboardLayout {
    private final Path mStrokePath;
    private final Paint mStrokePaint;
    private final Paint mFramePaint;

    private boolean mPunctuationFlag;

    public StrokeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        mImageView = new ImageView(context);
        mImageView.setImageBitmap(mBitmap);
        addView(mImageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        mPunctuationFlag = false;

        mStrokePath = new Path();
        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setColor(mKeyForegroundColor);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        mStrokePaint.setStrokeWidth(mDensity * 3);
        mStrokePaint.setStyle(Paint.Style.STROKE);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(mKeyForegroundColor);
        mFramePaint.setStrokeJoin(Paint.Join.ROUND);
        mFramePaint.setStrokeWidth(mDensity * 2);
        mFramePaint.setStyle(Paint.Style.STROKE);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStrokePath.reset();
                mStrokePath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mStrokePath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                mStrokePath.lineTo(x, y);
                processStroke();
                mStrokePath.reset();
                break;
            default:
                break;
        }
        drawKeyboard();
        invalidate();
        return true;
    }

    private void processStroke() {
        char ch;
        RectF bounds = new RectF();
        mStrokePath.computeBounds(bounds, true);

        boolean isTap = !(bounds.width() > 1) || !(bounds.height() > 1);

        if (isTap) {
            if (mPunctuationFlag) {
                mPunctuationFlag = false;
                mSoftKeyboard.handleCharacter('.');
            } else {
                mPunctuationFlag = true;
            }
            return;
        }

        Stroke.Result result = Stroke.recognize(mStrokePath);

        if (mPunctuationFlag) {
            mPunctuationFlag = false;
            ch = result.p;
            if (Character.isUpperCase(ch)) {
                processFunctionChar(ch);
            } else {
                mSoftKeyboard.handleCharacter(ch);
            }
            return;
        }

        if (bounds.centerX() > (mWidth / 2.0f)) {
            // 中心より右側
            ch = result.n;
            if (Character.isUpperCase(ch)) {
                processFunctionChar(ch);
            } else {
                mSoftKeyboard.handleCharacter(ch);
            }
            return;
        }
        // 中心より左側
        ch = result.a;
        if (Character.isUpperCase(ch)) {
            if (ch == 'C') {
                if (mShiftLockFlag) {
                    mShiftLockFlag = false;
                    mShiftSingleFlag = false;
                } else {
                    if (mShiftSingleFlag) {
                        mShiftLockFlag = true;
                        mShiftSingleFlag = false;
                    } else {
                        mShiftSingleFlag = true;
                    }
                }
            } else {
                processFunctionChar(ch);
            }
        } else {
            if ((mShiftSingleFlag || mShiftLockFlag) && Character.isLowerCase(ch)) {
                ch = Character.toUpperCase(ch);
            }
            mShiftSingleFlag = false;
            if (ch == ' ') {
                mSoftKeyboard.handleSpace();
            } else {
                mSoftKeyboard.handleCharacter(ch);
            }
        }
    }

    private void drawKeyboard() {

        mCanvas.drawColor(mBackgroundColor);

        float r = mDensity * 10.0f;
        mFramePaint.setStyle(Paint.Style.STROKE);
        mCanvas.drawRoundRect(mDensity, mDensity, mWidth - mDensity, mHeight - mDensity, r, r, mFramePaint);
        mCanvas.drawLine(mWidth / 2.0f, mDensity, mWidth / 2.0f, mHeight - mDensity, mFramePaint);

        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFramePaint.setTextSize(mHeight * 0.1f);
        mFramePaint.setTextAlign(Paint.Align.CENTER);
        float ofsX = mHeight * 0.07f;
        float ofsY = mHeight * 0.08f + ((mFramePaint.descent() + mFramePaint.ascent()) / 2);
        mCanvas.drawText("a", ofsX, mHeight - ofsY, mFramePaint);
        mCanvas.drawText("1", mWidth - ofsX, mHeight - ofsY, mFramePaint);

        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        float cx = mHeight * 0.1f;
        float cy = mHeight * 0.1f;
        r = mHeight * 0.05f;

        if (mPunctuationFlag) {
            mCanvas.drawCircle(cx, cy, r, mFramePaint);
        } else {
            int left = (int) (cx - r);
            int top = (int) (cy - r);
            int right = (int) (cx + r);
            int bottom = (int) (cy + r);
            if (mShiftSingleFlag) {
                mShiftSingleDrawable.setBounds(left, top, right, bottom);
                mShiftSingleDrawable.setTint(mKeyForegroundColor);
                mShiftSingleDrawable.draw(mCanvas);
            }
            if (mShiftLockFlag) {
                mShiftLockDrawable.setBounds(left, top, right, bottom);
                mShiftLockDrawable.setTint(mKeyForegroundColor);
                mShiftLockDrawable.draw(mCanvas);
            }
        }
        mCanvas.drawPath(mStrokePath, mStrokePaint);

        mImageView.setImageBitmap(mBitmap);
        invalidate();
    }

    // CAPS以外、アルファベット数字共通
    private void processFunctionChar(char ch) {
        switch (ch) {
            case 'B':   // backspace
                mSoftKeyboard.handleBackspace();
                break;
            case 'D':   // down
                mSoftKeyboard.handleCursorDown();
                break;
            case 'E':   // enter
                mSoftKeyboard.handleEnter();
                break;
            case 'L':   // left
                mSoftKeyboard.handleCursorLeft();
                break;
            case 'R':   // right
                mSoftKeyboard.handleCursorRight();
                break;
            case 'S':   // symbol
                mSoftKeyboard.handleSymbol();
                break;
            case 'U':   // up
                mSoftKeyboard.handleCursorUp();
                break;
            case 'M':   // menu
            case 'N':   // nop
            case 'P':   // preference
            default:
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        mDensity = dm.density;
        mWidth = w;
        mHeight = h;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        drawKeyboard();
    }
}
